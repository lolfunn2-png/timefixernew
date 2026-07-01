package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_logs")
data class SyncLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val action: String, // e.g., "Ping", "NTP Sync", "Manual Sync", "Boot Hook"
    val status: String, // "SUCCESS", "FAILED", "PENDING", "INFO"
    val message: String
)
