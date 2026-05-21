package com.example.data.local

import androidx.room.*
import com.example.data.model.Staff
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff ORDER BY id ASC")
    fun getAllStaff(): Flow<List<Staff>>

    @Query("SELECT * FROM staff WHERE id = :id")
    suspend fun getStaffById(id: Int): Staff?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaff(staff: Staff): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllStaff(staffList: List<Staff>)

    @Update
    suspend fun updateStaff(staff: Staff)

    @Delete
    suspend fun deleteStaff(staff: Staff)
}
