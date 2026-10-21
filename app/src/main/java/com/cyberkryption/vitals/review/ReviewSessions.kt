package com.cyberkryption.vitals.review

import android.net.Uri
import com.cyberkryption.vitals.core.DateSource
import com.cyberkryption.vitals.core.ParsedDisplay
import com.cyberkryption.vitals.core.Reading
import com.cyberkryption.vitals.core.ReadingType
import com.cyberkryption.vitals.extraction.ExifTag
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** One photo waiting for review. Its file is already in private storage. */
data class ReviewItem(
    val photoUri: Uri,
    val takenAt: Instant?,
    val dateSource: DateSource?,
    val exifTag: ExifTag? = null,
    /** The capture mode; null lets the parser guess the monitor type. */
    val typeHint: ReadingType? = null,
    /** Values the live camera agreed on before taking the photo, used if the photo itself reads fewer. */
    val live: ParsedDisplay? = null,
    /** Set when editing a saved reading instead of reviewing a new photo. */
    val existing: Reading? = null,
)

class ReviewSession(val id: String, val items: List<ReviewItem>) {
    val isEdit: Boolean get() = items.singleOrNull()?.existing != null
}

/** In-memory queue handed from Capture or Import to Review. */
class ReviewSessions {
    private val sessions = ConcurrentHashMap<String, ReviewSession>()

    fun create(items: List<ReviewItem>): String {
        val id = UUID.randomUUID().toString()
        sessions[id] = ReviewSession(id, items)
        return id
    }

    fun get(id: String): ReviewSession? = sessions[id]

    fun remove(id: String) {
        sessions.remove(id)
    }
}
