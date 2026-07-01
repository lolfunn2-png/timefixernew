package com.example.data.repository

import com.example.data.local.SyncLogDao
import com.example.data.model.SyncLog
import kotlinx.coroutines.flow.Flow

class SyncLogRepository(private val syncLogDao: SyncLogDao) {
    val latestLogs: Flow<List<SyncLog>> = syncLogDao.getLatestLogs()

    suspend fun insertLog(log: SyncLog) {
        syncLogDao.insertLog(log)
    }

    suspend fun clearLogs() {
        syncLogDao.clearAllLogs()
    }
}
