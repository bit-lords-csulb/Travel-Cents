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
        AppStateEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(TripLocalConverters::class)
abstract class TravelCentsDatabase : RoomDatabase() {
    abstract fun tripSummaryDao(): TripSummaryDao
    abstract fun userStubDao(): UserStubDao
    abstract fun syncStateDao(): SyncStateDao
    abstract fun appStateDao(): AppStateDao

    companion object {
        @Volatile
        private var instance: TravelCentsDatabase? = null

        fun getInstance(context: Context): TravelCentsDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TravelCentsDatabase::class.java,
                    "travel_cents.db"
                ).build().also { built -> instance = built }
            }
        }
    }
}
