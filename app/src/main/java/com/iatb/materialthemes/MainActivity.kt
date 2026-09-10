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
import com.iatb.materialthemes.data.ColorPalette
import com.iatb.materialthemes.data.DynamicThemeExtractor
import com.iatb.materialthemes.data.WeatherRepository
import com.iatb.materialthemes.data.WidgetContentMode
import com.iatb.materialthemes.render.ShapeWidgetCanvasRenderer
import com.iatb.materialthemes.render.WidgetCanvasRenderer
import com.iatb.materialthemes.ui.WidgetEditActivity
import com.iatb.materialthemes.ui.WidgetPresetsActivity
import com.iatb.materialthemes.widget.Diagonal4x3WidgetProvider
import com.iatb.materialthemes.widget.DiagonalWidgetProvider
import com.iatb.materialthemes.widget.OrganicWidgetProvider
import com.iatb.materialthemes.widget.OrganicWideWidgetProvider
import com.iatb.materialthemes.widget.ScallopWidgetProvider
import com.iatb.materialthemes.widget.ScallopWideWidgetProvider
import com.iatb.materialthemes.widget.WidgetUpdateScheduler

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var previewContainer: FrameLayout
    private lateinit var tvFloatingStatus: TextView

    private lateinit var cardHomeEdit: com.iatb.materialthemes.ui.GlassBlurCardView
    private lateinit var cardHomePresets: com.iatb.materialthemes.ui.GlassBlurCardView

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

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        viewModel.initFromPreferences(this)

        initViews()
        setupListeners()
        observeViewModel()

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
        viewModel.initFromPreferences(this)
        updateFloatingStatusBadge()
        updatePreview(animate = false)
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
                systemBars.bottom + (16 * density).toInt()
            )
            insets
        }

        previewContainer = findViewById(R.id.preview_container)
        tvFloatingStatus = findViewById(R.id.tv_floating_status)
        cardHomeEdit = findViewById(R.id.card_home_edit)
        cardHomePresets = findViewById(R.id.card_home_presets)

        val ambientBgView = findViewById<View>(R.id.ambient_bg_view)
        cardHomeEdit.setTargetBackgroundView(ambientBgView)
        cardHomePresets.setTargetBackgroundView(ambientBgView)

        findViewById<androidx.core.widget.NestedScrollView>(R.id.main_content_scroll).setOnScrollChangeListener { _, _, _, _, _ ->
            cardHomeEdit.refreshBlur()
            cardHomePresets.refreshBlur()
        }

        // Fluid press scale animations
        com.iatb.materialthemes.ui.WidgetPreviewHelper.applyPressScaleEffect(cardHomeEdit)
        com.iatb.materialthemes.ui.WidgetPreviewHelper.applyPressScaleEffect(cardHomePresets)
    }

    private fun setupListeners() {
        cardHomeEdit.setOnClickListener {
            startActivity(Intent(this, WidgetEditActivity::class.java))
        }

        cardHomePresets.setOnClickListener {
            startActivity(Intent(this, WidgetPresetsActivity::class.java))
        }

        previewContainer.setOnClickListener {
            updatePreview(animate = true)
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

        val providerClass = when (category) {
            WidgetCategory.DIAGONAL -> when (size) {
                WidgetSize.SIZE_4X3 -> Diagonal4x3WidgetProvider::class.java
                else -> DiagonalWidgetProvider::class.java
            }
            WidgetCategory.ORGANIC -> when (size) {
                WidgetSize.SIZE_4X2 -> OrganicWideWidgetProvider::class.java
                else -> OrganicWidgetProvider::class.java
            }
            WidgetCategory.SCALLOP -> when (size) {
                WidgetSize.SIZE_4X2 -> ScallopWideWidgetProvider::class.java
                else -> ScallopWidgetProvider::class.java
            }
        }

        val componentName = ComponentName(this, providerClass)
        appWidgetManager.requestPinAppWidget(componentName, null, null)
    }
}