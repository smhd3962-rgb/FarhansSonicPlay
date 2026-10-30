package com.farhan.sonicplay.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.farhan.sonicplay.playback.SonicPlaybackService
import com.farhan.sonicplay.ui.components.MiniPlayer
import com.farhan.sonicplay.ui.components.NowPlayingScreen
import com.farhan.sonicplay.ui.components.UpNextQueueSheet
import com.farhan.sonicplay.ui.screens.equalizer.EqualizerScreen
import com.farhan.sonicplay.ui.screens.library.LibraryHomeScreen
import com.farhan.sonicplay.ui.screens.search.SearchScreen
import com.farhan.sonicplay.ui.screens.settings.SettingsScreen
import com.farhan.sonicplay.ui.screens.tageditor.TagEditorScreen
import com.farhan.sonicplay.ui.theme.SonicPlayTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.READ_MEDIA_AUDIO] == true ||
                permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        if (audioGranted) {
            viewModel.rescanStorage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPermissions()
        startPlaybackService()

        setContent {
            val userPrefs by viewModel.userPreferences.collectAsState()

            SonicPlayTheme(
                themeMode = userPrefs.themeMode,
                useDynamicColor = userPrefs.useDynamicColor,
                accentColorHex = userPrefs.accentColorHex
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val currentDestination by viewModel.currentDestination.collectAsState()
                    val isNowPlayingExpanded by viewModel.isNowPlayingExpanded.collectAsState()
                    val isQueueSheetVisible by viewModel.isQueueSheetVisible.collectAsState()

                    val songs by viewModel.allSongs.collectAsState()
                    val albums by viewModel.albums.collectAsState()
                    val artists by viewModel.artists.collectAsState()
                    val folders by viewModel.folders.collectAsState()
                    val playlists by viewModel.allPlaylists.collectAsState()
                    val queues by viewModel.allQueues.collectAsState()

                    val currentSong by viewModel.playbackManager.currentSong.collectAsState()
                    val isPlaying by viewModel.playbackManager.isPlaying.collectAsState()
                    val currentPosMs by viewModel.playbackManager.currentPositionMs.collectAsState()
                    val durationMs by viewModel.playbackManager.durationMs.collectAsState()
                    val isShuffle by viewModel.playbackManager.isShuffle.collectAsState()
                    val repeatMode by viewModel.playbackManager.repeatMode.collectAsState()
                    val currentQueue by viewModel.playbackManager.currentQueue.collectAsState()

                    // Handle back presses
                    BackHandler(enabled = isNowPlayingExpanded || currentDestination !is AppDestination.Library) {
                        if (isNowPlayingExpanded) {
                            viewModel.isNowPlayingExpanded.value = false
                        } else {
                            viewModel.currentDestination.value = AppDestination.Library
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        // Main Content Based on Current Destination
                        when (val dest = currentDestination) {
                            is AppDestination.Library -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        LibraryHomeScreen(
                                            songs = songs,
                                            albums = albums,
                                            artists = artists,
                                            folders = folders,
                                            playlists = playlists,
                                            queues = queues,
                                            onSongClick = { song, list -> viewModel.playSong(song, list) },
                                            onPlayNext = { song -> viewModel.playNext(song) },
                                            onAddToQueue = { song, qId -> viewModel.addToQueue(song, qId) },
                                            onToggleFavorite = { song -> viewModel.toggleFavorite(song) },
                                            onEditTags = { song -> viewModel.currentDestination.value = AppDestination.TagEditor(song) },
                                            onFolderClick = {},
                                            onCreatePlaylist = { name -> viewModel.createPlaylist(name) },
                                            onRenameQueue = { qId, name -> viewModel.renameQueue(qId, name) },
                                            onOpenSearch = { viewModel.currentDestination.value = AppDestination.Search },
                                            onOpenSettings = { viewModel.currentDestination.value = AppDestination.Settings },
                                            playbackManager = viewModel.playbackManager
                                        )
                                    }

                                    // MiniPlayer placed above bottom edge
                                    if (currentSong != null && !isNowPlayingExpanded) {
                                        val progressFraction = if (durationMs > 0) currentPosMs.toFloat() / durationMs else 0f
                                        MiniPlayer(
                                            song = currentSong,
                                            isPlaying = isPlaying,
                                            progressFraction = progressFraction,
                                            onPlayPauseClick = { viewModel.playbackManager.togglePlayPause() },
                                            onSkipNextClick = { viewModel.playbackManager.skipNext() },
                                            onPlayerClick = { viewModel.isNowPlayingExpanded.value = true }
                                        )
                                    }
                                }
                            }
                            is AppDestination.Equalizer -> {
                                EqualizerScreen(
                                    effectsController = viewModel.playbackManager.audioEffectsController,
                                    onBack = { viewModel.currentDestination.value = AppDestination.Library }
                                )
                            }
                            is AppDestination.Search -> {
                                SearchScreen(
                                    songs = songs,
                                    onSongClick = { song -> viewModel.playSong(song, listOf(song)) },
                                    onPlayNext = { song -> viewModel.playNext(song) },
                                    onAddToQueue = { song, qId -> viewModel.addToQueue(song, qId) },
                                    onToggleFavorite = { song -> viewModel.toggleFavorite(song) },
                                    onEditTags = { song -> viewModel.currentDestination.value = AppDestination.TagEditor(song) },
                                    onBack = { viewModel.currentDestination.value = AppDestination.Library }
                                )
                            }
                            is AppDestination.Settings -> {
                                SettingsScreen(
                                    userPreferences = userPrefs,
                                    sleepTimerManager = viewModel.playbackManager.sleepTimerManager,
                                    onThemeModeChange = { viewModel.setThemeMode(it) },
                                    onAccentColorChange = { viewModel.setAccentColor(it) },
                                    onHeadsetDisconnectChange = { viewModel.setPauseOnHeadset(it) },
                                    onRescanLibrary = { viewModel.rescanStorage() },
                                    onBack = { viewModel.currentDestination.value = AppDestination.Library }
                                )
                            }
                            is AppDestination.TagEditor -> {
                                TagEditorScreen(
                                    song = dest.song,
                                    onSaveTags = { id, title, artist, album, year, track, genre ->
                                        viewModel.saveSongTags(id, title, artist, album, year, track, genre)
                                    },
                                    onBack = { viewModel.currentDestination.value = AppDestination.Library }
                                )
                            }
                        }

                        // Fullscreen Now Playing Overlay
                        AnimatedVisibility(
                            visible = isNowPlayingExpanded,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) {
                            NowPlayingScreen(
                                song = currentSong,
                                isPlaying = isPlaying,
                                currentPosMs = currentPosMs,
                                durationMs = durationMs,
                                isShuffle = isShuffle,
                                repeatMode = repeatMode,
                                currentQueueName = currentQueue.name,
                                onPlayPause = { viewModel.playbackManager.togglePlayPause() },
                                onSkipNext = { viewModel.playbackManager.skipNext() },
                                onSkipPrevious = { viewModel.playbackManager.skipPrevious() },
                                onSeekTo = { pos -> viewModel.playbackManager.seekTo(pos) },
                                onToggleShuffle = { viewModel.playbackManager.setShuffle(!isShuffle) },
                                onToggleRepeat = { viewModel.playbackManager.toggleRepeatMode() },
                                onToggleFavorite = { currentSong?.let { viewModel.toggleFavorite(it) } },
                                onOpenQueueSheet = { viewModel.isQueueSheetVisible.value = true },
                                onOpenEqualizer = {
                                    viewModel.isNowPlayingExpanded.value = false
                                    viewModel.currentDestination.value = AppDestination.Equalizer
                                },
                                onOpenLyrics = {},
                                onOpenSpeedPitch = {},
                                onDismiss = { viewModel.isNowPlayingExpanded.value = false }
                            )
                        }

                        // Up Next / Queue Bottom Sheet
                        if (isQueueSheetVisible) {
                            UpNextQueueSheet(
                                queueName = currentQueue.name,
                                songs = currentQueue.songs,
                                currentIndex = currentQueue.currentSongIndex,
                                onSongClick = { index ->
                                    viewModel.playbackManager.playQueue(currentQueue.songs, index)
                                },
                                onRemoveSong = { index ->
                                    viewModel.playbackManager.removeSongFromQueue(index)
                                },
                                onDismiss = { viewModel.isQueueSheetVisible.value = false }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun startPlaybackService() {
        val serviceIntent = Intent(this, SonicPlaybackService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun checkPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}
