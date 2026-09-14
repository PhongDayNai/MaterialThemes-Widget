package com.iatb.materialthemes.ui

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.iatb.materialthemes.R
import com.iatb.materialthemes.data.BatteryDeviceType
import com.iatb.materialthemes.data.BatteryPreferences
import com.iatb.materialthemes.data.BatteryRepository
import com.iatb.materialthemes.data.ColorPalette
import com.iatb.materialthemes.data.WidgetPreferences
import com.iatb.materialthemes.widget.BatteryWidgetProvider

class BatteryWidgetDetailActivity : AppCompatActivity() {

    private lateinit var ambientBgView: AmbientMeshBackgroundView
    private lateinit var btnBack: View
    private lateinit var cardPreview: GlassBlurCardView
    private lateinit var cardColorConfig: GlassBlurCardView
    private lateinit var cardBluetooth: GlassBlurCardView
    private lateinit var containerBatteryPreview: android.widget.FrameLayout
    private lateinit var switchFollowClockWeather: MaterialSwitch
    private lateinit var tvSyncStatusDesc: TextView
    private lateinit var layoutPalettePicker: LinearLayout

    private lateinit var btnPreviewX1: TextView
    private lateinit var btnPreviewX2: TextView
    private var isPreviewX1: Boolean = false

    private lateinit var swatchDynamic: MaterialCardView
    private lateinit var swatchCustom: MaterialCardView
    private lateinit var swatchOlive: MaterialCardView
    private lateinit var swatchTeal: MaterialCardView
    private lateinit var swatchSlate: MaterialCardView
    private lateinit var swatchAmber: MaterialCardView
    private lateinit var swatchCrimson: MaterialCardView

    private lateinit var tvLabelSwatchDynamic: TextView
    private lateinit var tvLabelSwatchCustom: TextView
    private lateinit var tvLabelSwatchOlive: TextView
    private lateinit var tvLabelSwatchTeal: TextView
    private lateinit var tvLabelSwatchSlate: TextView
    private lateinit var tvLabelSwatchAmber: TextView
    private lateinit var tvLabelSwatchCrimson: TextView

    private lateinit var discSwatchOlive: ImageView
    private lateinit var discSwatchTeal: ImageView
    private lateinit var discSwatchSlate: ImageView
    private lateinit var discSwatchAmber: ImageView
    private lateinit var discSwatchCrimson: ImageView

    private lateinit var tvTransparencyValue: TextView
    private lateinit var sliderTransparency: Slider

    private lateinit var tvBluetoothStatusTitle: TextView
    private lateinit var tvBluetoothStatusDesc: TextView
    private lateinit var btnGrantBluetooth: MaterialButton
    private lateinit var btnPinBatteryWidget: MaterialButton

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        updateBluetoothCard()
        updatePreview()
        BatteryWidgetProvider.updateAllWidgets(this)
    }

    private val batteryReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
            updatePreview()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_battery_widget_detail)

        initViews()
        setupAmbientBackground()
        setupBackNavigation()
        setupPaletteDiscs()
        setupListeners()
        updateSyncAndPaletteUi()
        updateBluetoothCard()
        updatePreview()
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
                    cardPreview.refreshBlur()
                    cardColorConfig.refreshBlur()
                    cardBluetooth.refreshBlur()
                },
                onComplete = {
                    cardPreview.refreshBlur()
                    cardColorConfig.refreshBlur()
                    cardBluetooth.refreshBlur()
                    SharedAmbientBackgroundHolder.saveSnapshot(ambientBgView.getOrbsSnapshot())
                }
            )
        }
    }

    private fun initViews() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.battery_detail_content_container)) { v, insets ->
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
        cardPreview = findViewById(R.id.card_preview)
        cardColorConfig = findViewById(R.id.card_color_config)
        cardBluetooth = findViewById(R.id.card_bluetooth)

        cardPreview.setTargetBackgroundView(ambientBgView)
        cardColorConfig.setTargetBackgroundView(ambientBgView)
        cardBluetooth.setTargetBackgroundView(ambientBgView)

        findViewById<androidx.core.widget.NestedScrollView>(R.id.scroll_content).setOnScrollChangeListener { _, _, _, _, _ ->
            cardPreview.refreshBlur()
            cardColorConfig.refreshBlur()
            cardBluetooth.refreshBlur()
        }

        btnPreviewX1 = findViewById(R.id.btn_preview_x1)
        btnPreviewX2 = findViewById(R.id.btn_preview_x2)

        containerBatteryPreview = findViewById(R.id.container_battery_preview)
        switchFollowClockWeather = findViewById(R.id.switch_follow_clock_weather)
        tvSyncStatusDesc = findViewById(R.id.tv_sync_status_desc)
        layoutPalettePicker = findViewById(R.id.layout_palette_picker)

        swatchDynamic = findViewById(R.id.swatch_dynamic)
        swatchCustom = findViewById(R.id.swatch_custom_picker)
        swatchOlive = findViewById(R.id.swatch_olive)
        swatchTeal = findViewById(R.id.swatch_teal)
        swatchSlate = findViewById(R.id.swatch_slate)
        swatchAmber = findViewById(R.id.swatch_amber)
        swatchCrimson = findViewById(R.id.swatch_crimson)

        tvLabelSwatchDynamic = findViewById(R.id.tv_label_swatch_dynamic)
        tvLabelSwatchCustom = findViewById(R.id.tv_label_swatch_custom)
        tvLabelSwatchOlive = findViewById(R.id.tv_label_swatch_olive)
        tvLabelSwatchTeal = findViewById(R.id.tv_label_swatch_teal)
        tvLabelSwatchSlate = findViewById(R.id.tv_label_swatch_slate)
        tvLabelSwatchAmber = findViewById(R.id.tv_label_swatch_amber)
        tvLabelSwatchCrimson = findViewById(R.id.tv_label_swatch_crimson)

        discSwatchOlive = findViewById(R.id.disc_swatch_olive)
        discSwatchTeal = findViewById(R.id.disc_swatch_teal)
        discSwatchSlate = findViewById(R.id.disc_swatch_slate)
        discSwatchAmber = findViewById(R.id.disc_swatch_amber)
        discSwatchCrimson = findViewById(R.id.disc_swatch_crimson)

        tvTransparencyValue = findViewById(R.id.tv_transparency_value)
        sliderTransparency = findViewById(R.id.slider_transparency)

        tvBluetoothStatusTitle = findViewById(R.id.tv_bluetooth_status_title)
        tvBluetoothStatusDesc = findViewById(R.id.tv_bluetooth_status_desc)
        btnGrantBluetooth = findViewById(R.id.btn_grant_bluetooth)
        btnPinBatteryWidget = findViewById(R.id.btn_pin_battery_widget)

        WidgetPreviewHelper.applyPressScaleEffect(btnBack)
        WidgetPreviewHelper.applyPressScaleEffect(btnPinBatteryWidget)
        WidgetPreviewHelper.applyPressScaleEffect(btnGrantBluetooth)
    }

    private fun setupPaletteDiscs() {
        val density = resources.displayMetrics.density
        val insetPx = (10 * density).toInt()

        fun createDualToneDisc(mainColor: Int, accentColor: Int): LayerDrawable {
            val bgOval = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(mainColor)
            }
            val centerDot = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(accentColor)
            }
            return LayerDrawable(arrayOf(bgOval, centerDot)).apply {
                setLayerInset(1, insetPx, insetPx, insetPx, insetPx)
            }
        }

        discSwatchOlive.setImageDrawable(createDualToneDisc(ColorPalette.OLIVE.bgColor, ColorPalette.OLIVE.textColor))
        discSwatchTeal.setImageDrawable(createDualToneDisc(ColorPalette.TEAL.bgColor, ColorPalette.TEAL.textColor))
        discSwatchSlate.setImageDrawable(createDualToneDisc(ColorPalette.SLATE.bgColor, ColorPalette.SLATE.textColor))
        discSwatchAmber.setImageDrawable(createDualToneDisc(ColorPalette.AMBER.bgColor, ColorPalette.AMBER.textColor))
        discSwatchCrimson.setImageDrawable(createDualToneDisc(ColorPalette.CRIMSON.bgColor, ColorPalette.CRIMSON.textColor))
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finishWithTransition()
        }

        containerBatteryPreview.setOnClickListener {
            updatePreview()
        }

        btnPreviewX1.setOnClickListener {
            if (!isPreviewX1) {
                isPreviewX1 = true
                updatePreviewSizeToggles()
                updatePreview()
            }
        }

        btnPreviewX2.setOnClickListener {
            if (isPreviewX1) {
                isPreviewX1 = false
                updatePreviewSizeToggles()
                updatePreview()
            }
        }

        switchFollowClockWeather.setOnCheckedChangeListener { _, isChecked ->
            BatteryPreferences.setFollowClockWeather(this, isChecked)
            updateSyncAndPaletteUi()
            updatePreview()
            BatteryWidgetProvider.updateAllWidgets(this)
        }

        swatchDynamic.setOnClickListener {
            selectPalette(ColorPalette.DYNAMIC)
        }

        swatchCustom.setOnClickListener {
            ColorWheelPickerDialog.show(this, BatteryPreferences.getCustomColor(this)) { selectedColor ->
                BatteryPreferences.setCustomColor(this, selectedColor)
                selectPalette(ColorPalette.CUSTOM)
            }
        }

        val swatchMap = listOf(
            swatchOlive to ColorPalette.OLIVE,
            swatchTeal to ColorPalette.TEAL,
            swatchSlate to ColorPalette.SLATE,
            swatchAmber to ColorPalette.AMBER,
            swatchCrimson to ColorPalette.CRIMSON
        )

        for ((card, palette) in swatchMap) {
            card.setOnClickListener {
                selectPalette(palette)
            }
        }

        sliderTransparency.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val intVal = value.toInt()
                tvTransparencyValue.text = getString(R.string.transparency_value_format, intVal)
                BatteryPreferences.setTransparency(this, intVal)
                updatePreview()
                BatteryWidgetProvider.updateAllWidgets(this)
            }
        }

        btnGrantBluetooth.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH)
            }
        }

        btnPinBatteryWidget.setOnClickListener {
            pinBatteryWidget()
        }
    }

    private fun updatePreviewSizeToggles() {
        if (isPreviewX1) {
            btnPreviewX1.setBackgroundResource(R.drawable.shape_drag_handle)
            btnPreviewX1.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.white)?.withAlpha(50)
            btnPreviewX1.setTextColor(Color.WHITE)

            btnPreviewX2.background = null
            btnPreviewX2.setTextColor(Color.argb(130, 255, 255, 255))
        } else {
            btnPreviewX2.setBackgroundResource(R.drawable.shape_drag_handle)
            btnPreviewX2.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.white)?.withAlpha(50)
            btnPreviewX2.setTextColor(Color.WHITE)

            btnPreviewX1.background = null
            btnPreviewX1.setTextColor(Color.argb(130, 255, 255, 255))
        }
    }

    private fun selectPalette(palette: ColorPalette) {
        BatteryPreferences.setColorPalette(this, palette)
        updateSyncAndPaletteUi()
        updatePreview()
        BatteryWidgetProvider.updateAllWidgets(this)
    }

    private fun updateSyncAndPaletteUi() {
        val isFollow = BatteryPreferences.isFollowClockWeather(this)
        switchFollowClockWeather.isChecked = isFollow
        layoutPalettePicker.visibility = if (isFollow) View.GONE else View.VISIBLE

        if (isFollow) {
            val syncedTrans = WidgetPreferences.getTransparency(this)
            tvSyncStatusDesc.text = "${getString(R.string.battery_section_sync_desc)} (${getString(R.string.transparency_value_format, syncedTrans)})"
        } else {
            tvSyncStatusDesc.text = getString(R.string.battery_section_sync_desc)
            val currentTrans = BatteryPreferences.getTransparency(this)
            sliderTransparency.value = currentTrans.toFloat()
            tvTransparencyValue.text = getString(R.string.transparency_value_format, currentTrans)
        }

        val activePalette = BatteryPreferences.getColorPalette(this)
        val swatches = listOf(
            swatchDynamic to (activePalette == ColorPalette.DYNAMIC),
            swatchCustom to (activePalette == ColorPalette.CUSTOM),
            swatchOlive to (activePalette == ColorPalette.OLIVE),
            swatchTeal to (activePalette == ColorPalette.TEAL),
            swatchSlate to (activePalette == ColorPalette.SLATE),
            swatchAmber to (activePalette == ColorPalette.AMBER),
            swatchCrimson to (activePalette == ColorPalette.CRIMSON)
        )

        val accentStrokeColor = ContextCompat.getColor(this, R.color.md_theme_light_primary)
        for ((card, isSelected) in swatches) {
            card.strokeColor = if (isSelected) accentStrokeColor else Color.TRANSPARENT
        }
    }

    private fun updateBluetoothCard() {
        val hasPermission = BatteryRepository.hasBluetoothPermission(this)
        if (hasPermission) {
            btnGrantBluetooth.visibility = View.GONE
            tvBluetoothStatusTitle.text = getString(R.string.battery_bluetooth_granted)
            val devices = BatteryRepository.getBatteryDevices(this)
            val btCount = devices.count { it.type != BatteryDeviceType.PHONE }
            if (btCount > 0) {
                tvBluetoothStatusDesc.text = getString(R.string.battery_widget_description)
            } else {
                tvBluetoothStatusDesc.text = getString(R.string.battery_bluetooth_no_devices)
            }
        } else {
            btnGrantBluetooth.visibility = View.VISIBLE
            tvBluetoothStatusTitle.text = getString(R.string.battery_bluetooth_permission_title)
            tvBluetoothStatusDesc.text = getString(R.string.battery_bluetooth_permission_desc)
        }
    }

    private fun updatePreview() {
        val devices = BatteryRepository.getBatteryDevices(this)
        val palette = BatteryPreferences.resolveColors(this)

        val density = resources.displayMetrics.density
        val targetHeightDp = if (isPreviewX1) 68 else if (devices.size >= 3) 220 else 156
        containerBatteryPreview.layoutParams = containerBatteryPreview.layoutParams.apply {
            height = (targetHeightDp * density).toInt()
        }

        val previewRemoteViews = if (isPreviewX1) {
            BatteryWidgetProvider.buildRowRemoteViews(this, 320, 68, devices, palette)
        } else {
            BatteryWidgetProvider.buildCardRemoteViews(this, 320, if (devices.size >= 3) 220 else 148, devices, palette)
        }

        try {
            val inflatedView = previewRemoteViews.apply(applicationContext, containerBatteryPreview)
            containerBatteryPreview.removeAllViews()
            containerBatteryPreview.addView(inflatedView)
            cardPreview.post { cardPreview.refreshBlur() }
        } catch (e: Exception) {
            android.util.Log.e("BatteryDetail", "Failed to inflate preview RemoteViews", e)
        }
    }

    private fun pinBatteryWidget() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            val componentName = ComponentName(this, BatteryWidgetProvider::class.java)
            val devices = BatteryRepository.getBatteryDevices(this)
            val palette = BatteryPreferences.resolveColors(this)
            val previewViews = if (isPreviewX1) {
                BatteryWidgetProvider.buildRowRemoteViews(this, 300, 68, devices, palette)
            } else {
                BatteryWidgetProvider.buildCardRemoteViews(this, 300, 130, devices, palette)
            }

            val extras = Bundle().apply {
                putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, previewViews)
            }
            appWidgetManager.requestPinAppWidget(componentName, extras, null)
        } else {
            Toast.makeText(this, R.string.resize_tip, Toast.LENGTH_SHORT).show()
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
        cardPreview.refreshBlur()
        cardColorConfig.refreshBlur()
        cardBluetooth.refreshBlur()
        updateBluetoothCard()
        updateSyncAndPaletteUi()
        updatePreview()

        val filter = android.content.IntentFilter().apply {
            addAction(android.content.Intent.ACTION_BATTERY_CHANGED)
            addAction(android.content.Intent.ACTION_POWER_CONNECTED)
            addAction(android.content.Intent.ACTION_POWER_DISCONNECTED)
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED")
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
