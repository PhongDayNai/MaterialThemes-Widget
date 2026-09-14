package com.iatb.materialthemes.ui

import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.iatb.materialthemes.MainViewModel
import com.iatb.materialthemes.R
import com.iatb.materialthemes.WidgetCategory
import com.iatb.materialthemes.WidgetSize
import com.iatb.materialthemes.data.ColorPalette
import com.iatb.materialthemes.data.DynamicThemeExtractor
import com.iatb.materialthemes.data.WeatherRepository
import com.iatb.materialthemes.data.WidgetContentMode
import com.iatb.materialthemes.data.WidgetPreferences
import com.iatb.materialthemes.render.ShapeWidgetCanvasRenderer
import com.iatb.materialthemes.render.WidgetCanvasRenderer
import com.iatb.materialthemes.widget.ActiveWidgetManager
import com.iatb.materialthemes.widget.WidgetUpdateScheduler

class ClockWeatherWidgetsActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var ambientBgView: AmbientMeshBackgroundView
    private lateinit var previewContainer: FrameLayout
    private lateinit var tvFloatingStatus: TextView
    private lateinit var btnBack: View
    private lateinit var cardActionEdit: GlassBlurCardView
    private lateinit var cardActionPresets: GlassBlurCardView

    private var previewAnimator: ValueAnimator? = null

    private val widgetTickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            DynamicThemeExtractor.invalidateCache()
            ShapeWidgetCanvasRenderer.invalidateCache()
            updatePreview()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_clock_weather_widgets)

        ActiveWidgetManager.syncActiveWidget(this)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        viewModel.initFromPreferences(this)

        initViews()
        setupAmbientBackground()
        setupBackNavigation()
        setupListeners()
        observeViewModel()
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
                    cardActionEdit.refreshBlur()
                    cardActionPresets.refreshBlur()
                },
                onComplete = {
                    cardActionEdit.refreshBlur()
                    cardActionPresets.refreshBlur()
                    SharedAmbientBackgroundHolder.saveSnapshot(ambientBgView.getOrbsSnapshot())
                }
            )
        }
    }

    private fun initViews() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.clock_weather_content_container)) { v, insets ->
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
        previewContainer = findViewById(R.id.preview_container)
        tvFloatingStatus = findViewById(R.id.tv_floating_status)
        btnBack = findViewById(R.id.btn_back)
        cardActionEdit = findViewById(R.id.card_action_edit)
        cardActionPresets = findViewById(R.id.card_action_presets)

        cardActionEdit.setTargetBackgroundView(ambientBgView)
        cardActionPresets.setTargetBackgroundView(ambientBgView)

        findViewById<androidx.core.widget.NestedScrollView>(R.id.scroll_content).setOnScrollChangeListener { _, _, _, _, _ ->
            cardActionEdit.refreshBlur()
            cardActionPresets.refreshBlur()
        }

        val interactiveViews = listOf(btnBack, cardActionEdit, cardActionPresets)
        for (view in interactiveViews) {
            WidgetPreviewHelper.applyPressScaleEffect(view)
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishWithTransition()
            }
        })
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finishWithTransition()
        }

        previewContainer.setOnClickListener {
            updatePreview(animate = true)
        }

        cardActionEdit.setOnClickListener {
            SharedAmbientBackgroundHolder.saveSnapshot(ambientBgView.getOrbsSnapshot())
            val intent = Intent(this, WidgetEditActivity::class.java)
            startActivity(intent)
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        cardActionPresets.setOnClickListener {
            SharedAmbientBackgroundHolder.saveSnapshot(ambientBgView.getOrbsSnapshot())
            val intent = Intent(this, WidgetPresetsActivity::class.java)
            startActivity(intent)
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun finishWithTransition() {
        SharedAmbientBackgroundHolder.saveSnapshot(ambientBgView.getOrbsSnapshot())
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun observeViewModel() {
        viewModel.category.observe(this) {
            updateFloatingStatusBadge()
            updatePreview(animate = false)
        }
        viewModel.size.observe(this) {
            updateFloatingStatusBadge()
            updatePreview(animate = false)
        }
        viewModel.palette.observe(this) {
            updateFloatingStatusBadge()
            updatePreview(animate = false)
        }
        viewModel.angle.observe(this) { updatePreview(animate = false) }
        viewModel.contentMode.observe(this) { updatePreview(animate = false) }
        viewModel.transparency.observe(this) { updatePreview(animate = false) }
    }

    override fun onResume() {
        super.onResume()
        ActiveWidgetManager.syncActiveWidget(this)
        viewModel.initFromPreferences(this)
        updateFloatingStatusBadge()
        updatePreview(animate = false)
        cardActionEdit.refreshBlur()
        cardActionPresets.refreshBlur()
    }

    override fun onPause() {
        super.onPause()
        SharedAmbientBackgroundHolder.saveSnapshot(ambientBgView.getOrbsSnapshot())
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(WidgetUpdateScheduler.ACTION_WIDGET_TICK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(widgetTickReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(widgetTickReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        runCatching { unregisterReceiver(widgetTickReceiver) }
    }

    override fun onDestroy() {
        super.onDestroy()
        previewAnimator?.cancel()
        previewAnimator = null
    }

    private fun updateFloatingStatusBadge() {
        val catName = when (viewModel.category.value) {
            WidgetCategory.ORGANIC -> getString(R.string.category_organic)
            WidgetCategory.SCALLOP -> getString(R.string.category_scallop)
            else -> getString(R.string.category_diagonal)
        }
        val sizeLabel = viewModel.size.value?.let { getString(it.labelResId) } ?: "2 × 2"
        val palName = viewModel.palette.value?.let { getString(it.labelResId) } ?: ""
        tvFloatingStatus.text = "$catName • $sizeLabel • $palName"
    }

    private fun updatePreview(animate: Boolean = false) {
        val category = viewModel.category.value ?: WidgetCategory.DIAGONAL
        val size = viewModel.size.value ?: WidgetSize.SIZE_2X2
        val angle = viewModel.angle.value ?: -45f
        val palette = viewModel.palette.value ?: ColorPalette.DYNAMIC
        val contentMode = viewModel.contentMode.value ?: WidgetContentMode.WEATHER
        val transparency = viewModel.transparency.value ?: 100

        previewAnimator = WidgetPreviewHelper.updatePreview(
            context = this,
            container = previewContainer,
            category = category,
            size = size,
            angle = angle,
            palette = palette,
            contentMode = contentMode,
            transparency = transparency,
            animate = animate,
            activeAnimator = previewAnimator
        )
    }
}
