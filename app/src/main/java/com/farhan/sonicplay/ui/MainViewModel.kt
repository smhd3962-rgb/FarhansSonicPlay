package com.farhan.sonicplay.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.farhan.sonicplay.SonicPlayApplication
import com.farhan.sonicplay.data.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class AppDestination {
    object Library : AppDestination()
    object Equalizer : AppDestination()
    object Search : AppDestination()
    object Settings : AppDestination()
    data class TagEditor(val song: Song) : AppDestination()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SonicPlayApplication
    private val repository = app.musicRepository
    val playbackManager = app.playbackManager
    private val preferencesManager = app.preferencesManager

    val userPreferences: StateFlow<UserPreferences> = preferencesManager.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    val allSongs: StateFlow<List<Song>> = repository.allSongsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylists: StateFlow<List<Playlist>> = repository.allPlaylistsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allQueues: StateFlow<List<PlaybackQueue>> = repository.allQueuesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentDestination = MutableStateFlow<AppDestination>(AppDestination.Library)
    val isNowPlayingExpanded = MutableStateFlow(false)
    val isQueueSheetVisible = MutableStateFlow(false)

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists: StateFlow<List<Artist>> = _artists.asStateFlow()

    private val _folders = MutableStateFlow<List<MusicFolder>>(emptyList())
    val folders: StateFlow<List<MusicFolder>> = _folders.asStateFlow()

    init {
        viewModelScope.launch {
            allSongs.collect {
                _albums.value = repository.getAlbums()
                _artists.value = repository.getArtists()
                _folders.value = repository.getFolders()
            }
        }
    }

    fun playSong(song: Song, songList: List<Song>) {
        val index = songList.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        playbackManager.playQueue(songList, index)
    }

    fun playNext(song: Song) {
        playbackManager.addSongToCurrentQueue(song, playNext = true)
    }

    fun addToQueue(song: Song, queueId: Int) {
        if (queueId == playbackManager.currentQueue.value.id) {
            playbackManager.addSongToCurrentQueue(song, playNext = false)
        } else {
            viewModelScope.launch {
                val targetSongs = repository.getQueueSongs(queueId).toMutableList()
                targetSongs.add(song)
                repository.saveQueue(queueId, targetSongs, 0, 0L)
            }
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            repository.toggleFavorite(song.id, !song.isFavorite)
        }
    }

    fun renameQueue(queueId: Int, newName: String) {
        viewModelScope.launch {
            repository.updateQueueName(queueId, newName)
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun rescanStorage() {
        viewModelScope.launch {
            repository.rescanLibrary()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesManager.setThemeMode(mode) }
    }

    fun setAccentColor(hex: String) {
        viewModelScope.launch { preferencesManager.setAccentColor(hex) }
    }

    fun setPauseOnHeadset(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setPauseOnHeadset(enabled) }
    }

    fun saveSongTags(
        songId: Long,
        title: String,
        artist: String,
        album: String,
        year: Int,
        trackNumber: Int,
        genre: String
    ) {
        viewModelScope.launch {
            val tagRepo = com.farhan.sonicplay.data.repository.TagEditorRepository(
                app,
                app.database.songDao()
            )
            tagRepo.updateSongTags(songId, title, artist, album, year, trackNumber, genre)
        }
    }
}
