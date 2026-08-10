package ph.attendance.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowStateTest {

    @Test
    fun `grades every point of the day`() {
        val schedule = schedule()
        assertEquals(ScheduleWindowState.BEFORE, windowStateAt(schedule, At.BEFORE))
        assertEquals(ScheduleWindowState.PRESENT, windowStateAt(schedule, At.PRESENT_START))
        assertEquals(ScheduleWindowState.PRESENT, windowStateAt(schedule, At.PRESENT_MIDDLE))
        assertEquals(ScheduleWindowState.LATE, windowStateAt(schedule, At.PRESENT_END))
        assertEquals(ScheduleWindowState.LATE, windowStateAt(schedule, At.LATE_MIDDLE))
        assertEquals(ScheduleWindowState.CLOSED, windowStateAt(schedule, At.LATE_END))
        assertEquals(ScheduleWindowState.CLOSED, windowStateAt(schedule, At.AFTER))
    }

    @Test
    fun `each window is start-inclusive and end-exclusive`() {
        // The boundary minute belongs to the window opening, never the one closing. Getting this
        // backwards would let a student scan at exactly 07:30 and be marked present after the
        // present window had already shut.
        val schedule = schedule()
        assertEquals(ScheduleWindowState.PRESENT, windowStateAt(schedule, At.PRESENT_START))
        assertEquals(ScheduleWindowState.LATE, windowStateAt(schedule, At.PRESENT_END))
        assertEquals(ScheduleWindowState.CLOSED, windowStateAt(schedule, At.LATE_END))
    }

    @Test
    fun `reports a gap when the schedule leaves a deliberate pause`() {
        val gapped = schedule(late = TimeWindow("08:00", "08:30"))
        assertEquals(ScheduleWindowState.GAP, windowStateAt(gapped, 7 * 60 + 45))
        assertNull(statusForWindow(ScheduleWindowState.GAP))
    }

    @Test
    fun `only the two open windows earn a status`() {
        assertEquals(AttendanceStatus.PRESENT, statusForWindow(ScheduleWindowState.PRESENT))
        assertEquals(AttendanceStatus.LATE, statusForWindow(ScheduleWindowState.LATE))
        assertNull(statusForWindow(ScheduleWindowState.BEFORE))
        assertNull(statusForWindow(ScheduleWindowState.CLOSED))
    }

    @Test
    fun `finds the next boundary and stops once the day is done`() {
        val schedule = schedule()
        assertEquals(At.PRESENT_START, nextTransitionMinute(schedule, 0))
        assertEquals(At.PRESENT_END, nextTransitionMinute(schedule, At.PRESENT_MIDDLE))
        assertEquals(At.LATE_END, nextTransitionMinute(schedule, At.LATE_MIDDLE))
        assertNull(nextTransitionMinute(schedule, At.LATE_END))
    }
}

class ResolveScanTest {

    private fun context(
        schedules: List<Schedule>,
        atMinutes: Int,
        existingKeys: Set<String> = emptySet(),
    ) = ScanContext("2024-1001", schedules, existingKeys, DATE, atMinutes, RECORDED_AT)

    @Test
    fun `records present inside the present window`() {
        val result = resolveScan(context(listOf(schedule()), At.PRESENT_MIDDLE))

        assertEquals(1, result.created.size)
        val record = result.created.single()
        assertEquals(AttendanceStatus.PRESENT, record.status)
        assertEquals(Ids.SCHEDULE, record.scheduleId)
        assertEquals(Ids.SECTION, record.sectionId)
        assertEquals("Morning Assembly", record.scheduleTitle)
    }

    @Test
    fun `records late inside the late window`() {
        val result = resolveScan(context(listOf(schedule()), At.LATE_MIDDLE))
        assertEquals(AttendanceStatus.LATE, result.created.single().status)
    }

    @Test
    fun `records nothing when both windows are shut`() {
        for (minute in listOf(At.BEFORE, At.AFTER)) {
            val result = resolveScan(context(listOf(schedule()), minute))
            assertTrue(result.created.isEmpty())
            assertEquals(listOf(Ids.SCHEDULE), result.inactiveScheduleIds)
        }
    }

    @Test
    fun `suppresses a second scan for a schedule already recorded today`() {
        val existing = setOf(recordKey("2024-1001", Ids.SCHEDULE, DATE))
        val result = resolveScan(context(listOf(schedule()), At.PRESENT_MIDDLE, existing))

        assertTrue(result.created.isEmpty())
        assertEquals(listOf(Ids.SCHEDULE), result.duplicateScheduleIds)
    }

    @Test
    fun `does not suppress the same student on a different day`() {
        val existing = setOf(recordKey("2024-1001", Ids.SCHEDULE, "2024-03-14"))
        val result = resolveScan(context(listOf(schedule()), At.PRESENT_MIDDLE, existing))
        assertEquals(1, result.created.size)
    }

    @Test
    fun `records every open schedule from a single scan`() {
        val schedules = listOf(
            schedule(id = Ids.SCHEDULE, title = "Assembly"),
            schedule(id = Ids.SCHEDULE_B, title = "Homeroom"),
        )
        val result = resolveScan(context(schedules, At.PRESENT_MIDDLE))
        assertEquals(listOf("Assembly", "Homeroom"), result.created.map { it.scheduleTitle })
    }

    @Test
    fun `suppresses per schedule rather than per scan`() {
        // Already present for the assembly; homeroom is open and unrecorded, so this scan must
        // record homeroom without touching the assembly.
        val existing = setOf(recordKey("2024-1001", Ids.SCHEDULE, DATE))
        val schedules = listOf(
            schedule(id = Ids.SCHEDULE, title = "Assembly"),
            schedule(id = Ids.SCHEDULE_B, title = "Homeroom"),
        )
        val result = resolveScan(context(schedules, At.PRESENT_MIDDLE, existing))

        assertEquals(listOf(Ids.SCHEDULE), result.duplicateScheduleIds)
        assertEquals(Ids.SCHEDULE_B, result.created.single().scheduleId)
    }

    @Test
    fun `ignores archived schedules entirely`() {
        val result = resolveScan(context(listOf(schedule(archived = true)), At.PRESENT_MIDDLE))
        assertTrue(result.created.isEmpty())
        assertTrue(result.inactiveScheduleIds.isEmpty())
        assertTrue(result.duplicateScheduleIds.isEmpty())
    }
}

class AbsenteeSweepTest {

    private val roster = listOf("2024-1001", "2024-1002", "2024-1003")

    private fun context(
        atMinutes: Int,
        existingKeys: Set<String> = emptySet(),
        schedule: Schedule = schedule(),
    ) = SweepContext(schedule, roster, existingKeys, DATE, atMinutes, RECORDED_AT)

    @Test
    fun `marks everyone unrecorded absent once the late window closes`() {
        val absentees = absentRecordsFor(context(At.AFTER))

        assertEquals(3, absentees.size)
        assertTrue(absentees.all { it.status == AttendanceStatus.ABSENT })
        assertEquals(roster, absentees.map { it.studentNumber })
    }

    @Test
    fun `skips students who already have a record`() {
        val existing = setOf(recordKey("2024-1002", Ids.SCHEDULE, DATE))
        val absentees = absentRecordsFor(context(At.AFTER, existing))
        assertEquals(listOf("2024-1001", "2024-1003"), absentees.map { it.studentNumber })
    }

    @Test
    fun `refuses to sweep while the schedule is still open`() {
        for (minute in listOf(At.BEFORE, At.PRESENT_MIDDLE, At.LATE_MIDDLE)) {
            assertTrue(absentRecordsFor(context(minute)).isEmpty())
        }
    }

    @Test
    fun `sweeps at the exact minute the late window closes`() {
        assertEquals(3, absentRecordsFor(context(At.LATE_END)).size)
    }

    @Test
    fun `is idempotent - a second sweep finds nothing left`() {
        val first = absentRecordsFor(context(At.AFTER))
        val afterFirst = first.map { it.key() }.toSet()
        assertTrue(absentRecordsFor(context(At.AFTER, afterFirst)).isEmpty())
    }

    @Test
    fun `ignores archived schedules`() {
        assertTrue(absentRecordsFor(context(At.AFTER, schedule = schedule(archived = true))).isEmpty())
    }
}

class ConflictResolutionTest {

    @Test
    fun `prefers the later write`() {
        assertTrue(incomingWins("2024-03-15T09:00:00Z", "a", "2024-03-15T08:00:00Z", "b"))
        assertTrue(!incomingWins("2024-03-15T08:00:00Z", "a", "2024-03-15T09:00:00Z", "b"))
    }

    @Test
    fun `breaks an exact tie by id, symmetrically`() {
        // Two devices must reach the same answer without talking to each other, so the rule has
        // to be total and antisymmetric.
        val moment = "2024-03-15T09:00:00Z"
        assertTrue(incomingWins(moment, "bbb", moment, "aaa"))
        assertTrue(!incomingWins(moment, "aaa", moment, "bbb"))
    }

    @Test
    fun `never lets a row win against itself`() {
        val moment = "2024-03-15T09:00:00Z"
        assertTrue(!incomingWins(moment, "aaa", moment, "aaa"))
    }
}
