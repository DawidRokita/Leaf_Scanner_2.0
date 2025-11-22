package com.example.leaf_scanner_test_2

// ScanResultDao.kt
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanResultDao {

    @Insert
    suspend fun insert(result: ScanResult): Long

    @Query("SELECT * FROM scan_results ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ScanResult>>

    @Query("SELECT * FROM scan_results WHERE id = :id")
    suspend fun getById(id: Long): ScanResult?

    @Delete
    fun delete(result: ScanResult)
}
