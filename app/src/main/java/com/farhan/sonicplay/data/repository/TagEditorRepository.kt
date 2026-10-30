package com.farhan.sonicplay.data.repository

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.farhan.sonicplay.data.local.SongDao
import com.farhan.sonicplay.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TagEditorRepository(
    private val context: Context,
    private val songDao: SongDao
) {
    suspend fun getSongTags(song: Song): Map<String, String> = withContext(Dispatchers.IO) {
        val tags = mutableMapOf<String, String>()
        tags["title"] = song.title
        tags["artist"] = song.artist
        tags["album"] = song.album
        tags["year"] = if (song.year > 0) song.year.toString() else ""
        tags["track"] = if (song.trackNumber > 0) song.trackNumber.toString() else ""
        tags["genre"] = song.genre

        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, song.contentUri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.let {
                tags["bitrate"] = "${it.toInt() / 1000} kbps"
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)?.let {
                tags["mimetype"] = it
            }
            retriever.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        tags
    }

    suspend fun updateSongTags(
        songId: Long,
        title: String,
        artist: String,
        album: String,
        year: Int,
        trackNumber: Int,
        genre: String
    ): Boolean = withContext(Dispatchers.IO) {
        val entity = songDao.getSongById(songId) ?: return@withContext false
        val updated = entity.copy(
            title = title,
            artist = artist,
            album = album,
            year = year,
            trackNumber = trackNumber,
            genre = genre
        )
        songDao.updateSong(updated)

        // Attempt updating MediaStore if system allows
        try {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.TITLE, title)
                    put(MediaStore.Audio.Media.ARTIST, artist)
                    put(MediaStore.Audio.Media.ALBUM, album)
                    put(MediaStore.Audio.Media.YEAR, year)
                    put(MediaStore.Audio.Media.TRACK, trackNumber)
                }
                context.contentResolver.update(
                    Uri.parse(entity.contentUriString),
                    values,
                    null,
                    null
                )
            }
        } catch (e: Exception) {
            // Scoped storage security exceptions on newer Android are caught gracefully
        }
        true
    }
}
