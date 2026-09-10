package com.iatb.materialthemes.ui

import android.animation.ValueAnimator
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
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
import com.iatb.materialthemes.data.WidgetContentMode
import com.iatb.materialthemes.data.WidgetPreferences
import com.iatb.materialthemes.widget.Diagonal4x3WidgetProvider
import com.iatb.materialthemes.widget.DiagonalWidgetProvider
import com.iatb.materialthemes.widget.OrganicWidgetProvider
import com.iatb.materialthemes.widget.OrganicWideWidgetProvider
import com.iatb.materialthemes.widget.ScallopWidgetProvider
import com.iatb.materialthemes.widget.ScallopWideWidgetProvider
import kotlin.math.abs

class WidgetEditActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel

    // Preview
    private lateinit var cardPreview: MaterialCardView
    private lateinit var ivPreviewWallpaper: ImageView
    private lateinit var previewContainer: FrameLayout
    private var previewAnimator: ValueAnimator? = null

    // Two-State Panels (Photo Editor Style)
    private lateinit var bottomControlContainer: FrameLayout
    private lateinit var panelMainTools: LinearLayout
    private lateinit var panelToolDetail: LinearLayout
    private lateinit var tvActiveToolTitle: TextView
    private lateinit var btnBackToTools: ImageButton
    private lateinit var btnConfirmTool: ImageButton

    // Tool Buttons in Carousel
    private lateinit var btnToolPalette: View
    private lateinit var btnToolSize: View
    private lateinit var btnToolAngle: View
    private lateinit var btnToolContent: View
    private lateinit var btnToolTransparency: View
    private lateinit var btnToolPin: View

    // Sub-panel Containers
    private lateinit var detailContainerPalette: View
    private lateinit var detailContainerSize: View
    private lateinit var detailContainerAngle: View
    private lateinit var detailContainerContent: View
    private lateinit var detailContainerTransparency: View

    // Shape Cards (Diagonal, Organic, Scallop - Always visible in State 1)
    private lateinit var cardShapeDiagonal: MaterialCardView
    private lateinit var cardShapeOrganic: MaterialCardView
    private lateinit var cardShapeScallop: MaterialCardView
    private lateinit var ivIconShapeDiagonal: ImageView
    private lateinit var ivIconShapeOrganic: ImageView
    private lateinit var ivIconShapeScallop: ImageView

    // Swatches (MaterialCardView wrapping disc + text + badge)
    private lateinit var swatchDynamic: MaterialCardView
    private lateinit var swatchCustomPicker: MaterialCardView
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

    // Angle Controls
    private lateinit var ivAngleNeedleIcon: ImageView
    private lateinit var tvAngleDisplayValue: TextView
    private lateinit var layoutAngleSliderContainer: View
    private lateinit var sliderAngle: Slider
    private lateinit var chipAngleNeg60: MaterialButton
    private lateinit var chipAngleNeg45: MaterialButton
    private lateinit var chipAngleNeg30: MaterialButton
    private lateinit var chipAngle0: MaterialButton
    private lateinit var chipAnglePos30: MaterialButton
    private lateinit var chipAnglePos45: MaterialButton
    private lateinit var chipAnglePos60: MaterialButton
    private lateinit var chipAngleCustom: MaterialButton

    private lateinit var toggleContent: MaterialButtonToggleGroup

    private enum class ToolType {
        PALETTE, SIZE, ANGLE, CONTENT, TRANSPARENCY
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_widget_edit)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        viewModel.initFromPreferences(this)

        initViews()
        setupListeners()
        setupVisualColorSwatches()
        setupBackNavigation()
        observeViewModel()
        syncUiWithViewModel()
        animateScreenEntrance()
    }

    private fun initViews() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.edit_content_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnBack = findViewById<ImageButton>(R.id.btn_back)
        val btnDone = findViewById<MaterialButton>(R.id.btn_done)
        val btnPinTop = findViewById<ImageButton>(R.id.btn_pin_top)

        btnBack.setOnClickListener {
            if (panelToolDetail.visibility == View.VISIBLE) {
                closeToolDetail()
            } else {
                finish()
            }
        }
        btnDone.setOnClickListener { finish() }
        btnPinTop.setOnClickListener {
            animateSelectionPop(btnPinTop)
            viewModel.saveAndApply(this)
            pinCurrentWidget()
        }

        cardPreview = findViewById(R.id.card_preview)
        ivPreviewWallpaper = findViewById(R.id.iv_preview_wallpaper)
        previewContainer = findViewById(R.id.preview_container)

        // Two-state panels
        bottomControlContainer = findViewById(R.id.bottom_control_container)
        panelMainTools = findViewById(R.id.panel_main_tools)
        panelToolDetail = findViewById(R.id.panel_tool_detail)
        tvActiveToolTitle = findViewById(R.id.tv_active_tool_title)
        btnBackToTools = findViewById(R.id.btn_back_to_tools)
        btnConfirmTool = findViewById(R.id.btn_confirm_tool)

        // Tool buttons in carousel
        btnToolPalette = findViewById(R.id.btn_tool_palette)
        btnToolSize = findViewById(R.id.btn_tool_size)
        btnToolAngle = findViewById(R.id.btn_tool_angle)
        btnToolContent = findViewById(R.id.btn_tool_content)
        btnToolTransparency = findViewById(R.id.btn_tool_transparency)
        btnToolPin = findViewById(R.id.btn_tool_pin)

        // Sub-panel containers
        detailContainerPalette = findViewById(R.id.detail_container_palette)
        detailContainerSize = findViewById(R.id.detail_container_size)
        detailContainerAngle = findViewById(R.id.detail_container_angle)
        detailContainerContent = findViewById(R.id.detail_container_content)
        detailContainerTransparency = findViewById(R.id.detail_container_transparency)

        // Shapes
        cardShapeDiagonal = findViewById(R.id.card_shape_diagonal)
        cardShapeOrganic = findViewById(R.id.card_shape_organic)
        cardShapeScallop = findViewById(R.id.card_shape_scallop)
        ivIconShapeDiagonal = findViewById(R.id.iv_icon_shape_diagonal)
        ivIconShapeOrganic = findViewById(R.id.iv_icon_shape_organic)
        ivIconShapeScallop = findViewById(R.id.iv_icon_shape_scallop)

        // Swatches (Cards wrapping disc + text + badge)
        swatchDynamic = findViewById(R.id.swatch_dynamic)
        swatchCustomPicker = findViewById(R.id.swatch_custom_picker)
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

        // Angle controls
        ivAngleNeedleIcon = findViewById(R.id.iv_angle_needle_icon)
        tvAngleDisplayValue = findViewById(R.id.tv_angle_display_value)
        layoutAngleSliderContainer = findViewById(R.id.layout_angle_slider_container)
        sliderAngle = findViewById(R.id.slider_angle)
        chipAngleNeg60 = findViewById(R.id.chip_angle_neg_60)
        chipAngleNeg45 = findViewById(R.id.chip_angle_neg_45)
        chipAngleNeg30 = findViewById(R.id.chip_angle_neg_30)
        chipAngle0 = findViewById(R.id.chip_angle_0)
        chipAnglePos30 = findViewById(R.id.chip_angle_pos_30)
        chipAnglePos45 = findViewById(R.id.chip_angle_pos_45)
        chipAnglePos60 = findViewById(R.id.chip_angle_pos_60)
        chipAngleCustom = findViewById(R.id.chip_angle_custom)

        toggleContent = findViewById(R.id.toggle_content)

        // Apply fluid press-scale animations to ALL interactive views
        val interactiveViews = listOf(
            btnBack, btnDone, btnPinTop, btnBackToTools, btnConfirmTool,
            cardShapeDiagonal, cardShapeOrganic, cardShapeScallop,
            btnToolPalette, btnToolSize, btnToolAngle, btnToolContent, btnToolTransparency, btnToolPin,
            swatchDynamic, swatchCustomPicker, swatchOlive, swatchTeal, swatchSlate, swatchAmber, swatchCrimson,
            cardSize2x2, cardSize4x2, cardSize3x3, cardSize4x3, cardSize3x2, cardSize2x3, cardSize2x4,
            chipAngleNeg60, chipAngleNeg45, chipAngleNeg30, chipAngle0,
            chipAnglePos30, chipAnglePos45, chipAnglePos60, chipAngleCustom
        )
        for (view in interactiveViews) {
            WidgetPreviewHelper.applyPressScaleEffect(view)
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (panelToolDetail.visibility == View.VISIBLE) {
                    closeToolDetail()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun setupListeners() {
        // Shape cards (State 1) with selection pop & preview pulse
        cardShapeDiagonal.setOnClickListener {
            animateSelectionPop(cardShapeDiagonal)
            viewModel.setCategory(WidgetCategory.DIAGONAL)
            viewModel.saveAndApply(this)
        }
        cardShapeOrganic.setOnClickListener {
            animateSelectionPop(cardShapeOrganic)
            viewModel.setCategory(WidgetCategory.ORGANIC)
            viewModel.saveAndApply(this)
        }
        cardShapeScallop.setOnClickListener {
            animateSelectionPop(cardShapeScallop)
            viewModel.setCategory(WidgetCategory.SCALLOP)
            viewModel.saveAndApply(this)
        }

        // Tool Carousel items -> open detail with transition
        btnToolPalette.setOnClickListener {
            animateSelectionPop(btnToolPalette)
            openToolDetail(ToolType.PALETTE)
        }
        btnToolSize.setOnClickListener {
            animateSelectionPop(btnToolSize)
            openToolDetail(ToolType.SIZE)
        }
        btnToolAngle.setOnClickListener {
            animateSelectionPop(btnToolAngle)
            openToolDetail(ToolType.ANGLE)
        }
        btnToolContent.setOnClickListener {
            animateSelectionPop(btnToolContent)
            openToolDetail(ToolType.CONTENT)
        }
        btnToolTransparency.setOnClickListener {
            animateSelectionPop(btnToolTransparency)
            openToolDetail(ToolType.TRANSPARENCY)
        }
        btnToolPin.setOnClickListener {
            animateSelectionPop(btnToolPin)
            viewModel.saveAndApply(this)
            pinCurrentWidget()
        }

        // Back from detail to main tools
        btnBackToTools.setOnClickListener { closeToolDetail() }
        btnConfirmTool.setOnClickListener { closeToolDetail() }

        // Swatches (wrapped in cards) with selection pop
        swatchDynamic.setOnClickListener {
            animateSelectionPop(swatchDynamic)
            viewModel.setColorPalette(ColorPalette.DYNAMIC)
            viewModel.saveAndApply(this)
        }

        swatchCustomPicker.setOnClickListener {
            animateSelectionPop(swatchCustomPicker)
            ColorWheelPickerDialog.show(this, WidgetPreferences.getCustomColor(this)) {
                viewModel.setColorPalette(ColorPalette.CUSTOM)
                viewModel.saveAndApply(this)
                setupVisualColorSwatches()
                updatePreview(animate = true)
            }
        }

        val standardSwatches = listOf(
            swatchOlive to ColorPalette.OLIVE,
            swatchTeal to ColorPalette.TEAL,
            swatchSlate to ColorPalette.SLATE,
            swatchAmber to ColorPalette.AMBER,
            swatchCrimson to ColorPalette.CRIMSON
        )
        for ((card, palette) in standardSwatches) {
            card.setOnClickListener {
                animateSelectionPop(card)
                viewModel.setColorPalette(palette)
                viewModel.saveAndApply(this)
            }
        }

        // Size Cards with selection pop
        val sizeCards = listOf(
            cardSize2x2 to WidgetSize.SIZE_2X2,
            cardSize4x2 to WidgetSize.SIZE_4X2,
            cardSize3x3 to WidgetSize.SIZE_3X3,
            cardSize4x3 to WidgetSize.SIZE_4X3,
            cardSize3x2 to WidgetSize.SIZE_3X2,
            cardSize2x3 to WidgetSize.SIZE_2X3,
            cardSize2x4 to WidgetSize.SIZE_2X4
        )
        for ((card, size) in sizeCards) {
            card.setOnClickListener {
                animateSelectionPop(card)
                viewModel.setSize(size)
                viewModel.saveAndApply(this)
            }
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

        // Angle Slider (Continuous when user drags slider)
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

        // Angle Preset Buttons with pop and compass needle physics
        val anglePresets = listOf(
            chipAngleNeg60 to -60f,
            chipAngleNeg45 to -45f,
            chipAngleNeg30 to -30f,
            chipAngle0 to 0f,
            chipAnglePos30 to 30f,
            chipAnglePos45 to 45f,
            chipAnglePos60 to 60f
        )

        for ((chip, targetAngle) in anglePresets) {
            chip.setOnClickListener {
                animateSelectionPop(chip)
                setPresetAngle(targetAngle)
            }
        }

        // Custom Angle Button (Expands slider smoothly if hidden)
        chipAngleCustom.setOnClickListener {
            animateSelectionPop(chipAngleCustom)
            if (layoutAngleSliderContainer.visibility != View.VISIBLE) {
                val container = findViewById<LinearLayout>(R.id.edit_content_container)
                android.transition.TransitionManager.beginDelayedTransition(
                    container,
                    android.transition.AutoTransition().apply { duration = 200 }
                )
                layoutAngleSliderContainer.visibility = View.VISIBLE
            }
            val current = viewModel.angle.value ?: -45f
            sliderAngle.value = current.coerceIn(-90f, 90f)
            highlightMatchingAngleButton(current, isCustomExplicit = true)
        }

        // Content Mode
        toggleContent.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btn_content_weather -> {
                        animateSelectionPop(findViewById(R.id.btn_content_weather))
                        viewModel.setContentMode(WidgetContentMode.WEATHER)
                    }
                    R.id.btn_content_clock -> {
                        animateSelectionPop(findViewById(R.id.btn_content_clock))
                        viewModel.setContentMode(WidgetContentMode.CLOCK)
                    }
                    R.id.btn_content_combo -> {
                        animateSelectionPop(findViewById(R.id.btn_content_combo))
                        viewModel.setContentMode(WidgetContentMode.COMBO)
                    }
                }
                viewModel.saveAndApply(this)
            }
        }
    }

    /**
     * Tactile spring pop effect when an item is selected.
     */
    private fun animateSelectionPop(view: View) {
        view.animate().cancel()
        view.scaleX = 0.90f
        view.scaleY = 0.90f
        view.animate()
            .scaleX(1.06f)
            .scaleY(1.06f)
            .setDuration(120)
            .setInterpolator(OvershootInterpolator(2.4f))
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(90)
                    .start()
            }
            .start()
    }

    /**
     * Smooth Photo-Editor style layout transition when opening tool drawer.
     */
    private fun openToolDetail(type: ToolType) {
        val container = findViewById<LinearLayout>(R.id.edit_content_container)
        android.transition.TransitionManager.beginDelayedTransition(
            container,
            android.transition.AutoTransition().apply {
                duration = 240
                interpolator = DecelerateInterpolator(1.4f)
            }
        )

        // Silky smooth zoom on the background wallpaper image
        ivPreviewWallpaper.animate()
            .scaleX(1.04f)
            .scaleY(1.04f)
            .setDuration(260)
            .setInterpolator(DecelerateInterpolator(1.4f))
            .start()

        panelMainTools.visibility = View.GONE

        detailContainerPalette.visibility = View.GONE
        detailContainerSize.visibility = View.GONE
        detailContainerAngle.visibility = View.GONE
        detailContainerContent.visibility = View.GONE
        detailContainerTransparency.visibility = View.GONE

        val activeSubView: View = when (type) {
            ToolType.PALETTE -> {
                tvActiveToolTitle.text = getString(R.string.tool_palette)
                detailContainerPalette
            }
            ToolType.SIZE -> {
                tvActiveToolTitle.text = getString(R.string.tool_size)
                detailContainerSize
            }
            ToolType.ANGLE -> {
                tvActiveToolTitle.text = getString(R.string.tool_angle)
                detailContainerAngle
            }
            ToolType.CONTENT -> {
                tvActiveToolTitle.text = getString(R.string.tool_content)
                detailContainerContent
            }
            ToolType.TRANSPARENCY -> {
                tvActiveToolTitle.text = getString(R.string.tool_transparency)
                detailContainerTransparency
            }
        }

        activeSubView.visibility = View.VISIBLE
        panelToolDetail.visibility = View.VISIBLE
    }

    /**
     * Smooth Photo-Editor style layout transition when closing tool drawer.
     */
    private fun closeToolDetail() {
        val container = findViewById<LinearLayout>(R.id.edit_content_container)
        android.transition.TransitionManager.beginDelayedTransition(
            container,
            android.transition.AutoTransition().apply {
                duration = 240
                interpolator = DecelerateInterpolator(1.4f)
            }
        )

        // Smooth return of background wallpaper image scale
        ivPreviewWallpaper.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(260)
            .setInterpolator(DecelerateInterpolator(1.4f))
            .start()

        panelToolDetail.visibility = View.GONE
        panelMainTools.visibility = View.VISIBLE
    }

    private fun setPresetAngle(targetAngle: Float) {
        sliderAngle.value = targetAngle.coerceIn(-90f, 90f)

        // Smooth mechanical compass bounce on needle
        ivAngleNeedleIcon.animate()
            .rotation(targetAngle)
            .setDuration(280)
            .setInterpolator(OvershootInterpolator(1.4f))
            .start()

        tvAngleDisplayValue.text = "${targetAngle.toInt()}°"

        // Collapse slider if visible with smooth layout transition
        if (layoutAngleSliderContainer.visibility == View.VISIBLE) {
            val container = findViewById<LinearLayout>(R.id.edit_content_container)
            android.transition.TransitionManager.beginDelayedTransition(
                container,
                android.transition.AutoTransition().apply { duration = 180 }
            )
            layoutAngleSliderContainer.visibility = View.GONE
        }

        viewModel.setRotationAngle(targetAngle)
        viewModel.saveAndApply(this)
        highlightMatchingAngleButton(targetAngle, isCustomExplicit = false)
        updatePreview(animate = true)
    }

    private fun updateAngleDisplayAndRotation(angle: Float) {
        tvAngleDisplayValue.text = "${angle.toInt()}°"
        ivAngleNeedleIcon.rotation = angle
        highlightMatchingAngleButton(angle, isCustomExplicit = false)
    }

    private fun highlightMatchingAngleButton(currentAngle: Float, isCustomExplicit: Boolean) {
        val chips = listOf(
            chipAngleNeg60 to -60f,
            chipAngleNeg45 to -45f,
            chipAngleNeg30 to -30f,
            chipAngle0 to 0f,
            chipAnglePos30 to 30f,
            chipAnglePos45 to 45f,
            chipAnglePos60 to 60f
        )
        val density = resources.displayMetrics.density
        val primaryColor = getThemeColor(androidx.appcompat.R.attr.colorPrimary, 0xFF006874.toInt())
        val surfaceVariant = getThemeColor(com.google.android.material.R.attr.colorSurfaceVariant, 0xFFDBE4E6.toInt())
        val surfaceColor = getThemeColor(com.google.android.material.R.attr.colorSurface, 0xFFF8FDFF.toInt())
        val onSurfaceVariant = getThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF3F484A.toInt())

        var foundPreset = false
        if (!isCustomExplicit) {
            for ((chip, angle) in chips) {
                val isSelected = abs(currentAngle - angle) < 0.5f
                if (isSelected) {
                    foundPreset = true
                    chip.strokeWidth = (2 * density).toInt()
                    chip.strokeColor = ContextCompat.getColorStateList(this, android.R.color.transparent)
                    chip.setBackgroundColor(surfaceVariant)
                    chip.setTextColor(primaryColor)
                } else {
                    chip.strokeWidth = (1 * density).toInt()
                    chip.strokeColor = ContextCompat.getColorStateList(this, com.google.android.material.R.color.material_dynamic_neutral60)
                    chip.setBackgroundColor(surfaceColor)
                    chip.setTextColor(onSurfaceVariant)
                }
            }
        } else {
            for ((chip, _) in chips) {
                chip.strokeWidth = (1 * density).toInt()
                chip.strokeColor = ContextCompat.getColorStateList(this, com.google.android.material.R.color.material_dynamic_neutral60)
                chip.setBackgroundColor(surfaceColor)
                chip.setTextColor(onSurfaceVariant)
            }
        }

        // Highlight Custom button if custom is active or no preset matched
        val isCustomSelected = isCustomExplicit || !foundPreset
        if (isCustomSelected) {
            chipAngleCustom.strokeWidth = (2 * density).toInt()
            chipAngleCustom.strokeColor = ContextCompat.getColorStateList(this, android.R.color.transparent)
            chipAngleCustom.setBackgroundColor(surfaceVariant)
            chipAngleCustom.setTextColor(primaryColor)
        } else {
            chipAngleCustom.strokeWidth = (1 * density).toInt()
            chipAngleCustom.strokeColor = ContextCompat.getColorStateList(this, com.google.android.material.R.color.material_dynamic_neutral60)
            chipAngleCustom.setBackgroundColor(surfaceColor)
            chipAngleCustom.setTextColor(onSurfaceVariant)
        }
    }

    private fun setupVisualColorSwatches() {
        val density = resources.displayMetrics.density
        val insetPx = (5 * density).toInt()

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
        tvAngleDisplayValue.text = "${angle.toInt()}°"
        ivAngleNeedleIcon.rotation = angle

        val presetAngles = listOf(-60f, -45f, -30f, 0f, 30f, 45f, 60f)
        val isPreset = presetAngles.any { abs(it - angle) < 0.5f }
        layoutAngleSliderContainer.visibility = if (isPreset) View.GONE else View.VISIBLE
        highlightMatchingAngleButton(angle, isCustomExplicit = !isPreset)

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

    /**
     * Highlights the selected color palette by applying the border and tinted background
     * to the OUTER MaterialCardView wrapping both disc AND label text.
     */
    private fun updatePaletteSwatchesUi(palette: ColorPalette?) {
        val pal = palette ?: ColorPalette.DYNAMIC
        val density = resources.displayMetrics.density
        val primaryColor = getThemeColor(androidx.appcompat.R.attr.colorPrimary, 0xFF006874.toInt())
        val surfaceVariant = getThemeColor(com.google.android.material.R.attr.colorSurfaceVariant, 0xFFDBE4E6.toInt())
        val onSurfaceVariant = getThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF3F484A.toInt())

        val swatches = listOf(
            swatchDynamic to tvLabelSwatchDynamic to (pal == ColorPalette.DYNAMIC),
            swatchCustomPicker to tvLabelSwatchCustom to (pal == ColorPalette.CUSTOM),
            swatchOlive to tvLabelSwatchOlive to (pal == ColorPalette.OLIVE),
            swatchTeal to tvLabelSwatchTeal to (pal == ColorPalette.TEAL),
            swatchSlate to tvLabelSwatchSlate to (pal == ColorPalette.SLATE),
            swatchAmber to tvLabelSwatchAmber to (pal == ColorPalette.AMBER),
            swatchCrimson to tvLabelSwatchCrimson to (pal == ColorPalette.CRIMSON),
        )

        for (item in swatches) {
            val card = item.first.first
            val label = item.first.second
            val isSelected = item.second

            if (isSelected) {
                card.strokeColor = primaryColor
                card.strokeWidth = (2 * density).toInt()
                card.setCardBackgroundColor(surfaceVariant)
                label.setTextColor(primaryColor)
            } else {
                card.strokeColor = Color.TRANSPARENT
                card.strokeWidth = 0
                card.setCardBackgroundColor(Color.TRANSPARENT)
                label.setTextColor(onSurfaceVariant)
            }
        }
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
        btnToolAngle.visibility = if (isDiagonal) View.VISIBLE else View.GONE
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

        if (animate) {
            // Subtle spring breathing pulse on the preview container
            previewContainer.animate().cancel()
            previewContainer.scaleX = 0.96f
            previewContainer.scaleY = 0.96f
            previewContainer.alpha = 0.88f
            previewContainer.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(260)
                .setInterpolator(OvershootInterpolator(1.2f))
                .start()
        }

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

    /**
     * Staggered entrance animations when entering the Edit screen.
     */
    private fun animateScreenEntrance() {
        cardPreview.alpha = 0f
        cardPreview.scaleX = 0.92f
        cardPreview.scaleY = 0.92f
        cardPreview.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(340)
            .setInterpolator(DecelerateInterpolator(1.4f))
            .start()

        // Wallpaper cinematic zoom-in entrance
        ivPreviewWallpaper.scaleX = 1.08f
        ivPreviewWallpaper.scaleY = 1.08f
        ivPreviewWallpaper.alpha = 0.5f
        ivPreviewWallpaper.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1.0f)
            .setDuration(450)
            .setInterpolator(DecelerateInterpolator(1.4f))
            .start()

        bottomControlContainer.alpha = 0f
        bottomControlContainer.translationY = 60f
        bottomControlContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(360)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .start()

        val tools = listOf(btnToolPalette, btnToolSize, btnToolAngle, btnToolContent, btnToolTransparency, btnToolPin)
        tools.forEachIndexed { index, tool ->
            tool.alpha = 0f
            tool.translationX = 35f
            tool.animate()
                .alpha(1f)
                .translationX(0f)
                .setStartDelay(100L + index * 25L)
                .setDuration(220)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
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
