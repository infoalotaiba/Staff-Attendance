package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_logs")
data class AttendanceLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val staffId: Int,
    val dateString: String, // format "YYYY-MM-DD"
    val clockInTime: Long?, // Timestamp clock-in
    val clockOutTime: Long?, // Timestamp clock-out
    val isLate: Boolean,
    val isAbsent: Boolean,
    val lateMinutes: Int,
    val faceScanMatchPercentage: Float // Dynamic biometric metrics
)
