package com.farhan.sonicplay.data.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val dataPath: String,
    val contentUri: Uri,
    val albumArtUri: Uri? = null,
    val trackNumber: Int = 0,
    val year: Int = 0,
    val genre: String = "Unknown",
    val dateAdded: Long = 0,
    val sizeBytes: Long = 0,
    val isFavorite: Boolean = false,
    val lyrics: String? = null
) {
    val formattedDuration: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return if (minutes >= 60) {
                val hours = minutes / 60
                val remMinutes = minutes % 60
                String.format("%d:%02d:%02d", hours, remMinutes, seconds)
            } else {
                String.format("%d:%02d", minutes, seconds)
            }
        }
}

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val songCount: Int,
    val year: Int,
    val albumArtUri: Uri? = null
)

data class Artist(
    val id: Long,
    val name: String,
    val songCount: Int,
    val albumCount: Int
)

data class Genre(
    val id: Long,
    val name: String,
    val songCount: Int
)

data class MusicFolder(
    val path: String,
    val name: String,
    val songCount: Int,
    val isExcluded: Boolean = false
)

data class Playlist(
    val id: Long,
    val name: String,
    val songCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class PlaybackQueue(
    val id: Int, // 1 to 20
    val name: String,
    val currentSongIndex: Int = 0,
    val currentPositionMs: Long = 0L,
    val songs: List<Song> = emptyList(),
    val isActive: Boolean = false
)

data class AudioEffectsState(
    val isEnabled: Boolean = true,
    val presetIndex: Int = 0,
    val bandLevels: Map<Int, Int> = emptyMap(), // Band index -> millibels level
    val bassBoostStrength: Int = 0, // 0 to 1000
    val virtualizerStrength: Int = 0, // 0 to 1000
    val reverbPreset: Short = 0, // PresetReverb constants
    val playbackSpeed: Float = 1.0f, // 0.25x to 2.5x
    val playbackPitch: Float = 1.0f // 0.5x to 2.0x
)

enum class ThemeMode {
    SYSTEM, LIGHT, DARK, AMOLED
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.AMOLED,
    val useDynamicColor: Boolean = true,
    val accentColorHex: String = "#00E5FF",
    val activeQueueId: Int = 1,
    val pauseOnHeadsetDisconnect: Boolean = true,
    val resumeOnHeadsetConnect: Boolean = false,
    val shuffleMode: Boolean = false,
    val repeatMode: Int = 0, // 0: None, 1: All, 2: One
    val crossfadeMs: Int = 0,
    val excludedFolders: Set<String> = emptySet(),
    val sleepTimerMinutes: Int = 0,
    val sleepTimerFinishCurrentSong: Boolean = false
)

enum class SortOrder(val displayName: String) {
    TITLE_ASC("Title (A-Z)"),
    TITLE_DESC("Title (Z-A)"),
    ARTIST_ASC("Artist (A-Z)"),
    ALBUM_ASC("Album (A-Z)"),
    DATE_ADDED_DESC("Recently Added"),
    DURATION_DESC("Longest Duration"),
    YEAR_DESC("Year (Newest)")
}
