package ph.attendance.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ph.attendance.domain.AttendanceRecord
import ph.attendance.domain.AttendanceStatus
import ph.attendance.domain.Gender
import ph.attendance.domain.Schedule
import ph.attendance.domain.SchoolDay
import ph.attendance.domain.Section
import ph.attendance.domain.Student
import ph.attendance.domain.TimeWindow

/**
 * The wire contract, matching `backend/src/protocol.ts` field for field.
 *
 * Deliberately separate from the domain types. The local model is free to change whenever the app
 * wants; the wire format cannot, because a phone that has not been updated in a month still has
 * to sync with a server the web client also talks to.
 */

const val PROTOCOL_VERSION = 1

@Serializable
data class WireSection(
    val id: String,
    val name: String,
    val archived: Boolean,
    val createdAt: String,
    val updatedAt: String,
) {
    fun toDomain() = Section(id, name, archived, createdAt, updatedAt)

    companion object {
        fun from(s: Section) = WireSection(s.id, s.name, s.archived, s.createdAt, s.updatedAt)
    }
}

@Serializable
data class WireStudent(
    val id: String,
    val sectionId: String,
    val studentNumber: String,
    val lastName: String,
    val firstName: String,
    val middleName: String = "",
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
        fun from(s: Student) = WireStudent(
            s.id, s.sectionId, s.studentNumber, s.lastName, s.firstName, s.middleName,
            s.gender.wire, s.archived, s.createdAt, s.updatedAt,
        )
    }
}

@Serializable
data class WireWindow(val start: String, val end: String)

@Serializable
data class WireSchedule(
    val id: String,
    val sectionId: String,
    val title: String,
    val venue: String = "",
    val present: WireWindow,
    val late: WireWindow,
    val archived: Boolean,
    val createdAt: String,
    val updatedAt: String,
) {
    fun toDomain() = Schedule(
        id, sectionId, title, venue,
        TimeWindow(present.start, present.end), TimeWindow(late.start, late.end),
        archived, createdAt, updatedAt,
    )

    companion object {
        fun from(s: Schedule) = WireSchedule(
            s.id, s.sectionId, s.title, s.venue,
            WireWindow(s.present.start, s.present.end), WireWindow(s.late.start, s.late.end),
            s.archived, s.createdAt, s.updatedAt,
        )
    }
}

@Serializable
data class WireRecord(
    val id: String,
    val scheduleId: String,
    val sectionId: String,
    val studentNumber: String,
    val date: String,
    val status: String,
    val scheduleTitle: String = "",
    val recordedAt: String,
) {
    fun toDomain() = AttendanceRecord(
        id, scheduleId, sectionId, studentNumber, date,
        AttendanceStatus.fromWire(status), scheduleTitle, recordedAt,
    )

    companion object {
        fun from(r: AttendanceRecord) = WireRecord(
            r.id, r.scheduleId, r.sectionId, r.studentNumber, r.date,
            r.status.wire, r.scheduleTitle, r.recordedAt,
        )
    }
}

@Serializable
data class WireSchoolDay(val date: String, val firstSeenAt: String) {
    fun toDomain() = SchoolDay(date, firstSeenAt)

    companion object {
        fun from(d: SchoolDay) = WireSchoolDay(d.date, d.firstSeenAt)
    }
}

@Serializable
data class ChangeSet(
    val sections: List<WireSection> = emptyList(),
    val students: List<WireStudent> = emptyList(),
    val schedules: List<WireSchedule> = emptyList(),
    val records: List<WireRecord> = emptyList(),
    val schoolDays: List<WireSchoolDay> = emptyList(),
) {
    val size: Int
        get() = sections.size + students.size + schedules.size + records.size + schoolDays.size
}

@Serializable
data class CreateWorkspaceRequest(val name: String)

@Serializable
data class JoinWorkspaceRequest(val joinCode: String)

@Serializable
data class WorkspaceResponse(
    val workspaceId: String,
    val name: String,
    val joinCode: String? = null,
    val token: String,
)

@Serializable
data class PullRequest(val since: Long = 0, val limit: Int = 500)

@Serializable
data class PullResponse(
    val changes: ChangeSet,
    val cursor: Long,
    val hasMore: Boolean,
    val serverTime: String,
)

@Serializable
data class PushRequest(val changes: ChangeSet)

@Serializable
data class PushResponse(
    val applied: Int,
    val skipped: Int,
    val cursor: Long,
    val serverTime: String,
)

@Serializable
data class SyncStatusResponse(
    val configured: Boolean,
    @SerialName("protocol") val protocolVersion: Int = PROTOCOL_VERSION,
)

@Serializable
data class ApiError(val error: String, val code: String)
