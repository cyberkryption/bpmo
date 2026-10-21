package com.cyberkryption.vitals.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.LocalDate

/** Saves every reading and photo to a file the user picks, and adds them back from one. */
class BackupManager(
    private val context: Context,
    private val readings: ReadingRepository,
    private val photos: PhotoStore,
    private val clock: Clock,
) {
    fun fileName(): String = "vitals-backup-${LocalDate.now(clock)}.zip"

    /** Returns how many readings the backup holds. */
    suspend fun backUp(target: Uri): Int = withContext(Dispatchers.IO) {
        val all = readings.all()
        try {
            val out = context.contentResolver.openOutputStream(target) ?: throw BackupException(BackupProblem.CANT_SAVE)
            out.use { BackupCodec.write(it, all, clock.instant(), { r -> photos.has(r.photoUri) }, { r -> photos.open(r.photoUri) }) }
        } catch (e: BackupException) {
            throw e
        } catch (e: Exception) {
            throw BackupException(BackupProblem.CANT_SAVE, e)
        }
        all.size
    }

    /** Adds the backup's readings that aren't here yet; existing readings are never changed. */
    suspend fun restore(source: Uri): RestoreResult = withContext(Dispatchers.IO) {
        val existing = readings.all().mapTo(HashSet()) { it.key() }
        val input = try {
            context.contentResolver.openInputStream(source)
        } catch (e: Exception) {
            throw BackupException(BackupProblem.CANT_READ, e)
        } ?: throw BackupException(BackupProblem.CANT_READ)
        val result = input.use { BackupCodec.read(it, existing, photos::saveRestored, photos::discard) }
        result.added.forEach { readings.save(it) }
        result
    }
}
