package com.iatb.materialthemes.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.iatb.materialthemes.R
import com.iatb.materialthemes.data.ColorPalette
import com.iatb.materialthemes.data.DayProgressPreferences
import com.iatb.materialthemes.data.DayProgressRepository
import com.iatb.materialthemes.data.WidgetPreferences
import com.iatb.materialthemes.widget.DayProgressWidgetProvider

class DayProgressWidgetDetailActivity : AppCompatActivity() {

    private lateinit var ambientBgView: AmbientMeshBackgroundView
    private lateinit var btnBack: View
    private lateinit var cardPreview: GlassBlurCardView
    private lateinit var flPreviewHost: FrameLayout
    private lateinit var cardColorConfig: GlassBlurCardView
    private lateinit var cardDayStats: GlassBlurCardView

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

    private lateinit var discSwatchOlive: ImageView
    private lateinit var discSwatchTeal: ImageView
    private lateinit var discSwatchSlate: ImageView
    private lateinit var discSwatchAmber: ImageView
    private lateinit var discSwatchCrimson: ImageView

    private lateinit var sliderTransparency: Slider
    private lateinit var tvTransparencyValue: TextView
    private lateinit var btnPinWidget: MaterialButton

    private lateinit var tvStatElapsedValue: TextView
    private lateinit var tvStatTimezoneValue: TextView
    private lateinit var tvStatDstValue: TextView

    private var activeWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_day_progress_widget_detail)

        activeWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        initViews()
        setupAmbientBackground()
        setupBackNavigation()
        setupPaletteDiscs()
        setupListeners()
        updateSyncAndPaletteUi(animate = false)
        updateDayStats()
        renderWidgetPreview()
    }

    private fun initViews() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.day_progress_detail_content_container)) { v, insets ->
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
        flPreviewHost = findViewById(R.id.fl_preview_host)
        cardColorConfig = findViewById(R.id.card_color_config)
        cardDayStats = findViewById(R.id.card_day_stats)

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

        discSwatchOlive = findViewById(R.id.disc_swatch_olive)
        discSwatchTeal = findViewById(R.id.disc_swatch_teal)
        discSwatchSlate = findViewById(R.id.disc_swatch_slate)
        discSwatchAmber = findViewById(R.id.disc_swatch_amber)
        discSwatchCrimson = findViewById(R.id.disc_swatch_crimson)

        sliderTransparency = findViewById(R.id.slider_transparency)
        tvTransparencyValue = findViewById(R.id.tv_transparency_value)
        btnPinWidget = findViewById(R.id.btn_pin_widget)

        tvStatElapsedValue = findViewById(R.id.tv_stat_elapsed_value)
        tvStatTimezoneValue = findViewById(R.id.tv_stat_timezone_value)
        tvStatDstValue = findViewById(R.id.tv_stat_dst_value)

        cardPreview.setTargetBackgroundView(ambientBgView)
        cardColorConfig.setTargetBackgroundView(ambientBgView)
        cardDayStats.setTargetBackgroundView(ambientBgView)

        findViewById<androidx.core.widget.NestedScrollView>(R.id.scroll_content).setOnScrollChangeListener { _, _, _, _, _ ->
            refreshAllBlur()
        }

        WidgetPreviewHelper.applyPressScaleEffect(btnBack)
        WidgetPreviewHelper.applyPressScaleEffect(btnPinWidget)
        btnBack.setOnClickListener { finishWithTransition() }
    }

    private fun refreshAllBlur() {
        cardPreview.refreshBlur()
        cardColorConfig.refreshBlur()
        cardDayStats.refreshBlur()
    }

    private fun setupAmbientBackground() {
        val snapshot = SharedAmbientBackgroundHolder.getSnapshot()
        if (snapshot != null) {
            ambientBgView.setOrbs(snapshot)
        }
        ambientBgView.post {
            ambientBgView.transitionToNewConstellation(
                durationMs = 950L,
                onUpdate = { refreshAllBlur() },
                onComplete = {
                    refreshAllBlur()
                    SharedAmbientBackgroundHolder.saveSnapshot(ambientBgView.getOrbsSnapshot())
                }
            )
        }
    }

    private fun setupPaletteDiscs() {
        discSwatchOlive.background = createDiscDrawable(ColorPalette.OLIVE.bgColor)
        discSwatchTeal.background = createDiscDrawable(ColorPalette.TEAL.bgColor)
        discSwatchSlate.background = createDiscDrawable(ColorPalette.SLATE.bgColor)
        discSwatchAmber.background = createDiscDrawable(ColorPalette.AMBER.bgColor)
        discSwatchCrimson.background = createDiscDrawable(ColorPalette.CRIMSON.bgColor)
    }

    private fun createDiscDrawable(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
    }

    private fun setupListeners() {
        switchFollowClockWeather.setOnCheckedChangeListener { _, isChecked ->
            DayProgressPreferences.setFollowClockWeather(this, isChecked)
            updateSyncAndPaletteUi(animate = true)
            DayProgressWidgetProvider.updateAllWidgets(this)
            renderWidgetPreview()
        }

        val swatchList = listOf(
            swatchDynamic to ColorPalette.DYNAMIC,
            swatchOlive to ColorPalette.OLIVE,
            swatchTeal to ColorPalette.TEAL,
            swatchSlate to ColorPalette.SLATE,
            swatchAmber to ColorPalette.AMBER,
            swatchCrimson to ColorPalette.CRIMSON
        )
        for ((card, palette) in swatchList) {
            WidgetPreviewHelper.applyPressScaleEffect(card)
            card.setOnClickListener {
                selectPalette(palette)
            }
        }

        WidgetPreviewHelper.applyPressScaleEffect(swatchCustom)
        swatchCustom.setOnClickListener {
            showColorPickerDialog()
        }

        sliderTransparency.addOnChangeListener { _, value, fromUser ->
            val v = value.toInt()
            tvTransparencyValue.text = getString(R.string.transparency_value_format, v)
            if (fromUser) {
                if (switchFollowClockWeather.isChecked) {
                    WidgetPreferences.setTransparency(this, v)
                } else {
                    DayProgressPreferences.setTransparency(this, v)
                }
                DayProgressWidgetProvider.updateAllWidgets(this)
                renderWidgetPreview()
            }
        }

        btnPinWidget.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val appWidgetManager = AppWidgetManager.getInstance(this)
                val provider = ComponentName(this, DayProgressWidgetProvider::class.java)
                if (appWidgetManager.isRequestPinAppWidgetSupported) {
                    appWidgetManager.requestPinAppWidget(provider, null, null)
                }
            }
        }
    }

    private fun selectPalette(palette: ColorPalette) {
        DayProgressPreferences.setColorPalette(this, palette)
        updateSyncAndPaletteUi(animate = false)
        DayProgressWidgetProvider.updateAllWidgets(this)
        renderWidgetPreview()
    }

    private fun showColorPickerDialog() {
        val colors = intArrayOf(
            0xFF3F51B5.toInt(), 0xFF009688.toInt(), 0xFFE91E63.toInt(), 0xFFFF9800.toInt(),
            0xFF9C27B0.toInt(), 0xFF2196F3.toInt(), 0xFF4CAF50.toInt(), 0xFFFF5722.toInt(),
            0xFF607D8B.toInt(), 0xFF795548.toInt(), 0xFF00BCD4.toInt(), 0xFF673AB7.toInt()
        )
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle(R.string.theme_custom)

        val grid = android.widget.GridLayout(this).apply {
            columnCount = 4
            setPadding(32, 24, 32, 24)
        }

        var dialog: androidx.appcompat.app.AlertDialog? = null
        for (c in colors) {
            val colorView = View(this).apply {
                layoutParams = android.widget.GridLayout.LayoutParams().apply {
                    width = (48 * resources.displayMetrics.density).toInt()
                    height = (48 * resources.displayMetrics.density).toInt()
                    setMargins(16, 16, 16, 16)
                }
                setBackgroundResource(R.drawable.shape_circle)
                backgroundTintList = ColorStateList.valueOf(c)
                setOnClickListener {
                    DayProgressPreferences.setCustomColor(this@DayProgressWidgetDetailActivity, c)
                    selectPalette(ColorPalette.CUSTOM)
                    dialog?.dismiss()
                }
            }
            grid.addView(colorView)
        }
        builder.setView(grid)
        builder.setNegativeButton(android.R.string.cancel, null)
        dialog = builder.create()
        dialog.show()
    }

    private fun updateSyncAndPaletteUi(animate: Boolean = false) {
        val isFollow = DayProgressPreferences.isFollowClockWeather(this)
        switchFollowClockWeather.isChecked = isFollow

        val targetVisibility = if (isFollow) View.GONE else View.VISIBLE
        if (layoutPalettePicker.visibility != targetVisibility) {
            if (animate) {
                val container = findViewById<ViewGroup>(R.id.day_progress_detail_content_container)
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
                            cardDayStats.refreshBlur()
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
            val currentTrans = DayProgressPreferences.getTransparency(this)
            sliderTransparency.value = currentTrans.toFloat()
            tvTransparencyValue.text = getString(R.string.transparency_value_format, currentTrans)
        }

        val activePalette = DayProgressPreferences.getColorPalette(this)
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

    private fun updateDayStats() {
        val info = DayProgressRepository.getDayProgress(this)
        tvStatElapsedValue.text = DayProgressRepository.formatElapsedSummary(this, info) + " (${info.percentage}%)"
        tvStatTimezoneValue.text = "${info.timezoneId} (${info.timezoneOffset})"
        tvStatDstValue.text = if (info.isDst) {
            getString(R.string.day_progress_dst_active)
        } else {
            getString(R.string.day_progress_dst_inactive)
        }
    }

    private fun renderWidgetPreview() {
        flPreviewHost.removeAllViews()

        val appWidgetManager = AppWidgetManager.getInstance(this)
        val density = resources.displayMetrics.density

        val (targetWidthDp, targetHeightDp) = if (activeWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val options = appWidgetManager.getAppWidgetOptions(activeWidgetId)
            val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            if (minW > 0 && minH > 0) Pair(minW, minH) else Pair(320, 68)
        } else {
            Pair(320, 68)
        }

        val rv = if (targetHeightDp < 95) {
            val info = DayProgressRepository.getDayProgress(this)
            val colors = DayProgressPreferences.resolveColors(this)
            DayProgressWidgetProvider.buildRowRemoteViews(this, targetWidthDp, targetHeightDp, info, colors, activeWidgetId)
        } else if (targetWidthDp <= 150 && targetHeightDp <= 150) {
            val info = DayProgressRepository.getDayProgress(this)
            val colors = DayProgressPreferences.resolveColors(this)
            DayProgressWidgetProvider.buildCompactRemoteViews(this, targetWidthDp, targetHeightDp, info, colors, activeWidgetId)
        } else if (targetHeightDp < 180) {
            val info = DayProgressRepository.getDayProgress(this)
            val colors = DayProgressPreferences.resolveColors(this)
            DayProgressWidgetProvider.buildCardRemoteViews(this, targetWidthDp, targetHeightDp, info, colors, activeWidgetId)
        } else {
            val info = DayProgressRepository.getDayProgress(this)
            val colors = DayProgressPreferences.resolveColors(this)
            DayProgressWidgetProvider.buildTallRemoteViews(this, targetWidthDp, targetHeightDp, info, colors, activeWidgetId)
        }

        try {
            val previewView = rv.apply(applicationContext, flPreviewHost)
            val lp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                (targetHeightDp * density).toInt().coerceAtLeast((64 * density).toInt())
            )
            flPreviewHost.addView(previewView, lp)
        } catch (e: Exception) {
            val fallbackTv = TextView(this).apply {
                text = "64% • Day Progress"
                setTextColor(Color.WHITE)
            }
            flPreviewHost.addView(fallbackTv)
        }

        flPreviewHost.post { cardPreview.refreshBlur() }
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
        refreshAllBlur()
        updateDayStats()
        renderWidgetPreview()
    }

    override fun onPause() {
        super.onPause()
        SharedAmbientBackgroundHolder.saveSnapshot(ambientBgView.getOrbsSnapshot())
    }
}
