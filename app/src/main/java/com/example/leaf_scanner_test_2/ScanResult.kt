package com.example.leaf_scanner_test_2

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_results")
data class ScanResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imageUri: String,
    val leafName: String,
    val prediction: String,
    val timestamp: Long
)
