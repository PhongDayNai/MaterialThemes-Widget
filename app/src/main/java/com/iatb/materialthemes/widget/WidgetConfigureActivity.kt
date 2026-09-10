package com.iatb.materialthemes.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.iatb.materialthemes.MainActivity
import com.iatb.materialthemes.R
import com.iatb.materialthemes.WidgetCategory
import com.iatb.materialthemes.WidgetSize
import com.iatb.materialthemes.data.ColorPalette
import com.iatb.materialthemes.data.DynamicThemeExtractor
import com.iatb.materialthemes.data.QuickPreset
import com.iatb.materialthemes.data.WeatherRepository
import com.iatb.materialthemes.data.WidgetPreferences
import com.iatb.materialthemes.render.ShapeWidgetCanvasRenderer
import com.iatb.materialthemes.render.WidgetCanvasRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WidgetConfigureActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var previewContainer: FrameLayout
    private lateinit var rvQuickPresets: RecyclerView
    private lateinit var ivPreviewWallpaper: ImageView
    private var livePreviewImageView: ImageView? = null

    private var currentCategory = WidgetCategory.DIAGONAL
    private var currentSize = WidgetSize.SIZE_4X2

    private var presetsAdapter: QuickPresetsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_OK)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val t0 = System.currentTimeMillis()
        android.util.Log.d("PERF_TIMING", "0. onCreate started")

        setContentView(R.layout.activity_widget_configure)
        val t1 = System.currentTimeMillis()
        android.util.Log.d("PERF_TIMING", "1. setContentView took ${t1 - t0}ms")

        initViews()
        detectWidgetCategoryAndSize()
        val t2 = System.currentTimeMillis()
        android.util.Log.d("PERF_TIMING", "2. detectWidgetCategoryAndSize took ${t2 - t1}ms")

        setupActions()
        setupQuickPresetsRecyclerView()
        val t3 = System.currentTimeMillis()
        android.util.Log.d("PERF_TIMING", "3. basic setup took ${t3 - t2}ms, total onCreate: ${t3 - t0}ms")

        // Defer heavier view population & image decoding to immediately after first frame
        window.decorView.post {
            val tPost = System.currentTimeMillis()
            android.util.Log.d("PERF_TIMING", "4. First frame presented at ${tPost - t0}ms")

            loadWallpaperAsync()
            setupLivePreview()
            loadPresetsAsync()
        }
    }

    private fun initViews() {
        previewContainer = findViewById(R.id.preview_container)
        rvQuickPresets = findViewById(R.id.rv_quick_presets)
        ivPreviewWallpaper = findViewById(R.id.iv_preview_wallpaper)
    }

    private fun detectWidgetCategoryAndSize() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val info = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            appWidgetManager.getAppWidgetInfo(appWidgetId)
        } else null

        val providerClass = info?.provider?.className.orEmpty()
        currentCategory = when {
            providerClass.contains("Organic") -> WidgetCategory.ORGANIC
            providerClass.contains("Scallop") -> WidgetCategory.SCALLOP
            else -> WidgetCategory.DIAGONAL
        }

        currentSize = when {
            providerClass.contains("4x3") -> WidgetSize.SIZE_4X3
            providerClass.contains("Wide") || providerClass.contains("4x2") -> WidgetSize.SIZE_4X2
            else -> WidgetSize.SIZE_2X2
        }

        val tvTitle = findViewById<TextView>(R.id.tv_title)
        val categoryName = when (currentCategory) {
            WidgetCategory.ORGANIC -> getString(R.string.category_organic)
            WidgetCategory.SCALLOP -> getString(R.string.category_scallop)
            WidgetCategory.DIAGONAL -> getString(R.string.category_diagonal)
        }
        tvTitle.text = categoryName
    }

    private fun setupQuickPresetsRecyclerView() {
        rvQuickPresets.layoutManager = GridLayoutManager(this, 2)
        presetsAdapter = QuickPresetsAdapter(emptyList())
        rvQuickPresets.adapter = presetsAdapter
    }

    private fun loadPresetsAsync() {
        val list = WidgetPreferences.getQuickPresets(this).filter { it.isEnabled }
        presetsAdapter?.updateItems(list)
    }

    private fun loadWallpaperAsync() {
        lifecycleScope.launch(Dispatchers.IO) {
            val drawable = ContextCompat.getDrawable(this@WidgetConfigureActivity, R.drawable.bg_preview_wallpaper)
            withContext(Dispatchers.Main) {
                ivPreviewWallpaper.setImageDrawable(drawable)
            }
        }
    }

    private fun setupLivePreview() {
        previewContainer.removeAllViews()
        val iv = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        val density = resources.displayMetrics.density
        val (wDp, hDp) = when (currentSize) {
            WidgetSize.SIZE_2X2 -> 140 to 140
            WidgetSize.SIZE_4X2, WidgetSize.SIZE_3X2 -> 260 to 130
            WidgetSize.SIZE_3X3, WidgetSize.SIZE_4X3 -> 220 to 140
            WidgetSize.SIZE_2X4 -> 140 to 260
            WidgetSize.SIZE_2X3 -> 140 to 200
        }
        val lp = FrameLayout.LayoutParams(
            (wDp * density).toInt(),
            (hDp * density).toInt()
        ).apply {
            gravity = android.view.Gravity.CENTER
        }
        previewContainer.addView(iv, lp)
        livePreviewImageView = iv

        updateLivePreviewImage()
    }

    private fun updateLivePreviewImage() {
        val iv = livePreviewImageView ?: return
        val density = resources.displayMetrics.density
        val (wDp, hDp) = when (currentSize) {
            WidgetSize.SIZE_2X2 -> 140 to 140
            WidgetSize.SIZE_4X2, WidgetSize.SIZE_3X2 -> 260 to 130
            WidgetSize.SIZE_3X3, WidgetSize.SIZE_4X3 -> 220 to 140
            WidgetSize.SIZE_2X4 -> 140 to 260
            WidgetSize.SIZE_2X3 -> 140 to 200
        }
        val wPx = (wDp * density).toInt()
        val hPx = (hDp * density).toInt()

        // Render preview asynchronously to keep UI thread fluid and instant
        lifecycleScope.launch(Dispatchers.Default) {
            val weather = WeatherRepository.getWeatherData(this@WidgetConfigureActivity)
            val bitmap = if (currentCategory == WidgetCategory.DIAGONAL) {
                val angle = WidgetPreferences.getRotationAngle(this@WidgetConfigureActivity)
                val palette = WidgetPreferences.getColorPalette(this@WidgetConfigureActivity)
                val mode = WidgetPreferences.getContentMode(this@WidgetConfigureActivity)
                val transparency = WidgetPreferences.getTransparency(this@WidgetConfigureActivity)

                WidgetCanvasRenderer.render(
                    context = this@WidgetConfigureActivity,
                    widthPx = wPx,
                    heightPx = hPx,
                    angleDeg = angle,
                    palette = palette,
                    contentMode = mode,
                    size = currentSize,
                    weather = weather,
                    transparency = transparency,
                    animProgress = 1.0f
                )
            } else {
                ShapeWidgetCanvasRenderer.render(
                    context = this@WidgetConfigureActivity,
                    category = currentCategory,
                    size = currentSize,
                    widthPx = wPx,
                    heightPx = hPx,
                    weather = weather,
                    animProgress = 1.0f
                )
            }

            withContext(Dispatchers.Main) {
                iv.setImageBitmap(bitmap)
            }
        }
    }

    private fun applyPresetInstantly(preset: QuickPreset) {
        WidgetPreferences.setColorPalette(this, preset.palette)
        WidgetPreferences.setTransparency(this, preset.transparency)
        if (currentCategory == WidgetCategory.DIAGONAL) {
            WidgetPreferences.setRotationAngle(this, preset.rotationAngle)
        }

        DynamicThemeExtractor.invalidateCache()

        // Cập nhật launcher widgets ngay lập tức
        DiagonalWidgetProvider.updateAllWidgets(this)
        Diagonal4x3WidgetProvider.updateAllWidgets4x3(this)
        OrganicWidgetProvider.updateAllWidgets(this)
        OrganicWideWidgetProvider.updateAllWidgets(this)
        ScallopWidgetProvider.updateAllWidgets(this)
        ScallopWideWidgetProvider.updateAllWidgets(this)

        // Cập nhật Live Preview trên màn hình ngay lập tức
        updateLivePreviewImage()

        // Cập nhật selection trong RecyclerView
        presetsAdapter?.notifyDataSetChanged()
    }

    private fun setupActions() {
        findViewById<View>(R.id.btn_close).setOnClickListener { finish() }

        // Header app icon opens MainActivity
        findViewById<View>(R.id.btn_open_app_header).setOnClickListener {
            val appIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            startActivity(appIntent)
            finish()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.slide_out_bottom)
    }

    private inner class QuickPresetsAdapter(
        private var items: List<QuickPreset>
    ) : RecyclerView.Adapter<QuickPresetsAdapter.PresetViewHolder>() {

        private var dynamicColorCached: Int? = null

        fun updateItems(newItems: List<QuickPreset>) {
            items = newItems
            notifyDataSetChanged()
        }

        inner class PresetViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val card: MaterialCardView = view.findViewById(R.id.card_preset)
            val viewPaletteColor: View = view.findViewById(R.id.view_palette_color)
            val tvName: TextView = view.findViewById(R.id.tv_preset_name)
            val tvSubtitle: TextView = view.findViewById(R.id.tv_preset_subtitle)
            val ivCheck: ImageView = view.findViewById(R.id.iv_check)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PresetViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_quick_preset, parent, false)
            return PresetViewHolder(view)
        }

        override fun onBindViewHolder(holder: PresetViewHolder, position: Int) {
            val preset = items[position]
            val currentPalette = WidgetPreferences.getColorPalette(this@WidgetConfigureActivity)
            val currentTransparency = WidgetPreferences.getTransparency(this@WidgetConfigureActivity)
            val currentAngle = WidgetPreferences.getRotationAngle(this@WidgetConfigureActivity)

            holder.tvName.text = preset.title
            val angleText = if (currentCategory == WidgetCategory.DIAGONAL) " • ${preset.rotationAngle.toInt()}°" else ""
            holder.tvSubtitle.text = getString(R.string.transparency_value_format, preset.transparency) + angleText

            // Color dot
            val dotDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                if (preset.palette == ColorPalette.DYNAMIC) {
                    val color = dynamicColorCached ?: DynamicThemeExtractor.getDynamicPalette(this@WidgetConfigureActivity).bgColor.also {
                        dynamicColorCached = it
                    }
                    setColor(color)
                } else {
                    setColor(preset.palette.bgColor)
                }
            }
            holder.viewPaletteColor.background = dotDrawable

            val isSelected = (preset.palette == currentPalette) &&
                    (preset.transparency == currentTransparency) &&
                    (currentCategory != WidgetCategory.DIAGONAL || preset.rotationAngle.toInt() == currentAngle.toInt())

            if (isSelected) {
                holder.ivCheck.visibility = View.VISIBLE
                holder.card.strokeColor = Color.parseColor("#4DD0E1")
                holder.card.strokeWidth = (2 * resources.displayMetrics.density).toInt()
                holder.card.setCardBackgroundColor(Color.parseColor("#22333B"))
            } else {
                holder.ivCheck.visibility = View.GONE
                holder.card.strokeColor = Color.parseColor("#2E383F")
                holder.card.strokeWidth = (1 * resources.displayMetrics.density).toInt()
                holder.card.setCardBackgroundColor(Color.parseColor("#151A1D"))
            }

            holder.card.setOnClickListener {
                applyPresetInstantly(preset)
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
