package com.iatb.materialthemes.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.iatb.materialthemes.R
import com.iatb.materialthemes.data.DayProgressInfo
import com.iatb.materialthemes.data.DayProgressPreferences
import com.iatb.materialthemes.data.DayProgressRepository
import com.iatb.materialthemes.data.DaySolarTimePhase
import com.iatb.materialthemes.data.SeasonTimePaletteResolver
import com.iatb.materialthemes.widget.DayProgressWidgetProvider

class DayProgressWidgetDetailActivity : AppCompatActivity() {

    private lateinit var ambientBgView: AmbientMeshBackgroundView
    private lateinit var btnBack: View
    private lateinit var cardPreview: GlassBlurCardView
    private lateinit var containerDayProgressPreview: FrameLayout
    private lateinit var flPreviewHost: FrameLayout
    private lateinit var cardTransparencyConfig: GlassBlurCardView
    private lateinit var cardDayStats: GlassBlurCardView

    private lateinit var sliderTransparency: Slider
    private lateinit var tvTransparencyValue: TextView
    private lateinit var btnPinWidget: MaterialButton

    private lateinit var tvStatElapsedValue: TextView
    private lateinit var tvStatTimezoneValue: TextView
    private lateinit var tvStatDstValue: TextView
    private lateinit var tvStatSolarValue: TextView
    private lateinit var tvStatDaylightValue: TextView

    private var activeWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private var simulatedPhaseIndex: Int = -1

    private var lastColors: DayProgressPreferences.DayProgressThemeColors? = null
    private var colorAnimator: ValueAnimator? = null

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
        setupTransparency()
        setupListeners()
        updateDayStats()
        renderWidgetPreview(animate = false)
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
        containerDayProgressPreview = findViewById(R.id.container_day_progress_preview)
        flPreviewHost = findViewById(R.id.fl_preview_host)
        cardTransparencyConfig = findViewById(R.id.card_transparency_config)
        cardDayStats = findViewById(R.id.card_day_stats)

        sliderTransparency = findViewById(R.id.slider_transparency)
        tvTransparencyValue = findViewById(R.id.tv_transparency_value)
        btnPinWidget = findViewById(R.id.btn_pin_widget)

        tvStatElapsedValue = findViewById(R.id.tv_stat_elapsed_value)
        tvStatTimezoneValue = findViewById(R.id.tv_stat_timezone_value)
        tvStatDstValue = findViewById(R.id.tv_stat_dst_value)
        tvStatSolarValue = findViewById(R.id.tv_stat_solar_value)
        tvStatDaylightValue = findViewById(R.id.tv_stat_daylight_value)

        cardPreview.setTargetBackgroundView(ambientBgView)
        cardTransparencyConfig.setTargetBackgroundView(ambientBgView)
        cardDayStats.setTargetBackgroundView(ambientBgView)

        findViewById<NestedScrollView>(R.id.scroll_content).setOnScrollChangeListener { _, _, _, _, _ ->
            refreshAllBlur()
        }

        WidgetPreviewHelper.applyPressScaleEffect(btnBack)
        WidgetPreviewHelper.applyPressScaleEffect(btnPinWidget)
        WidgetPreviewHelper.applyPressScaleEffect(containerDayProgressPreview)
        btnBack.setOnClickListener { finishWithTransition() }
    }

    private fun refreshAllBlur() {
        cardPreview.refreshBlur()
        cardTransparencyConfig.refreshBlur()
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

    private fun setupTransparency() {
        val currentTrans = DayProgressPreferences.getTransparency(this)
        sliderTransparency.value = currentTrans.toFloat()
        tvTransparencyValue.text = getString(R.string.transparency_value_format, currentTrans)
    }

    private fun setupListeners() {
        sliderTransparency.addOnChangeListener { _, value, fromUser ->
            val v = value.toInt()
            tvTransparencyValue.text = getString(R.string.transparency_value_format, v)
            if (fromUser) {
                DayProgressPreferences.setTransparency(this, v)
                DayProgressWidgetProvider.updateAllWidgets(this)
                renderWidgetPreview(animate = true)
            }
        }

        // Tap preview to cycle solar phases and experience color transition animation
        containerDayProgressPreview.setOnClickListener {
            simulatedPhaseIndex = (simulatedPhaseIndex + 2) % 6 - 1
            renderWidgetPreview(animate = true)
        }

        btnPinWidget.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val appWidgetManager = AppWidgetManager.getInstance(this)
                val provider = ComponentName(this, DayProgressWidgetProvider::class.java)
                if (appWidgetManager.isRequestPinAppWidgetSupported) {
                    val info = DayProgressRepository.getDayProgress(this)
                    val colors = DayProgressPreferences.resolveDayProgressColors(this, info.sunriseMillis, info.sunsetMillis)
                    val previewViews = DayProgressWidgetProvider.buildCardRemoteViews(this, 320, 138, info, colors)
                    val extras = Bundle().apply {
                        putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, previewViews)
                    }
                    appWidgetManager.requestPinAppWidget(provider, extras, null)
                }
            }
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
        tvStatSolarValue.text = "${info.sunriseFormatted} • ${info.sunsetFormatted}"
        tvStatDaylightValue.text = info.daylightDurationText
    }

    private fun getEffectiveColors(info: DayProgressInfo): Pair<DayProgressPreferences.DayProgressThemeColors, DayProgressInfo> {
        val transparency = DayProgressPreferences.getTransparency(this)
        val alphaInt = ((transparency.coerceIn(0, 100) / 100f) * 255).toInt()

        fun applyAlpha(color: Int): Int {
            return Color.argb(
                ((Color.alpha(color) / 255f) * (alphaInt / 255f) * 255).toInt().coerceIn(0, 255),
                Color.red(color),
                Color.green(color),
                Color.blue(color)
            )
        }

        val season = SeasonTimePaletteResolver.getSeason()
        val allPhases = DaySolarTimePhase.values()
        val phase = if (simulatedPhaseIndex in 0..4) {
            allPhases[simulatedPhaseIndex]
        } else {
            SeasonTimePaletteResolver.getSolarPhase(System.currentTimeMillis(), info.sunriseMillis, info.sunsetMillis)
        }

        val seasonal = SeasonTimePaletteResolver.getSeasonalSolarPalette(season, phase)
        val colors = DayProgressPreferences.DayProgressThemeColors(
            bgColor = applyAlpha(seasonal.cardBgColor),
            pillBgColor = applyAlpha(seasonal.pillBgColor),
            chipBgColor = applyAlpha(seasonal.chipBgColor),
            textColor = seasonal.textColor,
            subTextColor = seasonal.subTextColor,
            progressStartColor = seasonal.progressStartColor,
            progressEndColor = seasonal.progressEndColor,
            accentColor = seasonal.accentColor
        )

        val effectiveInfo = if (simulatedPhaseIndex in 0..4) {
            val simProgress: Float
            val phaseTitleRes: Int
            val phaseIcon: Int
            when (phase) {
                DaySolarTimePhase.DAWN -> {
                    simProgress = 0.22f
                    phaseTitleRes = R.string.day_phase_dawn
                    phaseIcon = R.drawable.ic_sunrise
                }
                DaySolarTimePhase.DAYLIGHT -> {
                    simProgress = 0.50f
                    phaseTitleRes = R.string.day_phase_daylight
                    phaseIcon = R.drawable.ic_weather_sunny
                }
                DaySolarTimePhase.SUNSET -> {
                    simProgress = 0.74f
                    phaseTitleRes = R.string.day_phase_dusk
                    phaseIcon = R.drawable.ic_sunset
                }
                DaySolarTimePhase.EVENING -> {
                    simProgress = 0.86f
                    phaseTitleRes = R.string.day_phase_night
                    phaseIcon = R.drawable.ic_moon
                }
                DaySolarTimePhase.MIDNIGHT -> {
                    simProgress = 0.04f
                    phaseTitleRes = R.string.day_phase_night
                    phaseIcon = R.drawable.ic_moon
                }
            }
            info.copy(
                progress = simProgress,
                percentage = (simProgress * 100).toInt(),
                phaseTitle = getString(phaseTitleRes),
                phaseIconResId = phaseIcon,
                isDaylight = (phase == DaySolarTimePhase.DAWN || phase == DaySolarTimePhase.DAYLIGHT || phase == DaySolarTimePhase.SUNSET)
            )
        } else {
            info
        }

        return Pair(colors, effectiveInfo)
    }

    private fun renderWidgetPreview(animate: Boolean = false) {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val density = resources.displayMetrics.density

        val effectiveWidgetId = if (activeWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            activeWidgetId
        } else {
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(this, DayProgressWidgetProvider::class.java))
            ids.lastOrNull() ?: AppWidgetManager.INVALID_APPWIDGET_ID
        }

        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        val (targetWidthDp, targetHeightDp) = if (effectiveWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val options = appWidgetManager.getAppWidgetOptions(effectiveWidgetId)
            val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            val maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
            val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)

            val w = if (isLandscape) {
                if (maxW > 0) maxW else minW.takeIf { it > 0 } ?: 320
            } else {
                if (minW > 0) minW else maxW.takeIf { it > 0 } ?: 320
            }

            val h = if (isLandscape) {
                if (minH > 0) minH else maxH.takeIf { it > 0 } ?: 138
            } else {
                if (maxH > 0) maxH else minH.takeIf { it > 0 } ?: 138
            }
            Pair(w, h)
        } else {
            Pair(320, 138)
        }

        val rawInfo = DayProgressRepository.getDayProgress(this)
        val (colors, info) = getEffectiveColors(rawInfo)

        val rv = if (targetHeightDp < 95) {
            DayProgressWidgetProvider.buildRowRemoteViews(this, targetWidthDp, targetHeightDp, info, colors, effectiveWidgetId)
        } else if (targetWidthDp <= 150 && targetHeightDp <= 150) {
            DayProgressWidgetProvider.buildCompactRemoteViews(this, targetWidthDp, targetHeightDp, info, colors, effectiveWidgetId)
        } else if (targetHeightDp < 180) {
            DayProgressWidgetProvider.buildCardRemoteViews(this, targetWidthDp, targetHeightDp, info, colors, effectiveWidgetId)
        } else {
            DayProgressWidgetProvider.buildTallRemoteViews(this, targetWidthDp, targetHeightDp, info, colors, effectiveWidgetId)
        }

        val containerLp = containerDayProgressPreview.layoutParams ?: ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        containerLp.height = (targetHeightDp * density).toInt()
        if (targetWidthDp in 1..220) {
            containerLp.width = (targetWidthDp * density).toInt()
        } else {
            containerLp.width = ViewGroup.LayoutParams.MATCH_PARENT
        }
        containerDayProgressPreview.layoutParams = containerLp

        try {
            val hostLp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            val newView = rv.apply(applicationContext, flPreviewHost)

            val oldColors = lastColors
            val oldView = if (flPreviewHost.childCount > 0) flPreviewHost.getChildAt(flPreviewHost.childCount - 1) else null

            if (animate && oldView != null && oldColors != null) {
                colorAnimator?.cancel()
                newView.alpha = 0f
                flPreviewHost.addView(newView, hostLp)

                val evaluator = ArgbEvaluator()
                val fromBg = oldColors.bgColor
                val toBg = colors.bgColor
                val fromPill = oldColors.pillBgColor
                val toPill = colors.pillBgColor

                colorAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 380L
                    interpolator = DecelerateInterpolator(1.4f)
                    addUpdateListener { va ->
                        val f = va.animatedValue as Float
                        newView.alpha = f
                        oldView.alpha = 1f - f

                        val currentBg = evaluator.evaluate(f, fromBg, toBg) as Int
                        val currentPill = evaluator.evaluate(f, fromPill, toPill) as Int

                        newView.findViewById<ImageView>(R.id.widget_day_progress_bg)?.apply {
                            setColorFilter(currentBg)
                            imageAlpha = Color.alpha(currentBg)
                        }
                        newView.findViewById<ImageView>(R.id.iv_card_bg)?.apply {
                            setColorFilter(currentPill)
                            imageAlpha = Color.alpha(currentPill)
                        }
                        newView.findViewById<ImageView>(R.id.iv_tall_bg)?.apply {
                            setColorFilter(currentPill)
                            imageAlpha = Color.alpha(currentPill)
                        }
                        newView.findViewById<ImageView>(R.id.iv_row_card_bg)?.apply {
                            setColorFilter(currentPill)
                            imageAlpha = Color.alpha(currentPill)
                        }
                    }
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            flPreviewHost.removeView(oldView)
                            newView.alpha = 1f
                            cardPreview.refreshBlur()
                        }
                    })
                    start()
                }
            } else {
                flPreviewHost.removeAllViews()
                flPreviewHost.addView(newView, hostLp)
                flPreviewHost.post { cardPreview.refreshBlur() }
            }
            lastColors = colors
        } catch (e: Exception) {
            val fallbackTv = TextView(this).apply {
                text = "${info.percentage}% • Day Progress"
                setTextColor(Color.WHITE)
            }
            flPreviewHost.removeAllViews()
            flPreviewHost.addView(fallbackTv)
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
        refreshAllBlur()
        updateDayStats()
        renderWidgetPreview(animate = false)
    }

    override fun onPause() {
        super.onPause()
        colorAnimator?.cancel()
        SharedAmbientBackgroundHolder.saveSnapshot(ambientBgView.getOrbsSnapshot())
    }
}
