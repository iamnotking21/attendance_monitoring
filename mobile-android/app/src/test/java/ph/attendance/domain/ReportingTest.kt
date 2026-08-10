package ph.attendance.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TallyTest {

    @Test
    fun `counts each status`() {
        val counts = listOf(
            record(status = AttendanceStatus.PRESENT, date = "2024-03-01"),
            record(status = AttendanceStatus.LATE, date = "2024-03-02"),
            record(status = AttendanceStatus.ABSENT, date = "2024-03-03"),
            record(status = AttendanceStatus.PRESENT, date = "2024-03-04"),
        ).tally()

        assertEquals(StatusTally(present = 2, late = 1, absent = 1), counts)
    }

    @Test
    fun `counts late as attending, not as an absence`() {
        // A student late every single day attended every single day. Folding late into absent
        // was a real reporting error in the original app.
        assertEquals(1f, StatusTally(present = 0, late = 10, absent = 0).rate, 0.0001f)
        assertEquals(0.8f, StatusTally(present = 5, late = 3, absent = 2).rate, 0.0001f)
    }

    @Test
    fun `reports zero rather than dividing by zero`() {
        assertEquals(0f, StatusTally().rate, 0.0001f)
    }

    @Test
    fun `formats as a whole percentage`() {
        assertEquals("80%", formatRate(0.8f))
        assertEquals("88%", formatRate(0.876f))
        assertEquals("0%", formatRate(0f))
    }
}

class SummariseStudentsTest {

    private val students = listOf(
        student(id = "stu-1", studentNumber = "s1", lastName = "Alvarez", firstName = "Ana"),
        student(id = "stu-2", studentNumber = "s2", lastName = "Bautista", firstName = "Ben"),
        student(id = "stu-3", studentNumber = "s3", lastName = "Castro", firstName = "Cara"),
    )

    private val records = listOf(
        record("s1", AttendanceStatus.PRESENT, "2024-03-01", id = "r1"),
        record("s1", AttendanceStatus.LATE, "2024-03-02", id = "r2"),
        record("s2", AttendanceStatus.ABSENT, "2024-03-01", id = "r3"),
        // Outside the range under test.
        record("s1", AttendanceStatus.PRESENT, "2024-02-28", id = "r4"),
        record("s2", AttendanceStatus.PRESENT, "2024-03-10", id = "r5"),
    )

    @Test
    fun `counts only records inside the range, inclusive of both ends`() {
        val byNumber = summariseStudents(students, records, "2024-03-01", "2024-03-05")
            .associateBy { it.student.studentNumber }

        assertEquals(StatusTally(present = 1, late = 1), byNumber.getValue("s1").counts)
        assertEquals(StatusTally(absent = 1), byNumber.getValue("s2").counts)
    }

    @Test
    fun `keeps students who have no records at all`() {
        // The student nobody ever scanned is precisely the one a coordinator is looking for.
        val cara = summariseStudents(students, records, "2024-03-01", "2024-03-05")
            .single { it.student.studentNumber == "s3" }

        assertEquals(StatusTally(), cara.counts)
        assertEquals(0, cara.sessions)
    }

    @Test
    fun `excludes archived students`() {
        val withArchived = students + student(id = "stu-4", studentNumber = "s4", archived = true)
        val summaries = summariseStudents(withArchived, records, "2024-03-01", "2024-03-05")
        assertTrue(summaries.none { it.student.studentNumber == "s4" })
    }

    @Test
    fun `sorts by display name`() {
        val summaries = summariseStudents(students, records, "2024-03-01", "2024-03-05")
        assertEquals(listOf("s1", "s2", "s3"), summaries.map { it.student.studentNumber })
    }
}

class DashboardTest {

    private val students = listOf(
        student(id = "stu-1", studentNumber = "s1", lastName = "Alvarez", gender = Gender.FEMALE),
        student(id = "stu-2", studentNumber = "s2", lastName = "Bautista", gender = Gender.MALE),
        student(id = "stu-3", studentNumber = "s3", lastName = "Castro", gender = Gender.MALE),
    )

    private val records = listOf(
        record("s1", AttendanceStatus.PRESENT, DATE, id = "r1"),
        record("s2", AttendanceStatus.LATE, DATE, id = "r2"),
        record("s1", AttendanceStatus.PRESENT, "2024-03-14", id = "r3"),
    )

    @Test
    fun `counts only the chosen day`() {
        val breakdown = buildDashboard(students, records, DATE)
        assertEquals(StatusTally(present = 1, late = 1), breakdown.counts)
    }

    @Test
    fun `lists students with no record as unaccounted for`() {
        val breakdown = buildDashboard(students, records, DATE)
        assertEquals(listOf("s3"), breakdown.unaccountedFor.map { it.studentNumber })
    }

    @Test
    fun `drops records whose student has since been removed`() {
        val orphaned = records + record("gone", AttendanceStatus.PRESENT, DATE, id = "r9")
        val breakdown = buildDashboard(students, orphaned, DATE)

        assertEquals(2, breakdown.entries.size)
        assertEquals(1, breakdown.counts.present)
    }

    @Test
    fun `splits by status and gender`() {
        val breakdown = buildDashboard(students, records, DATE)

        assertEquals(
            listOf("s1"),
            breakdown.entriesWith(AttendanceStatus.PRESENT).map { it.student.studentNumber },
        )
        assertEquals(
            listOf("s2"),
            breakdown.entriesWith(AttendanceStatus.LATE).ofGender(Gender.MALE)
                .map { it.student.studentNumber },
        )
        assertEquals(1, breakdown.entries.ofGender(Gender.FEMALE).size)
    }
}
