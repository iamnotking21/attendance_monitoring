package ph.attendance.domain

import kotlin.math.roundToInt

data class StatusTally(val present: Int = 0, val late: Int = 0, val absent: Int = 0) {
    val total: Int get() = present + late + absent

    /**
     * Share of sessions the student actually turned up for. Late counts as attending — it is a
     * punctuality problem, not an absence, and conflating the two was a real reporting error in
     * the original app.
     */
    val rate: Float get() = if (total == 0) 0f else (present + late).toFloat() / total
}

fun List<AttendanceRecord>.tally(): StatusTally {
    var present = 0
    var late = 0
    var absent = 0
    for (record in this) {
        when (record.status) {
            AttendanceStatus.PRESENT -> present++
            AttendanceStatus.LATE -> late++
            AttendanceStatus.ABSENT -> absent++
        }
    }
    return StatusTally(present, late, absent)
}

fun formatRate(rate: Float): String = "${(rate * 100).roundToInt()}%"

data class StudentSummary(
    val student: Student,
    val counts: StatusTally,
) {
    val displayName: String get() = student.displayName
    val sessions: Int get() = counts.total
    val rate: Float get() = counts.rate
}

/**
 * Attendance per student over an inclusive date range.
 *
 * Students with no records are still listed, with a zero tally. A student who never appears in the
 * data is exactly the one a coordinator needs to see, so dropping them would defeat the report.
 */
fun summariseStudents(
    students: List<Student>,
    records: List<AttendanceRecord>,
    start: String,
    end: String,
): List<StudentSummary> {
    val inRange = records.filter { it.date in start..end }.groupBy { it.studentNumber }

    return students
        .filterNot { it.archived }
        .map { student -> StudentSummary(student, inRange[student.studentNumber].orEmpty().tally()) }
        .sortedBy { it.displayName }
}

data class DashboardEntry(
    val student: Student,
    val status: AttendanceStatus,
    val scheduleTitle: String,
    val recordedAt: String,
)

data class DashboardBreakdown(
    val entries: List<DashboardEntry>,
    val counts: StatusTally,
    /** Active students with no record at all yet for the chosen day. */
    val unaccountedFor: List<Student>,
)

/**
 * The day view, resolved back to real students.
 *
 * Records whose student has since been removed are dropped rather than rendered as an unknown
 * row: the record survives in storage for the audit trail, but a roster is a list of people.
 */
fun buildDashboard(
    students: List<Student>,
    records: List<AttendanceRecord>,
    date: String,
): DashboardBreakdown {
    val active = students.filterNot { it.archived }
    val byNumber = active.associateBy { it.studentNumber }

    val forDate = records.filter { it.date == date && it.studentNumber in byNumber }
    val entries = forDate
        .mapNotNull { record ->
            byNumber[record.studentNumber]?.let { student ->
                DashboardEntry(student, record.status, record.scheduleTitle, record.recordedAt)
            }
        }
        .sortedBy { it.student.displayName }

    val accountedFor = forDate.map { it.studentNumber }.toSet()

    return DashboardBreakdown(
        entries = entries,
        counts = forDate.tally(),
        unaccountedFor = active.filterNot { it.studentNumber in accountedFor },
    )
}

fun DashboardBreakdown.entriesWith(status: AttendanceStatus): List<DashboardEntry> =
    entries.filter { it.status == status }

fun List<DashboardEntry>.ofGender(gender: Gender): List<DashboardEntry> =
    filter { it.student.gender == gender }
