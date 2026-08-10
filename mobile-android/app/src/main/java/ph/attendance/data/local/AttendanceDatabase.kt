package ph.attendance.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SectionEntity::class,
        StudentEntity::class,
        ScheduleEntity::class,
        RecordEntity::class,
        SchoolDayEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AttendanceDatabase : RoomDatabase() {
    abstract fun sections(): SectionDao
    abstract fun students(): StudentDao
    abstract fun schedules(): ScheduleDao
    abstract fun records(): RecordDao
    abstract fun schoolDays(): SchoolDayDao

    companion object {
        fun build(context: Context): AttendanceDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AttendanceDatabase::class.java,
                "attendance.db",
            )
                // No fallbackToDestructiveMigration. This database holds a term of attendance;
                // silently dropping it on a schema change would be the worst possible failure,
                // so a future version must ship a real migration or fail loudly in testing.
                .build()
    }
}
