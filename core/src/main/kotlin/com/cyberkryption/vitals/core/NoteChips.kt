package com.cyberkryption.vitals.core

/** Quick chips on the Review screen toggle their label in and out of the note. */
object NoteChips {
    const val SEPARATOR = " · "

    fun isSelected(note: String, chip: String): Boolean = segments(note).any { it.equals(chip, ignoreCase = true) }

    fun toggle(note: String, chip: String): String {
        val segments = segments(note)
        return if (segments.any { it.equals(chip, ignoreCase = true) }) {
            segments.filterNot { it.equals(chip, ignoreCase = true) }.joinToString(SEPARATOR)
        } else {
            (segments + chip).joinToString(SEPARATOR)
        }
    }

    private fun segments(note: String): List<String> = note.split('·').map { it.trim() }.filter { it.isNotEmpty() }
}
