package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.TuneFlowDatabase
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.data.model.TelegramConfig
import com.example.data.repository.MusicRepository
import com.example.player.AudioPlayerManager
import com.example.player.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class SyncStatusState {
    object Idle : SyncStatusState()
    object Syncing : SyncStatusState()
    data class Success(val count: Int) : SyncStatusState()
    data class Error(val message: String) : SyncStatusState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = TuneFlowDatabase.getDatabase(application)
    val repository = MusicRepository(db.songDao(), db.playlistDao())
    val playerManager = AudioPlayerManager(application)

    val playbackState: StateFlow<PlaybackState> = playerManager.playbackState

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGenre = MutableStateFlow<String?>(null)
    val selectedGenre: StateFlow<String?> = _selectedGenre.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatusState>(SyncStatusState.Idle)
    val syncStatus: StateFlow<SyncStatusState> = _syncStatus.asStateFlow()

    val rawSongs: StateFlow<List<Song>> = repository.allSongs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val filteredSongs: StateFlow<List<Song>> = combine(
        rawSongs,
        _searchQuery,
        _selectedGenre
    ) { songs, query, genre ->
        songs.filter { song ->
            val matchesQuery = query.isBlank() ||
                    song.title.contains(query, ignoreCase = true) ||
                    song.artist.contains(query, ignoreCase = true) ||
                    song.album.contains(query, ignoreCase = true)

            val matchesGenre = genre == null || song.genre.equals(genre, ignoreCase = true)

            matchesQuery && matchesGenre
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favorites: StateFlow<List<Song>> = repository.favoriteSongs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentlyPlayed: StateFlow<List<Song>> = repository.recentlyPlayed.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val playlists: StateFlow<List<Playlist>> = repository.allPlaylists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val telegramConfig: StateFlow<TelegramConfig> = repository.telegramConfig.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TelegramConfig()
    )

    init {
        viewModelScope.launch {
            repository.initializePreloadedMusic()
        }

        playerManager.setOnSongEndedListener { song ->
            viewModelScope.launch {
                repository.recordSongPlayed(song.id)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedGenre(genre: String?) {
        _selectedGenre.value = if (_selectedGenre.value == genre) null else genre
    }

    fun playSong(song: Song, queueList: List<Song> = emptyList()) {
        val currentSong = playbackState.value.currentSong
        if (currentSong?.id == song.id) {
            togglePlayPause()
            return
        }
        val queue = if (queueList.isNotEmpty()) queueList else filteredSongs.value
        playerManager.playSong(song, queue)
        viewModelScope.launch {
            repository.recordSongPlayed(song.id)
        }
    }

    fun togglePlayPause() = playerManager.togglePlayPause()

    fun seekTo(seconds: Int) = playerManager.seekTo(seconds)

    fun playNext() = playerManager.playNext()

    fun playPrevious() = playerManager.playPrevious()

    fun toggleShuffle() = playerManager.toggleShuffle()

    fun toggleRepeat() = playerManager.toggleRepeat()

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            repository.toggleFavorite(song.id, song.isFavorite)
        }
    }

    fun createPlaylist(name: String, description: String = "") {
        viewModelScope.launch {
            repository.createPlaylist(name, description)
        }
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun syncTelegram(botToken: String, channelId: String) {
        viewModelScope.launch {
            _syncStatus.value = SyncStatusState.Syncing
            val result = repository.syncTelegramChannel(botToken, channelId)
            result.onSuccess { count ->
                _syncStatus.value = SyncStatusState.Success(count)
            }.onFailure { error ->
                _syncStatus.value = SyncStatusState.Error(error.message ?: "Sync failed")
            }
        }
    }

    fun clearSyncStatus() {
        _syncStatus.value = SyncStatusState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
