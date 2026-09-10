package com.iatb.materialthemes.ui

import android.animation.ValueAnimator
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.iatb.materialthemes.MainViewModel
import com.iatb.materialthemes.R
import com.iatb.materialthemes.WidgetCategory
import com.iatb.materialthemes.WidgetSize
import com.iatb.materialthemes.data.ColorPalette
import com.iatb.materialthemes.data.DynamicThemeExtractor
import com.iatb.materialthemes.data.WidgetContentMode
import com.iatb.materialthemes.data.WidgetPreferences
import com.iatb.materialthemes.widget.Diagonal4x3WidgetProvider
import com.iatb.materialthemes.widget.DiagonalWidgetProvider
import com.iatb.materialthemes.widget.OrganicWidgetProvider
import com.iatb.materialthemes.widget.OrganicWideWidgetProvider
import com.iatb.materialthemes.widget.ScallopWidgetProvider
import com.iatb.materialthemes.widget.ScallopWideWidgetProvider

class WidgetEditActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel

    // Preview
    private lateinit var previewContainer: FrameLayout
    private var previewAnimator: ValueAnimator? = null

    // Shape Cards (Diagonal, Organic, Scallop - No labels)
    private lateinit var cardShapeDiagonal: MaterialCardView
    private lateinit var cardShapeOrganic: MaterialCardView
    private lateinit var cardShapeScallop: MaterialCardView
    private lateinit var ivIconShapeDiagonal: ImageView
    private lateinit var ivIconShapeOrganic: ImageView
    private lateinit var ivIconShapeScallop: ImageView

    // Swatches (First 2 have no heavy card bg)
    private lateinit var swatchDynamic: View
    private lateinit var swatchCustomPicker: View
    private lateinit var swatchOlive: View
    private lateinit var swatchTeal: View
    private lateinit var swatchSlate: View
    private lateinit var swatchAmber: View
    private lateinit var swatchCrimson: View

    private lateinit var ringSwatchDynamic: FrameLayout
    private lateinit var ringSwatchCustom: FrameLayout
    private lateinit var ringSwatchOlive: FrameLayout
    private lateinit var ringSwatchTeal: FrameLayout
    private lateinit var ringSwatchSlate: FrameLayout
    private lateinit var ringSwatchAmber: FrameLayout
    private lateinit var ringSwatchCrimson: FrameLayout

    private lateinit var discSwatchDynamic: ImageView
    private lateinit var discSwatchCustom: ImageView
    private lateinit var discSwatchOlive: ImageView
    private lateinit var discSwatchTeal: ImageView
    private lateinit var discSwatchSlate: ImageView
    private lateinit var discSwatchAmber: ImageView
    private lateinit var discSwatchCrimson: ImageView

    // Visual Size Cards
    private lateinit var cardSize2x2: MaterialCardView
    private lateinit var cardSize4x2: MaterialCardView
    private lateinit var cardSize3x3: MaterialCardView
    private lateinit var cardSize4x3: MaterialCardView
    private lateinit var cardSize3x2: MaterialCardView
    private lateinit var cardSize2x3: MaterialCardView
    private lateinit var cardSize2x4: MaterialCardView
    private lateinit var layoutSizeSecondary: View

    // Opacity
    private lateinit var sliderTransparency: Slider
    private lateinit var tvTransparencyValue: TextView

    // Diagonal Angle Controls
    private lateinit var layoutDiagonalOptions: View
    private lateinit var ivAngleNeedleIcon: ImageView
    private lateinit var tvAngleDisplayValue: TextView
    private lateinit var sliderAngle: Slider
    private lateinit var chipAngleNeg60: MaterialButton
    private lateinit var chipAngleNeg45: MaterialButton
    private lateinit var chipAngleNeg30: MaterialButton
    private lateinit var chipAngle0: MaterialButton
    private lateinit var chipAnglePos30: MaterialButton
    private lateinit var chipAnglePos45: MaterialButton
    private lateinit var chipAnglePos60: MaterialButton

    private lateinit var toggleContent: MaterialButtonToggleGroup

    // Bottom Pin Button
    private lateinit var btnPinEdit: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_widget_edit)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        viewModel.initFromPreferences(this)

        initViews()
        setupListeners()
        setupVisualColorSwatches()
        observeViewModel()
        syncUiWithViewModel()
    }

    private fun initViews() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.edit_content_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.btn_done).setOnClickListener { finish() }

        previewContainer = findViewById(R.id.preview_container)

        // Shapes
        cardShapeDiagonal = findViewById(R.id.card_shape_diagonal)
        cardShapeOrganic = findViewById(R.id.card_shape_organic)
        cardShapeScallop = findViewById(R.id.card_shape_scallop)
        ivIconShapeDiagonal = findViewById(R.id.iv_icon_shape_diagonal)
        ivIconShapeOrganic = findViewById(R.id.iv_icon_shape_organic)
        ivIconShapeScallop = findViewById(R.id.iv_icon_shape_scallop)

        // Swatches
        swatchDynamic = findViewById(R.id.swatch_dynamic)
        swatchCustomPicker = findViewById(R.id.swatch_custom_picker)
        swatchOlive = findViewById(R.id.swatch_olive)
        swatchTeal = findViewById(R.id.swatch_teal)
        swatchSlate = findViewById(R.id.swatch_slate)
        swatchAmber = findViewById(R.id.swatch_amber)
        swatchCrimson = findViewById(R.id.swatch_crimson)

        ringSwatchDynamic = findViewById(R.id.ring_swatch_dynamic)
        ringSwatchCustom = findViewById(R.id.ring_swatch_custom)
        ringSwatchOlive = findViewById(R.id.ring_swatch_olive)
        ringSwatchTeal = findViewById(R.id.ring_swatch_teal)
        ringSwatchSlate = findViewById(R.id.ring_swatch_slate)
        ringSwatchAmber = findViewById(R.id.ring_swatch_amber)
        ringSwatchCrimson = findViewById(R.id.ring_swatch_crimson)

        discSwatchDynamic = findViewById(R.id.disc_swatch_dynamic)
        discSwatchCustom = findViewById(R.id.disc_swatch_custom)
        discSwatchOlive = findViewById(R.id.disc_swatch_olive)
        discSwatchTeal = findViewById(R.id.disc_swatch_teal)
        discSwatchSlate = findViewById(R.id.disc_swatch_slate)
        discSwatchAmber = findViewById(R.id.disc_swatch_amber)
        discSwatchCrimson = findViewById(R.id.disc_swatch_crimson)

        // Size Cards
        cardSize2x2 = findViewById(R.id.card_size_2x2)
        cardSize4x2 = findViewById(R.id.card_size_4x2)
        cardSize3x3 = findViewById(R.id.card_size_3x3)
        cardSize4x3 = findViewById(R.id.card_size_4x3)
        cardSize3x2 = findViewById(R.id.card_size_3x2)
        cardSize2x3 = findViewById(R.id.card_size_2x3)
        cardSize2x4 = findViewById(R.id.card_size_2x4)
        layoutSizeSecondary = findViewById(R.id.layout_size_secondary)

        // Opacity
        sliderTransparency = findViewById(R.id.slider_transparency)
        tvTransparencyValue = findViewById(R.id.tv_transparency_value)

        // Diagonal options & Angle
        layoutDiagonalOptions = findViewById(R.id.layout_diagonal_options)
        ivAngleNeedleIcon = findViewById(R.id.iv_angle_needle_icon)
        tvAngleDisplayValue = findViewById(R.id.tv_angle_display_value)
        sliderAngle = findViewById(R.id.slider_angle)
        chipAngleNeg60 = findViewById(R.id.chip_angle_neg_60)
        chipAngleNeg45 = findViewById(R.id.chip_angle_neg_45)
        chipAngleNeg30 = findViewById(R.id.chip_angle_neg_30)
        chipAngle0 = findViewById(R.id.chip_angle_0)
        chipAnglePos30 = findViewById(R.id.chip_angle_pos_30)
        chipAnglePos45 = findViewById(R.id.chip_angle_pos_45)
        chipAnglePos60 = findViewById(R.id.chip_angle_pos_60)

        toggleContent = findViewById(R.id.toggle_content)

        // Bottom Pin Button
        btnPinEdit = findViewById(R.id.btn_pin_edit)

        // Apply fluid press scale animations
        WidgetPreviewHelper.applyPressScaleEffect(cardShapeDiagonal)
        WidgetPreviewHelper.applyPressScaleEffect(cardShapeOrganic)
        WidgetPreviewHelper.applyPressScaleEffect(cardShapeScallop)
        WidgetPreviewHelper.applyPressScaleEffect(cardSize2x2)
        WidgetPreviewHelper.applyPressScaleEffect(cardSize4x2)
        WidgetPreviewHelper.applyPressScaleEffect(cardSize3x3)
        WidgetPreviewHelper.applyPressScaleEffect(cardSize4x3)
        WidgetPreviewHelper.applyPressScaleEffect(cardSize3x2)
        WidgetPreviewHelper.applyPressScaleEffect(cardSize2x3)
        WidgetPreviewHelper.applyPressScaleEffect(cardSize2x4)
        WidgetPreviewHelper.applyPressScaleEffect(btnPinEdit)
    }

    private fun setupListeners() {
        // Shape cards
        cardShapeDiagonal.setOnClickListener {
            viewModel.setCategory(WidgetCategory.DIAGONAL)
            viewModel.saveAndApply(this)
        }
        cardShapeOrganic.setOnClickListener {
            viewModel.setCategory(WidgetCategory.ORGANIC)
            viewModel.saveAndApply(this)
        }
        cardShapeScallop.setOnClickListener {
            viewModel.setCategory(WidgetCategory.SCALLOP)
            viewModel.saveAndApply(this)
        }

        // Swatches
        swatchDynamic.setOnClickListener {
            viewModel.setColorPalette(ColorPalette.DYNAMIC)
            viewModel.saveAndApply(this)
        }

        swatchCustomPicker.setOnClickListener {
            ColorWheelPickerDialog.show(this, WidgetPreferences.getCustomColor(this)) {
                viewModel.setColorPalette(ColorPalette.CUSTOM)
                viewModel.saveAndApply(this)
                setupVisualColorSwatches()
                updatePreview(animate = true)
            }
        }

        swatchOlive.setOnClickListener {
            viewModel.setColorPalette(ColorPalette.OLIVE)
            viewModel.saveAndApply(this)
        }
        swatchTeal.setOnClickListener {
            viewModel.setColorPalette(ColorPalette.TEAL)
            viewModel.saveAndApply(this)
        }
        swatchSlate.setOnClickListener {
            viewModel.setColorPalette(ColorPalette.SLATE)
            viewModel.saveAndApply(this)
        }
        swatchAmber.setOnClickListener {
            viewModel.setColorPalette(ColorPalette.AMBER)
            viewModel.saveAndApply(this)
        }
        swatchCrimson.setOnClickListener {
            viewModel.setColorPalette(ColorPalette.CRIMSON)
            viewModel.saveAndApply(this)
        }

        // Size Cards
        cardSize2x2.setOnClickListener {
            viewModel.setSize(WidgetSize.SIZE_2X2)
            viewModel.saveAndApply(this)
        }
        cardSize4x2.setOnClickListener {
            viewModel.setSize(WidgetSize.SIZE_4X2)
            viewModel.saveAndApply(this)
        }
        cardSize3x3.setOnClickListener {
            viewModel.setSize(WidgetSize.SIZE_3X3)
            viewModel.saveAndApply(this)
        }
        cardSize4x3.setOnClickListener {
            viewModel.setSize(WidgetSize.SIZE_4X3)
            viewModel.saveAndApply(this)
        }
        cardSize3x2.setOnClickListener {
            viewModel.setSize(WidgetSize.SIZE_3X2)
            viewModel.saveAndApply(this)
        }
        cardSize2x3.setOnClickListener {
            viewModel.setSize(WidgetSize.SIZE_2X3)
            viewModel.saveAndApply(this)
        }
        cardSize2x4.setOnClickListener {
            viewModel.setSize(WidgetSize.SIZE_2X4)
            viewModel.saveAndApply(this)
        }

        // Opacity Slider
        sliderTransparency.addOnChangeListener { _, value, _ ->
            val intVal = value.toInt()
            viewModel.setTransparency(intVal)
            tvTransparencyValue.text = getString(R.string.transparency_value_format, intVal)
        }
        sliderTransparency.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}
            override fun onStopTrackingTouch(slider: Slider) {
                viewModel.saveAndApply(this@WidgetEditActivity)
                updatePreview(animate = true)
            }
        })

        // Angle Slider (Continuous with live preview animation)
        sliderAngle.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                updateAngleDisplayAndRotation(value)
                viewModel.setRotationAngle(value)
            }
        }
        sliderAngle.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}
            override fun onStopTrackingTouch(slider: Slider) {
                viewModel.saveAndApply(this@WidgetEditActivity)
                updatePreview(animate = true)
            }
        })

        // Angle Quick-Snap Chips with smooth animation
        val angleChips = listOf(
            chipAngleNeg60 to -60f,
            chipAngleNeg45 to -45f,
            chipAngleNeg30 to -30f,
            chipAngle0 to 0f,
            chipAnglePos30 to 30f,
            chipAnglePos45 to 45f,
            chipAnglePos60 to 60f
        )

        for ((chip, targetAngle) in angleChips) {
            chip.setOnClickListener {
                animateAngleTo(targetAngle)
            }
        }

        // Content Mode
        toggleContent.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btn_content_weather -> viewModel.setContentMode(WidgetContentMode.WEATHER)
                    R.id.btn_content_clock -> viewModel.setContentMode(WidgetContentMode.CLOCK)
                    R.id.btn_content_combo -> viewModel.setContentMode(WidgetContentMode.COMBO)
                }
                viewModel.saveAndApply(this)
            }
        }

        // Pin Button
        btnPinEdit.setOnClickListener {
            viewModel.saveAndApply(this)
            pinCurrentWidget()
        }
    }

    private fun updateAngleDisplayAndRotation(angle: Float) {
        tvAngleDisplayValue.text = "${angle.toInt()}°"
        ivAngleNeedleIcon.animate()
            .rotation(angle)
            .setDuration(80)
            .start()
        highlightMatchingAngleChip(angle)
    }

    private fun animateAngleTo(targetAngle: Float) {
        val currentAngle = sliderAngle.value
        val animator = ValueAnimator.ofFloat(currentAngle, targetAngle).apply {
            duration = 260
            interpolator = DecelerateInterpolator()
            addUpdateListener { va ->
                val a = va.animatedValue as Float
                sliderAngle.value = a
                tvAngleDisplayValue.text = "${a.toInt()}°"
                ivAngleNeedleIcon.rotation = a
            }
        }
        animator.start()

        viewModel.setRotationAngle(targetAngle)
        viewModel.saveAndApply(this)
        highlightMatchingAngleChip(targetAngle)
        updatePreview(animate = true)
    }

    private fun highlightMatchingAngleChip(currentAngle: Float) {
        val chips = listOf(
            chipAngleNeg60 to -60f,
            chipAngleNeg45 to -45f,
            chipAngleNeg30 to -30f,
            chipAngle0 to 0f,
            chipAnglePos30 to 30f,
            chipAnglePos45 to 45f,
            chipAnglePos60 to 60f
        )
        val primaryColor = getThemeColor(androidx.appcompat.R.attr.colorPrimary, 0xFF006874.toInt())
        val onSurfaceVariant = getThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF3F484A.toInt())

        for ((chip, angle) in chips) {
            val isSelected = Math.abs(currentAngle - angle) < 0.5f
            if (isSelected) {
                chip.strokeColor = ContextCompat.getColorStateList(this, android.R.color.transparent)
                chip.setBackgroundColor(getThemeColor(com.google.android.material.R.attr.colorSurfaceVariant, 0xFFDBE4E6.toInt()))
                chip.setTextColor(primaryColor)
            } else {
                chip.strokeColor = ContextCompat.getColorStateList(this, com.google.android.material.R.color.material_dynamic_neutral60)
                chip.setBackgroundColor(getThemeColor(com.google.android.material.R.attr.colorSurface, 0xFFF8FDFF.toInt()))
                chip.setTextColor(onSurfaceVariant)
            }
        }
    }

    private fun setupVisualColorSwatches() {
        val density = resources.displayMetrics.density
        val insetPx = (6 * density).toInt()

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

    private fun observeViewModel() {
        viewModel.category.observe(this) { category ->
            updateShapeCardsUi(category)
            updateCategoryDependentUi(category)
            updatePreview(animate = true)
        }
        viewModel.size.observe(this) { size ->
            updateSizeCardsUi(size)
            updatePreview(animate = true)
        }
        viewModel.angle.observe(this) {
            updatePreview(animate = true)
        }
        viewModel.palette.observe(this) { palette ->
            updatePaletteSwatchesUi(palette)
            updatePreview(animate = true)
        }
        viewModel.contentMode.observe(this) {
            updatePreview(animate = true)
        }
        viewModel.transparency.observe(this) { transparency ->
            tvTransparencyValue.text = getString(R.string.transparency_value_format, transparency)
            updatePreview(animate = false)
        }
    }

    private fun syncUiWithViewModel() {
        updateShapeCardsUi(viewModel.category.value)
        updatePaletteSwatchesUi(viewModel.palette.value)
        updateSizeCardsUi(viewModel.size.value)

        // Angle sync
        val angle = viewModel.angle.value ?: -45f
        sliderAngle.value = angle.coerceIn(-90f, 90f)
        updateAngleDisplayAndRotation(angle)

        // Content sync
        val contentBtnId = when (viewModel.contentMode.value) {
            WidgetContentMode.WEATHER -> R.id.btn_content_weather
            WidgetContentMode.CLOCK -> R.id.btn_content_clock
            WidgetContentMode.COMBO -> R.id.btn_content_combo
            null -> R.id.btn_content_weather
        }
        toggleContent.check(contentBtnId)

        // Opacity sync
        val trans = (viewModel.transparency.value ?: 100).toFloat()
        sliderTransparency.value = trans
        tvTransparencyValue.text = getString(R.string.transparency_value_format, trans.toInt())

        updateCategoryDependentUi(viewModel.category.value)
    }

    private fun getThemeColor(attr: Int, defaultColor: Int = 0): Int {
        val tv = android.util.TypedValue()
        return if (theme.resolveAttribute(attr, tv, true)) {
            if (tv.resourceId != 0) ContextCompat.getColor(this, tv.resourceId) else tv.data
        } else defaultColor
    }

    private fun updateShapeCardsUi(category: WidgetCategory?) {
        val cat = category ?: WidgetCategory.DIAGONAL
        val density = resources.displayMetrics.density
        val primaryColor = getThemeColor(androidx.appcompat.R.attr.colorPrimary, 0xFF006874.toInt())
        val outlineColor = getThemeColor(com.google.android.material.R.attr.colorOutline, 0xFF6F797A.toInt())
        val surfaceVariant = getThemeColor(com.google.android.material.R.attr.colorSurfaceVariant, 0xFFDBE4E6.toInt())
        val surfaceColor = getThemeColor(com.google.android.material.R.attr.colorSurface, 0xFFF8FDFF.toInt())
        val onSurfaceVariant = getThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF3F484A.toInt())

        fun applyCardStyle(card: MaterialCardView, icon: ImageView, isSelected: Boolean) {
            if (isSelected) {
                card.strokeColor = primaryColor
                card.strokeWidth = (2 * density).toInt()
                card.setCardBackgroundColor(surfaceVariant)
                icon.setColorFilter(primaryColor)
            } else {
                card.strokeColor = outlineColor
                card.strokeWidth = (1 * density).toInt()
                card.setCardBackgroundColor(surfaceColor)
                icon.setColorFilter(onSurfaceVariant)
            }
        }

        applyCardStyle(cardShapeDiagonal, ivIconShapeDiagonal, cat == WidgetCategory.DIAGONAL)
        applyCardStyle(cardShapeOrganic, ivIconShapeOrganic, cat == WidgetCategory.ORGANIC)
        applyCardStyle(cardShapeScallop, ivIconShapeScallop, cat == WidgetCategory.SCALLOP)
    }

    private fun updatePaletteSwatchesUi(palette: ColorPalette?) {
        val pal = palette ?: ColorPalette.DYNAMIC
        val density = resources.displayMetrics.density
        val primaryColor = getThemeColor(androidx.appcompat.R.attr.colorPrimary, 0xFF006874.toInt())

        fun updateRing(ring: FrameLayout, isSelected: Boolean) {
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                if (isSelected) {
                    setStroke((3 * density).toInt(), primaryColor)
                    setColor(0x00000000)
                } else {
                    setStroke(0, 0)
                    setColor(0x00000000)
                }
            }
            ring.background = drawable
        }

        updateRing(ringSwatchDynamic, pal == ColorPalette.DYNAMIC)
        updateRing(ringSwatchCustom, pal == ColorPalette.CUSTOM)
        updateRing(ringSwatchOlive, pal == ColorPalette.OLIVE)
        updateRing(ringSwatchTeal, pal == ColorPalette.TEAL)
        updateRing(ringSwatchSlate, pal == ColorPalette.SLATE)
        updateRing(ringSwatchAmber, pal == ColorPalette.AMBER)
        updateRing(ringSwatchCrimson, pal == ColorPalette.CRIMSON)
    }

    private fun updateSizeCardsUi(selectedSize: WidgetSize?) {
        val size = selectedSize ?: WidgetSize.SIZE_2X2
        val density = resources.displayMetrics.density
        val primaryColor = getThemeColor(androidx.appcompat.R.attr.colorPrimary, 0xFF006874.toInt())
        val outlineColor = getThemeColor(com.google.android.material.R.attr.colorOutline, 0xFF6F797A.toInt())
        val surfaceVariant = getThemeColor(com.google.android.material.R.attr.colorSurfaceVariant, 0xFFDBE4E6.toInt())
        val surfaceColor = getThemeColor(com.google.android.material.R.attr.colorSurface, 0xFFF8FDFF.toInt())
        val onSurfaceVariant = getThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF3F484A.toInt())

        val cards = listOf(
            Triple(cardSize2x2, R.id.iv_icon_size_2x2 to R.id.tv_label_size_2x2, WidgetSize.SIZE_2X2),
            Triple(cardSize4x2, R.id.iv_icon_size_4x2 to R.id.tv_label_size_4x2, WidgetSize.SIZE_4X2),
            Triple(cardSize3x3, R.id.iv_icon_size_3x3 to R.id.tv_label_size_3x3, WidgetSize.SIZE_3X3),
            Triple(cardSize4x3, R.id.iv_icon_size_4x3 to R.id.tv_label_size_4x3, WidgetSize.SIZE_4X3),
            Triple(cardSize3x2, R.id.iv_icon_size_3x2 to R.id.tv_label_size_3x2, WidgetSize.SIZE_3X2),
            Triple(cardSize2x3, R.id.iv_icon_size_2x3 to R.id.tv_label_size_2x3, WidgetSize.SIZE_2X3),
            Triple(cardSize2x4, R.id.iv_icon_size_2x4 to R.id.tv_label_size_2x4, WidgetSize.SIZE_2X4),
        )

        for ((card, viewIds, itemSize) in cards) {
            val isSelected = (size == itemSize)
            val icon = card.findViewById<ImageView>(viewIds.first)
            val label = card.findViewById<TextView>(viewIds.second)
            if (isSelected) {
                card.strokeColor = primaryColor
                card.strokeWidth = (2 * density).toInt()
                card.setCardBackgroundColor(surfaceVariant)
                icon.setColorFilter(primaryColor)
                label.setTextColor(primaryColor)
            } else {
                card.strokeColor = outlineColor
                card.strokeWidth = (1 * density).toInt()
                card.setCardBackgroundColor(surfaceColor)
                icon.setColorFilter(onSurfaceVariant)
                label.setTextColor(onSurfaceVariant)
            }
        }
    }

    private fun updateCategoryDependentUi(category: WidgetCategory?) {
        val isDiagonal = category == WidgetCategory.DIAGONAL
        layoutDiagonalOptions.visibility = if (isDiagonal) View.VISIBLE else View.GONE
        layoutSizeSecondary.visibility = if (isDiagonal) View.VISIBLE else View.GONE

        if (!isDiagonal) {
            val currentSize = viewModel.size.value
            if (currentSize == WidgetSize.SIZE_2X3 || currentSize == WidgetSize.SIZE_2X4 || currentSize == WidgetSize.SIZE_3X2) {
                viewModel.setSize(WidgetSize.SIZE_2X2)
                updateSizeCardsUi(WidgetSize.SIZE_2X2)
            }
        }
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
