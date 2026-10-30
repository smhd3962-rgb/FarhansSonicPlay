package com.farhan.sonicplay.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.farhan.sonicplay.data.local.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        QueueEntity::class,
        QueueItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SonicPlayDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun queueDao(): QueueDao

    companion object {
        @Volatile
        private var INSTANCE: SonicPlayDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): SonicPlayDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SonicPlayDatabase::class.java,
                    "farhans_sonic_play.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate 20 multi-queues like Musicolet
                        scope.launch(Dispatchers.IO) {
                            val preQueues = (1..20).map { i ->
                                QueueEntity(
                                    id = i,
                                    name = when (i) {
                                        1 -> "Main Queue"
                                        2 -> "Workout & Energy"
                                        3 -> "Chill & Acoustic"
                                        4 -> "Late Night Vibes"
                                        5 -> "Road Trip Hits"
                                        else -> "Queue $i"
                                    }
                                )
                            }
                            INSTANCE?.queueDao()?.insertAllQueues(preQueues)
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
