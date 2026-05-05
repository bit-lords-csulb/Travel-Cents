package com.example.travelcents.data.local.trip

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE trip_summary ADD COLUMN budgetTotal REAL NOT NULL DEFAULT 0.0"
                )
                database.execSQL(
                    "ALTER TABLE trip_summary ADD COLUMN interests TEXT NOT NULL DEFAULT ''"
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE trip_summary ADD COLUMN timeZoneId TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        @Volatile
        private var instance: TravelCentsDatabase? = null

        fun getInstance(context: Context): TravelCentsDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TravelCentsDatabase::class.java,
                    "travel_cents.db"
                )
                    .addMigrations(MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .addMigrations(MIGRATION_5_6)
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { built -> instance = built }
            }
        }
    }
}
