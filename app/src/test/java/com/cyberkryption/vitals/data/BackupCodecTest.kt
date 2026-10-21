package com.cyberkryption.vitals.data

import com.cyberkryption.vitals.core.DateSource
import com.cyberkryption.vitals.core.Field
import com.cyberkryption.vitals.core.Reading
import com.cyberkryption.vitals.core.ReadingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupCodecTest {
    private val bp = Reading(
        id = 7,
        type = ReadingType.BP,
        systolic = 116,
        diastolic = 74,
        pulse = 66,
        takenAt = Instant.parse("2026-10-20T08:42:00Z"),
        takenAtSource = DateSource.EXIF,
        note = "Before meds",
        photoUri = "content://photos/IMG_1.jpg",
        confidence = mapOf(Field.SYSTOLIC to 0.97f, Field.DIASTOLIC to 0.62f),
        createdAt = Instant.parse("2026-10-20T08:43:10Z"),
        editedFields = setOf(Field.DIASTOLIC),
    )
    private val ox = Reading(
        id = 8,
        type = ReadingType.OXIMETER,
        spo2 = 98,
        pulse = 68,
        takenAt = Instant.parse("2026-10-20T07:55:00Z"),
        takenAtSource = DateSource.CAPTURE,
        photoUri = "content://photos/IMG_2.jpg",
        createdAt = Instant.parse("2026-10-20T07:55:30Z"),
    )
    private val photos = mapOf(bp.photoUri to byteArrayOf(1, 2, 3), ox.photoUri to byteArrayOf(4, 5))

    private fun backup(readings: List<Reading> = listOf(bp, ox), hasPhoto: (Reading) -> Boolean = { it.photoUri in photos }): ByteArray {
        val out = ByteArrayOutputStream()
        BackupCodec.write(out, readings, Instant.parse("2026-10-21T09:00:00Z"), hasPhoto) { r -> photos[r.photoUri]?.inputStream() }
        return out.toByteArray()
    }

    /** Restores into a fake photo store: URIs are "restored/N" and their bytes are kept. */
    private class Store {
        val files = mutableMapOf<String, ByteArray>()

        fun save(input: java.io.InputStream): String = "restored/${files.size + 1}".also { files[it] = input.readBytes() }

        fun discard(uri: String) {
            files.remove(uri)
        }
    }

    private fun restore(bytes: ByteArray, existing: Set<ReadingKey> = emptySet(), store: Store = Store()) =
        BackupCodec.read(ByteArrayInputStream(bytes), existing, store::save, store::discard)

    @Test
    fun roundTripKeepsEveryFieldAndPhoto() {
        val store = Store()
        val result = restore(backup(), store = store)

        assertEquals(0, result.skipped)
        assertEquals(2, result.added.size)
        val restoredBp = result.added.single { it.type == ReadingType.BP }
        // A restored reading is new here: no id yet, and a photo URI in this phone's store.
        assertEquals(bp.copy(id = 0, photoUri = restoredBp.photoUri), restoredBp)
        assertEquals(listOf<Byte>(1, 2, 3), store.files.getValue(restoredBp.photoUri).toList())
        val restoredOx = result.added.single { it.type == ReadingType.OXIMETER }
        assertEquals(ox.copy(id = 0, photoUri = restoredOx.photoUri), restoredOx)
        assertEquals(2, store.files.size)
    }

    @Test
    fun readingsAlreadyHereAreSkippedWithTheirPhotos() {
        val store = Store()
        val result = restore(backup(), existing = setOf(bp.key()), store = store)

        assertEquals(1, result.skipped)
        assertEquals(listOf(ReadingType.OXIMETER), result.added.map { it.type })
        assertEquals(setOf(result.added.single().photoUri), store.files.keys)
    }

    @Test
    fun aMissingPhotoRestoresWithoutOne() {
        val result = restore(backup(hasPhoto = { it.type == ReadingType.OXIMETER }))

        assertEquals("", result.added.single { it.type == ReadingType.BP }.photoUri)
    }

    @Test
    fun anyZipWithoutAManifestIsNotABackup() {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("photos/1.jpg"))
            zip.write(byteArrayOf(9))
            zip.closeEntry()
        }
        val store = Store()
        assertProblem(BackupProblem.NOT_A_BACKUP) { restore(out.toByteArray(), store = store) }
        assertTrue("photos saved before the failure are removed", store.files.isEmpty())
    }

    @Test
    fun aFileThatIsNotAZipIsNotABackup() {
        assertProblem(BackupProblem.NOT_A_BACKUP) { restore("hello".toByteArray()) }
    }

    @Test
    fun aNewerFormatAsksForAnUpdate() {
        assertProblem(BackupProblem.NEWER_VERSION) { restore(zipWithManifest("""{"format":99,"createdAt":"x","readings":[]}""")) }
    }

    @Test
    fun aDamagedManifestChangesNothing() {
        assertProblem(BackupProblem.DAMAGED) {
            restore(zipWithManifest("""{"format":1,"createdAt":"x","readings":[{"type":"NOT_A_TYPE","pulse":1,"takenAt":"x","takenAtSource":"EXIF","createdAt":"x"}]}"""))
        }
    }

    private fun zipWithManifest(json: String): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("vitals.json"))
            zip.write(json.toByteArray())
            zip.closeEntry()
        }
        return out.toByteArray()
    }

    private fun assertProblem(expected: BackupProblem, block: () -> Unit) {
        try {
            block()
            fail("Expected $expected")
        } catch (e: BackupException) {
            assertEquals(expected, e.problem)
        }
    }
}
