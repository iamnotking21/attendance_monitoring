package ph.attendance.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import ph.attendance.domain.AttendanceRecord
import ph.attendance.domain.AttendanceStatus
import ph.attendance.domain.Gender
import ph.attendance.domain.Schedule
import ph.attendance.domain.SchoolDay
import ph.attendance.domain.Section
import ph.attendance.domain.Student
import ph.attendance.domain.TimeWindow

/**
 * Storage rows, kept separate from the domain types.
 *
 * The domain must not know what a database is — that separation is what lets the attendance rules
 * be tested without an emulator. The mapping either way is trivial and lives here.
 *
 * Statuses and genders are stored as their lowercase wire strings rather than as ordinals, so a
 * reordered enum cannot silently turn every "present" into "late".
 */

@Entity(tableName = "sections", indices = [Index("updatedAt")])
data class SectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val archived: Boolean,
    val createdAt: String,
    val updatedAt: String,
) {
    fun toDomain() = Section(id, name, archived, createdAt, updatedAt)

    companion object {
        fun from(section: Section) = SectionEntity(
            section.id, section.name, section.archived, section.createdAt, section.updatedAt,
        )
    }
}

@Entity(
    tableName = "students",
    indices = [Index("sectionId"), Index("studentNumber"), Index("updatedAt")],
)
data class StudentEntity(
    @PrimaryKey val id: String,
    val sectionId: String,
    val studentNumber: String,
    val lastName: String,
    val firstName: String,
    val middleName: String,
    val gender: String,
    val archived: Boolean,
    val createdAt: String,
    val updatedAt: String,
) {
    fun toDomain() = Student(
        id, sectionId, studentNumber, lastName, firstName, middleName,
        Gender.fromWire(gender), archived, createdAt, updatedAt,
    )

    companion object {
        fun from(student: Student) = StudentEntity(
            student.id, student.sectionId, student.studentNumber, student.lastName,
            student.firstName, student.middleName, student.gender.wire, student.archived,
            student.createdAt, student.updatedAt,
        )
    }
}

@Entity(tableName = "schedules", indices = [Index("sectionId"), Index("updatedAt")])
data class ScheduleEntity(
    @PrimaryKey val id: String,
    val sectionId: String,
    val title: String,
    val venue: String,
    val presentStart: String,
    val presentEnd: String,
    val lateStart: String,
    val lateEnd: String,
    val archived: Boolean,
    val createdAt: String,
    val updatedAt: String,
) {
    fun toDomain() = Schedule(
        id, sectionId, title, venue,
        TimeWindow(presentStart, presentEnd), TimeWindow(lateStart, lateEnd),
        archived, createdAt, updatedAt,
    )

    companion object {
        fun from(schedule: Schedule) = ScheduleEntity(
            schedule.id, schedule.sectionId, schedule.title, schedule.venue,
            schedule.present.start, schedule.present.end,
            schedule.late.start, schedule.late.end,
            schedule.archived, schedule.createdAt, schedule.updatedAt,
        )
    }
}

@Entity(
    tableName = "attendance_records",
    indices = [
        // The real guarantee behind "one record per student, per schedule, per day". Enforcing it
        // in application code alone would lose the race between a double-tap scan, the absentee
        // sweep, and an incoming sync.
        Index(value = ["studentNumber", "scheduleId", "date"], unique = true),
        Index(value = ["sectionId", "date"]),
        Index(value = ["scheduleId", "date"]),
        Index("recordedAt"),
    ],
)
data class RecordEntity(
    @PrimaryKey val id: String,
    val scheduleId: String,
    val sectionId: String,
    val studentNumber: String,
    val date: String,
    val status: String,
    val scheduleTitle: String,
    val recordedAt: String,
) {
    fun toDomain() = AttendanceRecord(
        id, scheduleId, sectionId, studentNumber, date,
        AttendanceStatus.fromWire(status), scheduleTitle, recordedAt,
    )

    companion object {
        fun from(record: AttendanceRecord) = RecordEntity(
            record.id, record.scheduleId, record.sectionId, record.studentNumber, record.date,
            record.status.wire, record.scheduleTitle, record.recordedAt,
        )
    }
}

@Entity(tableName = "school_days")
data class SchoolDayEntity(
    @PrimaryKey val date: String,
    val firstSeenAt: String,
) {
    fun toDomain() = SchoolDay(date, firstSeenAt)

    companion object {
        fun from(day: SchoolDay) = SchoolDayEntity(day.date, day.firstSeenAt)
    }
}
