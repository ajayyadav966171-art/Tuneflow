package com.example.player

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.Song
import com.example.data.telegram.TelegramApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RepeatMode {
    NONE, ALL, ONE
}

data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val currentPositionSeconds: Int = 0,
    val totalDurationSeconds: Int = 0,
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = -1,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.NONE,
    val errorMessage: String? = null
)

class AudioPlayerManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var positionTrackerJob: Job? = null
    private var loadJob: Job? = null

    private var retryCount = 0
    private val maxRetries = 3

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var onSongEndedCallback: ((Song) -> Unit)? = null

    // Single ExoPlayer instance kept alive across songs
    private val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(context.applicationContext).build().apply {
            addListener(playerListener)
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    Log.d("AudioPlayerManager", "ExoPlayer buffering audio stream...")
                    _playbackState.update { it.copy(isLoading = true) }
                }
                Player.STATE_READY -> {
                    Log.d("AudioPlayerManager", "ExoPlayer STATE_READY - Starting playback")
                    retryCount = 0
                    val durationMs = exoPlayer.duration
                    val durationSec = if (durationMs > 0) (durationMs / 1000).toInt() else (_playbackState.value.currentSong?.durationSeconds ?: 0)
                    _playbackState.update {
                        it.copy(
                            isLoading = false,
                            isPlaying = exoPlayer.isPlaying,
                            totalDurationSeconds = durationSec,
                            errorMessage = null
                        )
                    }
                    startPositionTracker()
                }
                Player.STATE_ENDED -> {
                    Log.d("AudioPlayerManager", "ExoPlayer playback completed")
                    handleSongCompletion()
                }
                Player.STATE_IDLE -> {
                    Log.d("AudioPlayerManager", "ExoPlayer STATE_IDLE")
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Log.d("AudioPlayerManager", "ExoPlayer onIsPlayingChanged: $isPlaying")
            _playbackState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(
                "AudioPlayerManager",
                "ExoPlayer playback error! Code: ${error.errorCode}, Name: ${error.errorCodeName}, Message: ${error.message}",
                error
            )
            handlePlaybackError(error)
        }
    }

    fun setOnSongEndedListener(listener: (Song) -> Unit) {
        onSongEndedCallback = listener
    }

    fun playSong(song: Song, newQueue: List<Song> = emptyList(), indexInQueue: Int = -1) {
        val currentQueue = if (newQueue.isNotEmpty()) newQueue else {
            if (_playbackState.value.queue.none { it.id == song.id }) {
                _playbackState.value.queue + song
            } else _playbackState.value.queue
        }

        val actualIndex = if (indexInQueue >= 0) indexInQueue else currentQueue.indexOfFirst { it.id == song.id }

        // If same song is already active in player, just toggle
        if (_playbackState.value.currentSong?.id == song.id && exoPlayer.mediaItemCount > 0) {
            togglePlayPause()
            return
        }

        loadJob?.cancel()

        _playbackState.update {
            it.copy(
                currentSong = song,
                isLoading = true,
                isPlaying = true,
                errorMessage = null,
                queue = currentQueue,
                queueIndex = if (actualIndex >= 0) actualIndex else 0,
                currentPositionSeconds = 0
            )
        }

        retryCount = 0

        loadJob = scope.launch(Dispatchers.Main) {
            val resolvedUrl = resolveStreamUrl(song)
            Log.d("AudioPlayerManager", "Preparing ExoPlayer for '${song.title}' with stream URL: $resolvedUrl")

            try {
                val mediaItem = MediaItem.fromUri(resolvedUrl)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            } catch (e: Exception) {
                Log.e("AudioPlayerManager", "Failed to set MediaItem or prepare ExoPlayer", e)
                _playbackState.update {
                    it.copy(
                        isLoading = false,
                        isPlaying = false,
                        errorMessage = e.localizedMessage ?: "Failed to stream audio"
                    )
                }
            }
        }
    }

    fun togglePlayPause() {
        val state = _playbackState.value
        val song = state.currentSong ?: return

        if (exoPlayer.mediaItemCount == 0) {
            playSong(song, state.queue, state.queueIndex)
            return
        }

        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            _playbackState.update { it.copy(isPlaying = false) }
        } else {
            exoPlayer.play()
            _playbackState.update { it.copy(isPlaying = true) }
            startPositionTracker()
        }
    }

    fun seekTo(seconds: Int) {
        try {
            val targetMs = (seconds * 1000L).coerceAtLeast(0L)
            exoPlayer.seekTo(targetMs)
            _playbackState.update { it.copy(currentPositionSeconds = seconds) }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error seeking to $seconds seconds", e)
        }
    }

    fun playNext() {
        val state = _playbackState.value
        if (state.queue.isEmpty()) return

        val nextIndex = when {
            state.shuffleEnabled -> (0 until state.queue.size).random()
            state.queueIndex < state.queue.size - 1 -> state.queueIndex + 1
            state.repeatMode == RepeatMode.ALL -> 0
            else -> -1
        }

        if (nextIndex in state.queue.indices) {
            playSong(state.queue[nextIndex], state.queue, nextIndex)
        } else {
            _playbackState.update { it.copy(isPlaying = false, currentPositionSeconds = 0) }
        }
    }

    fun playPrevious() {
        val state = _playbackState.value
        if (state.queue.isEmpty()) return

        if (state.currentPositionSeconds > 3) {
            seekTo(0)
            return
        }

        val prevIndex = when {
            state.queueIndex > 0 -> state.queueIndex - 1
            state.repeatMode == RepeatMode.ALL -> state.queue.size - 1
            else -> 0
        }

        if (prevIndex in state.queue.indices) {
            playSong(state.queue[prevIndex], state.queue, prevIndex)
        }
    }

    fun toggleShuffle() {
        _playbackState.update { it.copy(shuffleEnabled = !it.shuffleEnabled) }
    }

    fun toggleRepeat() {
        _playbackState.update {
            val nextMode = when (it.repeatMode) {
                RepeatMode.NONE -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.NONE
            }
            it.copy(repeatMode = nextMode)
        }
    }

    fun addToQueue(song: Song) {
        _playbackState.update {
            val updatedQueue = it.queue + song
            it.copy(queue = updatedQueue)
        }
    }

    fun removeFromQueue(index: Int) {
        _playbackState.update {
            if (index in it.queue.indices) {
                val updatedQueue = it.queue.toMutableList().apply { removeAt(index) }
                val newIndex = if (index < it.queueIndex) it.queueIndex - 1 else it.queueIndex
                it.copy(queue = updatedQueue, queueIndex = newIndex.coerceIn(-1, updatedQueue.size - 1))
            } else it
        }
    }

    fun clearQueue() {
        _playbackState.update {
            it.copy(queue = emptyList(), queueIndex = -1)
        }
    }

    private fun handleSongCompletion() {
        val state = _playbackState.value
        state.currentSong?.let { onSongEndedCallback?.invoke(it) }

        when (state.repeatMode) {
            RepeatMode.ONE -> {
                state.currentSong?.let { song ->
                    exoPlayer.seekTo(0)
                    exoPlayer.play()
                }
            }
            else -> {
                playNext()
            }
        }
    }

    private fun handlePlaybackError(error: PlaybackException) {
        if (retryCount < maxRetries) {
            retryCount++
            Log.w("AudioPlayerManager", "Retrying playback ($retryCount/$maxRetries) due to error: ${error.message}")
            scope.launch(Dispatchers.Main) {
                delay(1000L * retryCount)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }
        } else {
            _playbackState.update {
                it.copy(
                    isLoading = false,
                    isPlaying = false,
                    errorMessage = "Playback failed: ${error.message ?: "Unable to stream audio track"}"
                )
            }
        }
    }

    private fun startPositionTracker() {
        positionTrackerJob?.cancel()
        positionTrackerJob = scope.launch(Dispatchers.Main) {
            while (true) {
                try {
                    if (exoPlayer.isPlaying) {
                        val posSec = (exoPlayer.currentPosition / 1000).toInt()
                        _playbackState.update { it.copy(currentPositionSeconds = posSec) }
                    }
                } catch (e: Exception) {
                    // Ignored transitory player state exception
                }
                delay(500)
            }
        }
    }

    private suspend fun resolveStreamUrl(song: Song): String {
        val url = song.audioUrl

        // If URL is direct mp3/audio stream and not a Telegram getFile query endpoint, use directly
        if (!url.contains("/getFile") && !url.contains("file_id=")) {
            return url
        }

        // Extract bot token and file_id if present
        var botToken: String? = null
        var fileId: String? = song.telegramFileId

        val tokenMatch = Regex("bot([0-9]+:[A-Za-z0-9_-]+)").find(url)
        if (tokenMatch != null) {
            botToken = tokenMatch.groupValues[1]
        }

        val fileIdMatch = Regex("file_id=([^&]+)").find(url)
        if (fileIdMatch != null) {
            fileId = fileIdMatch.groupValues[1]
        }

        if (!botToken.isNullOrEmpty() && !fileId.isNullOrEmpty()) {
            try {
                Log.d("AudioPlayerManager", "Resolving Telegram file_path via getFile API...")
                val response = TelegramApiClient.service.getFile(botToken, fileId)
                if (response.isSuccessful && response.body()?.ok == true) {
                    val filePath = response.body()?.result?.file_path
                    if (!filePath.isNullOrEmpty()) {
                        val directDownloadUrl = TelegramApiClient.getFileDirectUrl(botToken, filePath)
                        Log.d("AudioPlayerManager", "Resolved direct stream URL: $directDownloadUrl")
                        return directDownloadUrl
                    }
                }
            } catch (e: Exception) {
                Log.e("AudioPlayerManager", "Network error resolving Telegram getFile", e)
            }
        }

        return url
    }

    fun release() {
        positionTrackerJob?.cancel()
        loadJob?.cancel()
        try {
            exoPlayer.removeListener(playerListener)
            exoPlayer.release()
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error releasing ExoPlayer", e)
        }
    }
}
