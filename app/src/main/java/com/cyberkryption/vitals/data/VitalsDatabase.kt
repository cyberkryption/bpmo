package com.cyberkryption.vitals.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Readings must survive app updates. To change the schema, bump [version] and add an
 * `AutoMigration` (or a `Migration`) from the previous schema, which CI exports to app/schemas.
 * Never fall back to a destructive migration: it would wipe everyone's readings.
 */
@Database(entities = [ReadingEntity::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class VitalsDatabase : RoomDatabase() {
    abstract fun readingDao(): ReadingDao

    companion object {
        fun build(context: Context): VitalsDatabase =
            Room.databaseBuilder(context, VitalsDatabase::class.java, "vitals.db").build()
    }
}
