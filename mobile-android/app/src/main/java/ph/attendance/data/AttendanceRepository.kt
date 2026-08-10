package ph.attendance.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ph.attendance.data.local.AttendanceDatabase
import ph.attendance.data.local.RecordEntity
import ph.attendance.data.local.ScheduleEntity
import ph.attendance.data.local.SchoolDayEntity
import ph.attendance.data.local.SectionEntity
import ph.attendance.data.local.StudentEntity
import ph.attendance.domain.AttendanceRecord
import ph.attendance.domain.Clocks
import ph.attendance.domain.Gender
import ph.attendance.domain.Id
import ph.attendance.domain.ScanContext
import ph.attendance.domain.Schedule
import ph.attendance.domain.SchoolDay
import ph.attendance.domain.ScheduleDraft
import ph.attendance.domain.ScheduleWindowState
import ph.attendance.domain.Section
import ph.attendance.domain.Student
import ph.attendance.domain.SweepContext
import ph.attendance.domain.TimeWindow
import ph.attendance.domain.Validated
import ph.attendance.domain.absentRecordsFor
import ph.attendance.domain.key
import ph.attendance.domain.newId
import ph.attendance.domain.resolveScan
import ph.attendance.domain.validateSchedule
import ph.attendance.domain.validateStudentNumber
import ph.attendance.domain.validateText
import ph.attendance.domain.windowStateAt

/**
 * The one place the rest of the app reads and writes attendance data.
 *
 * Validation happens here rather than in the screens, so a value that never passed through a form
 * — an imported row, a sync payload, a QR code — is checked by exactly the same rules.
 */
class AttendanceRepository(private val db: AttendanceDatabase) {

    /* ------------------------------------------------------------------ read */

    fun observeSections(): Flow<List<Section>> =
        db.sections().observeActive().map { rows -> rows.map(SectionEntity::toDomain) }

    fun observeStudentCounts(): Flow<Map<String, Int>> =
        db.students().observeCountsBySection().map { rows -> rows.associate { it.sectionId to it.total } }

    fun observeStudents(sectionId: Id): Flow<List<Student>> =
        db.students().observeBySection(sectionId)
            .map { rows -> rows.map(StudentEntity::toDomain).sortedBy { it.displayName } }

    fun observeSchedules(sectionId: Id): Flow<List<Schedule>> =
        db.schedules().observeBySection(sectionId).map { rows -> rows.map(ScheduleEntity::toDomain) }

    fun observeRecords(sectionId: Id, date: String): Flow<List<AttendanceRecord>> =
        db.records().observeBySectionAndDate(sectionId, date)
            .map { rows -> rows.map(RecordEntity::toDomain) }

    fun observeRecordsBetween(sectionId: Id, start: String, end: String): Flow<List<AttendanceRecord>> =
        db.records().observeBySectionBetween(sectionId, start, end)
            .map { rows -> rows.map(RecordEntity::toDomain) }

    suspend fun section(id: Id): Section? = db.sections().byId(id)?.toDomain()

    suspend fun counts(): DataCounts = DataCounts(
        sections = db.sections().count(),
        students = db.students().count(),
        schedules = db.schedules().listActive().size,
        records = db.records().count(),
        schoolDays = db.schoolDays().count(),
    )

    /* --------------------------------------------------------------- sections */

    suspend fun createSection(name: String): Result<Id> {
        val validated = validateText(name, "Section name", 80)
        if (validated is Validated.Invalid) return Result.failure(IllegalArgumentException(validated.message))
        val clean = (validated as Validated.Valid).value

        if (db.sections().countNamed(clean, exceptId = "") > 0) {
            return Result.failure(IllegalStateException("A section named \"$clean\" already exists."))
        }

        val now = Clocks.nowIso()
        val section = Section(newId(), clean, false, now, now)
        db.sections().upsert(listOf(SectionEntity.from(section)))
        return Result.success(section.id)
    }

    suspend fun renameSection(id: Id, name: String): Result<Unit> {
        val validated = validateText(name, "Section name", 80)
        if (validated is Validated.Invalid) return Result.failure(IllegalArgumentException(validated.message))
        val clean = (validated as Validated.Valid).value

        if (db.sections().countNamed(clean, exceptId = id) > 0) {
            return Result.failure(IllegalStateException("A section named \"$clean\" already exists."))
        }

        val existing = db.sections().byId(id) ?: return Result.failure(NoSuchElementException())
        db.sections().upsert(listOf(existing.copy(name = clean, updatedAt = Clocks.nowIso())))
        return Result.success(Unit)
    }

    /**
     * Soft delete. Attendance history references the section, and hard-deleting it would silently
     * rewrite past reports. It is also what makes deletion syncable: a removed row still exists
     * to be replicated.
     */
    suspend fun archiveSection(id: Id) {
        val now = Clocks.nowIso()
        db.sections().archive(id, now)
        db.students().archiveBySection(id, now)
        db.schedules().archiveBySection(id, now)
    }

    /* --------------------------------------------------------------- students */

    suspend fun saveStudent(
        existingId: Id?,
        sectionId: Id,
        studentNumber: String,
        lastName: String,
        firstName: String,
        middleName: String,
        gender: Gender,
    ): Result<Id> {
        val number = validateStudentNumber(studentNumber)
        if (number is Validated.Invalid) return Result.failure(IllegalArgumentException(number.message))

        val last = validateText(lastName, "Last name", 60)
        if (last is Validated.Invalid) return Result.failure(IllegalArgumentException(last.message))

        val first = validateText(firstName, "First name", 60)
        if (first is Validated.Invalid) return Result.failure(IllegalArgumentException(first.message))

        val middle = validateText(middleName, "Middle name", 60, required = false)
        if (middle is Validated.Invalid) return Result.failure(IllegalArgumentException(middle.message))

        val cleanNumber = (number as Validated.Valid).value
        if (db.students().countWithNumber(cleanNumber, exceptId = existingId.orEmpty()) > 0) {
            return Result.failure(
                IllegalStateException("Student number \"$cleanNumber\" is already in use."),
            )
        }

        val now = Clocks.nowIso()
        val id = existingId ?: newId()
        val createdAt = existingId?.let { db.students().all().firstOrNull { row -> row.id == it }?.createdAt } ?: now

        val student = Student(
            id = id,
            sectionId = sectionId,
            studentNumber = cleanNumber,
            lastName = (last as Validated.Valid).value,
            firstName = (first as Validated.Valid).value,
            middleName = (middle as Validated.Valid).value,
            gender = gender,
            archived = false,
            createdAt = createdAt,
            updatedAt = now,
        )
        db.students().upsert(listOf(StudentEntity.from(student)))
        return Result.success(id)
    }

    suspend fun archiveStudent(id: Id) = db.students().archive(id, Clocks.nowIso())

    /* -------------------------------------------------------------- schedules */

    suspend fun saveSchedule(
        existingId: Id?,
        sectionId: Id,
        draft: ScheduleDraft,
    ): Result<Id> {
        val validated = validateSchedule(draft)
        if (validated is Validated.Invalid) return Result.failure(IllegalArgumentException(validated.message))
        val clean = (validated as Validated.Valid).value

        val now = Clocks.nowIso()
        val id = existingId ?: newId()
        val createdAt = existingId?.let { db.schedules().all().firstOrNull { row -> row.id == it }?.createdAt } ?: now

        val schedule = Schedule(
            id = id,
            sectionId = sectionId,
            title = clean.title,
            venue = clean.venue,
            present = clean.present,
            late = clean.late,
            archived = false,
            createdAt = createdAt,
            updatedAt = now,
        )
        db.schedules().upsert(listOf(ScheduleEntity.from(schedule)))
        return Result.success(id)
    }

    suspend fun archiveSchedule(id: Id) = db.schedules().archive(id, Clocks.nowIso())

    /* ------------------------------------------------------------- attendance */

    suspend fun markSchoolDay(date: String) {
        db.schoolDays().insertIgnoringDuplicates(
            listOf(SchoolDayEntity.from(SchoolDay(date, Clocks.nowIso()))),
        )
    }

    /**
     * The whole scan path, from raw camera payload to stored records.
     *
     * Validation is unconditional and comes first: anyone can print a QR code and hold it to the
     * camera, so nothing reaches a query until it has passed the student-number rules.
     */
    suspend fun recordScan(payload: String): ScanOutcome {
        val number = validateStudentNumber(payload)
        if (number is Validated.Invalid) return ScanOutcome.Malformed(number.message)
        val studentNumber = (number as Validated.Valid).value

        val student = db.students().activeByNumber(studentNumber)?.toDomain()
            ?: return ScanOutcome.Unknown(studentNumber)

        val date = Clocks.today()
        markSchoolDay(date)

        val schedules = db.schedules().listBySection(student.sectionId).map(ScheduleEntity::toDomain)
        val atMinutes = Clocks.minutesOfDay()
        val existing = db.records().listByDate(date).map { it.toDomain().key() }.toSet()

        val resolution = resolveScan(
            ScanContext(studentNumber, schedules, existing, date, atMinutes, Clocks.nowIso()),
        )

        if (resolution.created.isNotEmpty()) {
            val written = append(resolution.created)
            // Zero means the unique index rejected everything, which can only happen if another
            // writer got there first. To the operator that is a duplicate scan.
            return if (written == 0) ScanOutcome.Duplicate(student)
            else ScanOutcome.Recorded(student, resolution.created)
        }

        if (resolution.duplicateScheduleIds.isNotEmpty()) return ScanOutcome.Duplicate(student)

        return ScanOutcome.Closed(student, schedules.map { windowStateAt(it, atMinutes) })
    }

    /**
     * Marks absent everyone who never scanned, for every schedule whose late window has closed.
     *
     * Safe at any time: open schedules are skipped and recorded students are skipped, so repeated
     * runs converge rather than piling up duplicates.
     */
    suspend fun sweepAbsentees(): Int {
        val date = Clocks.today()
        val atMinutes = Clocks.minutesOfDay()
        var written = 0

        for (schedule in db.schedules().listActive().map(ScheduleEntity::toDomain)) {
            if (windowStateAt(schedule, atMinutes) != ScheduleWindowState.CLOSED) continue

            val roster = db.students().listBySection(schedule.sectionId)
                .map { it.studentNumber }
            if (roster.isEmpty()) continue

            val existing = db.records().listByScheduleAndDate(schedule.id, date)
                .map { it.toDomain().key() }
                .toSet()

            written += append(
                absentRecordsFor(
                    SweepContext(schedule, roster, existing, date, atMinutes, Clocks.nowIso()),
                ),
            )
        }

        return written
    }

    /** Called when the app opens: registers today and settles anything left from a closed window. */
    suspend fun openDay() {
        markSchoolDay(Clocks.today())
        sweepAbsentees()
    }

    /** Returns how many rows actually landed; the index silently drops the rest. */
    suspend fun append(records: List<AttendanceRecord>): Int {
        if (records.isEmpty()) return 0
        val ids = db.records().insertIgnoringDuplicates(records.map(RecordEntity::from))
        return ids.count { it != -1L }
    }

    /* ------------------------------------------------------------------ bulk */

    suspend fun replaceAll(
        sections: List<Section>,
        students: List<Student>,
        schedules: List<Schedule>,
        records: List<AttendanceRecord>,
        schoolDays: List<SchoolDay>,
    ) {
        db.sections().clear()
        db.students().clear()
        db.schedules().clear()
        db.records().clear()
        db.schoolDays().clear()

        db.sections().upsert(sections.map(SectionEntity::from))
        db.students().upsert(students.map(StudentEntity::from))
        db.schedules().upsert(schedules.map(ScheduleEntity::from))
        db.records().insertIgnoringDuplicates(records.map(RecordEntity::from))
        db.schoolDays().insertIgnoringDuplicates(schoolDays.map(SchoolDayEntity::from))
    }

    suspend fun eraseEverything() = replaceAll(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())

    suspend fun snapshot(): Snapshot = Snapshot(
        sections = db.sections().all().map(SectionEntity::toDomain),
        students = db.students().all().map(StudentEntity::toDomain),
        schedules = db.schedules().all().map(ScheduleEntity::toDomain),
        records = db.records().all().map(RecordEntity::toDomain),
        schoolDays = db.schoolDays().all().map(SchoolDayEntity::toDomain),
    )

    suspend fun changedSince(since: String): Snapshot = Snapshot(
        sections = db.sections().changedSince(since).map(SectionEntity::toDomain),
        students = db.students().changedSince(since).map(StudentEntity::toDomain),
        schedules = db.schedules().changedSince(since).map(ScheduleEntity::toDomain),
        records = db.records().changedSince(since).map(RecordEntity::toDomain),
        schoolDays = db.schoolDays().changedSince(since).map(SchoolDayEntity::toDomain),
    )

    suspend fun sectionsById(): Map<Id, SectionEntity> = db.sections().all().associateBy { it.id }
    suspend fun studentsById(): Map<Id, StudentEntity> = db.students().all().associateBy { it.id }
    suspend fun schedulesById(): Map<Id, ScheduleEntity> = db.schedules().all().associateBy { it.id }

    suspend fun upsertSections(sections: List<Section>) =
        db.sections().upsert(sections.map(SectionEntity::from))

    suspend fun upsertStudents(students: List<Student>) =
        db.students().upsert(students.map(StudentEntity::from))

    suspend fun upsertSchedules(schedules: List<Schedule>) =
        db.schedules().upsert(schedules.map(ScheduleEntity::from))

    suspend fun insertSchoolDays(days: List<SchoolDay>): Int =
        db.schoolDays().insertIgnoringDuplicates(days.map(SchoolDayEntity::from)).count { it != -1L }
}

data class DataCounts(
    val sections: Int,
    val students: Int,
    val schedules: Int,
    val records: Int,
    val schoolDays: Int,
)

data class Snapshot(
    val sections: List<Section>,
    val students: List<Student>,
    val schedules: List<Schedule>,
    val records: List<AttendanceRecord>,
    val schoolDays: List<SchoolDay>,
) {
    val size: Int get() = sections.size + students.size + schedules.size + records.size + schoolDays.size
    val isEmpty: Boolean get() = size == 0
}

sealed interface ScanOutcome {
    /** The payload was not a well-formed student number. Nothing was looked up. */
    data class Malformed(val reason: String) : ScanOutcome

    /** Well formed, but no active student carries that number. */
    data class Unknown(val studentNumber: String) : ScanOutcome

    /** The student exists, but no schedule in their section is collecting scans right now. */
    data class Closed(val student: Student, val states: List<ScheduleWindowState>) : ScanOutcome

    /** Already recorded for every open schedule — a second scan changes nothing. */
    data class Duplicate(val student: Student) : ScanOutcome

    data class Recorded(val student: Student, val records: List<AttendanceRecord>) : ScanOutcome
}

/** Convenience for the schedule form, which edits four times as plain strings. */
fun scheduleDraft(title: String, venue: String, presentStart: String, presentEnd: String, lateStart: String, lateEnd: String) =
    ScheduleDraft(title, venue, TimeWindow(presentStart, presentEnd), TimeWindow(lateStart, lateEnd))
