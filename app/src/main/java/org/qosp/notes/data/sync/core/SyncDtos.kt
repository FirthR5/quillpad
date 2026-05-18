package org.qosp.notes.data.sync.core

import org.qosp.notes.data.model.Note

// Sealed class to represent remote operations
sealed class RemoteOperation {
    data class Create(val note: Note, val import: Boolean = false) : RemoteOperation()
    data class Update(val note: Note) : RemoteOperation()
    data class Delete(val note: Note) : RemoteOperation()
}

enum class SyncMethod {
    MAPPING,
    TITLE,
}

data class SyncNote(
    val id: Long,
    val idStr: String,
    val content: String?,
    val title: String,
    val lastModified: Long, // Epoch seconds
    val extra: String? = null,
    val category: String = "",
    val favorite: Boolean? = null,
    val readOnly: Boolean = false,
)

/*
* Detect if all lines in the note content are task list items.
* Very useful for Markdown Local Notes sync.
*
* Example of task list items:
* - [] Task 1
* - [x] Task 2
* */
fun SyncNote.markdownNoteIsTaskList(): Boolean {
    if (content == null) return false

    val regex = Regex("""^\s*[-+*]\s*\[([ xX])]\s*(.*)$""")
    return content.lines().all { line ->
        line.isBlank() || regex.matches(line)
    }
}
