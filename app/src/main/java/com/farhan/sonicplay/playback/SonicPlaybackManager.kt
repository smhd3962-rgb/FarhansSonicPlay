package com.farhan.sonicplay.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.farhan.sonicplay.data.model.PlaybackQueue
import com.farhan.sonicplay.data.model.Song
import com.farhan.sonicplay.data.repository.MusicRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SonicPlaybackManager(
    private val context: Context,
    private val repository: MusicRepository
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val player: ExoPlayer = ExoPlayer.Builder(context).build()
    val audioEffectsController = AudioEffectsController()
    val sleepTimerManager = SleepTimerManager { finishCurrentSong ->
        if (finishCurrentSong) {
            // Stop after current song finishes
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        pause()
                        player.removeListener(this)
                    }
                }
            })
        } else {
            pause()
        }
    }

    private val _currentQueue = MutableStateFlow(
        PlaybackQueue(id = 1, name = "Main Queue", currentSongIndex = 0, currentPositionMs = 0L)
    )
    val currentQueue: StateFlow<PlaybackQueue> = _currentQueue.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private var progressJob: Job? = null

    init {
        setupPlayerListeners()
        startProgressTicker()
    }

    private fun setupPlayerListeners() {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
                if (playing) {
                    audioEffectsController.attachToAudioSession(player.audioSessionId)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _durationMs.value = player.duration.coerceAtLeast(0L)
                    audioEffectsController.attachToAudioSession(player.audioSessionId)
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = player.currentMediaItemIndex
                val queueSongs = _currentQueue.value.songs
                if (index in queueSongs.indices) {
                    _currentSong.value = queueSongs[index]
                    _currentQueue.value = _currentQueue.value.copy(currentSongIndex = index)
                    saveCurrentQueueState()
                }
            }
        })
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (player.isPlaying) {
                    _currentPositionMs.value = player.currentPosition.coerceAtLeast(0L)
                    _durationMs.value = player.duration.coerceAtLeast(0L)
                }
                delay(200L)
            }
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0, startPosMs: Long = 0L) {
        if (songs.isEmpty()) return

        val safeIndex = startIndex.coerceIn(0, songs.size - 1)
        _currentQueue.value = _currentQueue.value.copy(
            songs = songs,
            currentSongIndex = safeIndex,
            currentPositionMs = startPosMs
        )
        _currentSong.value = songs[safeIndex]

        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(song.contentUri)
                .build()
        }

        player.setMediaItems(mediaItems, safeIndex, startPosMs)
        player.prepare()
        player.play()
        saveCurrentQueueState()
    }

    fun play() {
        if (player.mediaItemCount > 0) {
            player.play()
        }
    }

    fun pause() {
        player.pause()
        saveCurrentQueueState()
    }

    fun togglePlayPause() {
        if (player.isPlaying) pause() else play()
    }

    fun skipNext() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else if (player.mediaItemCount > 0 && player.repeatMode == Player.REPEAT_MODE_ALL) {
            player.seekTo(0, 0L)
        }
    }

    fun skipPrevious() {
        if (player.currentPosition > 3000L) {
            player.seekTo(0L)
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            player.seekTo(0L)
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _currentPositionMs.value = positionMs
    }

    fun setShuffle(enabled: Boolean) {
        _isShuffle.value = enabled
        player.shuffleModeEnabled = enabled
    }

    fun toggleRepeatMode() {
        val nextMode = when (_repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        _repeatMode.value = nextMode
        player.repeatMode = nextMode
    }

    // Switch between the 20 independent queues seamlessly
    fun switchQueue(newQueueId: Int) {
        if (_currentQueue.value.id == newQueueId) return

        // 1. Save current queue's progress
        val current = _currentQueue.value
        val currentPos = player.currentPosition
        val currentIndex = player.currentMediaItemIndex

        scope.launch {
            repository.saveQueue(current.id, current.songs, currentIndex, currentPos)

            // 2. Load target queue
            val targetSongs = repository.getQueueSongs(newQueueId)
            val queues = repository.allQueuesFlow
            // Fetch queue metadata
            val targetQueueEntity = repository.allQueuesFlow
            // Setup target queue
            _currentQueue.value = PlaybackQueue(
                id = newQueueId,
                name = "Queue $newQueueId",
                currentSongIndex = 0,
                currentPositionMs = 0L,
                songs = targetSongs
            )

            if (targetSongs.isNotEmpty()) {
                val mediaItems = targetSongs.map { song ->
                    MediaItem.Builder()
                        .setMediaId(song.id.toString())
                        .setUri(song.contentUri)
                        .build()
                }
                player.setMediaItems(mediaItems, 0, 0L)
                player.prepare()
                _currentSong.value = targetSongs.first()
            } else {
                player.clearMediaItems()
                _currentSong.value = null
            }
        }
    }

    fun addSongToCurrentQueue(song: Song, playNext: Boolean = false) {
        val currentList = _currentQueue.value.songs.toMutableList()
        val currentIndex = player.currentMediaItemIndex

        val insertIndex = if (playNext && currentIndex in currentList.indices) {
            currentIndex + 1
        } else {
            currentList.size
        }

        currentList.add(insertIndex, song)
        val mediaItem = MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.contentUri)
            .build()

        player.addMediaItem(insertIndex, mediaItem)
        _currentQueue.value = _currentQueue.value.copy(songs = currentList)
        saveCurrentQueueState()
    }

    fun removeSongFromQueue(index: Int) {
        val currentList = _currentQueue.value.songs.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            player.removeMediaItem(index)
            _currentQueue.value = _currentQueue.value.copy(songs = currentList)
            saveCurrentQueueState()
        }
    }

    private fun saveCurrentQueueState() {
        val q = _currentQueue.value
        val index = player.currentMediaItemIndex
        val pos = player.currentPosition
        scope.launch {
            repository.saveQueue(q.id, q.songs, index, pos)
        }
    }

    fun release() {
        progressJob?.cancel()
        audioEffectsController.release()
        player.release()
    }
}
