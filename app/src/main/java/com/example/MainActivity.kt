package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.components.AppNavDestination
import com.example.ui.components.BottomNavigationBar
import com.example.ui.components.FullScreenPlayer
import com.example.ui.components.MiniPlayer
import com.example.ui.screens.AdminSettingsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.theme.BackgroundVoid
import com.example.ui.theme.TuneFlowTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TuneFlowTheme {
                TuneFlowApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TuneFlowApp(viewModel: MainViewModel) {
    var currentDestination by remember { mutableStateOf(AppNavDestination.HOME) }
    var isPlayerExpanded by remember { mutableStateOf(false) }

    val filteredSongs by viewModel.filteredSongs.collectAsStateWithLifecycle()
    val rawSongs by viewModel.rawSongs.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val telegramConfig by viewModel.telegramConfig.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedGenre by viewModel.selectedGenre.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundVoid,
        bottomBar = {
            BottomNavigationBar(
                currentDestination = currentDestination,
                onNavigate = { destination -> currentDestination = destination }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundVoid)
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            // Main Screen Content
            when (currentDestination) {
                AppNavDestination.HOME -> HomeScreen(
                    songs = filteredSongs,
                    recentlyPlayed = recentlyPlayed,
                    playlists = playlists,
                    playbackState = playbackState,
                    onSongClick = { song -> viewModel.playSong(song) },
                    onFavoriteToggle = { song -> viewModel.toggleFavorite(song) },
                    onAddToPlaylist = { playlistId, songId -> viewModel.addSongToPlaylist(playlistId, songId) },
                    onAddToQueue = { song -> viewModel.playerManager.addToQueue(song) },
                    onNavigateSettings = { currentDestination = AppNavDestination.SETTINGS }
                )

                AppNavDestination.SEARCH -> SearchScreen(
                    searchQuery = searchQuery,
                    selectedGenre = selectedGenre,
                    songs = filteredSongs,
                    playlists = playlists,
                    playbackState = playbackState,
                    onSearchQueryChange = { query -> viewModel.setSearchQuery(query) },
                    onGenreSelect = { genre -> viewModel.setSelectedGenre(genre) },
                    onSongClick = { song -> viewModel.playSong(song) },
                    onFavoriteToggle = { song -> viewModel.toggleFavorite(song) },
                    onAddToPlaylist = { playlistId, songId -> viewModel.addSongToPlaylist(playlistId, songId) },
                    onAddToQueue = { song -> viewModel.playerManager.addToQueue(song) }
                )

                AppNavDestination.LIBRARY -> LibraryScreen(
                    favorites = favorites,
                    playlists = playlists,
                    allSongs = rawSongs,
                    playbackState = playbackState,
                    onSongClick = { song -> viewModel.playSong(song) },
                    onFavoriteToggle = { song -> viewModel.toggleFavorite(song) },
                    onCreatePlaylist = { name, desc -> viewModel.createPlaylist(name, desc) },
                    onDeletePlaylist = { playlistId -> viewModel.deletePlaylist(playlistId) },
                    onAddToPlaylist = { playlistId, songId -> viewModel.addSongToPlaylist(playlistId, songId) },
                    onAddToQueue = { song -> viewModel.playerManager.addToQueue(song) }
                )

                AppNavDestination.SETTINGS -> AdminSettingsScreen(
                    config = telegramConfig,
                    syncStatus = syncStatus,
                    onSyncTelegram = { token, channel -> viewModel.syncTelegram(token, channel) },
                    onClearSyncStatus = { viewModel.clearSyncStatus() }
                )
            }

            // Docked Mini Player floating above bottom bar
            if (playbackState.currentSong != null) {
                Box(
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    MiniPlayer(
                        state = playbackState,
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onPlayNext = { viewModel.playNext() },
                        onFavoriteToggle = {
                            playbackState.currentSong?.let { viewModel.toggleFavorite(it) }
                        },
                        onExpandPlayer = { isPlayerExpanded = true }
                    )
                }
            }

            // Full Screen Player Modal Sheet
            if (isPlayerExpanded && playbackState.currentSong != null) {
                FullScreenPlayer(
                    state = playbackState,
                    onDismiss = { isPlayerExpanded = false },
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onPlayNext = { viewModel.playNext() },
                    onPlayPrevious = { viewModel.playPrevious() },
                    onSeekTo = { pos -> viewModel.seekTo(pos) },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onToggleRepeat = { viewModel.toggleRepeat() },
                    onFavoriteToggle = {
                        playbackState.currentSong?.let { viewModel.toggleFavorite(it) }
                    },
                    onRemoveFromQueue = { index -> viewModel.playerManager.removeFromQueue(index) },
                    onClearQueue = { viewModel.playerManager.clearQueue() }
                )
            }
        }
    }
}
