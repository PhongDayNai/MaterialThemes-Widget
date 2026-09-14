package com.iatb.materialthemes.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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
    private lateinit var tvBatteryQuickStatus: TextView

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

    private fun updateBatteryStatus() {
        val devices = BatteryRepository.getBatteryDevices(this)
        val phone = devices.firstOrNull { it.type == BatteryDeviceType.PHONE }
        val btCount = devices.count { it.type != BatteryDeviceType.PHONE }

        if (phone != null) {
            val baseStatus = phone.statusText ?: if (phone.isCharging) {
                getString(R.string.battery_status_charging)
            } else {
                getString(R.string.battery_status_discharging)
            }

            val fullStatus = if (btCount > 0) {
                "${phone.levelPercent}% • $baseStatus • $btCount ${getString(R.string.battery_bluetooth_permission_title)}"
            } else {
                "${phone.levelPercent}% • $baseStatus"
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
    }

    override fun onPause() {
        super.onPause()
        SharedAmbientBackgroundHolder.saveSnapshot(ambientBgView.getOrbsSnapshot())
    }
}
