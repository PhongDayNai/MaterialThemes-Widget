package com.iatb.materialthemes.ui

import android.Manifest
import android.appwidget.AppWidgetManager
import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RemoteViews
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
    private lateinit var cardBatteryPrediction: GlassBlurCardView
    private lateinit var tvPredictionStatusTitle: TextView
    private lateinit var tvPredictionStatusDesc: TextView
    private lateinit var btnGrantPrediction: MaterialButton
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
            updatePreview(forceCharging = forceCharging, batteryIntent = intent)
            updateBluetoothCard()
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
        updatePredictionCard()
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
                    cardBatteryPrediction.refreshBlur()
                },
                onComplete = {
                    cardPreview.refreshBlur()
                    cardColorConfig.refreshBlur()
                    cardBluetooth.refreshBlur()
                    cardBatteryPrediction.refreshBlur()
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
        cardBatteryPrediction = findViewById(R.id.card_battery_prediction)

        cardPreview.setTargetBackgroundView(ambientBgView)
        cardColorConfig.setTargetBackgroundView(ambientBgView)
        cardBluetooth.setTargetBackgroundView(ambientBgView)
        cardBatteryPrediction.setTargetBackgroundView(ambientBgView)

        findViewById<androidx.core.widget.NestedScrollView>(R.id.scroll_content).setOnScrollChangeListener { _, _, _, _, _ ->
            cardPreview.refreshBlur()
            cardColorConfig.refreshBlur()
            cardBluetooth.refreshBlur()
            cardBatteryPrediction.refreshBlur()
        }

        tvPredictionStatusTitle = findViewById(R.id.tv_prediction_status_title)
        tvPredictionStatusDesc = findViewById(R.id.tv_prediction_status_desc)
        btnGrantPrediction = findViewById(R.id.btn_grant_prediction)

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

        switchFollowClockWeather.setOnCheckedChangeListener { _, isChecked ->
            BatteryPreferences.setFollowClockWeather(this, isChecked)
            updateSyncAndPaletteUi(animate = true)
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
            }
        }

        btnGrantPrediction.setOnClickListener {
            showBatteryPredictionDialog()
        }

        btnPinBatteryWidget.setOnClickListener {
            pinBatteryWidget()
        }
    }



    private fun selectPalette(palette: ColorPalette) {
        BatteryPreferences.setColorPalette(this, palette)
        updateSyncAndPaletteUi(animate = false)
        updatePreview()
        BatteryWidgetProvider.updateAllWidgets(this)
    }

    private fun updateSyncAndPaletteUi(animate: Boolean = false) {
        val isFollow = BatteryPreferences.isFollowClockWeather(this)
        switchFollowClockWeather.isChecked = isFollow

        val targetVisibility = if (isFollow) View.GONE else View.VISIBLE
        if (layoutPalettePicker.visibility != targetVisibility) {
            if (animate) {
                val container = findViewById<ViewGroup>(R.id.battery_detail_content_container)
                val transition = android.transition.TransitionSet().apply {
                    ordering = android.transition.TransitionSet.ORDERING_TOGETHER
                    addTransition(android.transition.ChangeBounds())
                    addTransition(android.transition.Fade())
                    duration = 280L
                    interpolator = android.view.animation.DecelerateInterpolator(1.4f)
                    addListener(object : android.transition.Transition.TransitionListener {
                        override fun onTransitionStart(transition: android.transition.Transition?) {}
                        override fun onTransitionEnd(transition: android.transition.Transition?) {
                            cardColorConfig.refreshBlur()
                            cardBluetooth.refreshBlur()
                        }
                        override fun onTransitionCancel(transition: android.transition.Transition?) {}
                        override fun onTransitionPause(transition: android.transition.Transition?) {}
                        override fun onTransitionResume(transition: android.transition.Transition?) {}
                    })
                }
                android.transition.TransitionManager.beginDelayedTransition(container, transition)
            }
            layoutPalettePicker.visibility = targetVisibility
        }

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

    private fun updatePredictionCard() {
        val hasPermission = BatteryRepository.hasBatteryStatsPermission(this)
        if (hasPermission) {
            btnGrantPrediction.visibility = View.GONE
            tvPredictionStatusTitle.text = getString(R.string.battery_prediction_granted_title)
            tvPredictionStatusDesc.text = getString(R.string.battery_prediction_granted_desc)
        } else {
            btnGrantPrediction.visibility = View.VISIBLE
            tvPredictionStatusTitle.text = getString(R.string.battery_prediction_title)
            tvPredictionStatusDesc.text = getString(R.string.battery_prediction_desc)
        }
    }

    private fun showBatteryPredictionDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.battery_prediction_dialog_title)
            .setMessage(getString(R.string.battery_prediction_dialog_message, packageName))
            .setPositiveButton(R.string.battery_prediction_dialog_btn_settings) { _, _ ->
                val intent = Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:$packageName")
                }
                try {
                    startActivity(intent)
                } catch (_: Exception) {
                    try {
                        startActivity(Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    } catch (_: Exception) {}
                }
            }
            .setNegativeButton(R.string.battery_prediction_dialog_btn_close, null)
            .show()
    }

    private fun updatePreview(forceCharging: Boolean? = null, batteryIntent: Intent? = null) {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val widgetIds = appWidgetManager.getAppWidgetIds(ComponentName(this, BatteryWidgetProvider::class.java))

        val passedId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val activeWidgetId = if (passedId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            passedId
        } else {
            widgetIds.lastOrNull() ?: AppWidgetManager.INVALID_APPWIDGET_ID
        }

        val options = if (activeWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            appWidgetManager.getAppWidgetOptions(activeWidgetId)
        } else null

        val minW = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) ?: 0
        val minH = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0

        val devices = BatteryRepository.getBatteryDevices(this, forceCharging, batteryIntent)
        val palette = BatteryPreferences.resolveColors(this)
        val density = resources.displayMetrics.density

        val previewRemoteViews: RemoteViews
        val targetHeightDp: Int
        val isRow = minH == 0 || minH < 95

        if (isRow) {
            targetHeightDp = if (minH > 0) minH.coerceIn(54, 80) else 68
            val actualW = if (minW > 0) minW else 300
            previewRemoteViews = BatteryWidgetProvider.buildRowRemoteViews(this, actualW, targetHeightDp, devices, palette, activeWidgetId)
        } else if (minH < 180) {
            targetHeightDp = minH.coerceIn(120, 160)
            val actualW = if (minW > 0) minW else 300
            previewRemoteViews = BatteryWidgetProvider.buildCardRemoteViews(this, actualW, targetHeightDp, devices, palette, activeWidgetId)
        } else {
            targetHeightDp = minH.coerceIn(180, 280)
            val actualW = if (minW > 0) minW else 300
            previewRemoteViews = BatteryWidgetProvider.buildTallRemoteViews(this, actualW, targetHeightDp, devices, palette, activeWidgetId)
        }

        val lp = (containerBatteryPreview.layoutParams as? LinearLayout.LayoutParams)
            ?: LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (targetHeightDp * density).toInt()
            )
        lp.height = (targetHeightDp * density).toInt()
        if (minW in 1..230) {
            // Narrow 2x1 cell: center exactly at the launcher's physical cell width
            lp.width = (minW * density).toInt()
            lp.gravity = Gravity.CENTER_HORIZONTAL
        } else {
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT
            lp.gravity = Gravity.CENTER_HORIZONTAL
        }
        containerBatteryPreview.layoutParams = lp

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
            val previewViews = BatteryWidgetProvider.buildRowRemoteViews(this, 300, 68, devices, palette)

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
        cardBatteryPrediction.refreshBlur()
        updateBluetoothCard()
        updatePredictionCard()
        updateSyncAndPaletteUi()
        updatePreview()

        val filter = android.content.IntentFilter().apply {
            addAction(android.content.Intent.ACTION_BATTERY_CHANGED)
            addAction(android.content.Intent.ACTION_POWER_CONNECTED)
            addAction(android.content.Intent.ACTION_POWER_DISCONNECTED)
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED")
            addAction(android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED)
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
