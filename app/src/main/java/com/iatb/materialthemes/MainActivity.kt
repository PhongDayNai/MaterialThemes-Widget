package com.iatb.materialthemes

import android.animation.ValueAnimator
import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.RemoteViews
import com.iatb.materialthemes.data.ColorPalette
import com.iatb.materialthemes.data.DynamicThemeExtractor
import com.iatb.materialthemes.data.WeatherRepository
import com.iatb.materialthemes.data.WidgetContentMode
import com.iatb.materialthemes.render.ShapeWidgetCanvasRenderer
import com.iatb.materialthemes.render.WidgetCanvasRenderer
import com.iatb.materialthemes.ui.WidgetEditActivity
import com.iatb.materialthemes.ui.WidgetPresetsActivity
import com.iatb.materialthemes.widget.ActiveWidgetManager
import com.iatb.materialthemes.widget.DiagonalWidgetProvider
import com.iatb.materialthemes.widget.OrganicWidgetProvider
import com.iatb.materialthemes.widget.ScallopWidgetProvider
import com.iatb.materialthemes.widget.WidgetUpdateScheduler

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FROM_SPLASH = "extra_from_splash"
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var previewContainer: FrameLayout
    private lateinit var tvFloatingStatus: TextView

    private lateinit var cardHomeClockWeather: com.iatb.materialthemes.ui.GlassBlurCardView
    private lateinit var cardHomeUtilities: com.iatb.materialthemes.ui.GlassBlurCardView
    private lateinit var btnHomeSettings: View
    private lateinit var ambientBgView: com.iatb.materialthemes.ui.AmbientMeshBackgroundView
    private var isFirstResume: Boolean = true
    private var isFromSplash: Boolean = false

    private var previewAnimator: ValueAnimator? = null
    private var wallpaperColorsListener: WallpaperManager.OnColorsChangedListener? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            WeatherRepository.refreshWeather(this, force = true) { success ->
                if (success) {
                    runOnUiThread {
                        ShapeWidgetCanvasRenderer.invalidateCache()
                        updatePreview()
                    }
                }
            }
        }
    }

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
        setContentView(R.layout.activity_main)

        ActiveWidgetManager.syncActiveWidget(this)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        viewModel.initFromPreferences(this)

        isFromSplash = intent.getBooleanExtra(EXTRA_FROM_SPLASH, false)

        initViews()

        if (isFromSplash) {
            val snapshot = com.iatb.materialthemes.ui.SplashBackgroundTransitionHolder.consumeSnapshot()
            if (snapshot != null) {
                ambientBgView.setOrbs(snapshot)
            }
        }

        playHomeScreenEntranceAnimation()

        setupListeners()
        observeViewModel()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAffinity()
                finishAndRemoveTask()
                android.os.Process.killProcess(android.os.Process.myPid())
                kotlin.system.exitProcess(0)
            }
        })

        val hasLocation = checkLocationPermission()
        WeatherRepository.refreshWeather(this, force = hasLocation) { success ->
            if (success) {
                runOnUiThread {
                    ShapeWidgetCanvasRenderer.invalidateCache()
                    updatePreview()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ActiveWidgetManager.syncActiveWidget(this)
        viewModel.initFromPreferences(this)
        updateFloatingStatusBadge()
        updatePreview(animate = false)

        val existingOrbs = com.iatb.materialthemes.ui.SharedAmbientBackgroundHolder.getSnapshot()
        if (existingOrbs != null) {
            ambientBgView.setOrbs(existingOrbs)
        }

        if (!isFirstResume || isFromSplash) {
            val shouldAnimateFromSplash = isFromSplash
            isFromSplash = false
            isFirstResume = false

            val duration = if (shouldAnimateFromSplash) 1200L else 950L
            ambientBgView.post {
                ambientBgView.transitionToNewConstellation(
                    durationMs = duration,
                    onUpdate = {
                        cardHomeClockWeather.refreshBlur()
                        cardHomeUtilities.refreshBlur()
                    },
                    onComplete = {
                        cardHomeClockWeather.refreshBlur()
                        cardHomeUtilities.refreshBlur()
                        com.iatb.materialthemes.ui.SharedAmbientBackgroundHolder.saveSnapshot(ambientBgView.getOrbsSnapshot())
                    }
                )
            }
        } else {
            isFirstResume = false
            com.iatb.materialthemes.ui.SharedAmbientBackgroundHolder.saveSnapshot(ambientBgView.getOrbsSnapshot())
        }
    }

    override fun onPause() {
        super.onPause()
        com.iatb.materialthemes.ui.SharedAmbientBackgroundHolder.saveSnapshot(ambientBgView.getOrbsSnapshot())
    }

    private fun checkLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return false
        }
        return true
    }

    override fun onStart() {
        super.onStart()
        DynamicThemeExtractor.invalidateCache()
        ShapeWidgetCanvasRenderer.invalidateCache()
        updatePreview(animate = false)

        val filter = IntentFilter(WidgetUpdateScheduler.ACTION_WIDGET_TICK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(widgetTickReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(widgetTickReceiver, filter)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                val wm = getSystemService(WallpaperManager::class.java)
                val listener = WallpaperManager.OnColorsChangedListener { colors, _ ->
                    DynamicThemeExtractor.onWallpaperColorsChanged(colors)
                    ShapeWidgetCanvasRenderer.invalidateCache()
                    runOnUiThread { updatePreview() }
                }
                wm.addOnColorsChangedListener(listener, android.os.Handler(android.os.Looper.getMainLooper()))
                wallpaperColorsListener = listener
            } catch (_: Exception) {
            }
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(widgetTickReceiver)
        } catch (_: Exception) {
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            wallpaperColorsListener?.let {
                try {
                    val wm = getSystemService(WallpaperManager::class.java)
                    wm.removeOnColorsChangedListener(it)
                } catch (_: Exception) {
                }
            }
            wallpaperColorsListener = null
        }
    }

    private fun initViews() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_content_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val density = resources.displayMetrics.density
            val hPad = (22 * density).toInt()
            v.setPadding(
                hPad + systemBars.left,
                systemBars.top + (12 * density).toInt(),
                hPad + systemBars.right,
                systemBars.bottom + (42 * density).toInt()
            )
            insets
        }

        previewContainer = findViewById(R.id.preview_container)
        tvFloatingStatus = findViewById(R.id.tv_floating_status)
        cardHomeClockWeather = findViewById(R.id.card_home_clock_weather)
        cardHomeUtilities = findViewById(R.id.card_home_utilities)

        ambientBgView = findViewById(R.id.ambient_bg_view)
        cardHomeClockWeather.setTargetBackgroundView(ambientBgView)
        cardHomeUtilities.setTargetBackgroundView(ambientBgView)

        findViewById<androidx.core.widget.NestedScrollView>(R.id.main_content_scroll).setOnScrollChangeListener { _, _, _, _, _ ->
            cardHomeClockWeather.refreshBlur()
            cardHomeUtilities.refreshBlur()
        }

        // Fluid press scale animations
        com.iatb.materialthemes.ui.WidgetPreviewHelper.applyPressScaleEffect(cardHomeClockWeather)
        com.iatb.materialthemes.ui.WidgetPreviewHelper.applyPressScaleEffect(cardHomeUtilities)
        btnHomeSettings = findViewById(R.id.btn_home_settings)
        com.iatb.materialthemes.ui.WidgetPreviewHelper.applyPressScaleEffect(btnHomeSettings)
    }

    private fun playHomeScreenEntranceAnimation() {
        val density = resources.displayMetrics.density
        val headerLayout = findViewById<View>(R.id.home_header_layout)
        val cardPreview = findViewById<View>(R.id.card_preview)
        val cardClockWeather = findViewById<View>(R.id.card_home_clock_weather)
        val cardUtilities = findViewById<View>(R.id.card_home_utilities)
        val contentScroll = findViewById<View>(R.id.main_content_scroll)

        // 1. Initial hidden and offset states
        contentScroll.alpha = 0f
        headerLayout.alpha = 0f
        headerLayout.translationY = -20f * density

        cardPreview.alpha = 0f
        cardPreview.translationY = 24f * density
        cardPreview.scaleX = 0.94f
        cardPreview.scaleY = 0.94f

        cardClockWeather.alpha = 0f
        cardClockWeather.translationY = 24f * density
        cardClockWeather.scaleX = 0.94f
        cardClockWeather.scaleY = 0.94f

        // Graceful offset for bottom card to ensure it stays fully unclipped during entrance
        cardUtilities.alpha = 0f
        cardUtilities.translationY = 16f * density
        cardUtilities.scaleX = 0.95f
        cardUtilities.scaleY = 0.95f

        // 2. Play orchestrated staggered cascade entrance
        contentScroll.animate()
            .alpha(1f)
            .setDuration(260)
            .start()

        headerLayout.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(520)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .start()

        cardPreview.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(580)
            .setStartDelay(70)
            .setInterpolator(DecelerateInterpolator(1.4f))
            .start()

        cardClockWeather.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(580)
            .setStartDelay(150)
            .setInterpolator(DecelerateInterpolator(1.4f))
            .start()

        cardUtilities.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(580)
            .setStartDelay(230)
            .setInterpolator(DecelerateInterpolator(1.4f))
            .start()
    }

    private fun setupListeners() {
        cardHomeClockWeather.setOnClickListener {
            com.iatb.materialthemes.ui.SharedAmbientBackgroundHolder.saveSnapshot(ambientBgView.getOrbsSnapshot())
            startActivity(Intent(this, com.iatb.materialthemes.ui.ClockWeatherWidgetsActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        cardHomeUtilities.setOnClickListener {
            com.iatb.materialthemes.ui.SharedAmbientBackgroundHolder.saveSnapshot(ambientBgView.getOrbsSnapshot())
            startActivity(Intent(this, com.iatb.materialthemes.ui.UtilityWidgetsActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        previewContainer.setOnClickListener {
            updatePreview(animate = true)
        }

        btnHomeSettings.setOnClickListener {
            com.iatb.materialthemes.ui.SharedAmbientBackgroundHolder.saveSnapshot(ambientBgView.getOrbsSnapshot())
            startActivity(Intent(this, com.iatb.materialthemes.ui.SettingsActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun observeViewModel() {
        viewModel.category.observe(this) {
            updateFloatingStatusBadge()
            updatePreview(animate = true)
        }
        viewModel.size.observe(this) {
            updateFloatingStatusBadge()
            updatePreview(animate = true)
        }
        viewModel.angle.observe(this) {
            updateFloatingStatusBadge()
            updatePreview(animate = true)
        }
        viewModel.palette.observe(this) {
            updateFloatingStatusBadge()
            updatePreview(animate = true)
        }
        viewModel.contentMode.observe(this) {
            updatePreview(animate = true)
        }
        viewModel.transparency.observe(this) {
            updateFloatingStatusBadge()
            updatePreview(animate = false)
        }
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

        previewAnimator = com.iatb.materialthemes.ui.WidgetPreviewHelper.updatePreview(
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

    private fun pinCurrentWidget() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        if (!appWidgetManager.isRequestPinAppWidgetSupported) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.pin_widget_dialog_title)
                .setMessage(R.string.pin_widget_not_supported)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }

        val category = viewModel.category.value ?: WidgetCategory.DIAGONAL
        val size = viewModel.size.value ?: WidgetSize.SIZE_2X2
        val angle = viewModel.angle.value ?: -45f
        val palette = viewModel.palette.value ?: ColorPalette.DYNAMIC
        val contentMode = viewModel.contentMode.value ?: WidgetContentMode.COMBO
        val transparency = viewModel.transparency.value ?: 100

        val providerClass = when (category) {
            WidgetCategory.DIAGONAL -> DiagonalWidgetProvider::class.java
            WidgetCategory.ORGANIC -> OrganicWidgetProvider::class.java
            WidgetCategory.SCALLOP -> ScallopWidgetProvider::class.java
        }

        val componentName = ComponentName(this, providerClass)

        // Generate live preview bitmap matching the user's customized configuration
        val (wDp, hDp) = when (size) {
            WidgetSize.SIZE_2X2 -> 200 to 200
            WidgetSize.SIZE_3X2 -> 300 to 200
            WidgetSize.SIZE_4X2 -> 400 to 200
            WidgetSize.SIZE_2X3 -> 200 to 300
            WidgetSize.SIZE_2X4 -> 200 to 400
            WidgetSize.SIZE_3X3 -> 300 to 300
            WidgetSize.SIZE_4X3 -> 400 to 300
        }
        val density = resources.displayMetrics.density
        val widthPx = (wDp * density).toInt()
        val heightPx = (hDp * density).toInt()

        val weather = WeatherRepository.getWeatherData(this)
        val previewBitmap = if (category == WidgetCategory.DIAGONAL) {
            WidgetCanvasRenderer.render(
                context = this,
                widthPx = widthPx,
                heightPx = heightPx,
                angleDeg = angle,
                palette = palette,
                contentMode = contentMode,
                size = size,
                weather = weather,
                transparency = transparency,
                animProgress = 1.0f
            )
        } else {
            ShapeWidgetCanvasRenderer.render(
                context = this,
                category = category,
                size = size,
                widthPx = widthPx,
                heightPx = heightPx,
                palette = palette,
                transparency = transparency,
                weather = weather,
                animProgress = 1.0f
            )
        }

        val previewViews = RemoteViews(packageName, R.layout.widget_pin_preview).apply {
            setImageViewBitmap(R.id.iv_pin_preview_render, previewBitmap)
        }

        val extras = Bundle().apply {
            putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, previewViews)
        }

        appWidgetManager.requestPinAppWidget(componentName, extras, null)
    }
}