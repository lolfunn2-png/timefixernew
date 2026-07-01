package com.example.data.local

import android.content.Context
import java.io.File

class PreferencesHelper(private val context: Context) {
    private val prefs = context.getSharedPreferences("autotimefix_prefs", Context.MODE_PRIVATE)

    fun getSyncConfig(): SyncConfig {
        return SyncConfig(
            ntpServer = prefs.getString("ntp_server", "time.google.com") ?: "time.google.com",
            bootDelaySec = prefs.getString("boot_delay", "30") ?: "30",
            netTimeoutSec = prefs.getString("net_timeout", "150") ?: "150",
            retryCount = prefs.getString("retry_count", "10") ?: "10"
        )
    }

    fun saveSyncConfig(config: SyncConfig) {
        prefs.edit().apply {
            putString("ntp_server", config.ntpServer)
            putString("boot_delay", config.bootDelaySec)
            putString("net_timeout", config.netTimeoutSec)
            putString("retry_count", config.retryCount)
            apply()
        }
        
        // Force file permissions to be world-readable using su
        try {
            val prefFile = File(context.filesDir.parentFile, "shared_prefs/autotimefix_prefs.xml")
            if (prefFile.exists()) {
                val cmd = "chmod 644 ${prefFile.absolutePath} && chmod 755 ${prefFile.parentFile.absolutePath} && chmod 755 ${context.filesDir.parentFile.absolutePath}"
                val p = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
                p.waitFor()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

data class SyncConfig(
    val ntpServer: String,
    val bootDelaySec: String,
    val netTimeoutSec: String,
    val retryCount: String
)
