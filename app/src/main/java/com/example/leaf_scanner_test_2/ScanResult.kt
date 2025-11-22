package com.example.leaf_scanner_test_2

// ScanResult.kt
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_results")
data class ScanResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imageUri: String,       // uri zdjęcia (z kamery lub z pliku)
    val leafName: String,       // np. "Apple leaf"
    val prediction: String,     // np. "Healthy" / "Diseased"
    val timestamp: Long         // System.currentTimeMillis()
)
