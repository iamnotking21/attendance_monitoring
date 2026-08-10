package ph.attendance.domain

import java.util.UUID

/**
 * The domain, ported from the web app's `src/domain` so both clients enforce the same rules and
 * speak the same wire format.
 *
 * Nothing in this package touches Android, Room, or the network. That is what lets the attendance
 * rules be tested as plain JVM unit tests, with no emulator and no mocking.
 */

typealias Id = String

/** Identifiers are UUIDs because two devices recording offline must never mint the same one. */
fun newId(): Id = UUID.randomUUID().toString()

enum class Gender { MALE, FEMALE;
    val wire: String get() = name.lowercase()

    companion object {
        fun fromWire(value: String): Gender = if (value.equals("female", true)) FEMALE else MALE
    }
}

enum class AttendanceStatus { PRESENT, LATE, ABSENT;
    val wire: String get() = name.lowercase()

    companion object {
        fun fromWire(value: String): AttendanceStatus = when (value.lowercase()) {
            "late" -> LATE
            "absent" -> ABSENT
            else -> PRESENT
        }
    }
}

data class Section(
    val id: Id,
    val name: String,
    val archived: Boolean = false,
    val createdAt: String,
    /** Last local write. The tiebreaker when two devices edit the same row. */
    val updatedAt: String,
)

data class Student(
    val id: Id,
    val sectionId: Id,
    val studentNumber: String,
    val lastName: String,
    val firstName: String,
    val middleName: String = "",
    val gender: Gender,
    val archived: Boolean = false,
    val createdAt: String,
    val updatedAt: String,
) {
    /** "Dela Cruz, Juan P." — the ordering a printed class list uses. */
    val displayName: String
        get() = buildString {
            append(lastName)
            append(", ")
            append(firstName)
            if (middleName.isNotBlank()) {
                append(' ')
                append(middleName.first())
                append('.')
            }
        }
}

data class TimeWindow(val start: String, val end: String)

data class Schedule(
    val id: Id,
    val sectionId: Id,
    val title: String,
    val venue: String = "",
    val present: TimeWindow,
    val late: TimeWindow,
    val archived: Boolean = false,
    val createdAt: String,
    val updatedAt: String,
)

data class AttendanceRecord(
    val id: Id,
    val scheduleId: Id,
    val sectionId: Id,
    val studentNumber: String,
    /** Local calendar date, `YYYY-MM-DD`. */
    val date: String,
    val status: AttendanceStatus,
    /** Denormalised so a report stays truthful after a schedule is renamed or archived. */
    val scheduleTitle: String,
    val recordedAt: String,
)

data class SchoolDay(val date: String, val firstSeenAt: String)

/** Uniqueness key: one record per student, per schedule, per day. */
fun recordKey(studentNumber: String, scheduleId: Id, date: String): String =
    "$studentNumber|$scheduleId|$date"

fun AttendanceRecord.key(): String = recordKey(studentNumber, scheduleId, date)

/**
 * Last-write-wins with a deterministic tiebreak.
 *
 * Two devices editing one row while both offline is normal. Whoever wrote last wins; when the
 * timestamps are identical — clocks are coarse — the larger id wins. Arbitrary, but identical
 * everywhere, which is the only property that matters. Without it replicas can disagree forever.
 */
fun incomingWins(
    incomingUpdatedAt: String,
    incomingId: Id,
    existingUpdatedAt: String,
    existingId: Id,
): Boolean =
    if (incomingUpdatedAt != existingUpdatedAt) incomingUpdatedAt > existingUpdatedAt
    else incomingId > existingId
