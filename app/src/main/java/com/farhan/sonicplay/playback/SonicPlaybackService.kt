package com.farhan.sonicplay.playback

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.farhan.sonicplay.SonicPlayApplication

class SonicPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var notificationManager: MediaNotificationManager
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = MediaNotificationManager(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val app = application as SonicPlayApplication
        val player = app.playbackManager.player

        mediaSession = MediaSession.Builder(this, player).build()

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateNotification()
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                updateNotification()
            }
        })

        setupHeadsetListeners(app)
    }

    private fun setupHeadsetListeners(app: SonicPlayApplication) {
        HeadsetReceiver.onHeadsetDisconnected = {
            app.playbackManager.pause()
        }
        HeadsetReceiver.onHeadsetConnected = {
            // Optional auto-resume handled by preferences
        }
    }

    private fun updateNotification() {
        val app = application as SonicPlayApplication
        val currentSong = app.playbackManager.currentSong.value
        val isPlaying = app.playbackManager.isPlaying.value

        val notification = notificationManager.buildNotification(currentSong, isPlaying, mediaSession)

        if (isPlaying) {
            startForeground(MediaNotificationManager.NOTIFICATION_ID, notification)
        } else {
            stopForeground(STOP_FOREGROUND_DETACH)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as SonicPlayApplication
        when (intent?.action) {
            MediaNotificationManager.ACTION_PLAY -> app.playbackManager.play()
            MediaNotificationManager.ACTION_PAUSE -> app.playbackManager.pause()
            MediaNotificationManager.ACTION_NEXT -> app.playbackManager.skipNext()
            MediaNotificationManager.ACTION_PREV -> app.playbackManager.skipPrevious()
            MediaNotificationManager.ACTION_STOP -> {
                app.playbackManager.pause()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { focusChange ->
                    handleAudioFocusChange(focusChange)
                }
                .build()

            audioFocusRequest = request
            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                { focusChange -> handleAudioFocusChange(focusChange) },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    private fun handleAudioFocusChange(focusChange: Int) {
        val app = application as SonicPlayApplication
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                app.playbackManager.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                app.playbackManager.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Lower volume or pause
                app.playbackManager.player.volume = 0.2f
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                app.playbackManager.player.volume = 1.0f
                app.playbackManager.play()
            }
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        abandonAudioFocus()
        super.onDestroy()
    }
}
