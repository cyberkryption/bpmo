package com.cyberkryption.vitals.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [ReadingEntity::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class VitalsDatabase : RoomDatabase() {
    abstract fun readingDao(): ReadingDao

    companion object {
        fun build(context: Context): VitalsDatabase =
            Room.databaseBuilder(context, VitalsDatabase::class.java, "vitals.db").build()
    }
}
