package com.farhan.sonicplay

import android.app.Application
import com.farhan.sonicplay.data.local.PreferencesManager
import com.farhan.sonicplay.data.local.SonicPlayDatabase
import com.farhan.sonicplay.data.repository.MusicRepository
import com.farhan.sonicplay.data.repository.MusicScanner
import com.farhan.sonicplay.playback.SonicPlaybackManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SonicPlayApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { SonicPlayDatabase.getDatabase(this, applicationScope) }
    val preferencesManager by lazy { PreferencesManager(this) }
    val musicScanner by lazy { MusicScanner(this) }

    val musicRepository by lazy {
        MusicRepository(
            songDao = database.songDao(),
            playlistDao = database.playlistDao(),
            queueDao = database.queueDao(),
            scanner = musicScanner
        )
    }

    val playbackManager by lazy {
        SonicPlaybackManager(this, musicRepository)
    }

    override fun onCreate() {
        super.onCreate()
        // Initial scan of music library on startup
        applicationScope.launch {
            try {
                musicRepository.rescanLibrary()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
