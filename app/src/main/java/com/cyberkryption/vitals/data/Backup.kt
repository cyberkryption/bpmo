package com.cyberkryption.vitals.data

import com.cyberkryption.vitals.core.DateSource
import com.cyberkryption.vitals.core.Field
import com.cyberkryption.vitals.core.Reading
import com.cyberkryption.vitals.core.ReadingType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

enum class BackupProblem { NOT_A_BACKUP, NEWER_VERSION, DAMAGED, CANT_SAVE, CANT_READ }

class BackupException(val problem: BackupProblem, cause: Throwable? = null) : Exception(problem.name, cause)

/** What makes two readings the same one, so restoring a backup twice doesn't double up. */
data class ReadingKey(
    val type: ReadingType,
    val takenAt: Instant,
    val systolic: Int?,
    val diastolic: Int?,
    val pulse: Int,
    val spo2: Int?,
)

fun Reading.key(): ReadingKey = ReadingKey(type, takenAt, systolic, diastolic, pulse, spo2)

class RestoreResult(val added: List<Reading>, val skipped: Int)

@Serializable
private data class BackupManifest(
    val format: Int,
    val createdAt: String,
    val readings: List<BackupReading>,
)

/** A reading as stored in a backup: plain names and ISO-8601 times, so later versions can read it. */
@Serializable
private data class BackupReading(
    val type: String,
    val systolic: Int? = null,
    val diastolic: Int? = null,
    val pulse: Int,
    val spo2: Int? = null,
    val takenAt: String,
    val takenAtSource: String,
    val note: String? = null,
    /** The photo's entry in the zip, if it was backed up. */
    val photo: String? = null,
    val confidence: Map<String, Float> = emptyMap(),
    val createdAt: String,
    val editedFields: List<String> = emptyList(),
)

/**
 * A backup is a zip holding `vitals.json`, with every reading, followed by each reading's photo
 * under `photos/`. It survives uninstalling the app or moving to a new phone.
 */
object BackupCodec {
    const val FORMAT = 1
    const val MIME_TYPE = "application/zip"

    private const val MANIFEST = "vitals.json"
    private const val PHOTOS = "photos/"

    /** Generous for one phone photo, small enough that a hostile file can't fill the disk. */
    private const val MAX_ENTRY_BYTES = 40L * 1024 * 1024

    private val json = Json { ignoreUnknownKeys = true }

    /** Writes [readings] and the photos [hasPhoto] reports; [openPhoto] streams each one. */
    fun write(
        out: OutputStream,
        readings: List<Reading>,
        createdAt: Instant,
        hasPhoto: (Reading) -> Boolean,
        openPhoto: (Reading) -> InputStream?,
    ) {
        val zip = ZipOutputStream(out)
        val photoNames = readings.mapIndexed { i, r -> if (hasPhoto(r)) "${PHOTOS}${i + 1}.jpg" else null }
        // The manifest goes first, so a restore knows which photos it needs before reaching them.
        zip.putNextEntry(ZipEntry(MANIFEST))
        val manifest = BackupManifest(FORMAT, createdAt.toString(), readings.zip(photoNames) { r, photo -> r.toBackup(photo) })
        zip.write(json.encodeToString(BackupManifest.serializer(), manifest).toByteArray())
        zip.closeEntry()
        readings.zip(photoNames).forEach { (reading, name) ->
            if (name == null) return@forEach
            openPhoto(reading)?.use { input ->
                zip.putNextEntry(ZipEntry(name))
                input.copyTo(zip)
                zip.closeEntry()
            }
        }
        zip.close()
    }

    /**
     * Reads a backup, keeping only readings not in [existing]. [savePhoto] stores a photo and
     * returns its new URI; [discardPhoto] removes one that turns out not to be needed.
     */
    fun read(
        input: InputStream,
        existing: Set<ReadingKey>,
        savePhoto: (InputStream) -> String,
        discardPhoto: (String) -> Unit,
    ): RestoreResult {
        val saved = mutableMapOf<String, String>()
        try {
            var manifest: BackupManifest? = null
            var wanted: Set<String>? = null
            val zip = ZipInputStream(input)
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) continue
                val data = EntryStream(zip, MAX_ENTRY_BYTES)
                if (entry.name == MANIFEST) {
                    val m = decode(data.readBytes())
                    manifest = m
                    wanted = m.readings.filter { it.toReading("").key() !in existing }.mapNotNull { it.photo }.toSet()
                } else if (entry.name.startsWith(PHOTOS) && (wanted == null || entry.name in wanted)) {
                    saved[entry.name] = savePhoto(data)
                }
            }
            val m = manifest ?: throw BackupException(BackupProblem.NOT_A_BACKUP)
            val all = m.readings.map { it.toReading(it.photo?.let(saved::get).orEmpty()) }
            val added = all.filter { it.key() !in existing }
            val used = added.map { it.photoUri }.toSet()
            saved.values.filterNot { it in used }.forEach(discardPhoto)
            return RestoreResult(added, skipped = all.size - added.size)
        } catch (e: Exception) {
            saved.values.forEach(discardPhoto)
            throw when (e) {
                is BackupException -> e
                is java.util.zip.ZipException -> BackupException(BackupProblem.NOT_A_BACKUP, e)
                else -> BackupException(BackupProblem.DAMAGED, e)
            }
        }
    }

    private fun decode(bytes: ByteArray): BackupManifest {
        val text = bytes.decodeToString()
        val format = json.parseToJsonElement(text).jsonObject["format"]?.jsonPrimitive?.intOrNull
            ?: throw BackupException(BackupProblem.NOT_A_BACKUP)
        if (format > FORMAT) throw BackupException(BackupProblem.NEWER_VERSION)
        return json.decodeFromString(BackupManifest.serializer(), text)
    }

    private fun Reading.toBackup(photo: String?) = BackupReading(
        type = type.name,
        systolic = systolic,
        diastolic = diastolic,
        pulse = pulse,
        spo2 = spo2,
        takenAt = takenAt.toString(),
        takenAtSource = takenAtSource.name,
        note = note,
        photo = photo,
        confidence = confidence.mapKeys { it.key.name },
        createdAt = createdAt.toString(),
        editedFields = editedFields.map { it.name },
    )

    private fun BackupReading.toReading(photoUri: String) = Reading(
        type = ReadingType.valueOf(type),
        systolic = systolic,
        diastolic = diastolic,
        pulse = pulse,
        spo2 = spo2,
        takenAt = Instant.parse(takenAt),
        takenAtSource = DateSource.valueOf(takenAtSource),
        note = note,
        photoUri = photoUri,
        confidence = confidence.mapNotNull { (k, v) -> Field.entries.firstOrNull { it.name == k }?.let { it to v } }.toMap(),
        createdAt = Instant.parse(createdAt),
        editedFields = editedFields.mapNotNull { name -> Field.entries.firstOrNull { it.name == name } }.toSet(),
    )

    /** The current zip entry, capped at [limit] bytes; closing it leaves the zip open. */
    private class EntryStream(private val zip: InputStream, private val limit: Long) : InputStream() {
        private var count = 0L

        override fun read(): Int = zip.read().also { if (it >= 0) counted(1) }

        override fun read(b: ByteArray, off: Int, len: Int): Int = zip.read(b, off, len).also { if (it > 0) counted(it.toLong()) }

        private fun counted(n: Long) {
            count += n
            if (count > limit) throw BackupException(BackupProblem.DAMAGED)
        }

        override fun close() = Unit
    }
}
