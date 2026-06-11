package com.example

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "calibration_records")
data class CalibrationRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val flickSpeed: Float, // px/ms
    val calculatedDpi: Int, // DPI tuning value at time of click
    val game: String, // Free Fire or Free Fire Max
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface CalibrationDao {
    @Query("SELECT * FROM calibration_records ORDER BY timestamp DESC")
    fun getAllRecordsFlow(): Flow<List<CalibrationRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: CalibrationRecord)

    @Query("DELETE FROM calibration_records")
    suspend fun clearAllRecords()
}

@Database(entities = [CalibrationRecord::class], version = 1, exportSchema = false)
abstract class CalibrationDatabase : RoomDatabase() {
    abstract fun calibrationDao(): CalibrationDao

    companion object {
        @Volatile
        private var INSTANCE: CalibrationDatabase? = null

        fun getDatabase(context: Context): CalibrationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CalibrationDatabase::class.java,
                    "calibration_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class CalibrationRepository(private val calibrationDao: CalibrationDao) {
    val allRecords: Flow<List<CalibrationRecord>> = calibrationDao.getAllRecordsFlow()

    suspend fun insert(record: CalibrationRecord) {
        calibrationDao.insertRecord(record)
    }

    suspend fun clear() {
        calibrationDao.clearAllRecords()
    }
}
