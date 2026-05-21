package com.example.data.repository

import com.example.data.local.StaffDao
import com.example.data.local.AttendanceLogDao
import com.example.data.local.WorkConfigDao
import com.example.data.model.Staff
import com.example.data.model.AttendanceLog
import com.example.data.model.WorkConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WorkforceRepository(
    private val staffDao: StaffDao,
    private val attendanceLogDao: AttendanceLogDao,
    private val workConfigDao: WorkConfigDao
) {
    val allStaff: Flow<List<Staff>> = staffDao.getAllStaff()
    val allLogs: Flow<List<AttendanceLog>> = attendanceLogDao.getAllLogs()

    // Retrieve default configurations
    suspend fun getShiftStartTime(): Pair<Int, Int> {
        val hourVal = workConfigDao.getConfig("shift_start_hour")?.configValue?.toIntOrNull() ?: 9
        val minVal = workConfigDao.getConfig("shift_start_minute")?.configValue?.toIntOrNull() ?: 0
        return Pair(hourVal, minVal)
    }

    suspend fun getGracePeriodMins(): Int {
        return workConfigDao.getConfig("grace_period_mins")?.configValue?.toIntOrNull() ?: 5
    }

    suspend fun saveShiftConfig(hour: Int, minute: Int, gracePeriod: Int) {
        workConfigDao.insertConfig(WorkConfig("shift_start_hour", hour.toString()))
        workConfigDao.insertConfig(WorkConfig("shift_start_minute", minute.toString()))
        workConfigDao.insertConfig(WorkConfig("grace_period_mins", gracePeriod.toString()))
    }

    // Helper to get today's date formatted as "YYYY-MM-DD"
    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    // Dynamic checks
    fun getLogsForDate(dateString: String): Flow<List<AttendanceLog>> {
        return attendanceLogDao.getLogsForDate(dateString)
    }

    fun getLogsForStaff(staffId: Int): Flow<List<AttendanceLog>> {
        return attendanceLogDao.getLogsForStaff(staffId)
    }

    fun getLiveStaffLogForToday(staffId: Int): Flow<AttendanceLog?> {
        return attendanceLogDao.getLiveStaffLogForDate(staffId, getTodayDateString())
    }

    // Pre-populate 5 staff members if the db table is empty
    suspend fun checkAndPrepopulate() {
        val existing = staffDao.getAllStaff().first()
        if (existing.isEmpty()) {
            val defaults = listOf(
                Staff(name = "Alice Vance", role = "Lead Engineer", avatarColorHex = "#FF10B981", targetArrivalHour = 9, targetArrivalMinute = 0),
                Staff(name = "Bob Miller", role = "UX Architect", avatarColorHex = "#FF3B82F6", targetArrivalHour = 9, targetArrivalMinute = 0),
                Staff(name = "Charlie Song", role = "Senior PM", avatarColorHex = "#FFF59E0B", targetArrivalHour = 9, targetArrivalMinute = 0),
                Staff(name = "Diana Prince", role = "Operations Lead", avatarColorHex = "#FFE11D48", targetArrivalHour = 9, targetArrivalMinute = 0),
                Staff(name = "Ethan Hunt", role = "Security Specialist", avatarColorHex = "#FF8B5CF6", targetArrivalHour = 9, targetArrivalMinute = 0)
            )
            staffDao.insertAllStaff(defaults)
            
            // Default shift config setup
            saveShiftConfig(9, 0, 5)
        }
    }

    /**
     * Attempts to clock-in a staff member with face recognition.
     * Computes late hours & minutes.
     */
    suspend fun clockInStaff(staffId: Int, confidence: Float): AttendanceLog {
        val today = getTodayDateString()
        val calendar = Calendar.getInstance()
        val currentTime = calendar.timeInMillis
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        val staff = staffDao.getStaffById(staffId) ?: throw IllegalArgumentException("Staff member not found")
        val (shiftHour, shiftMin) = getShiftStartTime()
        val gracePeriod = getGracePeriodMins()

        // Calculate if late
        val isLate: Boolean
        val lateMinutes: Int

        // Convert times to relative minutes of the day to easily compare
        val currentMinsOfDay = currentHour * 60 + currentMinute
        val targetMinsOfDay = shiftHour * 60 + shiftMin
        val limitMinsOfDay = targetMinsOfDay + gracePeriod

        if (currentMinsOfDay > limitMinsOfDay) {
            isLate = true
            lateMinutes = currentMinsOfDay - targetMinsOfDay
        } else {
            isLate = false
            lateMinutes = 0
        }

        // Check for existing log of today
        val existingLog = attendanceLogDao.getStaffLogForDate(staffId, today)
        val finalLog = if (existingLog != null) {
            // Already clocked in or marked absent
            existingLog.copy(
                clockInTime = existingLog.clockInTime ?: currentTime,
                isLate = existingLog.clockInTime?.let { existingLog.isLate } ?: isLate,
                lateMinutes = existingLog.clockInTime?.let { existingLog.lateMinutes } ?: lateMinutes,
                isAbsent = false, // clocked in, so no longer absent
                faceScanMatchPercentage = confidence
            )
        } else {
            AttendanceLog(
                staffId = staffId,
                dateString = today,
                clockInTime = currentTime,
                clockOutTime = null,
                isLate = isLate,
                isAbsent = false,
                lateMinutes = lateMinutes,
                faceScanMatchPercentage = confidence
            )
        }

        attendanceLogDao.insertLog(finalLog)
        return finalLog
    }

    /**
     * Clocks out a staff member
     */
    suspend fun clockOutStaff(staffId: Int): AttendanceLog? {
        val today = getTodayDateString()
        val currentTime = System.currentTimeMillis()
        val existingLog = attendanceLogDao.getStaffLogForDate(staffId, today)

        if (existingLog != null) {
            val updatedLog = existingLog.copy(
                clockOutTime = currentTime
            )
            attendanceLogDao.insertLog(updatedLog)
            return updatedLog
        }
        return null
    }

    /**
     * Mark a staff member as absent for today
     */
    suspend fun markAsAbsent(staffId: Int) {
        val today = getTodayDateString()
        val existingLog = attendanceLogDao.getStaffLogForDate(staffId, today)
        
        if (existingLog == null) {
            val log = AttendanceLog(
                staffId = staffId,
                dateString = today,
                clockInTime = null,
                clockOutTime = null,
                isLate = false,
                isAbsent = true,
                lateMinutes = 0,
                faceScanMatchPercentage = 0f
            )
            attendanceLogDao.insertLog(log)
        } else if (existingLog.clockInTime == null) {
            // If already exists and no clock-in time, make sure isAbsent is true
            attendanceLogDao.insertLog(existingLog.copy(isAbsent = true))
        }
    }

    /**
     * Auto check all active staff members and mark them as absent for a date if they
     * haven't clocked in yet.
     */
    suspend fun triggerAbsenteeCheck() {
        val today = getTodayDateString()
        val staffList = staffDao.getAllStaff().first()
        for (staff in staffList) {
            val log = attendanceLogDao.getStaffLogForDate(staff.id, today)
            if (log == null) {
                // Not clocked in, set to absent
                markAsAbsent(staff.id)
            }
        }
    }

    suspend fun clearHistory() {
        attendanceLogDao.clearAllLogs()
    }
}
