package com.farhan.sonicplay.data.repository

import android.net.Uri
import com.farhan.sonicplay.data.local.PlaylistDao
import com.farhan.sonicplay.data.local.QueueDao
import com.farhan.sonicplay.data.local.SongDao
import com.farhan.sonicplay.data.local.entities.PlaylistEntity
import com.farhan.sonicplay.data.local.entities.PlaylistSongCrossRef
import com.farhan.sonicplay.data.local.entities.QueueItemEntity
import com.farhan.sonicplay.data.local.entities.SongEntity
import com.farhan.sonicplay.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class MusicRepository(
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val queueDao: QueueDao,
    private val scanner: MusicScanner
) {

    val allSongsFlow: Flow<List<Song>> = songDao.getAllSongsFlow().map { entities ->
        entities.map { it.toDomainModel() }
    }

    val favoriteSongsFlow: Flow<List<Song>> = songDao.getFavoriteSongsFlow().map { entities ->
        entities.map { it.toDomainModel() }
    }

    val allPlaylistsFlow: Flow<List<Playlist>> = playlistDao.getAllPlaylistsFlow().map { entities ->
        entities.map { Playlist(id = it.id, name = it.name, createdAt = it.createdAt) }
    }

    suspend fun rescanLibrary(excludedFolders: Set<String> = emptySet()) = withContext(Dispatchers.IO) {
        val scanned = scanner.scanLocalMusic(excludedFolders)
        if (scanned.isNotEmpty()) {
            val existingSongs = songDao.getAllSongs().associateBy { it.id }
            // Preserve user favorites and custom lyrics
            val merged = scanned.map { s ->
                val existing = existingSongs[s.id]
                if (existing != null) {
                    s.copy(
                        isFavorite = existing.isFavorite,
                        customLyrics = existing.customLyrics ?: s.customLyrics,
                        playCount = existing.playCount
                    )
                } else s
            }
            songDao.insertSongs(merged)
            songDao.deleteStaleSongs(scanned.map { it.id })
        }
    }

    suspend fun toggleFavorite(songId: Long, isFavorite: Boolean) {
        songDao.updateFavorite(songId, isFavorite)
    }

    suspend fun saveLyrics(songId: Long, lyrics: String?) {
        songDao.updateLyrics(songId, lyrics)
    }

    suspend fun createPlaylist(name: String): Long {
        return playlistDao.insertPlaylist(PlaylistEntity(name = name))
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long, orderIndex: Int) {
        playlistDao.insertPlaylistSongs(listOf(PlaylistSongCrossRef(playlistId, songId, orderIndex)))
    }

    fun getSongsInPlaylist(playlistId: Long): Flow<List<Song>> {
        return playlistDao.getSongsInPlaylistFlow(playlistId).map { list -> list.map { it.toDomainModel() } }
    }

    // Multi-Queue operations (up to 20 queues)
    val allQueuesFlow: Flow<List<PlaybackQueue>> = queueDao.getAllQueuesFlow().map { queueEntities ->
        queueEntities.map { q ->
            PlaybackQueue(
                id = q.id,
                name = q.name,
                currentSongIndex = q.currentSongIndex,
                currentPositionMs = q.currentPositionMs,
                songs = emptyList() // Will be loaded per active queue
            )
        }
    }

    suspend fun getQueueSongs(queueId: Int): List<Song> = withContext(Dispatchers.IO) {
        val items = queueDao.getQueueItems(queueId)
        val songEntities = songDao.getSongsByIds(items.map { it.songId })
        val songMap = songEntities.associateBy { it.id }
        items.mapNotNull { item -> songMap[item.songId]?.toDomainModel() }
    }

    suspend fun saveQueue(queueId: Int, songs: List<Song>, currentIndex: Int, currentPosMs: Long) = withContext(Dispatchers.IO) {
        queueDao.clearQueueItems(queueId)
        val items = songs.mapIndexed { idx, song ->
            QueueItemEntity(queueId = queueId, orderIndex = idx, songId = song.id)
        }
        queueDao.insertQueueItems(items)
        queueDao.updateQueueProgress(queueId, currentIndex, currentPosMs)
    }

    suspend fun updateQueueName(queueId: Int, name: String) = withContext(Dispatchers.IO) {
        queueDao.updateQueueName(queueId, name)
    }

    suspend fun getAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val songs = songDao.getAllSongs()
        songs.groupBy { it.albumId }.map { (albumId, songsInAlbum) ->
            val first = songsInAlbum.first()
            Album(
                id = albumId,
                title = first.album,
                artist = first.artist,
                songCount = songsInAlbum.size,
                year = songsInAlbum.maxOfOrNull { it.year } ?: 0,
                albumArtUri = first.albumArtUriString?.let { Uri.parse(it) }
            )
        }.sortedBy { it.title.lowercase() }
    }

    suspend fun getArtists(): List<Artist> = withContext(Dispatchers.IO) {
        val songs = songDao.getAllSongs()
        songs.groupBy { it.artist }.mapIndexed { index, (artistName, songsByArtist) ->
            val albums = songsByArtist.map { it.albumId }.distinct()
            Artist(
                id = index.toLong() + 1,
                name = artistName,
                songCount = songsByArtist.size,
                albumCount = albums.size
            )
        }.sortedBy { it.name.lowercase() }
    }

    suspend fun getFolders(): List<MusicFolder> = withContext(Dispatchers.IO) {
        val songs = songDao.getAllSongs()
        val folderMap = mutableMapOf<String, Int>()
        songs.forEach { song ->
            if (song.dataPath.isNotEmpty()) {
                val parent = File(song.dataPath).parent ?: ""
                if (parent.isNotEmpty()) {
                    folderMap[parent] = (folderMap[parent] ?: 0) + 1
                }
            }
        }
        folderMap.map { (path, count) ->
            MusicFolder(
                path = path,
                name = File(path).name,
                songCount = count
            )
        }.sortedBy { it.name.lowercase() }
    }
}

fun SongEntity.toDomainModel(): Song {
    return Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        durationMs = durationMs,
        dataPath = dataPath,
        contentUri = Uri.parse(contentUriString),
        albumArtUri = albumArtUriString?.let { Uri.parse(it) },
        trackNumber = trackNumber,
        year = year,
        genre = genre,
        dateAdded = dateAdded,
        sizeBytes = sizeBytes,
        isFavorite = isFavorite,
        lyrics = customLyrics
    )
}
