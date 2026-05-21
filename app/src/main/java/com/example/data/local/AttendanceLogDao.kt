package com.example.data.local

import androidx.room.*
import com.example.data.model.AttendanceLog
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceLogDao {
    @Query("SELECT * FROM attendance_logs ORDER BY dateString DESC, id DESC")
    fun getAllLogs(): Flow<List<AttendanceLog>>

    @Query("SELECT * FROM attendance_logs WHERE dateString = :dateString ORDER BY id ASC")
    fun getLogsForDate(dateString: String): Flow<List<AttendanceLog>>

    @Query("SELECT * FROM attendance_logs WHERE staffId = :staffId ORDER BY dateString DESC")
    fun getLogsForStaff(staffId: Int): Flow<List<AttendanceLog>>

    @Query("SELECT * FROM attendance_logs WHERE staffId = :staffId AND dateString = :dateString LIMIT 1")
    suspend fun getStaffLogForDate(staffId: Int, dateString: String): AttendanceLog?

    @Query("SELECT * FROM attendance_logs WHERE staffId = :staffId AND dateString = :dateString LIMIT 1")
    fun getLiveStaffLogForDate(staffId: Int, dateString: String): Flow<AttendanceLog?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AttendanceLog)

    @Update
    suspend fun updateLog(log: AttendanceLog)

    @Delete
    suspend fun deleteLog(log: AttendanceLog)

    @Query("DELETE FROM attendance_logs")
    suspend fun clearAllLogs()
}
