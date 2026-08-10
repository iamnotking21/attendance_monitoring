package ph.attendance.sync

import ph.attendance.data.AttendanceRepository
import ph.attendance.data.SettingsStore
import ph.attendance.data.Snapshot
import ph.attendance.domain.AttendanceRecord
import ph.attendance.domain.Clocks
import ph.attendance.domain.Schedule
import ph.attendance.domain.SchoolDay
import ph.attendance.domain.Section
import ph.attendance.domain.Student
import ph.attendance.domain.incomingWins

/**
 * Replication, from the device's side.
 *
 * Push first, then pull. In that order a local change is on the server before anything can arrive
 * to compete with it, so last-write-wins decides between two complete versions rather than
 * adjudicating a conflict that had not finished happening.
 *
 * Nothing here is required for the app to work: every screen reads Room, and sync is a background
 * reconciliation on top. An aeroplane, a dead access point, and a server with no database all
 * degrade to "works exactly as before, on this device".
 */
class SyncEngine(
    private val repository: AttendanceRepository,
    private val settings: SettingsStore,
    private val api: SyncApi,
) {

    data class Outcome(val pushed: Int, val pulled: Int, val cursor: Long, val at: String)

    sealed interface Result {
        data class Success(val outcome: Outcome) : Result
        data class Failure(val failure: SyncFailure) : Result
    }

    suspend fun syncNow(): Result {
        val connection = settings.currentConnection()
            ?: return Result.Failure(
                SyncFailure.Unauthorized("This device is not connected to a workspace."),
            )

        return try {
            val startedAt = Clocks.nowIso()
            val pushed = pushLocalChanges(connection.token, startedAt)
            val pulled = pullRemoteChanges(connection.token)

            val at = Clocks.nowIso()
            settings.saveLastSyncedAt(at)

            Result.Success(Outcome(pushed, pulled.first, pulled.second, at))
        } catch (error: SyncException) {
            Result.Failure(error.failure)
        }
    }

    /* --------------------------------------------------------------------- push */

    private suspend fun pushLocalChanges(token: String, startedAt: String): Int {
        val watermark = settings.pushWatermark()
        val outgoing = repository.changedSince(watermark)
        if (outgoing.isEmpty) {
            settings.savePushWatermark(startedAt)
            return 0
        }

        var sent = 0
        for (batch in outgoing.toBatches(BATCH_SIZE)) {
            api.push(token, batch)
            sent += batch.size
        }

        // Advanced only after every batch has landed. If the connection drops halfway the
        // watermark stays put and the whole set is offered again; the server deduplicates, so
        // re-sending is cheap and losing a change is not possible.
        settings.savePushWatermark(startedAt)
        return sent
    }

    private fun Snapshot.toBatches(size: Int): List<ChangeSet> {
        val batches = mutableListOf<ChangeSet>()
        var current = ChangeSet()
        var count = 0

        fun flushIfFull() {
            if (count >= size) {
                batches += current
                current = ChangeSet()
                count = 0
            }
        }

        for (row in sections) {
            current = current.copy(sections = current.sections + WireSection.from(row)); count++; flushIfFull()
        }
        for (row in schedules) {
            current = current.copy(schedules = current.schedules + WireSchedule.from(row)); count++; flushIfFull()
        }
        for (row in students) {
            current = current.copy(students = current.students + WireStudent.from(row)); count++; flushIfFull()
        }
        for (row in records) {
            current = current.copy(records = current.records + WireRecord.from(row)); count++; flushIfFull()
        }
        for (row in schoolDays) {
            current = current.copy(schoolDays = current.schoolDays + WireSchoolDay.from(row)); count++; flushIfFull()
        }

        if (count > 0) batches += current
        return batches
    }

    /* --------------------------------------------------------------------- pull */

    private suspend fun pullRemoteChanges(token: String): Pair<Int, Long> {
        var cursor = settings.cursor()
        var applied = 0

        // Bounded so a corrupt cursor cannot spin forever. 200 pages of 500 rows is far more than
        // any school will ever hold.
        repeat(MAX_PAGES) {
            val response = api.pull(token, cursor, PAGE_SIZE)
            applied += applyChanges(response.changes)

            cursor = response.cursor
            settings.saveCursor(cursor)

            if (!response.hasMore) return applied to cursor
        }

        return applied to cursor
    }

    /**
     * Merges a server change set into local storage.
     *
     * Every row is compared against what is already here and an older edit is dropped rather than
     * applied. It is the same rule the server uses, so both sides converge without having to
     * agree in advance about who is authoritative.
     */
    suspend fun applyChanges(changes: ChangeSet): Int {
        var applied = 0

        val existingSections = repository.sectionsById()
        val sections = mutableListOf<Section>()
        for (wire in changes.sections) {
            val incoming = wire.toDomain()
            val existing = existingSections[incoming.id]
            if (existing == null ||
                incomingWins(incoming.updatedAt, incoming.id, existing.updatedAt, existing.id)
            ) {
                sections += incoming
            }
        }
        if (sections.isNotEmpty()) {
            repository.upsertSections(sections)
            applied += sections.size
        }

        val existingSchedules = repository.schedulesById()
        val schedules = mutableListOf<Schedule>()
        for (wire in changes.schedules) {
            val incoming = wire.toDomain()
            val existing = existingSchedules[incoming.id]
            if (existing == null ||
                incomingWins(incoming.updatedAt, incoming.id, existing.updatedAt, existing.id)
            ) {
                schedules += incoming
            }
        }
        if (schedules.isNotEmpty()) {
            repository.upsertSchedules(schedules)
            applied += schedules.size
        }

        val existingStudents = repository.studentsById()
        val students = mutableListOf<Student>()
        for (wire in changes.students) {
            val incoming = wire.toDomain()
            val existing = existingStudents[incoming.id]
            if (existing == null ||
                incomingWins(incoming.updatedAt, incoming.id, existing.updatedAt, existing.id)
            ) {
                students += incoming
            }
        }
        if (students.isNotEmpty()) {
            repository.upsertStudents(students)
            applied += students.size
        }

        // Records are append-only. The unique index on (studentNumber, scheduleId, date) is what
        // recognises a record this device already holds under a different id, so no comparison is
        // needed here — the insert simply reports how many were genuinely new.
        val records: List<AttendanceRecord> = changes.records.map(WireRecord::toDomain)
        if (records.isNotEmpty()) applied += repository.append(records)

        val days: List<SchoolDay> = changes.schoolDays.map(WireSchoolDay::toDomain)
        if (days.isNotEmpty()) applied += repository.insertSchoolDays(days)

        return applied
    }

    private companion object {
        const val BATCH_SIZE = 500
        const val PAGE_SIZE = 500
        const val MAX_PAGES = 200
    }
}
