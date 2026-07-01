package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.XposedChecker
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesHelper
import com.example.data.local.SyncConfig
import com.example.data.model.SyncLog
import com.example.data.repository.SyncLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SyncLogRepository
    private val prefsHelper = PreferencesHelper(application)

    val logListState: StateFlow<List<SyncLog>>
    
    private val _configState = MutableStateFlow(prefsHelper.getSyncConfig())
    val configState: StateFlow<SyncConfig> = _configState.asStateFlow()

    private val _isModuleActiveState = MutableStateFlow(XposedChecker.isModuleActive())
    val isModuleActiveState: StateFlow<Boolean> = _isModuleActiveState.asStateFlow()

    private val _isInternetAvailableState = MutableStateFlow<Boolean?>(null)
    val isInternetAvailableState: StateFlow<Boolean?> = _isInternetAvailableState.asStateFlow()

    private val _pingLatencyState = MutableStateFlow("N/A")
    val pingLatencyState: StateFlow<String> = _pingLatencyState.asStateFlow()

    private val _isSyncingState = MutableStateFlow(false)
    val isSyncingState: StateFlow<Boolean> = _isSyncingState.asStateFlow()

    private val _currentTimeState = MutableStateFlow("")
    val currentTimeState: StateFlow<String> = _currentTimeState.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = SyncLogRepository(database.syncLogDao())
        
        logListState = repository.latestLogs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Start dynamic clock updater
        viewModelScope.launch {
            while (true) {
                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                _currentTimeState.value = formatter.format(Date())
                delay(1000)
            }
        }

        // Run initial diagnostics
        refreshDiagnostics()
    }

    fun refreshDiagnostics() {
        viewModelScope.launch(Dispatchers.IO) {
            _isModuleActiveState.value = XposedChecker.isModuleActive()
            
            try {
                val startTime = System.currentTimeMillis()
                val process = Runtime.getRuntime().exec("ping -c 1 8.8.8.8")
                val exitCode = process.waitFor()
                val endTime = System.currentTimeMillis()
                
                if (exitCode == 0) {
                    _isInternetAvailableState.value = true
                    _pingLatencyState.value = "${endTime - startTime} ms"
                } else {
                    _isInternetAvailableState.value = false
                    _pingLatencyState.value = "Fail"
                }
            } catch (e: Exception) {
                _isInternetAvailableState.value = false
                _pingLatencyState.value = "Error"
            }
        }
    }

    fun saveConfig(ntpServer: String, bootDelaySec: String, netTimeoutSec: String, retryCount: String) {
        val newConfig = SyncConfig(
            ntpServer = ntpServer.trim(),
            bootDelaySec = bootDelaySec.trim(),
            netTimeoutSec = netTimeoutSec.trim(),
            retryCount = retryCount.trim()
        )
        prefsHelper.saveSyncConfig(newConfig)
        _configState.value = newConfig
        
        viewModelScope.launch {
            repository.insertLog(
                SyncLog(
                    action = "Config Change",
                    status = "SUCCESS",
                    message = "Saved new settings: Server=${newConfig.ntpServer}, Delay=${newConfig.bootDelaySec}s, Retries=${newConfig.retryCount}"
                )
            )
        }
    }

    fun triggerManualSync() {
        if (_isSyncingState.value) return
        _isSyncingState.value = true

        viewModelScope.launch {
            repository.insertLog(
                SyncLog(
                    action = "Manual Sync",
                    status = "PENDING",
                    message = "Starting manual NTP time synchronization..."
                )
            )

            val config = _configState.value
            val success = withContext(Dispatchers.IO) {
                var syncSuccess = false
                val logBuilder = StringBuilder()
                
                try {
                    // Try busybox ntpd command first
                    val cmd = "su -c \"busybox ntpd -n -q -p ${config.ntpServer}\""
                    logBuilder.append("Executing: $cmd\n")
                    val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "busybox ntpd -n -q -p ${config.ntpServer}"))
                    
                    val stdErr = BufferedReader(InputStreamReader(process.errorStream))
                    val stdOut = BufferedReader(InputStreamReader(process.inputStream))
                    var line: String?
                    while (stdOut.readLine().also { line = it } != null) {
                        logBuilder.append("OUT: ").append(line).append("\n")
                    }
                    while (stdErr.readLine().also { line = it } != null) {
                        logBuilder.append("ERR: ").append(line).append("\n")
                    }
                    
                    val exitCode = process.waitFor()
                    logBuilder.append("Exit code: $exitCode\n")
                    
                    if (exitCode == 0) {
                        syncSuccess = true
                    } else {
                        logBuilder.append("NTPD command failed. Trying global system settings update...\n")
                        val settingCmd = "settings put global ntp_server ${config.ntpServer}"
                        val p2 = Runtime.getRuntime().exec(arrayOf("su", "-c", settingCmd))
                        val exit2 = p2.waitFor()
                        logBuilder.append("Settings update completed with exit: $exit2\n")
                        if (exit2 == 0) {
                            syncSuccess = true
                        }
                    }
                } catch (e: Exception) {
                    logBuilder.append("Exception: ").append(e.message).append("\n")
                }
                
                Pair(syncSuccess, logBuilder.toString())
            }

            _isSyncingState.value = false
            repository.insertLog(
                SyncLog(
                    action = "Manual Sync",
                    status = if (success.first) "SUCCESS" else "FAILED",
                    message = if (success.first) "Time synchronization completed successfully!" else "Sync failed. Ensure busybox is installed or root permissions are granted.\nDetails: ${success.second}"
                )
            )
            refreshDiagnostics()
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            repository.insertLog(
                SyncLog(
                    action = "Log Manage",
                    status = "INFO",
                    message = "Logs cleared by user."
                )
            )
        }
    }
}
