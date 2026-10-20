package com.cyberkryption.vitals.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface ReadingDao {
    @Query("SELECT * FROM readings ORDER BY takenAt DESC")
    fun observeAll(): Flow<List<ReadingEntity>>

    @Query("SELECT * FROM readings WHERE takenAt >= :from AND takenAt < :to ORDER BY takenAt DESC")
    fun observeBetween(from: Instant, to: Instant): Flow<List<ReadingEntity>>

    @Query("SELECT * FROM readings ORDER BY takenAt DESC LIMIT :limit")
    fun observeLatest(limit: Int): Flow<List<ReadingEntity>>

    @Query("SELECT * FROM readings WHERE id = :id")
    fun observe(id: Long): Flow<ReadingEntity?>

    @Query("SELECT COUNT(*) FROM readings")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM readings WHERE id = :id")
    suspend fun get(id: Long): ReadingEntity?

    @Query("SELECT * FROM readings ORDER BY takenAt ASC")
    suspend fun all(): List<ReadingEntity>

    @Query("SELECT * FROM readings WHERE takenAt >= :from AND takenAt < :to ORDER BY takenAt ASC")
    suspend fun between(from: Instant, to: Instant): List<ReadingEntity>

    @Insert
    suspend fun insert(reading: ReadingEntity): Long

    @Update
    suspend fun update(reading: ReadingEntity)

    @Query("DELETE FROM readings WHERE id = :id")
    suspend fun delete(id: Long)
}
