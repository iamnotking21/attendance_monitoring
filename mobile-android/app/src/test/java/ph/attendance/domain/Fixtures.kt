package ph.attendance.domain

/** Fixed ids rather than generated ones, so a failing assertion prints something stable. */
object Ids {
    const val SECTION = "11111111-1111-4111-8111-111111111111"
    const val SECTION_B = "11111111-1111-4111-8111-222222222222"
    const val SCHEDULE = "22222222-2222-4222-8222-111111111111"
    const val SCHEDULE_B = "22222222-2222-4222-8222-222222222222"
    const val STUDENT = "33333333-3333-4333-8333-111111111111"
    const val RECORD = "44444444-4444-4444-8444-111111111111"
}

const val CREATED_AT = "2024-03-01T00:00:00Z"
const val DATE = "2024-03-15"
const val RECORDED_AT = "2024-03-15T07:15:00Z"

/** Present 07:00–07:30, late 07:30–08:00 — the shape the legacy app shipped with. */
fun schedule(
    id: Id = Ids.SCHEDULE,
    sectionId: Id = Ids.SECTION,
    title: String = "Morning Assembly",
    present: TimeWindow = TimeWindow("07:00", "07:30"),
    late: TimeWindow = TimeWindow("07:30", "08:00"),
    archived: Boolean = false,
    updatedAt: String = CREATED_AT,
) = Schedule(id, sectionId, title, "Quadrangle", present, late, archived, CREATED_AT, updatedAt)

fun student(
    id: Id = Ids.STUDENT,
    studentNumber: String = "2024-1001",
    lastName: String = "Dela Cruz",
    firstName: String = "Juan",
    middleName: String = "Ramos",
    gender: Gender = Gender.MALE,
    archived: Boolean = false,
) = Student(
    id = id,
    sectionId = Ids.SECTION,
    studentNumber = studentNumber,
    lastName = lastName,
    firstName = firstName,
    middleName = middleName,
    gender = gender,
    archived = archived,
    createdAt = CREATED_AT,
    updatedAt = CREATED_AT,
)

fun record(
    studentNumber: String = "2024-1001",
    status: AttendanceStatus = AttendanceStatus.PRESENT,
    date: String = DATE,
    id: Id = Ids.RECORD,
    scheduleId: Id = Ids.SCHEDULE,
) = AttendanceRecord(
    id = id,
    scheduleId = scheduleId,
    sectionId = Ids.SECTION,
    studentNumber = studentNumber,
    date = date,
    status = status,
    scheduleTitle = "Morning Assembly",
    recordedAt = RECORDED_AT,
)

object At {
    const val BEFORE = 6 * 60 + 59
    const val PRESENT_START = 7 * 60
    const val PRESENT_MIDDLE = 7 * 60 + 15
    const val PRESENT_END = 7 * 60 + 30
    const val LATE_MIDDLE = 7 * 60 + 45
    const val LATE_END = 8 * 60
    const val AFTER = 8 * 60 + 1
}
