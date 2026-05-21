package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.model.Staff
import com.example.data.model.AttendanceLog
import com.example.data.model.WorkConfig

@Database(
    entities = [Staff::class, AttendanceLog::class, WorkConfig::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun staffDao(): StaffDao
    abstract fun attendanceLogDao(): AttendanceLogDao
    abstract fun workConfigDao(): WorkConfigDao
}
