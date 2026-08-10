package ph.attendance.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SectionDao {
    @Query("SELECT * FROM sections WHERE archived = 0 ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<SectionEntity>>

    @Query("SELECT * FROM sections WHERE archived = 0 ORDER BY name COLLATE NOCASE")
    suspend fun listActive(): List<SectionEntity>

    @Query("SELECT * FROM sections WHERE id = :id")
    suspend fun byId(id: String): SectionEntity?

    @Query("SELECT COUNT(*) FROM sections")
    suspend fun count(): Int

    /** Case-insensitive, and blind to the row being renamed so saving an unchanged name works. */
    @Query(
        "SELECT COUNT(*) FROM sections WHERE archived = 0 AND id != :exceptId " +
            "AND name = :name COLLATE NOCASE",
    )
    suspend fun countNamed(name: String, exceptId: String): Int

    @Upsert
    suspend fun upsert(sections: List<SectionEntity>)

    @Query("UPDATE sections SET archived = 1, updatedAt = :at WHERE id = :id")
    suspend fun archive(id: String, at: String)

    @Query("SELECT * FROM sections WHERE updatedAt > :since")
    suspend fun changedSince(since: String): List<SectionEntity>

    @Query("SELECT * FROM sections")
    suspend fun all(): List<SectionEntity>

    @Query("DELETE FROM sections")
    suspend fun clear()
}

@Dao
interface StudentDao {
    @Query("SELECT * FROM students WHERE sectionId = :sectionId AND archived = 0")
    fun observeBySection(sectionId: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE sectionId = :sectionId AND archived = 0")
    suspend fun listBySection(sectionId: String): List<StudentEntity>

    @Query("SELECT * FROM students WHERE archived = 0")
    suspend fun listActive(): List<StudentEntity>

    @Query("SELECT * FROM students WHERE studentNumber = :studentNumber AND archived = 0 LIMIT 1")
    suspend fun activeByNumber(studentNumber: String): StudentEntity?

    @Query(
        "SELECT COUNT(*) FROM students WHERE archived = 0 AND id != :exceptId " +
            "AND studentNumber = :studentNumber",
    )
    suspend fun countWithNumber(studentNumber: String, exceptId: String): Int

    @Query("SELECT sectionId, COUNT(*) AS total FROM students WHERE archived = 0 GROUP BY sectionId")
    fun observeCountsBySection(): Flow<List<SectionCount>>

    @Upsert
    suspend fun upsert(students: List<StudentEntity>)

    @Query("UPDATE students SET archived = 1, updatedAt = :at WHERE id = :id")
    suspend fun archive(id: String, at: String)

    @Query("UPDATE students SET archived = 1, updatedAt = :at WHERE sectionId = :sectionId")
    suspend fun archiveBySection(sectionId: String, at: String)

    @Query("SELECT * FROM students WHERE updatedAt > :since")
    suspend fun changedSince(since: String): List<StudentEntity>

    @Query("SELECT * FROM students")
    suspend fun all(): List<StudentEntity>

    @Query("SELECT COUNT(*) FROM students")
    suspend fun count(): Int

    @Query("DELETE FROM students")
    suspend fun clear()
}

data class SectionCount(val sectionId: String, val total: Int)

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules WHERE sectionId = :sectionId AND archived = 0 ORDER BY presentStart")
    fun observeBySection(sectionId: String): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE sectionId = :sectionId AND archived = 0 ORDER BY presentStart")
    suspend fun listBySection(sectionId: String): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE archived = 0 ORDER BY presentStart")
    suspend fun listActive(): List<ScheduleEntity>

    @Upsert
    suspend fun upsert(schedules: List<ScheduleEntity>)

    @Query("UPDATE schedules SET archived = 1, updatedAt = :at WHERE id = :id")
    suspend fun archive(id: String, at: String)

    @Query("UPDATE schedules SET archived = 1, updatedAt = :at WHERE sectionId = :sectionId")
    suspend fun archiveBySection(sectionId: String, at: String)

    @Query("SELECT * FROM schedules WHERE updatedAt > :since")
    suspend fun changedSince(since: String): List<ScheduleEntity>

    @Query("SELECT * FROM schedules")
    suspend fun all(): List<ScheduleEntity>

    @Query("DELETE FROM schedules")
    suspend fun clear()
}

@Dao
interface RecordDao {
    @Query("SELECT * FROM attendance_records WHERE sectionId = :sectionId AND date = :date")
    fun observeBySectionAndDate(sectionId: String, date: String): Flow<List<RecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE sectionId = :sectionId AND date BETWEEN :start AND :end")
    fun observeBySectionBetween(sectionId: String, start: String, end: String): Flow<List<RecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE date = :date")
    suspend fun listByDate(date: String): List<RecordEntity>

    @Query("SELECT * FROM attendance_records WHERE scheduleId = :scheduleId AND date = :date")
    suspend fun listByScheduleAndDate(scheduleId: String, date: String): List<RecordEntity>

    /**
     * IGNORE rather than REPLACE. Records are append-only and the unique index is what stops a
     * duplicate; replacing would let a later scan silently overwrite an earlier, correct one.
     * The returned row ids say how many actually landed — -1 means the index rejected it.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringDuplicates(records: List<RecordEntity>): List<Long>

    @Query("SELECT * FROM attendance_records WHERE recordedAt > :since")
    suspend fun changedSince(since: String): List<RecordEntity>

    @Query("SELECT * FROM attendance_records")
    suspend fun all(): List<RecordEntity>

    @Query("SELECT COUNT(*) FROM attendance_records")
    suspend fun count(): Int

    @Query("DELETE FROM attendance_records")
    suspend fun clear()
}

@Dao
interface SchoolDayDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringDuplicates(days: List<SchoolDayEntity>): List<Long>

    @Query("SELECT * FROM school_days ORDER BY date")
    suspend fun all(): List<SchoolDayEntity>

    @Query("SELECT * FROM school_days WHERE firstSeenAt > :since")
    suspend fun changedSince(since: String): List<SchoolDayEntity>

    @Query("SELECT COUNT(*) FROM school_days")
    suspend fun count(): Int

    @Query("DELETE FROM school_days")
    suspend fun clear()
}
