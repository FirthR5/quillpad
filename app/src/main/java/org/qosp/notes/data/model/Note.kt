package org.qosp.notes.data.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.time.Instant

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            onDelete = ForeignKey.SET_NULL,
            entity = Notebook::class,
            parentColumns = ["id"],
            childColumns = ["notebookId"]
        ),
    ]
)
@Serializable
data class NoteEntity(
    val title: String,
    val content: String,
    val isList: Boolean,
    val taskList: List<NoteTask>,
    val isArchived: Boolean,
    val isDeleted: Boolean,
    val isPinned: Boolean,
    val isHidden: Boolean,
    val isMarkdownEnabled: Boolean,
    val isLocalOnly: Boolean,
    val isCompactPreview: Boolean,
    val screenAlwaysOn: Boolean,
    val creationDate: Long,
    val modifiedDate: Long,
    val deletionDate: Long?,
    val attachments: List<Attachment>,
    val color: NoteColor,
    @ColumnInfo(index = true)
    val notebookId: Long?,
    @PrimaryKey(autoGenerate = true)
    val id: Long,
)

@Serializable
@Parcelize
data class Note(
    val title: String = "",
    val content: String = "",
    val isList: Boolean = false,
    val taskList: List<NoteTask> = listOf(),
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val isPinned: Boolean = false,
    val isHidden: Boolean = false,
    val isMarkdownEnabled: Boolean = true,
    val isLocalOnly: Boolean = false,
    val isCompactPreview: Boolean = false,
    val screenAlwaysOn: Boolean = false,
    val creationDate: Long = Instant.now().epochSecond,
    val modifiedDate: Long = Instant.now().epochSecond,
    val deletionDate: Long? = null,
    val attachments: List<Attachment> = listOf(),
    val color: NoteColor = NoteColor.Default,
    val notebookId: Long? = null,
    val id: Long = 0L,
    @Relation(
        entity = Tag::class,
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = NoteTagJoin::class,
            parentColumn = "noteId",
            entityColumn = "tagId",
        )
    )
    val tags: List<Tag> = listOf(),
    @Relation(
        parentColumn = "id",
        entityColumn = "noteId",
    )
    val reminders: List<Reminder> = listOf(),
) : Parcelable {

    fun isEmpty(): Boolean {
        val baseCondition =
            title.isBlank() && attachments.isEmpty() && reminders.isEmpty() && tags.isEmpty()
        return when {
            isList -> baseCondition && taskList.isEmpty()
            else -> baseCondition && content.isBlank()
        }
    }

    fun stringToTaskList(): List<NoteTask> {
        var nextId = 0L

        if (markdownNoteIsTaskList()){
            val regex = Regex("""^\s*[-+*]\s*\[([ xX])]\s*(.*)$""")
            return content.lineSequence().mapNotNull { line ->
                val match = regex.matchEntire(line)
                if (match != null) {
                    NoteTask(
                        id = nextId++,
                        content = match.groupValues[2],
                        isDone = match.groupValues[1].equals("x", ignoreCase = true)
                    )
                } else null
            }.toList()
        }
        else {
            return content
                .lines()
                .map { NoteTask(nextId++, it.trim(), false) }
        }
    }

    /*
    * Detect if all lines in the note content are task list items.
    * Very useful for Markdown Local Notes sync.
    *
    * Example of task list items:
    * - [] Task 1
    * - [x] Task 2
    * */
    fun markdownNoteIsTaskList(): Boolean {
        if (content.isBlank()) return false

        val regex = Regex("""^\s*[-+*]\s*\[([ xX])]\s*(.*)$""")
        return content.lines().all { line ->
            line.isBlank() || regex.matches(line)
        }
    }

    fun mdToTaskList(content: String): List<NoteTask> {
        val regex = Regex("""^\s*[-+*]\s*\[([ xX])]\s*(.*)$""")
        val tasks = mutableListOf<NoteTask>()
        content.lineSequence().forEach { line ->
            val match = regex.matchEntire(line)

            if (match != null) {
                tasks += NoteTask(
                    id = tasks.size.toLong(),
                    content = match.groupValues[2],
                    isDone = match.groupValues[1].equals("x", ignoreCase = true)
                )
            } else if (tasks.isNotEmpty()) {
                val last = tasks.last()
                tasks[tasks.lastIndex] = last.copy(
                    content = last.content + "\n" + line
                )
            }
        }
        return tasks
    }

    fun toStorableContent(): String {
        return when {
            isList -> taskListToMd()
            else -> content
        }
    }

    fun taskListToMd(): String {
        return taskList.joinToString("\n") {
            val prefix = if (it.isDone) "- [x]" else "- [ ]"
            "$prefix ${it.content.trim()}"
        }
    }

    fun taskListToString(withCheckmarks: Boolean = false): String {
        return taskList.joinToString("\n") {
            val prefix = when {
                withCheckmarks -> if (it.isDone) "☑ " else "☐ "
                else -> ""
            }
            "$prefix${it.content.trim()}"
        }
    }

    fun toEntity(): NoteEntity = NoteEntity(
        title = title,
        content = content,
        isList = isList,
        taskList = taskList,
        isArchived = isArchived,
        isDeleted = isDeleted,
        isPinned = isPinned,
        isHidden = isHidden,
        isMarkdownEnabled = isMarkdownEnabled,
        isLocalOnly = isLocalOnly,
        isCompactPreview = isCompactPreview,
        screenAlwaysOn = screenAlwaysOn,
        creationDate = creationDate,
        modifiedDate = modifiedDate,
        deletionDate = deletionDate,
        attachments = attachments,
        color = color,
        notebookId = notebookId,
        id = id
    )
}
