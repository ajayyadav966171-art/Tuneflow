package com.example.data.repository

import com.example.data.db.PlaylistDao
import com.example.data.db.PlaylistEntity
import com.example.data.db.SongDao
import com.example.data.db.SongEntity
import com.example.data.db.TelegramConfigEntity
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.data.model.TelegramConfig
import com.example.data.telegram.TelegramApiClient
import com.example.data.telegram.TelegramMusicParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MusicRepository(
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao
) {
    val allSongs: Flow<List<Song>> = songDao.getAllSongs().map { entities ->
        entities.map { it.toSong() }
    }

    val favoriteSongs: Flow<List<Song>> = songDao.getFavoriteSongs().map { entities ->
        entities.map { it.toSong() }
    }

    val recentlyPlayed: Flow<List<Song>> = songDao.getRecentlyPlayedSongs().map { entities ->
        entities.map { it.toSong() }
    }

    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAllPlaylists().map { entities ->
        entities.map { it.toPlaylist() }
    }

    val telegramConfig: Flow<TelegramConfig> = playlistDao.getTelegramConfig().map { entity ->
        entity?.toConfig() ?: TelegramConfig()
    }

    suspend fun toggleFavorite(songId: String, currentFavorite: Boolean) {
        songDao.updateFavoriteStatus(songId, !currentFavorite)
    }

    suspend fun recordSongPlayed(songId: String) {
        songDao.recordSongPlayed(songId, System.currentTimeMillis())
    }

    suspend fun createPlaylist(name: String, description: String = "") {
        val newPlaylist = Playlist(
            id = "pl_${System.currentTimeMillis()}",
            name = name,
            description = description,
            createdAt = System.currentTimeMillis()
        )
        playlistDao.insertPlaylist(PlaylistEntity.fromPlaylist(newPlaylist))
    }

    suspend fun addSongToPlaylist(playlistId: String, songId: String) {
        val entity = playlistDao.getPlaylistById(playlistId) ?: return
        val playlist = entity.toPlaylist()
        if (!playlist.songIds.contains(songId)) {
            val updated = playlist.copy(songIds = playlist.songIds + songId)
            playlistDao.insertPlaylist(PlaylistEntity.fromPlaylist(updated))
        }
    }

    suspend fun deletePlaylist(playlistId: String) {
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun saveTelegramConfig(config: TelegramConfig) {
        playlistDao.saveTelegramConfig(TelegramConfigEntity.fromConfig(config))
    }

    suspend fun syncTelegramChannel(botToken: String, channelId: String): Result<Int> = withContext(Dispatchers.IO) {
        if (botToken.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Bot Token cannot be empty"))
        }

        try {
            val response = TelegramApiClient.service.getUpdates(token = botToken, limit = 100)
            if (!response.isSuccessful || response.body()?.ok != true) {
                val errorMsg = response.body()?.description ?: "HTTP ${response.code()}: Unable to fetch updates"
                return@withContext Result.failure(Exception(errorMsg))
            }

            val updates = response.body()?.result ?: emptyList()
            val parsedSongs = mutableListOf<Song>()

            for (update in updates) {
                val message = update.channel_post ?: update.message ?: continue
                val song = TelegramMusicParser.parseMessageToSong(message, botToken, channelId)
                if (song != null) {
                    // Try resolving direct download path via getFile
                    val fileId = song.telegramFileId
                    var finalAudioUrl = song.audioUrl
                    if (!fileId.isNullOrEmpty()) {
                        try {
                            val fileResp = TelegramApiClient.service.getFile(botToken, fileId)
                            if (fileResp.isSuccessful && fileResp.body()?.ok == true) {
                                val filePath = fileResp.body()?.result?.file_path
                                if (!filePath.isNullOrEmpty()) {
                                    finalAudioUrl = TelegramApiClient.getFileDirectUrl(botToken, filePath)
                                }
                            }
                        } catch (e: Exception) {
                            // Fallback to initial fileUrl
                        }
                    }
                    parsedSongs.add(song.copy(audioUrl = finalAudioUrl))
                }
            }

            if (parsedSongs.isNotEmpty()) {
                songDao.insertSongs(parsedSongs.map { SongEntity.fromSong(it) })
            }

            // Save updated config timestamp
            saveTelegramConfig(
                TelegramConfig(
                    botToken = botToken,
                    channelId = channelId,
                    lastSyncedTimestamp = System.currentTimeMillis()
                )
            )

            Result.success(parsedSongs.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun initializePreloadedMusic() = withContext(Dispatchers.IO) {
        // Preload sample royalty-free songs with audio streams & rich lyrics if database is empty
        val sampleSongs = listOf(
            Song(
                id = "sample_1",
                title = "Midnight Horizon",
                artist = "Neon Dreamer",
                album = "Cyber Pulse",
                durationSeconds = 214,
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                coverUrl = "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=600&q=80",
                genre = "Synthwave",
                lyrics = """
                    [00:00.00] Midnight horizon calling out my name
                    [00:15.20] Emerald lights fading through the neon rain
                    [00:30.00] Driving down the highway with the bass alive
                    [00:45.50] Feel the rhythm flowing as we synchronize
                    [01:02.10] Midnight horizon, take me far away
                    [01:20.00] In this synthwave city where we stay
                """.trimIndent(),
                isFavorite = true
            ),
            Song(
                id = "sample_2",
                title = "Emerald Aurora",
                artist = "Lofi Vibes",
                album = "Cosmic Chillout",
                durationSeconds = 185,
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                coverUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&q=80",
                genre = "Lofi / Chill",
                lyrics = """
                    [00:00.00] Soft beats dripping like coffee drops
                    [00:20.00] Quiet rain on the rooftop tops
                    [00:40.00] Smooth guitar in the midnight light
                    [01:00.00] Chill with me through the starry night
                """.trimIndent(),
                isFavorite = false
            ),
            Song(
                id = "sample_3",
                title = "Electric Symphony",
                artist = "Aura Groove",
                album = "Vibrant Waves",
                durationSeconds = 240,
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&q=80",
                genre = "Electronic",
                lyrics = """
                    [00:00.00] Pulse in the dark, sparks in the sound
                    [00:30.00] Elevate your soul above the ground
                    [01:10.00] Electric symphony in my mind
                    [01:40.00] Pure energy we left behind
                """.trimIndent(),
                isFavorite = true
            ),
            Song(
                id = "sample_4",
                title = "Acoustic Sunset",
                artist = "Solaris Trio",
                album = "Golden Hour Sessions",
                durationSeconds = 195,
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&q=80",
                genre = "Indie Rock",
                lyrics = """
                    [00:00.00] Strumming warmth under amber skies
                    [00:25.00] Ocean breeze and quiet goodbyes
                    [00:55.00] Golden light in every chord we play
                    [01:30.00] Acoustic sunset guide our way
                """.trimIndent(),
                isFavorite = false
            ),
            Song(
                id = "sample_5",
                title = "Starlight Echoes",
                artist = "Celestia",
                album = "Deep Space",
                durationSeconds = 228,
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                coverUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&q=80",
                genre = "Ambient",
                lyrics = """
                    [00:00.00] Echoes traveling across the galaxy
                    [00:40.00] Floating free in zero gravity
                    [01:20.00] Infinite stars shining so bright
                    [02:00.00] Lost in cosmic peace tonight
                """.trimIndent(),
                isFavorite = true
            )
        )

        val existing = songDao.getSongById("sample_1")
        if (existing == null) {
            songDao.insertSongs(sampleSongs.map { SongEntity.fromSong(it) })
            // Create default starter playlist
            createPlaylist("Chill Vibez", "Smooth tracks for relaxation and deep work")
            createPlaylist("Workout Pulse", "High energy electronic tracks")
        }
    }
}
