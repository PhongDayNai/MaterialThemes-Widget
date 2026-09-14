package com.iatb.materialthemes.ui

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.iatb.materialthemes.R
import com.iatb.materialthemes.data.BatteryDeviceType
import com.iatb.materialthemes.data.BatteryRepository

class UtilityWidgetsActivity : AppCompatActivity() {

    private lateinit var ambientBgView: AmbientMeshBackgroundView
    private lateinit var btnBack: View
    private lateinit var cardUtilityBattery: GlassBlurCardView
    private lateinit var cardUtilityComingSoon: GlassBlurCardView
    private lateinit var flBatteryIconContainer: FrameLayout
    private lateinit var progressBatteryIcon: com.google.android.material.progressindicator.CircularProgressIndicator
    private lateinit var ivBatteryIcon: ImageView
    private lateinit var tvBatteryPercentBadge: TextView
    private lateinit var tvBatteryQuickStatus: TextView

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED") {
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
                val level = intent.getIntExtra("android.bluetooth.device.extra.BATTERY_LEVEL", -1)
                if (device != null && level in 0..100) {
                    BatteryRepository.updateCachedBatteryLevel(device.address, level)
                }
            }

            val forceCharging = when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> true
                Intent.ACTION_POWER_DISCONNECTED -> false
                else -> null
            }
            updateBatteryStatus(forceCharging = forceCharging, batteryIntent = intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_utility_widgets)

        initViews()
        setupAmbientBackground()
        setupBackNavigation()
        setupListeners()
    }

    private fun setupAmbientBackground() {
        val snapshot = SharedAmbientBackgroundHolder.getSnapshot()
        if (snapshot != null) {
            ambientBgView.setOrbs(snapshot)
        }
        ambientBgView.post {
            ambientBgView.transitionToNewConstellation(
                durationMs = 950L,
                onUpdate = {
                    cardUtilityBattery.refreshBlur()
                    cardUtilityComingSoon.refreshBlur()
                },
                onComplete = {
                    cardUtilityBattery.refreshBlur()
                    cardUtilityComingSoon.refreshBlur()
                    SharedAmbientBackgroundHolder.saveSnapshot(ambientBgView.getOrbsSnapshot())
                }
            )
        }
    }

    private fun initViews() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.utility_content_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val density = resources.displayMetrics.density
            val hPad = (22 * density).toInt()
            v.setPadding(
                hPad + systemBars.left,
                systemBars.top + (14 * density).toInt(),
                hPad + systemBars.right,
                systemBars.bottom + (48 * density).toInt()
            )
            insets
        }

        ambientBgView = findViewById(R.id.ambient_bg_view)
        btnBack = findViewById(R.id.btn_back)
        cardUtilityBattery = findViewById(R.id.card_utility_battery)
        cardUtilityComingSoon = findViewById(R.id.card_utility_coming_soon)
        flBatteryIconContainer = findViewById(R.id.fl_battery_icon_container)
        progressBatteryIcon = findViewById(R.id.progress_battery_icon)
        ivBatteryIcon = findViewById(R.id.iv_battery_icon)
        tvBatteryPercentBadge = findViewById(R.id.tv_battery_percent_badge)
        tvBatteryQuickStatus = findViewById(R.id.tv_battery_quick_status)

        cardUtilityBattery.setTargetBackgroundView(ambientBgView)
        cardUtilityComingSoon.setTargetBackgroundView(ambientBgView)

        findViewById<androidx.core.widget.NestedScrollView>(R.id.scroll_content).setOnScrollChangeListener { _, _, _, _, _ ->
            cardUtilityBattery.refreshBlur()
            cardUtilityComingSoon.refreshBlur()
        }

        WidgetPreviewHelper.applyPressScaleEffect(btnBack)
        WidgetPreviewHelper.applyPressScaleEffect(cardUtilityBattery)
        WidgetPreviewHelper.applyPressScaleEffect(cardUtilityComingSoon)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finishWithTransition()
        }

        cardUtilityBattery.setOnClickListener {
            SharedAmbientBackgroundHolder.saveSnapshot(ambientBgView.getOrbsSnapshot())
            val intent = Intent(this, BatteryWidgetDetailActivity::class.java)
            startActivity(intent)
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        cardUtilityComingSoon.setOnClickListener {
            Toast.makeText(this, R.string.toast_coming_soon, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateBatteryStatus(forceCharging: Boolean? = null, batteryIntent: Intent? = null) {
        val devices = BatteryRepository.getBatteryDevices(this, forceCharging, batteryIntent)
        val phone = devices.firstOrNull { it.type == BatteryDeviceType.PHONE }
        val btDevices = devices.filter { it.type != BatteryDeviceType.PHONE }

        if (phone != null) {
            tvBatteryPercentBadge.visibility = View.VISIBLE
            tvBatteryPercentBadge.text = "${phone.levelPercent}%"

            val exactProgress = phone.levelPercent.coerceIn(0, 100)
            progressBatteryIcon.setProgressCompat(exactProgress, true)

            val primaryColor = ContextCompat.getColor(this, R.color.md_theme_light_primary)
            if (phone.isCharging) {
                ivBatteryIcon.setImageResource(R.drawable.ic_bolt)
                progressBatteryIcon.setIndicatorColor(Color.parseColor("#4CAF50"))
                tvBatteryPercentBadge.backgroundTintList = ColorStateList.valueOf(0x334CAF50)
                tvBatteryPercentBadge.setTextColor(Color.parseColor("#4CAF50"))
            } else {
                ivBatteryIcon.setImageResource(R.drawable.ic_battery_std)
                if (phone.levelPercent <= 20) {
                    progressBatteryIcon.setIndicatorColor(Color.parseColor("#FF5252"))
                    tvBatteryPercentBadge.backgroundTintList = ColorStateList.valueOf(0x33FF5252)
                    tvBatteryPercentBadge.setTextColor(Color.parseColor("#FF5252"))
                } else {
                    progressBatteryIcon.setIndicatorColor(primaryColor)
                    tvBatteryPercentBadge.backgroundTintList = ColorStateList.valueOf(0x24FFFFFF)
                    tvBatteryPercentBadge.setTextColor(Color.WHITE)
                }
            }

            val baseStatus = phone.statusText ?: if (phone.isCharging) {
                getString(R.string.battery_status_charging)
            } else {
                getString(R.string.battery_status_discharging)
            }

            val fullStatus = if (btDevices.isNotEmpty()) {
                val btSummary = btDevices.joinToString(" • ") { dev ->
                    if (dev.levelPercent >= 0) "${dev.name} ${dev.levelPercent}%" else dev.name
                }
                "$baseStatus • $btSummary"
            } else {
                baseStatus
            }
            tvBatteryQuickStatus.text = fullStatus
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishWithTransition()
            }
        })
    }

    private fun finishWithTransition() {
        SharedAmbientBackgroundHolder.saveSnapshot(ambientBgView.getOrbsSnapshot())
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onResume() {
        super.onResume()
        cardUtilityBattery.refreshBlur()
        cardUtilityComingSoon.refreshBlur()
        updateBatteryStatus()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED")
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        try {
            ContextCompat.registerReceiver(this, batteryReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        } catch (_: Exception) {
            try { registerReceiver(batteryReceiver, filter) } catch (_: Exception) {}
        }
    }

    override fun onPause() {
        super.onPause()
        runCatching { unregisterReceiver(batteryReceiver) }
        SharedAmbientBackgroundHolder.saveSnapshot(ambientBgView.getOrbsSnapshot())
    }
}
