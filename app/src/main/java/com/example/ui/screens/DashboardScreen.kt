package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.SyncLog
import com.example.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val config by viewModel.configState.collectAsStateWithLifecycle()
    val logs by viewModel.logListState.collectAsStateWithLifecycle()
    val isModuleActive by viewModel.isModuleActiveState.collectAsStateWithLifecycle()
    val isInternetAvailable by viewModel.isInternetAvailableState.collectAsStateWithLifecycle()
    val pingLatency by viewModel.pingLatencyState.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncingState.collectAsStateWithLifecycle()
    val currentTime by viewModel.currentTimeState.collectAsStateWithLifecycle()

    var ntpServerInput by remember { mutableStateOf("") }
    var bootDelayInput by remember { mutableStateOf("") }
    var netTimeoutInput by remember { mutableStateOf("") }
    var retryCountInput by remember { mutableStateOf("") }

    // Sync input variables when config loads
    LaunchedEffect(config) {
        ntpServerInput = config.ntpServer
        bootDelayInput = config.bootDelaySec
        netTimeoutInput = config.netTimeoutSec
        retryCountInput = config.retryCount
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Logo",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "XPOSED MODULE V1.0.2",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                letterSpacing = 1.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
        ) {
            // 1. Live System Clock Widget
            item {
                ClockWidget(
                    timeStr = currentTime,
                    ntpServer = config.ntpServer
                )
            }

            // 2. Sleek Hero Status Card
            item {
                HeroStatusCard(isActive = isModuleActive)
            }

            // 3. Operation Checklist (Boot Sequence Progress)
            item {
                BootSequenceCard(
                    isModuleActive = isModuleActive,
                    isInternetAvailable = isInternetAvailable,
                    pingLatency = pingLatency,
                    bootDelay = config.bootDelaySec,
                    ntpServer = config.ntpServer
                )
            }

            // 4. Diagnostics & Manual Control
            item {
                DiagnosticsCard(
                    isInternetAvailable = isInternetAvailable,
                    pingLatency = pingLatency,
                    isSyncing = isSyncing,
                    onRefresh = { viewModel.refreshDiagnostics() },
                    onSyncNow = { viewModel.triggerManualSync() }
                )
            }

            // 5. Configuration Settings Panel (Outlined Card style to match Sleek Theme)
            item {
                ConfigurationCard(
                    ntpServer = ntpServerInput,
                    onNtpChange = { ntpServerInput = it },
                    bootDelay = bootDelayInput,
                    onBootDelayChange = { bootDelayInput = it },
                    netTimeout = netTimeoutInput,
                    onNetTimeoutChange = { netTimeoutInput = it },
                    retryCount = retryCountInput,
                    onRetryChange = { retryCountInput = it },
                    onSave = {
                        viewModel.saveConfig(
                            ntpServerInput,
                            bootDelayInput,
                            netTimeoutInput,
                            retryCountInput
                        )
                    }
                )
            }

            // 6. Interactive Terminal Debug Logs Console
            item {
                TerminalConsoleWidget(
                    logs = logs,
                    onClear = { viewModel.clearLogs() }
                )
            }
        }
    }
}

@Composable
fun ClockWidget(timeStr: String, ntpServer: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("clock_widget"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "CURRENT SYSTEM TIME",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = timeStr.ifEmpty { "Loading..." },
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "NTP",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Sync Target: $ntpServer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun HeroStatusCard(isActive: Boolean) {
    // Sleek theme colors for the status hero
    val containerColor = if (isActive) Color(0xFFEADDFF) else Color(0xFFFFEBEE)
    val borderColor = if (isActive) Color(0xFFD0BCFF) else Color(0xFFFFCDD2)
    val contentColor = if (isActive) Color(0xFF21005D) else Color(0xFFC62828)
    val descriptionColor = if (isActive) Color(0xFF49454F) else Color(0xFF5D4037)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("xposed_status_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Rounded white circle with icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = if (isActive) "Active" else "Inactive",
                    modifier = Modifier.size(28.dp),
                    tint = contentColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isActive) "System Sync Active" else "Xposed Module Inactive",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isActive) {
                    "NTP synchronization is bound and actively hooking the systemready boot pipeline."
                } else {
                    "Please enable Auto Time Fix in your Xposed/LSPosed manager and restart the device."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = descriptionColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Badges row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusBadge(text = if (isActive) "BOOT READY" else "AWAITING ACTIVE", color = contentColor)
                StatusBadge(text = "ROOT REQUIRED", color = contentColor)
            }
        }
    }
}

@Composable
fun StatusBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(color)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun BootSequenceCard(
    isModuleActive: Boolean,
    isInternetAvailable: Boolean?,
    pingLatency: String,
    bootDelay: String,
    ntpServer: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("boot_sequence_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "BOOT SEQUENCE CHECKLIST",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF625B71),
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Step 1: System Ready Hook
            BootStepRow(
                icon = Icons.Default.Info,
                title = "System Ready Hook",
                subtitle = if (isModuleActive) "AMS Hook bound! Loaded after ${bootDelay}s" else "Module not loaded in system yet.",
                isCompleted = isModuleActive
            )

            Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 8.dp))

            // Step 2: Network Available
            BootStepRow(
                icon = Icons.Default.Refresh,
                title = "Network Connectivity",
                subtitle = when (isInternetAvailable) {
                    true -> "Connected! Latency: $pingLatency"
                    false -> "Disconnected. Waiting for interface..."
                    null -> "Evaluating network interface..."
                },
                isCompleted = isInternetAvailable == true
            )

            Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 8.dp))

            // Step 3: NTP Force Sync
            BootStepRow(
                icon = Icons.Default.Star,
                title = "NTP Sync Engine",
                subtitle = "Force clock sync targeting $ntpServer",
                isCompleted = isModuleActive && (isInternetAvailable == true)
            )
        }
    }
}

@Composable
fun BootStepRow(
    isCompleted: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Small circular grey background container
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFF3EDF7)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color(0xFF6750A4)
            )
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF625B71)
            )
        }

        Icon(
            imageVector = if (isCompleted) Icons.Default.Check else Icons.Default.Warning,
            contentDescription = if (isCompleted) "Completed" else "Pending",
            tint = if (isCompleted) Color(0xFF2E7D32) else Color(0xFF938F99),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun DiagnosticsCard(
    isInternetAvailable: Boolean?,
    pingLatency: String,
    isSyncing: Boolean,
    onRefresh: () -> Unit,
    onSyncNow: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("diagnostics_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.diagnostic_title).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF625B71),
                    letterSpacing = 1.5.sp
                )
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh diagnostics",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.diagnostic_ip),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when (isInternetAvailable) {
                        true -> "CONNECTED"
                        false -> "DISCONNECTED"
                        null -> "CHECKING..."
                    },
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (isInternetAvailable) {
                        true -> Color(0xFF2E7D32)
                        false -> Color(0xFFC62828)
                        null -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.diagnostic_ping),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = pingLatency.uppercase(),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSyncNow,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sync_now_button"),
                enabled = !isSyncing,
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onSecondary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SYNCHRONIZING TIME...")
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_sync_now).uppercase())
                }
            }
        }
    }
}

@Composable
fun ConfigurationCard(
    ntpServer: String,
    onNtpChange: (String) -> Unit,
    bootDelay: String,
    onBootDelayChange: (String) -> Unit,
    netTimeout: String,
    onNetTimeoutChange: (String) -> Unit,
    retryCount: String,
    onRetryChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("configuration_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.config_title).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF625B71),
                letterSpacing = 1.5.sp
            )

            // NTP server selection
            OutlinedTextField(
                value = ntpServer,
                onValueChange = onNtpChange,
                label = { Text(stringResource(R.string.config_ntp_server)) },
                modifier = Modifier.fillMaxWidth().testTag("config_ntp_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Preset Chips for NTP Servers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf("time.google.com", "pool.ntp.org", "time.windows.com")
                presets.forEach { preset ->
                    SuggestionChip(
                        onClick = { onNtpChange(preset) },
                        label = { Text(preset, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        shape = RoundedCornerShape(100.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = bootDelay,
                    onValueChange = onBootDelayChange,
                    label = { Text(stringResource(R.string.config_boot_delay)) },
                    modifier = Modifier.weight(1f).testTag("config_delay_input"),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = retryCount,
                    onValueChange = onRetryChange,
                    label = { Text(stringResource(R.string.config_retry_count)) },
                    modifier = Modifier.weight(1f).testTag("config_retry_input"),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = netTimeout,
                onValueChange = onNetTimeoutChange,
                label = { Text(stringResource(R.string.config_net_timeout)) },
                modifier = Modifier.fillMaxWidth().testTag("config_timeout_input"),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_config_button"),
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.btn_save_config).uppercase())
            }
        }
    }
}

@Composable
fun TerminalConsoleWidget(
    logs: List<SyncLog>,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("terminal_console_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F)) // Terminal deep dark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Console Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF44336)) // Red window dot
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFEB3B)) // Yellow window dot
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50)) // Green window dot
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "DEBUG LOGS",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD0BCFF)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "STB_V4.4.2_KITKAT",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF938F99),
                        fontSize = 9.sp
                    )
                    
                    if (logs.isNotEmpty()) {
                        IconButton(
                            onClick = onClear,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear Terminal",
                                tint = Color(0xFF938F99),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF49454F))
                    .padding(bottom = 12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Scrolling output lines
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Default start lines to mimic boot sequence logs
                TerminalLogLine(timestamp = "12:00:01", message = "XposedBridge: Loading AutoTimeFix...", color = Color(0xFF938F99))
                TerminalLogLine(timestamp = "12:00:02", message = "AutoTimeFix: systemReady hooked successfully!", color = Color(0xFFD0BCFF))

                if (logs.isEmpty()) {
                    TerminalLogLine(
                        timestamp = "12:00:03",
                        message = "No actions run yet. Use manual sync or reboot device.",
                        color = Color(0xFF938F99)
                    )
                } else {
                    logs.take(15).forEach { log ->
                        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                        val lineTextColor = when (log.status) {
                            "SUCCESS" -> Color(0xFF4ADE80) // Vivid Terminal Green
                            "FAILED" -> Color(0xFFF87171)  // Vivid Terminal Red
                            "PENDING" -> Color(0xFFFBBF24) // Vivid Terminal Yellow
                            else -> Color(0xFFE6E1E5)
                        }
                        TerminalLogLine(
                            timestamp = timeStr,
                            message = "${log.action}: ${log.message}",
                            color = lineTextColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TerminalLogLine(timestamp: String, message: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "[$timestamp]",
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            ),
            color = Color(0xFF938F99)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            ),
            color = color,
            modifier = Modifier.weight(1f)
        )
    }
}
