package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "staff")
data class Staff(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val role: String,
    val avatarColorHex: String, // Hex color for customized stylish profiles
    val targetArrivalHour: Int = 9,
    val targetArrivalMinute: Int = 0
)
