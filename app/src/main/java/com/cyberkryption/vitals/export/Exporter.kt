package com.cyberkryption.vitals.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.cyberkryption.vitals.R
import com.cyberkryption.vitals.core.Csv
import com.cyberkryption.vitals.core.Guideline
import com.cyberkryption.vitals.core.Period
import com.cyberkryption.vitals.data.ReadingRepository
import com.cyberkryption.vitals.ui.components.shortLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/** Writes exports to the cache and hands them to the share sheet. */
class Exporter(private val context: Context, private val readings: ReadingRepository) {
    private val dir: File get() = File(context.cacheDir, "exports").apply { mkdirs() }

    /** Every reading, oldest first, with its category under [guideline]. */
    suspend fun csv(guideline: Guideline): Uri = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val text = Csv.readings(readings.all(), zone, guideline) { context.getString(it.shortLabel()) }
        val file = File(dir, "vitals-${LocalDate.now(zone)}.csv")
        file.writeText(text)
        uriFor(file)
    }

    /** One-page-per-section PDF summary of a trends period, with categories under [guideline]. */
    suspend fun pdf(period: Period, guideline: Guideline): Uri = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val inPeriod = readings.between(period.startInstant(zone), period.endInstant(zone))
        val file = File(dir, "vitals-${period.start}-${period.end}.pdf")
        PdfReport(context, zone).write(file, period, inPeriod, guideline)
        uriFor(file)
    }

    fun shareIntent(uri: Uri, mimeType: String): Intent {
        val send = Intent(Intent.ACTION_SEND)
            .setType(mimeType)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return Intent.createChooser(send, context.getString(R.string.export_chooser_title))
    }

    private fun uriFor(file: File): Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
