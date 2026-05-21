package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.WorkConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkConfigDao {
    @Query("SELECT * FROM work_configs WHERE configKey = :key LIMIT 1")
    suspend fun getConfig(key: String): WorkConfig?

    @Query("SELECT * FROM work_configs WHERE configKey = :key LIMIT 1")
    fun getConfigFlow(key: String): Flow<WorkConfig?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: WorkConfig)
}
