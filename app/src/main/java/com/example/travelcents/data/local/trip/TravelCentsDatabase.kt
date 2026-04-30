package com.example.travelcents.data.local.trip

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        TripSummaryEntity::class,
        UserStubEntity::class,
        SyncStateEntity::class,
        AppStateEntity::class,
        TripEventEntity::class,
        TripMemberEntity::class,
        EventOptionEntity::class,
        MediaAssetEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(TripLocalConverters::class)
abstract class TravelCentsDatabase : RoomDatabase() {
    abstract fun tripSummaryDao(): TripSummaryDao
    abstract fun tripEventDao(): TripEventDao
    abstract fun tripMemberDao(): TripMemberDao
    abstract fun eventOptionDao(): EventOptionDao
    abstract fun userStubDao(): UserStubDao
    abstract fun syncStateDao(): SyncStateDao
    abstract fun appStateDao(): AppStateDao
    abstract fun mediaAssetDao(): MediaAssetDao

    companion object {
        @Volatile
        private var instance: TravelCentsDatabase? = null

        fun getInstance(context: Context): TravelCentsDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TravelCentsDatabase::class.java,
                    "travel_cents.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { built -> instance = built }
            }
        }
    }
}
