package ph.attendance.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import ph.attendance.domain.AttendanceRecord
import ph.attendance.domain.AttendanceStatus
import ph.attendance.domain.Clocks
import ph.attendance.domain.Gender
import ph.attendance.domain.Schedule
import ph.attendance.domain.SchoolDay
import ph.attendance.domain.Section
import ph.attendance.domain.Student
import ph.attendance.domain.TimeWindow
import ph.attendance.domain.minutesToTime
import ph.attendance.domain.newId

/**
 * Demo data, so a first run shows a working system instead of six empty screens. Only ever runs
 * against an empty database — it must never overwrite real records.
 */
object DemoData {

    private val SECTION_NAMES = listOf("Grade 11 - Rizal", "Grade 12 - Mabini")

    private val ROSTER = listOf(
        Quad("Dela Cruz", "Juan", "Ramos", Gender.MALE),
        Quad("Santos", "Maria", "Lopez", Gender.FEMALE),
        Quad("Reyes", "Jose", "Bautista", Gender.MALE),
        Quad("Bautista", "Ana", "Cruz", Gender.FEMALE),
        Quad("Garcia", "Miguel", "Torres", Gender.MALE),
        Quad("Mendoza", "Sofia", "Villanueva", Gender.FEMALE),
        Quad("Torres", "Gabriel", "Aquino", Gender.MALE),
        Quad("Villanueva", "Isabel", "Ramos", Gender.FEMALE),
        Quad("Aquino", "Rafael", "Santos", Gender.MALE),
        Quad("Ramos", "Camille", "Dizon", Gender.FEMALE),
        Quad("Castillo", "Andres", "Reyes", Gender.MALE),
        Quad("Domingo", "Patricia", "Gomez", Gender.FEMALE),
        Quad("Navarro", "Emilio", "Salazar", Gender.MALE),
        Quad("Salazar", "Bianca", "Navarro", Gender.FEMALE),
        Quad("Gomez", "Lorenzo", "Castro", Gender.MALE),
        Quad("Fernandez", "Angelica", "Rivera", Gender.FEMALE),
        Quad("Rivera", "Tomas", "Fernandez", Gender.MALE),
        Quad("Dizon", "Katrina", "Ocampo", Gender.FEMALE),
        Quad("Ocampo", "Diego", "Manalo", Gender.MALE),
        Quad("Manalo", "Trisha", "Alonzo", Gender.FEMALE),
        Quad("Alonzo", "Paolo", "Herrera", Gender.MALE),
        Quad("Herrera", "Danica", "Pascual", Gender.FEMALE),
        Quad("Pascual", "Enrique", "Lim", Gender.MALE),
        Quad("Lim", "Jasmine", "Bernardo", Gender.FEMALE),
        Quad("Bernardo", "Marco", "Espino", Gender.MALE),
        Quad("Espino", "Rowena", "Cordero", Gender.FEMALE),
        Quad("Cordero", "Vicente", "Padilla", Gender.MALE),
        Quad("Padilla", "Andrea", "Bautista", Gender.FEMALE),
        Quad("Soriano", "Julian", "Gatchalian", Gender.MALE),
        Quad("Gatchalian", "Michelle", "Soriano", Gender.FEMALE),
        Quad("Ignacio", "Ramon", "Velasco", Gender.MALE),
        Quad("Velasco", "Clarissa", "Ignacio", Gender.FEMALE),
        Quad("Mercado", "Adrian", "Sarmiento", Gender.MALE),
        Quad("Sarmiento", "Nicole", "Mercado", Gender.FEMALE),
    )

    private data class Quad(
        val last: String,
        val first: String,
        val middle: String,
        val gender: Gender,
    )

    /**
     * A small linear congruential generator, seeded rather than random, so the demo history is
     * the same on every device and a screenshot matches what a new user sees.
     */
    private class Seeded(seed: Int) {
        private var state = seed
        fun next(): Float {
            state = (state * 1664525 + 1013904223)
            return ((state ushr 8) and 0xFFFFFF) / 16777216f
        }
    }

    suspend fun seedIfEmpty(repository: AttendanceRepository, now: LocalDateTime = LocalDateTime.now()): Boolean {
        val counts = repository.counts()
        if (counts.sections > 0 || counts.students > 0 || counts.records > 0) return false

        val createdAt = Clocks.isoInstant(now)
        val random = Seeded(20_240_617)

        val sectionIds = SECTION_NAMES.map { newId() }
        val sections = SECTION_NAMES.mapIndexed { index, name ->
            Section(sectionIds[index], name, false, createdAt, createdAt)
        }

        val students = ROSTER.mapIndexed { index, person ->
            Student(
                id = newId(),
                sectionId = sectionIds[if (index < 18) 0 else 1],
                studentNumber = "2024-%04d".format(1001 + index),
                lastName = person.last,
                firstName = person.first,
                middleName = person.middle,
                gender = person.gender,
                archived = false,
                createdAt = createdAt,
                updatedAt = createdAt,
            )
        }

        // One window straddling this moment, so the scanner does something real straight away,
        // plus a fixed morning window that supplies the history.
        val currentMinute = now.hour * 60 + now.minute
        fun clamp(minute: Int) = minute.coerceIn(0, 23 * 60 + 59)

        val scheduleIds = List(3) { newId() }
        val schedules = listOf(
            Schedule(
                scheduleIds[0], sectionIds[0], "Morning Assembly", "Quadrangle",
                TimeWindow("07:00", "07:30"), TimeWindow("07:30", "08:00"),
                false, createdAt, createdAt,
            ),
            Schedule(
                scheduleIds[1], sectionIds[0], "Homeroom (live demo)", "Room 201",
                TimeWindow(minutesToTime(clamp(currentMinute - 30)), minutesToTime(clamp(currentMinute + 30))),
                TimeWindow(minutesToTime(clamp(currentMinute + 30)), minutesToTime(clamp(currentMinute + 90))),
                false, createdAt, createdAt,
            ),
            Schedule(
                scheduleIds[2], sectionIds[1], "Morning Assembly", "Quadrangle",
                TimeWindow("07:00", "07:30"), TimeWindow("07:30", "08:00"),
                false, createdAt, createdAt,
            ),
        )

        val records = mutableListOf<AttendanceRecord>()
        val schoolDays = mutableListOf<SchoolDay>()
        val start = now.toLocalDate().minusDays(27)

        for (offset in 0L until 28L) {
            val day: LocalDate = start.plusDays(offset)
            if (day.dayOfWeek.value >= 6) continue

            val date = day.toString()
            schoolDays += SchoolDay(date, createdAt)

            for ((sectionIndex, scheduleIndex) in listOf(0 to 0, 1 to 2)) {
                for (student in students) {
                    if (student.sectionId != sectionIds[sectionIndex]) continue

                    val roll = random.next()
                    val status = when {
                        roll < 0.86f -> AttendanceStatus.PRESENT
                        roll < 0.94f -> AttendanceStatus.LATE
                        else -> AttendanceStatus.ABSENT
                    }
                    records += AttendanceRecord(
                        id = newId(),
                        scheduleId = scheduleIds[scheduleIndex],
                        sectionId = student.sectionId,
                        studentNumber = student.studentNumber,
                        date = date,
                        status = status,
                        scheduleTitle = schedules[scheduleIndex].title,
                        recordedAt = Clocks.isoInstant(
                            LocalDateTime.of(day, LocalTime.of(7, (random.next() * 55).toInt())),
                        ),
                    )
                }
            }
        }

        val today = now.toLocalDate().toString()
        if (schoolDays.none { it.date == today }) schoolDays += SchoolDay(today, createdAt)

        repository.upsertSections(sections)
        repository.upsertStudents(students)
        repository.upsertSchedules(schedules)
        repository.append(records)
        repository.insertSchoolDays(schoolDays)

        return true
    }
}
