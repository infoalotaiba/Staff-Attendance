package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "work_configs")
data class WorkConfig(
    @PrimaryKey val configKey: String,
    val configValue: String
)
