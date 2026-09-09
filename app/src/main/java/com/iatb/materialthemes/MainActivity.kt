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
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.slider.Slider
import com.iatb.materialthemes.data.ColorPalette
import com.iatb.materialthemes.data.DynamicThemeExtractor
import com.iatb.materialthemes.data.WeatherRepository
import com.iatb.materialthemes.data.WidgetContentMode
import com.iatb.materialthemes.data.WidgetPreferences
import com.iatb.materialthemes.render.ShapeWidgetCanvasRenderer
import com.iatb.materialthemes.render.WidgetCanvasRenderer
import com.iatb.materialthemes.widget.DiagonalWidgetProvider
import com.iatb.materialthemes.widget.Diagonal4x3WidgetProvider
import com.iatb.materialthemes.widget.OrganicWidgetProvider
import com.iatb.materialthemes.widget.OrganicWideWidgetProvider
import com.iatb.materialthemes.widget.ScallopWidgetProvider
import com.iatb.materialthemes.widget.WidgetAnimationManager
import com.iatb.materialthemes.widget.ScallopWideWidgetProvider
import com.iatb.materialthemes.widget.WidgetUpdateScheduler

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var previewContainer: FrameLayout
    private lateinit var toggleCategory: MaterialButtonToggleGroup
    private lateinit var toggleAngle: MaterialButtonToggleGroup
    private lateinit var toggleContent: MaterialButtonToggleGroup
    private lateinit var togglePalette: MaterialButtonToggleGroup
    private lateinit var toggleSize: MaterialButtonToggleGroup
    private lateinit var sliderTransparency: Slider
    private lateinit var tvTransparencyValue: TextView
    private lateinit var btnApply: MaterialButton
    private lateinit var btnPin: MaterialButton

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
        syncUiWithViewModel()

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

    override fun onResume() {
        super.onResume()
        DynamicThemeExtractor.invalidateCache()
        ShapeWidgetCanvasRenderer.invalidateCache()
        val filter = IntentFilter(WidgetUpdateScheduler.ACTION_WIDGET_TICK)
        ContextCompat.registerReceiver(
            this,
            widgetTickReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        setupWallpaperListener()
        updatePreview()

        if (WeatherRepository.isDefaultOrStale(this)) {
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

    override fun onPause() {
        super.onPause()
        removeWallpaperListener()
        try {
            unregisterReceiver(widgetTickReceiver)
        } catch (_: Exception) {
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        DynamicThemeExtractor.invalidateCache()
        updatePreview()
        DiagonalWidgetProvider.updateAllWidgets(this)
        OrganicWidgetProvider.updateAllWidgets(this)
        ScallopWidgetProvider.updateAllWidgets(this)
    }

    private fun setupWallpaperListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val wm = getSystemService(WallpaperManager::class.java)
                ?: WallpaperManager.getInstance(this)
            wallpaperColorsListener = WallpaperManager.OnColorsChangedListener { colors, _ ->
                DynamicThemeExtractor.onWallpaperColorsChanged(colors)
                updatePreview()
                DiagonalWidgetProvider.updateAllWidgets(this)
                OrganicWidgetProvider.updateAllWidgets(this)
                ScallopWidgetProvider.updateAllWidgets(this)
            }
            try {
                wallpaperColorsListener?.let {
                    wm?.addOnColorsChangedListener(it, Handler(Looper.getMainLooper()))
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun removeWallpaperListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                val wm = getSystemService(WallpaperManager::class.java)
                    ?: WallpaperManager.getInstance(this)
                wallpaperColorsListener?.let { wm?.removeOnColorsChangedListener(it) }
            } catch (_: Exception) {
            }
            wallpaperColorsListener = null
        }
    }

    private fun initViews() {
        previewContainer = findViewById(R.id.preview_container)
        toggleCategory = findViewById(R.id.toggle_category)
        toggleAngle = findViewById(R.id.toggle_angle)
        toggleContent = findViewById(R.id.toggle_content)
        togglePalette = findViewById(R.id.toggle_palette)
        toggleSize = findViewById(R.id.toggle_size)
        sliderTransparency = findViewById(R.id.slider_transparency)
        tvTransparencyValue = findViewById(R.id.tv_transparency_value)
        btnApply = findViewById(R.id.btn_apply)
        btnPin = findViewById(R.id.btn_pin)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_scroll)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupListeners() {
        toggleCategory.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btn_category_diagonal -> viewModel.setCategory(WidgetCategory.DIAGONAL)
                    R.id.btn_category_organic -> viewModel.setCategory(WidgetCategory.ORGANIC)
                    R.id.btn_category_scallop -> viewModel.setCategory(WidgetCategory.SCALLOP)
                }
            }
        }

        toggleAngle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val angle = when (checkedId) {
                    R.id.btn_angle_minus_45 -> -45f
                    R.id.btn_angle_minus_30 -> -30f
                    R.id.btn_angle_0 -> 0f
                    R.id.btn_angle_30 -> 30f
                    R.id.btn_angle_45 -> 45f
                    R.id.btn_angle_60 -> 60f
                    else -> -45f
                }
                viewModel.setRotationAngle(angle)
            }
        }

        toggleContent.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btn_content_weather -> viewModel.setContentMode(WidgetContentMode.WEATHER)
                    R.id.btn_content_clock -> viewModel.setContentMode(WidgetContentMode.CLOCK)
                    R.id.btn_content_combo -> viewModel.setContentMode(WidgetContentMode.COMBO)
                }
            }
        }

        togglePalette.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val palette = when (checkedId) {
                    R.id.btn_palette_olive -> ColorPalette.OLIVE
                    R.id.btn_palette_teal -> ColorPalette.TEAL
                    R.id.btn_palette_slate -> ColorPalette.SLATE
                    R.id.btn_palette_amber -> ColorPalette.AMBER
                    R.id.btn_palette_crimson -> ColorPalette.CRIMSON
                    R.id.btn_palette_dynamic -> ColorPalette.DYNAMIC
                    else -> ColorPalette.OLIVE
                }
                viewModel.setColorPalette(palette)
            }
        }

        toggleSize.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btn_size_2x2 -> viewModel.setSize(WidgetSize.SIZE_2X2)
                    R.id.btn_size_3x2 -> viewModel.setSize(WidgetSize.SIZE_3X2)
                    R.id.btn_size_4x2 -> viewModel.setSize(WidgetSize.SIZE_4X2)
                    R.id.btn_size_2x3 -> viewModel.setSize(WidgetSize.SIZE_2X3)
                    R.id.btn_size_3x3 -> viewModel.setSize(WidgetSize.SIZE_3X3)
                    R.id.btn_size_2x4 -> viewModel.setSize(WidgetSize.SIZE_2X4)
                    R.id.btn_size_4x3 -> viewModel.setSize(WidgetSize.SIZE_4X3)
                }
            }
        }

        sliderTransparency.addOnChangeListener { _, value, _ ->
            val intVal = value.toInt()
            viewModel.setTransparency(intVal)
            tvTransparencyValue.text = getString(R.string.transparency_value_format, intVal)
        }

        sliderTransparency.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}
            override fun onStopTrackingTouch(slider: Slider) {
                updatePreview(animate = true)
            }
        })

        previewContainer.setOnClickListener {
            updatePreview(animate = true)
        }

        btnApply.setOnClickListener {
            viewModel.saveAndApply(this)
            WeatherRepository.refreshWeather(this)
            sendBroadcast(Intent(WidgetUpdateScheduler.ACTION_WIDGET_TICK).setPackage(packageName))
            Toast.makeText(this, getString(R.string.settings_saved_toast), Toast.LENGTH_SHORT).show()
        }

        btnPin.setOnClickListener {
            viewModel.saveAndApply(this)
            pinCurrentWidget()
        }
    }

    private fun observeViewModel() {
        viewModel.category.observe(this) { cat ->
            updateCategoryDependentUi(cat)
            updatePreview(animate = true)
        }
        viewModel.size.observe(this) { updatePreview(animate = true) }
        viewModel.angle.observe(this) { updatePreview(animate = true) }
        viewModel.palette.observe(this) { updatePreview(animate = true) }
        viewModel.contentMode.observe(this) { updatePreview(animate = true) }
        viewModel.transparency.observe(this) { transparency ->
            tvTransparencyValue.text = getString(R.string.transparency_value_format, transparency)
            updatePreview(animate = false)
        }
    }

    private fun syncUiWithViewModel() {
        // 1. Category
        val catBtnId = when (viewModel.category.value) {
            WidgetCategory.DIAGONAL -> R.id.btn_category_diagonal
            WidgetCategory.ORGANIC -> R.id.btn_category_organic
            WidgetCategory.SCALLOP -> R.id.btn_category_scallop
            null -> R.id.btn_category_diagonal
        }
        toggleCategory.check(catBtnId)

        // 2. Angle
        val angleBtnId = when (viewModel.angle.value) {
            -45f -> R.id.btn_angle_minus_45
            -30f -> R.id.btn_angle_minus_30
            0f -> R.id.btn_angle_0
            30f -> R.id.btn_angle_30
            45f -> R.id.btn_angle_45
            60f -> R.id.btn_angle_60
            else -> R.id.btn_angle_minus_45
        }
        toggleAngle.check(angleBtnId)

        // 3. Content
        val contentBtnId = when (viewModel.contentMode.value) {
            WidgetContentMode.WEATHER -> R.id.btn_content_weather
            WidgetContentMode.CLOCK -> R.id.btn_content_clock
            WidgetContentMode.COMBO -> R.id.btn_content_combo
            null -> R.id.btn_content_weather
        }
        toggleContent.check(contentBtnId)

        // 4. Palette
        val paletteBtnId = when (viewModel.palette.value) {
            ColorPalette.OLIVE -> R.id.btn_palette_olive
            ColorPalette.TEAL -> R.id.btn_palette_teal
            ColorPalette.SLATE -> R.id.btn_palette_slate
            ColorPalette.AMBER -> R.id.btn_palette_amber
            ColorPalette.CRIMSON -> R.id.btn_palette_crimson
            ColorPalette.DYNAMIC -> R.id.btn_palette_dynamic
            null -> R.id.btn_palette_olive
        }
        togglePalette.check(paletteBtnId)

        // 5. Size
        val sizeBtnId = when (viewModel.size.value) {
            WidgetSize.SIZE_2X2 -> R.id.btn_size_2x2
            WidgetSize.SIZE_3X2 -> R.id.btn_size_3x2
            WidgetSize.SIZE_4X2 -> R.id.btn_size_4x2
            WidgetSize.SIZE_2X3 -> R.id.btn_size_2x3
            WidgetSize.SIZE_3X3 -> R.id.btn_size_3x3
            WidgetSize.SIZE_2X4 -> R.id.btn_size_2x4
            WidgetSize.SIZE_4X3 -> R.id.btn_size_4x3
            null -> R.id.btn_size_2x2
        }
        toggleSize.check(sizeBtnId)

        // 6. Transparency
        val transparencyVal = (viewModel.transparency.value ?: 100).toFloat()
        sliderTransparency.value = transparencyVal
        tvTransparencyValue.text = getString(R.string.transparency_value_format, transparencyVal.toInt())

        updateCategoryDependentUi(viewModel.category.value)
    }

    private fun updateCategoryDependentUi(category: WidgetCategory?) {
        val isDiagonal = category == WidgetCategory.DIAGONAL
        findViewById<View>(R.id.btn_size_2x3)?.visibility = if (isDiagonal) View.VISIBLE else View.GONE
        findViewById<View>(R.id.btn_size_2x4)?.visibility = if (isDiagonal) View.VISIBLE else View.GONE

        if (!isDiagonal) {
            val currentSize = viewModel.size.value
            if (currentSize == WidgetSize.SIZE_2X3 || currentSize == WidgetSize.SIZE_2X4) {
                viewModel.setSize(WidgetSize.SIZE_2X2)
                toggleSize.check(R.id.btn_size_2x2)
            }
        }
    }

    private fun updatePreview(animate: Boolean = false) {
        previewAnimator?.cancel()
        previewAnimator = null

        val density = resources.displayMetrics.density
        val size = viewModel.size.value ?: WidgetSize.SIZE_2X2
        val category = viewModel.category.value ?: WidgetCategory.DIAGONAL

        val (wDp, hDp) = when (size) {
            WidgetSize.SIZE_2X2 -> 160 to 160
            WidgetSize.SIZE_3X2 -> 240 to 160
            WidgetSize.SIZE_4X2 -> 290 to 150
            WidgetSize.SIZE_2X3 -> 130 to 180
            WidgetSize.SIZE_2X4 -> 120 to 190
            WidgetSize.SIZE_3X3 -> 190 to 190
            WidgetSize.SIZE_4X3 -> 250 to 190
        }

        val widthPx = (wDp * density).toInt()
        val heightPx = (hDp * density).toInt()

        previewContainer.removeAllViews()

        if (category == WidgetCategory.DIAGONAL) {
            val angle = viewModel.angle.value ?: -45f
            val palette = viewModel.palette.value ?: ColorPalette.OLIVE
            val mode = viewModel.contentMode.value ?: WidgetContentMode.WEATHER
            val transparency = viewModel.transparency.value ?: 100
            val weather = WeatherRepository.getWeatherData(this)

            val imageView = ImageView(this).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            val params = FrameLayout.LayoutParams(widthPx, heightPx).apply {
                gravity = Gravity.CENTER
            }
            previewContainer.addView(imageView, params)

            if (animate) {
                val animator = ValueAnimator.ofFloat(0.00f, 1.0f).apply {
                    duration = 340L
                    interpolator = DecelerateInterpolator(1.2f)
                    addUpdateListener { va ->
                        val p = va.animatedValue as Float
                        val bmp = WidgetCanvasRenderer.render(
                            this@MainActivity,
                            widthPx,
                            heightPx,
                            angle,
                            palette,
                            mode,
                            size,
                            weather,
                            transparency,
                            animProgress = p
                        )
                        imageView.setImageBitmap(bmp)
                    }
                }
                previewAnimator = animator
                animator.start()
            } else {
                val bitmap = WidgetCanvasRenderer.render(
                    this,
                    widthPx,
                    heightPx,
                    angle,
                    palette,
                    mode,
                    size,
                    weather,
                    transparency,
                    animProgress = 1.0f
                )
                imageView.setImageBitmap(bitmap)
            }
        } else {
            val weather = WeatherRepository.getWeatherData(this)
            val imageView = ImageView(this).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            val params = FrameLayout.LayoutParams(widthPx, heightPx).apply {
                gravity = Gravity.CENTER
            }
            previewContainer.addView(imageView, params)

            if (animate) {
                val animator = ValueAnimator.ofFloat(0.00f, 1.0f).apply {
                    duration = 340L
                    interpolator = DecelerateInterpolator(1.2f)
                    addUpdateListener { va ->
                        val p = va.animatedValue as Float
                        val bmp = ShapeWidgetCanvasRenderer.render(
                            context = this@MainActivity,
                            category = category,
                            size = size,
                            widthPx = widthPx,
                            heightPx = heightPx,
                            weather = weather,
                            animProgress = p
                        )
                        imageView.setImageBitmap(bmp)
                    }
                }
                previewAnimator = animator
                animator.start()
            } else {
                val bitmap = ShapeWidgetCanvasRenderer.render(
                    context = this,
                    category = category,
                    size = size,
                    widthPx = widthPx,
                    heightPx = heightPx,
                    weather = weather,
                    animProgress = 1.0f
                )
                imageView.setImageBitmap(bitmap)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        previewAnimator?.cancel()
        previewAnimator = null
    }

    private fun pinCurrentWidget() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                val category = viewModel.category.value ?: WidgetCategory.DIAGONAL
                val size = viewModel.size.value ?: WidgetSize.SIZE_2X2
                val providerClass = when (category) {
                    WidgetCategory.DIAGONAL -> {
                        if (size == WidgetSize.SIZE_4X3 || size == WidgetSize.SIZE_3X3) {
                            Diagonal4x3WidgetProvider::class.java
                        } else {
                            DiagonalWidgetProvider::class.java
                        }
                    }
                    WidgetCategory.ORGANIC -> {
                        if (size == WidgetSize.SIZE_4X2 || size == WidgetSize.SIZE_3X2 || size == WidgetSize.SIZE_4X3 || size == WidgetSize.SIZE_3X3) {
                            OrganicWideWidgetProvider::class.java
                        } else {
                            OrganicWidgetProvider::class.java
                        }
                    }
                    WidgetCategory.SCALLOP -> {
                        if (size == WidgetSize.SIZE_4X2 || size == WidgetSize.SIZE_3X2 || size == WidgetSize.SIZE_4X3 || size == WidgetSize.SIZE_3X3) {
                            ScallopWideWidgetProvider::class.java
                        } else {
                            ScallopWidgetProvider::class.java
                        }
                    }
                }
                val provider = ComponentName(this, providerClass)

                val extras = if (category == WidgetCategory.DIAGONAL) {
                    val angle = viewModel.angle.value ?: -45f
                    val palette = viewModel.palette.value ?: ColorPalette.OLIVE
                    val mode = viewModel.contentMode.value ?: WidgetContentMode.WEATHER
                    val transparency = viewModel.transparency.value ?: 100
                    val previewViews = DiagonalWidgetProvider.createPreviewRemoteViews(
                        this,
                        size,
                        angle,
                        palette,
                        mode,
                        transparency
                    )
                    Bundle().apply {
                        putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, previewViews)
                    }
                } else {
                    null
                }

                appWidgetManager.requestPinAppWidget(provider, extras, null)
                Toast.makeText(this, getString(R.string.pin_widget_success), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.pin_widget_not_supported), Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, getString(R.string.pin_widget_not_supported), Toast.LENGTH_LONG).show()
        }
    }
}