package com.cyberkryption.vitals.data

import com.cyberkryption.vitals.core.Reading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class ReadingRepository(private val dao: ReadingDao, private val photos: PhotoStore) {
    fun observeAll(): Flow<List<Reading>> = dao.observeAll().map { list -> list.map { it.toReading() } }

    fun observeBetween(from: Instant, to: Instant): Flow<List<Reading>> =
        dao.observeBetween(from, to).map { list -> list.map { it.toReading() } }

    fun observeLatest(limit: Int): Flow<List<Reading>> = dao.observeLatest(limit).map { list -> list.map { it.toReading() } }

    fun observe(id: Long): Flow<Reading?> = dao.observe(id).map { it?.toReading() }

    fun observeCount(): Flow<Int> = dao.observeCount()

    suspend fun get(id: Long): Reading? = dao.get(id)?.toReading()

    suspend fun all(): List<Reading> = dao.all().map { it.toReading() }

    suspend fun between(from: Instant, to: Instant): List<Reading> = dao.between(from, to).map { it.toReading() }

    /** Inserts a new reading or updates an existing one; returns its id. */
    suspend fun save(reading: Reading): Long =
        if (reading.id == 0L) {
            dao.insert(reading.toEntity())
        } else {
            dao.update(reading.toEntity())
            reading.id
        }

    /** Deletes the reading and its private copy of the photo. */
    suspend fun delete(reading: Reading) {
        dao.delete(reading.id)
        photos.delete(reading.photoUri)
    }
}
