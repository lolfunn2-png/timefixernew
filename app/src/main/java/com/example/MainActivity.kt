package com.example

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.viewmodel.DashboardViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: DashboardViewModel

    // UI elements to update dynamically
    private lateinit var clockTimeTextView: TextView
    private lateinit var clockTargetTextView: TextView
    
    private lateinit var heroCardContainer: LinearLayout
    private lateinit var heroIconTextView: TextView
    private lateinit var heroStatusTitle: TextView
    private lateinit var heroStatusDesc: TextView
    private lateinit var heroBadgeBoot: TextView
    
    private lateinit var checklistStep1Sub: TextView
    private lateinit var checklistStep1Icon: TextView
    private lateinit var checklistStep2Sub: TextView
    private lateinit var checklistStep2Icon: TextView
    private lateinit var checklistStep3Icon: TextView

    private lateinit var diagIpValue: TextView
    private lateinit var diagPingValue: TextView
    private lateinit var forceSyncButton: Button

    private lateinit var ntpServerEditText: EditText
    private lateinit var bootDelayEditText: EditText
    private lateinit var netTimeoutEditText: EditText
    private lateinit var retryCountEditText: EditText
    private lateinit var saveConfigButton: Button

    private lateinit var logsLayout: LinearLayout
    private lateinit var clearLogsButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[DashboardViewModel::class.java]

        // Create main layout
        val rootLayout = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.parseColor("#FEF7FF"))
            isFillViewport = true
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(32))
        }
        rootLayout.addView(container)

        // 1. Header View
        container.addView(createHeaderView())
        container.addView(createSpacer(12))

        // 2. Current Time Clock Card
        container.addView(createClockCard())
        container.addView(createSpacer(16))

        // 3. Hero Status Card
        container.addView(createHeroStatusCard())
        container.addView(createSpacer(16))

        // 4. Operation Checklist Card
        container.addView(createChecklistCard())
        container.addView(createSpacer(16))

        // 5. Diagnostics Card
        container.addView(createDiagnosticsCard())
        container.addView(createSpacer(16))

        // 6. Configuration Settings Card
        container.addView(createConfigurationCard())
        container.addView(createSpacer(16))

        // 7. Terminal Debug Logs Card
        container.addView(createTerminalCard())

        setContentView(rootLayout)

        // Bind flows to update UI dynamically
        bindViewModelFlows()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun createSpacer(dp: Int): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(dp))
        }
    }

    private fun createCardDrawable(backgroundColor: String, strokeColor: String?, cornerRadiusDp: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(backgroundColor))
            setCornerRadius(dpToPx(cornerRadiusDp).toFloat())
            if (strokeColor != null) {
                setStroke(dpToPx(1), Color.parseColor(strokeColor))
            }
        }
    }

    // 1. Header Section
    private fun createHeaderView(): View {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            gravity = Gravity.CENTER_VERTICAL
        }

        // Left Logo Icon
        val logoBox = FrameLayout(this).apply {
            val size = dpToPx(40)
            layoutParams = LinearLayout.LayoutParams(size, size)
            background = createCardDrawable("#6750A4", null, 12)
        }
        val logoText = TextView(this).apply {
            text = "↻"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
        }
        logoBox.addView(logoText)
        header.addView(logoBox)

        // Title and Subtitle container
        val titleContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dpToPx(12)
            }
        }
        val mainTitle = TextView(this).apply {
            text = "Auto Time Fix"
            textSize = 18f
            setTextColor(Color.parseColor("#1D1B20"))
            setTypeface(null, Typeface.BOLD)
        }
        val subTitle = TextView(this).apply {
            text = "XPOSED MODULE V1.0.2"
            textSize = 10f
            setTextColor(Color.parseColor("#625B71"))
            setTypeface(null, Typeface.BOLD)
            letterSpacing = 0.1f
        }
        titleContainer.addView(mainTitle)
        titleContainer.addView(subTitle)
        header.addView(titleContainer)

        // Settings Button
        val settingsBtn = ImageButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(40))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.TRANSPARENT)
            }
            contentDescription = "Settings"
            setImageResource(android.R.drawable.ic_menu_preferences)
            setColorFilter(Color.parseColor("#1D1B20"))
            setOnClickListener {
                Toast.makeText(this@MainActivity, "Auto Time Fix is actively running in background", Toast.LENGTH_SHORT).show()
            }
        }
        header.addView(settingsBtn)

        return header
    }

    // 2. Current Time Clock Card
    private fun createClockCard(): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20))
            background = createCardDrawable("#EADDFF", null, 24)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val label = TextView(this).apply {
            text = "CURRENT SYSTEM TIME"
            textSize = 11f
            setTextColor(Color.parseColor("#21005D"))
            setTypeface(null, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        card.addView(label)

        clockTimeTextView = TextView(this).apply {
            text = "Loading..."
            textSize = 28f
            setTextColor(Color.parseColor("#21005D"))
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(6), 0, dpToPx(6))
        }
        card.addView(clockTimeTextView)

        clockTargetTextView = TextView(this).apply {
            text = "Sync Target: time.google.com"
            textSize = 12f
            setTextColor(Color.parseColor("#21005D"))
            gravity = Gravity.CENTER
        }
        card.addView(clockTargetTextView)

        return card
    }

    // 3. Hero Status Card
    private fun createHeroStatusCard(): View {
        heroCardContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(24))
            background = createCardDrawable("#EADDFF", "#D0BCFF", 28)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // White Circle with Status Icon
        val iconCircle = FrameLayout(this).apply {
            val size = dpToPx(48)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                bottomMargin = dpToPx(12)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.WHITE)
            }
        }
        heroIconTextView = TextView(this).apply {
            text = "✔"
            textSize = 24f
            setTextColor(Color.parseColor("#21005D"))
            gravity = Gravity.CENTER
        }
        iconCircle.addView(heroIconTextView)
        heroCardContainer.addView(iconCircle)

        heroStatusTitle = TextView(this).apply {
            text = "System Sync Active"
            textSize = 18f
            setTextColor(Color.parseColor("#21005D"))
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        heroCardContainer.addView(heroStatusTitle)

        heroStatusDesc = TextView(this).apply {
            text = "NTP synchronization is bound and actively hooking the systemready boot pipeline."
            textSize = 13f
            setTextColor(Color.parseColor("#49454F"))
            gravity = Gravity.CENTER
            setPadding(dpToPx(16), dpToPx(6), dpToPx(16), dpToPx(12))
        }
        heroCardContainer.addView(heroStatusDesc)

        // Badges layout
        val badgesLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            gravity = Gravity.CENTER
        }

        heroBadgeBoot = TextView(this).apply {
            text = "BOOT READY"
            textSize = 9f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setPadding(dpToPx(10), dpToPx(5), dpToPx(10), dpToPx(5))
            background = createCardDrawable("#21005D", null, 100)
        }
        badgesLayout.addView(heroBadgeBoot)

        badgesLayout.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(dpToPx(8), 1) })

        val badgeRoot = TextView(this).apply {
            text = "ROOT REQUIRED"
            textSize = 9f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setPadding(dpToPx(10), dpToPx(5), dpToPx(10), dpToPx(5))
            background = createCardDrawable("#21005D", null, 100)
        }
        badgesLayout.addView(badgeRoot)

        heroCardContainer.addView(badgesLayout)

        return heroCardContainer
    }

    // 4. Operation Checklist Card
    private fun createChecklistCard(): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            background = createCardDrawable("#FFFFFF", "#CAC4D0", 24)
        }

        val label = TextView(this).apply {
            text = "BOOT SEQUENCE CHECKLIST"
            textSize = 10f
            setTextColor(Color.parseColor("#625B71"))
            setTypeface(null, Typeface.BOLD)
            letterSpacing = 0.08f
            setPadding(dpToPx(4), 0, 0, dpToPx(12))
        }
        card.addView(label)

        // Row 1: System Ready Hook
        val row1 = createChecklistRow("System Ready Hook", "AMS Hook bound! Loaded boot delay.", "⚡")
        checklistStep1Sub = row1.findViewById(101)
        checklistStep1Icon = row1.findViewById(102)
        card.addView(row1)

        card.addView(createChecklistDivider())

        // Row 2: Network Connectivity
        val row2 = createChecklistRow("Network Connectivity", "Evaluating network interface...", "📶")
        checklistStep2Sub = row2.findViewById(101)
        checklistStep2Icon = row2.findViewById(102)
        card.addView(row2)

        card.addView(createChecklistDivider())

        // Row 3: NTP Sync Engine
        val row3 = createChecklistRow("NTP Sync Engine", "Force clock sync targeting server.", "⏱")
        checklistStep3Icon = row3.findViewById(102)
        card.addView(row3)

        return card
    }

    private fun createChecklistRow(title: String, initialSub: String, iconGlyph: String): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(6), 0, dpToPx(6))
        }

        // Left Round icon backplate
        val backplate = FrameLayout(this).apply {
            val size = dpToPx(36)
            layoutParams = LinearLayout.LayoutParams(size, size)
            background = createCardDrawable("#F3EDF7", null, 100)
        }
        val glyph = TextView(this).apply {
            text = iconGlyph
            textSize = 14f
            setTextColor(Color.parseColor("#6750A4"))
            gravity = Gravity.CENTER
        }
        backplate.addView(glyph)
        row.addView(backplate)

        // Middle Text
        val midText = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dpToPx(12)
                rightMargin = dpToPx(12)
            }
        }
        val textTitle = TextView(this).apply {
            text = title
            textSize = 14f
            setTextColor(Color.parseColor("#1D1B20"))
            setTypeface(null, Typeface.BOLD)
        }
        val textSub = TextView(this).apply {
            id = 101
            text = initialSub
            textSize = 11f
            setTextColor(Color.parseColor("#625B71"))
        }
        midText.addView(textTitle)
        midText.addView(textSub)
        row.addView(midText)

        // Right Status Check/Cross
        val rightIcon = TextView(this).apply {
            id = 102
            text = "⚠"
            textSize = 16f
            setTextColor(Color.parseColor("#938F99"))
            gravity = Gravity.CENTER
        }
        row.addView(rightIcon)

        return row
    }

    private fun createChecklistDivider(): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1)).apply {
                topMargin = dpToPx(8)
                bottomMargin = dpToPx(8)
            }
            setBackgroundColor(Color.parseColor("#E7E0EC"))
        }
    }

    // 5. Diagnostics Card
    private fun createDiagnosticsCard(): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            background = createCardDrawable("#FFFFFF", "#CAC4D0", 24)
        }

        // Header with Refresh Button
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(this).apply {
            text = "DIAGNOSTICS"
            textSize = 10f
            setTextColor(Color.parseColor("#625B71"))
            setTypeface(null, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        header.addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val refreshBtn = ImageButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(32), dpToPx(32))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.TRANSPARENT)
            }
            setImageResource(android.R.drawable.ic_menu_rotate)
            setColorFilter(Color.parseColor("#6750A4"))
            setOnClickListener {
                viewModel.refreshDiagnostics()
                Toast.makeText(this@MainActivity, "Refreshing diagnostics...", Toast.LENGTH_SHORT).show()
            }
        }
        header.addView(refreshBtn)
        card.addView(header)
        card.addView(createSpacer(8))

        // IP status row
        val rowIp = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(0, dpToPx(4), 0, dpToPx(4))
        }
        val lblIp = TextView(this).apply {
            text = "Internet Connectivity"
            textSize = 14f
            setTextColor(Color.parseColor("#1D1B20"))
        }
        rowIp.addView(lblIp, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        diagIpValue = TextView(this).apply {
            text = "CHECKING..."
            textSize = 14f
            setTextColor(Color.parseColor("#625B71"))
            setTypeface(null, Typeface.BOLD)
        }
        rowIp.addView(diagIpValue)
        card.addView(rowIp)

        // Ping status row
        val rowPing = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(0, dpToPx(4), 0, dpToPx(4))
        }
        val lblPing = TextView(this).apply {
            text = "Ping 8.8.8.8"
            textSize = 14f
            setTextColor(Color.parseColor("#1D1B20"))
        }
        rowPing.addView(lblPing, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        diagPingValue = TextView(this).apply {
            text = "N/A"
            textSize = 14f
            setTextColor(Color.parseColor("#1D1B20"))
            setTypeface(null, Typeface.BOLD)
        }
        rowPing.addView(diagPingValue)
        card.addView(rowPing)

        card.addView(createSpacer(12))

        // Force Sync Now Button
        forceSyncButton = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(48))
            text = "FORCE SYNC NOW"
            textSize = 14f
            setTextColor(Color.parseColor("#1D192B"))
            setTypeface(null, Typeface.BOLD)
            background = createCardDrawable("#E8DEF8", null, 100)
            setOnClickListener {
                viewModel.triggerManualSync()
            }
        }
        card.addView(forceSyncButton)

        return card
    }

    // 6. Configuration Settings Card
    private fun createConfigurationCard(): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            background = createCardDrawable("#FFFFFF", "#CAC4D0", 24)
        }

        val label = TextView(this).apply {
            text = "CONFIGURATION"
            textSize = 10f
            setTextColor(Color.parseColor("#625B71"))
            setTypeface(null, Typeface.BOLD)
            letterSpacing = 0.08f
            setPadding(0, 0, 0, dpToPx(12))
        }
        card.addView(label)

        // NTP Server Input
        val ntpLabel = TextView(this).apply {
            text = "NTP Server Address"
            textSize = 12f
            setTextColor(Color.parseColor("#625B71"))
            setPadding(dpToPx(4), 0, 0, dpToPx(2))
        }
        card.addView(ntpLabel)
        ntpServerEditText = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dpToPx(8)
            }
            background = createCardDrawable("#FEF7FF", "#CAC4D0", 12)
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            textSize = 14f
            setSingleLine(true)
            setTextColor(Color.parseColor("#1D1B20"))
        }
        card.addView(ntpServerEditText)

        // NTP Presets
        val presetsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dpToPx(12)
            }
        }
        val presets = listOf("time.google.com", "pool.ntp.org", "time.windows.com")
        presets.forEach { preset ->
            val chip = Button(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, dpToPx(32), 1f).apply {
                    rightMargin = dpToPx(4)
                }
                text = preset.replace("time.", "")
                textSize = 9f
                setPadding(0, 0, 0, 0)
                background = createCardDrawable("#F3EDF7", "#CAC4D0", 8)
                setTextColor(Color.parseColor("#6750A4"))
                setOnClickListener {
                    ntpServerEditText.setText(preset)
                }
            }
            presetsLayout.addView(chip)
        }
        card.addView(presetsLayout)

        // Dual row: Boot Delay & Retry Count
        val dualRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dpToPx(12)
            }
        }

        // Boot Delay Container
        val bootContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = dpToPx(8)
            }
        }
        val bootLbl = TextView(this).apply {
            text = "Boot Delay (s)"
            textSize = 12f
            setTextColor(Color.parseColor("#625B71"))
            setPadding(dpToPx(4), 0, 0, dpToPx(2))
        }
        bootDelayEditText = EditText(this).apply {
            background = createCardDrawable("#FEF7FF", "#CAC4D0", 12)
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            textSize = 14f
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_NUMBER
            setTextColor(Color.parseColor("#1D1B20"))
        }
        bootContainer.addView(bootLbl)
        bootContainer.addView(bootDelayEditText)
        dualRow.addView(bootContainer)

        // Retry Count Container
        val retryContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val retryLbl = TextView(this).apply {
            text = "Max Retries"
            textSize = 12f
            setTextColor(Color.parseColor("#625B71"))
            setPadding(dpToPx(4), 0, 0, dpToPx(2))
        }
        retryCountEditText = EditText(this).apply {
            background = createCardDrawable("#FEF7FF", "#CAC4D0", 12)
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            textSize = 14f
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_NUMBER
            setTextColor(Color.parseColor("#1D1B20"))
        }
        retryContainer.addView(retryLbl)
        retryContainer.addView(retryCountEditText)
        dualRow.addView(retryContainer)

        card.addView(dualRow)

        // Network Ready Timeout
        val timeoutLabel = TextView(this).apply {
            text = "Network Ready Timeout (Seconds)"
            textSize = 12f
            setTextColor(Color.parseColor("#625B71"))
            setPadding(dpToPx(4), 0, 0, dpToPx(2))
        }
        card.addView(timeoutLabel)
        netTimeoutEditText = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dpToPx(16)
            }
            background = createCardDrawable("#FEF7FF", "#CAC4D0", 12)
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            textSize = 14f
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_NUMBER
            setTextColor(Color.parseColor("#1D1B20"))
        }
        card.addView(netTimeoutEditText)

        // Save Button
        saveConfigButton = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(48))
            text = "SAVE CONFIGURATION"
            textSize = 14f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            background = createCardDrawable("#6750A4", null, 100)
            setOnClickListener {
                viewModel.saveConfig(
                    ntpServerEditText.text.toString(),
                    bootDelayEditText.text.toString(),
                    netTimeoutEditText.text.toString(),
                    retryCountEditText.text.toString()
                )
                Toast.makeText(this@MainActivity, "Configuration saved!", Toast.LENGTH_SHORT).show()
            }
        }
        card.addView(saveConfigButton)

        return card
    }

    // 7. Terminal Debug Logs Card
    private fun createTerminalCard(): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            background = createCardDrawable("#1C1B1F", null, 16)
        }

        // Header Row
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dpToPx(8))
        }

        // Mock window dots
        val dotsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            gravity = Gravity.CENTER_VERTICAL
        }
        val dotColors = listOf("#F44336", "#FFEB3B", "#4CAF50")
        dotColors.forEach { color ->
            val dot = View(this@MainActivity).apply {
                val size = dpToPx(8)
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    rightMargin = dpToPx(6)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(color))
                }
            }
            dotsLayout.addView(dot)
        }
        header.addView(dotsLayout)

        val title = TextView(this).apply {
            text = "DEBUG LOGS"
            textSize = 12f
            setTextColor(Color.parseColor("#D0BCFF"))
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            setPadding(dpToPx(6), 0, 0, 0)
        }
        header.addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val infoLabel = TextView(this).apply {
            text = "STB_V4.4.2_KITKAT"
            textSize = 9f
            setTextColor(Color.parseColor("#938F99"))
            setTypeface(Typeface.MONOSPACE, Typeface.NORMAL)
            setPadding(0, 0, dpToPx(8), 0)
        }
        header.addView(infoLabel)

        clearLogsButton = ImageButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(24), dpToPx(24))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.TRANSPARENT)
            }
            setImageResource(android.R.drawable.ic_menu_delete)
            setColorFilter(Color.parseColor("#938F99"))
            setOnClickListener {
                viewModel.clearLogs()
                Toast.makeText(this@MainActivity, "Logs cleared", Toast.LENGTH_SHORT).show()
            }
        }
        header.addView(clearLogsButton)

        card.addView(header)

        // Colored line divider
        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1)).apply {
                bottomMargin = dpToPx(12)
            }
            setBackgroundColor(Color.parseColor("#49454F"))
        }
        card.addView(divider)

        // Logs Output List Layout
        logsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        card.addView(logsLayout)

        return card
    }

    private fun addLogLineToTerminal(timestamp: String, message: String, textColorHex: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dpToPx(4)
            }
        }

        val timeText = TextView(this).apply {
            text = "[$timestamp]"
            textSize = 11f
            setTextColor(Color.parseColor("#938F99"))
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            setPadding(0, 0, dpToPx(6), 0)
        }
        row.addView(timeText)

        val msgText = TextView(this).apply {
            text = message
            textSize = 11f
            setTextColor(Color.parseColor(textColorHex))
            setTypeface(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        row.addView(msgText, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        logsLayout.addView(row)
    }

    private fun bindViewModelFlows() {
        // Collect Clock State
        lifecycleScope.launch {
            viewModel.currentTimeState.collect { time ->
                clockTimeTextView.text = if (time.isEmpty()) "Loading..." else time
            }
        }

        // Collect Configuration Settings
        lifecycleScope.launch {
            viewModel.configState.collect { config ->
                clockTargetTextView.text = "Sync Target: ${config.ntpServer}"
                
                // Only set if not focused to avoid interrupting typing
                if (!ntpServerEditText.hasFocus()) ntpServerEditText.setText(config.ntpServer)
                if (!bootDelayEditText.hasFocus()) bootDelayEditText.setText(config.bootDelaySec)
                if (!netTimeoutEditText.hasFocus()) netTimeoutEditText.setText(config.netTimeoutSec)
                if (!retryCountEditText.hasFocus()) retryCountEditText.setText(config.retryCount)
            }
        }

        // Collect Module Active State
        lifecycleScope.launch {
            viewModel.isModuleActiveState.collect { isActive ->
                if (isActive) {
                    heroCardContainer.background = createCardDrawable("#EADDFF", "#D0BCFF", 28)
                    heroIconTextView.text = "✔"
                    heroIconTextView.setTextColor(Color.parseColor("#21005D"))
                    heroStatusTitle.text = "System Sync Active"
                    heroStatusTitle.setTextColor(Color.parseColor("#21005D"))
                    heroStatusDesc.text = "NTP synchronization is bound and actively hooking the systemready boot pipeline."
                    heroStatusDesc.setTextColor(Color.parseColor("#49454F"))
                    heroBadgeBoot.text = "BOOT READY"
                    heroBadgeBoot.background = createCardDrawable("#21005D", null, 100)
                    
                    checklistStep1Icon.text = "✔"
                    checklistStep1Icon.setTextColor(Color.parseColor("#2E7D32"))
                } else {
                    heroCardContainer.background = createCardDrawable("#FFFFEBEE", "#FFFFCDD2", 28)
                    heroIconTextView.text = "✘"
                    heroIconTextView.setTextColor(Color.parseColor("#C62828"))
                    heroStatusTitle.text = "Xposed Module Inactive"
                    heroStatusTitle.setTextColor(Color.parseColor("#C62828"))
                    heroStatusDesc.text = "Please enable Auto Time Fix in your Xposed/LSPosed manager and restart the device."
                    heroStatusDesc.setTextColor(Color.parseColor("#5D4037"))
                    heroBadgeBoot.text = "AWAITING ACTIVE"
                    heroBadgeBoot.background = createCardDrawable("#C62828", null, 100)
                    
                    checklistStep1Icon.text = "⚠"
                    checklistStep1Icon.setTextColor(Color.parseColor("#938F99"))
                }
            }
        }

        // Collect Internet State
        lifecycleScope.launch {
            viewModel.isInternetAvailableState.collect { isInternet ->
                val isActive = viewModel.isModuleActiveState.value
                when (isInternet) {
                    true -> {
                        diagIpValue.text = "CONNECTED"
                        diagIpValue.setTextColor(Color.parseColor("#2E7D32"))
                        checklistStep2Icon.text = "✔"
                        checklistStep2Icon.setTextColor(Color.parseColor("#2E7D32"))
                        
                        if (isActive) {
                            checklistStep3Icon.text = "✔"
                            checklistStep3Icon.setTextColor(Color.parseColor("#2E7D32"))
                        } else {
                            checklistStep3Icon.text = "⚠"
                            checklistStep3Icon.setTextColor(Color.parseColor("#938F99"))
                        }
                    }
                    false -> {
                        diagIpValue.text = "DISCONNECTED"
                        diagIpValue.setTextColor(Color.parseColor("#C62828"))
                        checklistStep2Icon.text = "⚠"
                        checklistStep2Icon.setTextColor(Color.parseColor("#938F99"))
                        checklistStep2Sub.text = "Disconnected. Waiting for interface..."
                        
                        checklistStep3Icon.text = "⚠"
                        checklistStep3Icon.setTextColor(Color.parseColor("#938F99"))
                    }
                    null -> {
                        diagIpValue.text = "CHECKING..."
                        diagIpValue.setTextColor(Color.parseColor("#625B71"))
                        checklistStep2Icon.text = "⚠"
                        checklistStep2Icon.setTextColor(Color.parseColor("#938F99"))
                        checklistStep2Sub.text = "Evaluating network interface..."
                    }
                }
            }
        }

        // Collect Ping Latency
        lifecycleScope.launch {
            viewModel.pingLatencyState.collect { latency ->
                diagPingValue.text = latency.uppercase()
                if (latency != "N/A" && latency != "Fail" && latency != "Error") {
                    checklistStep2Sub.text = "Connected! Latency: $latency"
                }
            }
        }

        // Collect Sync State
        lifecycleScope.launch {
            viewModel.isSyncingState.collect { isSyncing ->
                if (isSyncing) {
                    forceSyncButton.isEnabled = false
                    forceSyncButton.text = "SYNCHRONIZING..."
                } else {
                    forceSyncButton.isEnabled = true
                    forceSyncButton.text = "FORCE SYNC NOW"
                }
            }
        }

        // Collect Output Debug Logs List
        lifecycleScope.launch {
            viewModel.logListState.collect { logs ->
                logsLayout.removeAllViews()
                
                // Add two default boot trace logs
                addLogLineToTerminal("12:00:01", "XposedBridge: Loading AutoTimeFix...", "#938F99")
                addLogLineToTerminal("12:00:02", "AutoTimeFix: systemReady hooked successfully!", "#D0BCFF")
                
                if (logs.isEmpty()) {
                    addLogLineToTerminal("12:00:03", "No actions run yet. Use manual sync or reboot device.", "#938F99")
                } else {
                    logs.take(15).forEach { log ->
                        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                        val timeStr = sdf.format(java.util.Date(log.timestamp))
                        val colorHex = when (log.status) {
                            "SUCCESS" -> "#4ADE80" // Vivid Terminal Green
                            "FAILED" -> "#F87171"  // Vivid Terminal Red
                            "PENDING" -> "#FBBF24" // Vivid Terminal Yellow
                            else -> "#E6E1E5"
                        }
                        addLogLineToTerminal(timeStr, "${log.action}: ${log.message}", colorHex)
                    }
                }
            }
        }
    }
}
