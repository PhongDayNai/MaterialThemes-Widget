package com.iatb.materialthemes.ui

import android.animation.ValueAnimator
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.iatb.materialthemes.MainViewModel
import com.iatb.materialthemes.R
import com.iatb.materialthemes.WidgetCategory
import com.iatb.materialthemes.WidgetSize
import com.iatb.materialthemes.data.ColorPalette
import com.iatb.materialthemes.data.DynamicThemeExtractor
import com.iatb.materialthemes.data.QuickPreset
import com.iatb.materialthemes.data.WidgetContentMode
import com.iatb.materialthemes.data.WidgetPreferences
import com.iatb.materialthemes.render.WidgetCanvasRenderer

class WidgetPresetsActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var previewContainer: FrameLayout
    private var previewAnimator: ValueAnimator? = null

    private lateinit var rvPresets: RecyclerView
    private var presetsAdapter: PresetsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_widget_presets)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        viewModel.initFromPreferences(this)

        initViews()
        setupPresetsRecyclerView()
        observeViewModel()
    }

    private fun initViews() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.presets_content_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btn_back_presets).setOnClickListener { finish() }
        previewContainer = findViewById(R.id.preview_container)
        rvPresets = findViewById(R.id.rv_presets_grid)

        findViewById<MaterialButton>(R.id.btn_add_preset_header).setOnClickListener {
            showAddPresetDialog()
        }
    }

    private fun setupPresetsRecyclerView() {
        rvPresets.layoutManager = GridLayoutManager(this, 2)
        presetsAdapter = PresetsAdapter(emptyList())
        rvPresets.adapter = presetsAdapter
        refreshPresetsList()
    }

    private fun refreshPresetsList() {
        val list = WidgetPreferences.getQuickPresets(this).filter { it.isEnabled }
        presetsAdapter?.updateItems(list)
    }

    private fun observeViewModel() {
        viewModel.category.observe(this) { updatePreview(animate = true) }
        viewModel.size.observe(this) { updatePreview(animate = true) }
        viewModel.palette.observe(this) { updatePreview(animate = true) }
        viewModel.transparency.observe(this) { updatePreview(animate = false) }
        viewModel.angle.observe(this) { updatePreview(animate = true) }
        viewModel.contentMode.observe(this) { updatePreview(animate = true) }
    }

    private fun showAddPresetDialog() {
        val input = EditText(this).apply {
            hint = getString(R.string.dialog_add_preset_hint)
            setSingleLine()
        }
        val container = FrameLayout(this).apply {
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, pad / 2)
            addView(input)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_add_preset_title)
            .setView(container)
            .setPositiveButton(R.string.dialog_btn_add) { _, _ ->
                val title = input.text.toString().trim()
                if (title.isNotEmpty()) {
                    WidgetPreferences.addQuickPreset(
                        context = this,
                        title = title,
                        palette = viewModel.palette.value ?: ColorPalette.DYNAMIC,
                        transparency = viewModel.transparency.value ?: 100,
                        angle = viewModel.angle.value ?: -45f
                    )
                    refreshPresetsList()
                }
            }
            .setNegativeButton(R.string.dialog_btn_cancel, null)
            .show()
    }

    private fun getThemeColor(attr: Int, defaultColor: Int = 0): Int {
        val tv = android.util.TypedValue()
        return if (theme.resolveAttribute(attr, tv, true)) {
            if (tv.resourceId != 0) ContextCompat.getColor(this, tv.resourceId) else tv.data
        } else defaultColor
    }

    private fun updatePreview(animate: Boolean = false) {
        previewAnimator?.cancel()

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

    private inner class PresetsAdapter(
        private var items: List<QuickPreset>
    ) : RecyclerView.Adapter<PresetsAdapter.PresetViewHolder>() {

        private var dynamicColorCached: Pair<Int, Int>? = null

        fun updateItems(newItems: List<QuickPreset>) {
            items = newItems
            notifyDataSetChanged()
        }

        inner class PresetViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val card: MaterialCardView = view.findViewById(R.id.card_preset)
            val ivPalettePreview: ImageView = view.findViewById(R.id.iv_palette_preview)
            val tvName: TextView = view.findViewById(R.id.tv_preset_name)
            val tvSubtitle: TextView = view.findViewById(R.id.tv_preset_subtitle)
            val badgeCheckContainer: View = view.findViewById(R.id.badge_check_container)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PresetViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_quick_preset, parent, false)
            return PresetViewHolder(view)
        }

        override fun onBindViewHolder(holder: PresetViewHolder, position: Int) {
            val preset = items[position]
            val currentPalette = viewModel.palette.value ?: ColorPalette.DYNAMIC
            val currentTransparency = viewModel.transparency.value ?: 100
            val currentAngle = viewModel.angle.value ?: -45f

            holder.tvName.text = preset.getLocalizedTitle(this@WidgetPresetsActivity)
            val paletteName = getString(preset.palette.labelResId)
            val opacityText = getString(R.string.transparency_value_format, preset.transparency)
            holder.tvSubtitle.text = "$paletteName • $opacityText"

            // Dual-tone swatch
            val (mainColor, accentColor) = when (preset.palette) {
                ColorPalette.DYNAMIC -> {
                    dynamicColorCached ?: run {
                        val dyn = DynamicThemeExtractor.getDynamicPalette(this@WidgetPresetsActivity)
                        (dyn.bgColor to dyn.textColor).also { dynamicColorCached = it }
                    }
                }
                ColorPalette.CUSTOM -> {
                    val custom = WidgetPreferences.getCustomPalette(this@WidgetPresetsActivity)
                    custom.bgColor to custom.textColor
                }
                else -> {
                    preset.palette.bgColor to preset.palette.textColor
                }
            }

            val density = resources.displayMetrics.density
            val insetPx = (8 * density).toInt()
            val bgOval = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(mainColor)
            }
            val centerDot = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(accentColor)
            }
            val layerDrawable = LayerDrawable(arrayOf(bgOval, centerDot)).apply {
                setLayerInset(1, insetPx, insetPx, insetPx, insetPx)
            }
            holder.ivPalettePreview.setImageDrawable(layerDrawable)

            val isSelected = (preset.palette == currentPalette) &&
                    (preset.transparency == currentTransparency) &&
                    (viewModel.category.value != WidgetCategory.DIAGONAL || preset.rotationAngle.toInt() == currentAngle.toInt())

            val primaryColor = getThemeColor(androidx.appcompat.R.attr.colorPrimary, 0xFF006874.toInt())
            val outlineColor = getThemeColor(com.google.android.material.R.attr.colorOutline, 0xFF6F797A.toInt())
            val surfaceVariant = getThemeColor(com.google.android.material.R.attr.colorSurfaceVariant, 0xFFDBE4E6.toInt())
            val surfaceColor = getThemeColor(com.google.android.material.R.attr.colorSurface, 0xFFF8FDFF.toInt())

            if (isSelected) {
                holder.badgeCheckContainer.visibility = View.VISIBLE
                holder.card.strokeColor = primaryColor
                holder.card.strokeWidth = (2 * density).toInt()
                holder.card.setCardBackgroundColor(surfaceVariant)
            } else {
                holder.badgeCheckContainer.visibility = View.GONE
                holder.card.strokeColor = outlineColor
                holder.card.strokeWidth = (1 * density).toInt()
                holder.card.setCardBackgroundColor(surfaceColor)
            }

            holder.card.setOnClickListener {
                viewModel.setColorPalette(preset.palette)
                viewModel.setTransparency(preset.transparency)
                if (viewModel.category.value == WidgetCategory.DIAGONAL) {
                    viewModel.setRotationAngle(preset.rotationAngle)
                }
                viewModel.saveAndApply(this@WidgetPresetsActivity)
                notifyDataSetChanged()
                updatePreview(animate = true)
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
