package com.cyberkryption.vitals.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.UUID

/** Source photos live in app-private storage and are shared out by content URI. */
class PhotoStore(private val context: Context) {
    private val dir: File get() = File(context.filesDir, "photos").apply { mkdirs() }
    private val authority = "${context.packageName}.fileprovider"

    fun newFile(): File = File(dir, "IMG_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg")

    fun uriFor(file: File): Uri = FileProvider.getUriForFile(context, authority, file)

    /** Copies a picked photo into private storage before the picker's read grant lapses. */
    suspend fun importCopy(source: Uri): Uri = withContext(Dispatchers.IO) {
        val file = newFile()
        val input = context.contentResolver.openInputStream(source) ?: throw IOException("Cannot open $source")
        input.use { stream -> file.outputStream().use { stream.copyTo(it) } }
        uriFor(file)
    }

    suspend fun delete(uri: String) {
        withContext(Dispatchers.IO) { discard(uri) }
    }

    fun has(uri: String): Boolean = fileFor(uri)?.isFile == true

    /** Blocking; call off the main thread. */
    fun open(uri: String): InputStream? = fileFor(uri)?.takeIf { it.isFile }?.inputStream()

    /** Saves a photo from a backup into private storage and returns its URI. Blocking. */
    fun saveRestored(input: InputStream): String {
        val file = newFile()
        try {
            file.outputStream().use { input.copyTo(it) }
        } catch (e: Exception) {
            file.delete()
            throw e
        }
        return uriFor(file).toString()
    }

    /** Blocking; call off the main thread. */
    fun discard(uri: String) {
        fileFor(uri)?.delete()
    }

    fun fileName(uri: String): String? = Uri.parse(uri).lastPathSegment

    private fun fileFor(uri: String): File? {
        val name = fileName(uri)?.takeIf { NAME.matches(it) } ?: return null
        return File(dir, name)
    }

    private companion object {
        val NAME = Regex("IMG_[A-Za-z0-9_-]+\\.jpg")
    }
}
