package ph.attendance.domain

/**
 * The daily lifecycle of one schedule.
 *
 *   BEFORE ──▶ PRESENT ──▶ [GAP] ──▶ LATE ──▶ CLOSED
 *
 * `GAP` exists only when a schedule leaves a deliberate pause between the two windows; when the
 * late window opens exactly as the present window closes, the machine never enters it. A scan is
 * graded by whichever window is open, and scans in BEFORE, GAP, or CLOSED record nothing at all —
 * matching the original app, where a scan outside both windows was discarded.
 */
enum class ScheduleWindowState { BEFORE, PRESENT, GAP, LATE, CLOSED }

fun windowStateAt(schedule: Schedule, atMinutes: Int): ScheduleWindowState {
    val presentStart = timeToMinutes(schedule.present.start)
    val presentEnd = timeToMinutes(schedule.present.end)
    val lateStart = timeToMinutes(schedule.late.start)
    val lateEnd = timeToMinutes(schedule.late.end)

    return when {
        atMinutes < presentStart -> ScheduleWindowState.BEFORE
        atMinutes < presentEnd -> ScheduleWindowState.PRESENT
        atMinutes < lateStart -> ScheduleWindowState.GAP
        atMinutes < lateEnd -> ScheduleWindowState.LATE
        else -> ScheduleWindowState.CLOSED
    }
}

/** The status a scan earns in a given state, or null when the scan records nothing. */
fun statusForWindow(state: ScheduleWindowState): AttendanceStatus? = when (state) {
    ScheduleWindowState.PRESENT -> AttendanceStatus.PRESENT
    ScheduleWindowState.LATE -> AttendanceStatus.LATE
    else -> null
}

fun isCollectingScans(schedule: Schedule, atMinutes: Int): Boolean =
    statusForWindow(windowStateAt(schedule, atMinutes)) != null

/**
 * The minute this schedule next changes state, or null once it is closed for the day. The UI uses
 * it to refresh exactly when a window opens or shuts instead of polling.
 */
fun nextTransitionMinute(schedule: Schedule, atMinutes: Int): Int? = listOf(
    timeToMinutes(schedule.present.start),
    timeToMinutes(schedule.present.end),
    timeToMinutes(schedule.late.start),
    timeToMinutes(schedule.late.end),
).sorted().firstOrNull { it > atMinutes }

data class ScanContext(
    val studentNumber: String,
    /** Active schedules belonging to the scanned student's section. */
    val schedules: List<Schedule>,
    val existingKeys: Set<String>,
    val date: String,
    val atMinutes: Int,
    val recordedAt: String,
)

data class ScanResolution(
    /** Records to append. Empty when nothing about this scan was new. */
    val created: List<AttendanceRecord>,
    /** Schedules already recorded today — a second scan changes nothing. */
    val duplicateScheduleIds: List<Id>,
    /** Schedules whose windows were shut at scan time. */
    val inactiveScheduleIds: List<Id>,
)

/**
 * Grades one scan against every active schedule for the student's section.
 *
 * A student can sit in more than one schedule at once — a class and a flag ceremony — so a single
 * scan may produce several records. Duplicate suppression is per schedule rather than per scan,
 * so scanning again once a new window opens records the new window and leaves the old one alone.
 */
fun resolveScan(context: ScanContext): ScanResolution {
    val created = mutableListOf<AttendanceRecord>()
    val duplicates = mutableListOf<Id>()
    val inactive = mutableListOf<Id>()

    for (schedule in context.schedules) {
        if (schedule.archived) continue

        val status = statusForWindow(windowStateAt(schedule, context.atMinutes))
        if (status == null) {
            inactive += schedule.id
            continue
        }

        if (recordKey(context.studentNumber, schedule.id, context.date) in context.existingKeys) {
            duplicates += schedule.id
            continue
        }

        created += AttendanceRecord(
            id = newId(),
            scheduleId = schedule.id,
            sectionId = schedule.sectionId,
            studentNumber = context.studentNumber,
            date = context.date,
            status = status,
            scheduleTitle = schedule.title,
            recordedAt = context.recordedAt,
        )
    }

    return ScanResolution(created, duplicates, inactive)
}

data class SweepContext(
    val schedule: Schedule,
    /** Student numbers of every active student in the schedule's section. */
    val activeStudentNumbers: List<String>,
    val existingKeys: Set<String>,
    val date: String,
    val atMinutes: Int,
    val recordedAt: String,
)

/**
 * Once the late window has shut, every active student in the section with no record for this
 * schedule and date is absent.
 *
 * Idempotent by construction: it writes exactly the records whose absence it detects, so a second
 * run finds nothing left to write. Returns nothing while the schedule is still open — sweeping
 * early would mark students absent who have not yet had their chance to scan.
 */
fun absentRecordsFor(context: SweepContext): List<AttendanceRecord> {
    val schedule = context.schedule
    if (schedule.archived) return emptyList()
    if (windowStateAt(schedule, context.atMinutes) != ScheduleWindowState.CLOSED) return emptyList()

    return context.activeStudentNumbers
        .filter { recordKey(it, schedule.id, context.date) !in context.existingKeys }
        .map { studentNumber ->
            AttendanceRecord(
                id = newId(),
                scheduleId = schedule.id,
                sectionId = schedule.sectionId,
                studentNumber = studentNumber,
                date = context.date,
                status = AttendanceStatus.ABSENT,
                scheduleTitle = schedule.title,
                recordedAt = context.recordedAt,
            )
        }
}
