package com.iatb.materialthemes.ui

import android.animation.ValueAnimator
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.text.InputFilter
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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
import com.iatb.materialthemes.widget.ActiveWidgetManager
import kotlin.math.abs

class WidgetPresetEditActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IS_EDIT = "extra_is_edit"
        const val EXTRA_PRESET_ID = "extra_preset_id"
        const val EXTRA_PRESET_TITLE = "extra_preset_title"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_PALETTE = "extra_palette"
        const val EXTRA_TRANSPARENCY = "extra_transparency"
        const val EXTRA_ANGLE = "extra_angle"
        const val EXTRA_SIZE = "extra_size"
        const val EXTRA_CONTENT_MODE = "extra_content_mode"
        const val MAX_PRESET_TITLE_LENGTH = 20
    }

    private var isEditMode: Boolean = false
    private var editingPresetId: String? = null
    private var editingPresetTitle: String? = null

    private lateinit var viewModel: MainViewModel
    private lateinit var ambientBgView: AmbientMeshBackgroundView

    // Preview
    private lateinit var cardPreview: MaterialCardView
    private lateinit var ivPreviewWallpaper: ImageView
    private lateinit var previewContainer: FrameLayout
    private var previewAnimator: ValueAnimator? = null

    // Two-State Panels
    private lateinit var panelMainTools: LinearLayout
    private lateinit var panelToolDetail: LinearLayout
    private lateinit var tvActiveToolTitle: TextView
    private lateinit var btnBackToTools: ImageButton
    private lateinit var btnSavePreset: ImageButton
    private lateinit var btnEditPresetName: ImageButton
    private lateinit var btnBack: ImageButton

    // Tool Buttons in Carousel
    private lateinit var btnToolPalette: View
    private lateinit var btnToolSize: View
    private lateinit var btnToolAngle: View
    private lateinit var btnToolContent: View
    private lateinit var btnToolTransparency: View

    // Sub-panel Containers
    private lateinit var detailContainerPalette: View
    private lateinit var detailContainerSize: View
    private lateinit var detailContainerAngle: View
    private lateinit var detailContainerContent: View
    private lateinit var detailContainerTransparency: View

    // Shapes
    private lateinit var cardShapeDiagonal: MaterialCardView
    private lateinit var cardShapeOrganic: MaterialCardView
    private lateinit var cardShapeScallop: MaterialCardView
    private lateinit var ivIconShapeDiagonal: ImageView
    private lateinit var ivIconShapeOrganic: ImageView
    private lateinit var ivIconShapeScallop: ImageView

    // Swatches
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

    // Size Cards
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

    // Angle controls
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

    private enum class ToolType(val titleResId: Int) {
        PALETTE(R.string.tool_palette),
        SIZE(R.string.tool_size),
        ANGLE(R.string.tool_angle),
        CONTENT(R.string.tool_content),
        TRANSPARENCY(R.string.tool_transparency)
    }

    private var currentActiveTool: ToolType? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_widget_preset_edit)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        initInitialState()

        initViews()

        val snapshot = SharedAmbientBackgroundHolder.getSnapshot()
        if (snapshot != null) {
            ambientBgView.setOrbs(snapshot)
        }
        ambientBgView.post {
            ambientBgView.transitionToNewConstellation(durationMs = 950L) {
                SharedAmbientBackgroundHolder.saveSnapshot(ambientBgView.getOrbsSnapshot())
            }
        }

        setupBackNavigation()
        setupListeners()
        observeViewModel()
        animateScreenEntrance()
    }

    override fun onPause() {
        super.onPause()
        SharedAmbientBackgroundHolder.saveSnapshot(ambientBgView.getOrbsSnapshot())
    }

    private fun initInitialState() {
        ActiveWidgetManager.syncActiveWidget(this)
        val activeCategory = WidgetPreferences.getCategory(this)
        val activeSize = WidgetPreferences.getSize(this)

        isEditMode = intent.getBooleanExtra(EXTRA_IS_EDIT, false)
        if (isEditMode) {
            editingPresetId = intent.getStringExtra(EXTRA_PRESET_ID)
            editingPresetTitle = intent.getStringExtra(EXTRA_PRESET_TITLE)

            val initialPal = intent.getStringExtra(EXTRA_PALETTE)?.let {
                runCatching { ColorPalette.valueOf(it) }.getOrNull()
            } ?: ColorPalette.DYNAMIC

            val initialTrans = intent.getIntExtra(EXTRA_TRANSPARENCY, 100)
            val initialAngle = intent.getFloatExtra(EXTRA_ANGLE, -45f)

            val initialContent = intent.getStringExtra(EXTRA_CONTENT_MODE)?.let {
                runCatching { WidgetContentMode.valueOf(it) }.getOrNull()
            } ?: WidgetContentMode.WEATHER

            viewModel.setCategory(activeCategory)
            viewModel.setSize(activeSize)
            viewModel.setColorPalette(initialPal)
            viewModel.setTransparency(initialTrans)
            viewModel.setRotationAngle(initialAngle)
            viewModel.setContentMode(initialContent)
        } else {
            // Case 1: Add Mode (Brand-new preset canvas using active widget shape & size)
            editingPresetId = null
            editingPresetTitle = null

            viewModel.setCategory(activeCategory)
            viewModel.setSize(activeSize)
            viewModel.setColorPalette(ColorPalette.DYNAMIC)
            viewModel.setTransparency(100)
            viewModel.setRotationAngle(-45f)
            viewModel.setContentMode(WidgetContentMode.WEATHER)
        }
    }

    private fun initViews() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.edit_content_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            findViewById<View>(R.id.bottom_control_container).setPadding(
                0,
                0,
                0,
                systemBars.bottom
            )
            insets
        }

        val tvScreenTitle = findViewById<TextView>(R.id.tv_screen_title)
        tvScreenTitle?.setText(if (isEditMode) R.string.preset_edit_screen_title else R.string.preset_create_screen_title)

        btnBack = findViewById(R.id.btn_back)
        btnSavePreset = findViewById(R.id.btn_save_preset)
        btnEditPresetName = findViewById(R.id.btn_edit_preset_name)

        if (isEditMode) {
            btnEditPresetName.visibility = View.VISIBLE
            btnEditPresetName.setOnClickListener {
                animateSelectionPop(btnEditPresetName)
                showEditPresetNameDialog()
            }
        } else {
            btnEditPresetName.visibility = View.GONE
        }

        ambientBgView = findViewById(R.id.ambient_bg_view)

        btnBack.setOnClickListener {
            if (panelToolDetail.visibility == View.VISIBLE) {
                closeToolDetail()
            } else {
                finishWithTransition()
            }
        }

        btnSavePreset.setOnClickListener {
            animateSelectionPop(btnSavePreset)
            checkAndSavePreset()
        }

        cardPreview = findViewById(R.id.card_preview)
        ivPreviewWallpaper = findViewById(R.id.iv_preview_wallpaper)
        previewContainer = findViewById(R.id.preview_container)

        // Two-state panels
        panelMainTools = findViewById(R.id.panel_main_tools)
        panelToolDetail = findViewById(R.id.panel_tool_detail)
        tvActiveToolTitle = findViewById(R.id.tv_active_tool_title)
        btnBackToTools = findViewById(R.id.btn_back_to_tools)

        // Tool buttons in carousel
        btnToolPalette = findViewById(R.id.btn_tool_palette)
        btnToolSize = findViewById(R.id.btn_tool_size)
        btnToolAngle = findViewById(R.id.btn_tool_angle)
        btnToolContent = findViewById(R.id.btn_tool_content)
        btnToolTransparency = findViewById(R.id.btn_tool_transparency)

        // Sub-panel containers
        detailContainerPalette = findViewById(R.id.detail_container_palette)
        detailContainerSize = findViewById(R.id.detail_container_size)
        detailContainerAngle = findViewById(R.id.detail_container_angle)
        detailContainerContent = findViewById(R.id.detail_container_content)
        detailContainerTransparency = findViewById(R.id.detail_container_transparency)

        findViewById<View>(R.id.section_shapes)?.visibility = View.GONE
        btnToolSize.visibility = View.GONE
        detailContainerSize.visibility = View.GONE

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

        // Apply press-scale effect
        val interactiveViews = listOf(
            btnBack, btnSavePreset, btnBackToTools,
            cardShapeDiagonal, cardShapeOrganic, cardShapeScallop,
            btnToolPalette, btnToolSize, btnToolAngle, btnToolContent, btnToolTransparency,
            swatchDynamic, swatchCustomPicker, swatchOlive, swatchTeal, swatchSlate, swatchAmber, swatchCrimson,
            cardSize2x2, cardSize4x2, cardSize3x3, cardSize4x3, cardSize3x2, cardSize2x3, cardSize2x4,
            chipAngleNeg60, chipAngleNeg45, chipAngleNeg30, chipAngle0,
            chipAnglePos30, chipAnglePos45, chipAnglePos60, chipAngleCustom
        )
        for (view in interactiveViews) {
            WidgetPreviewHelper.applyPressScaleEffect(view)
        }
    }

    private fun finishWithTransition() {
        SharedAmbientBackgroundHolder.saveSnapshot(ambientBgView.getOrbsSnapshot())
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (panelToolDetail.visibility == View.VISIBLE) {
                    closeToolDetail()
                } else {
                    finishWithTransition()
                }
            }
        })
    }

    private fun setupListeners() {
        // Shapes
        cardShapeDiagonal.setOnClickListener {
            animateSelectionPop(cardShapeDiagonal)
            viewModel.setCategory(WidgetCategory.DIAGONAL)
        }
        cardShapeOrganic.setOnClickListener {
            animateSelectionPop(cardShapeOrganic)
            viewModel.setCategory(WidgetCategory.ORGANIC)
        }
        cardShapeScallop.setOnClickListener {
            animateSelectionPop(cardShapeScallop)
            viewModel.setCategory(WidgetCategory.SCALLOP)
        }

        // Tools
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

        btnBackToTools.setOnClickListener { closeToolDetail() }

        // Palette swatches
        swatchDynamic.setOnClickListener {
            animateSelectionPop(swatchDynamic)
            viewModel.setColorPalette(ColorPalette.DYNAMIC)
        }

        swatchCustomPicker.setOnClickListener {
            animateSelectionPop(swatchCustomPicker)
            ColorWheelPickerDialog.show(this, WidgetPreferences.getCustomColor(this)) {
                viewModel.setColorPalette(ColorPalette.CUSTOM)
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
            }
        }

        // Size cards
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
            }
        }

        // Transparency
        sliderTransparency.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                viewModel.setTransparency(value.toInt())
            }
        }

        // Angle
        chipAngleNeg60.setOnClickListener { setPresetAngle(-60f) }
        chipAngleNeg45.setOnClickListener { setPresetAngle(-45f) }
        chipAngleNeg30.setOnClickListener { setPresetAngle(-30f) }
        chipAngle0.setOnClickListener { setPresetAngle(0f) }
        chipAnglePos30.setOnClickListener { setPresetAngle(30f) }
        chipAnglePos45.setOnClickListener { setPresetAngle(45f) }
        chipAnglePos60.setOnClickListener { setPresetAngle(60f) }

        chipAngleCustom.setOnClickListener {
            val isExpanded = layoutAngleSliderContainer.visibility == View.VISIBLE
            val container = findViewById<LinearLayout>(R.id.edit_content_container)
            android.transition.TransitionManager.beginDelayedTransition(
                container,
                android.transition.AutoTransition().apply { duration = 200 }
            )
            layoutAngleSliderContainer.visibility = if (isExpanded) View.GONE else View.VISIBLE
            updateAngleChipsUi(viewModel.angle.value ?: -45f)
        }

        sliderAngle.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                ivAngleNeedleIcon.rotation = value
                tvAngleDisplayValue.text = "${value.toInt()}°"
                viewModel.setRotationAngle(value)
                updateAngleChipsUi(value)
            }
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
            }
        }
    }

    private fun openToolDetail(tool: ToolType) {
        currentActiveTool = tool
        tvActiveToolTitle.setText(tool.titleResId)

        val container = findViewById<LinearLayout>(R.id.edit_content_container)
        android.transition.TransitionManager.beginDelayedTransition(
            container,
            android.transition.AutoTransition().apply {
                duration = 240
                interpolator = DecelerateInterpolator(1.4f)
            }
        )

        ivPreviewWallpaper.animate()
            .scaleX(1.04f)
            .scaleY(1.04f)
            .setDuration(260)
            .setInterpolator(DecelerateInterpolator(1.4f))
            .start()

        panelMainTools.visibility = View.GONE

        val allSubViews = listOf(
            detailContainerPalette,
            detailContainerSize,
            detailContainerAngle,
            detailContainerContent,
            detailContainerTransparency
        )
        for (view in allSubViews) {
            view.visibility = View.GONE
        }

        val activeSubView = when (tool) {
            ToolType.PALETTE -> detailContainerPalette
            ToolType.SIZE -> detailContainerSize
            ToolType.ANGLE -> detailContainerAngle
            ToolType.CONTENT -> detailContainerContent
            ToolType.TRANSPARENCY -> detailContainerTransparency
        }

        activeSubView.visibility = View.VISIBLE
        panelToolDetail.visibility = View.VISIBLE
        panelToolDetail.alpha = 0f
        panelToolDetail.translationY = 24f
        panelToolDetail.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(220)
            .setInterpolator(DecelerateInterpolator(1.4f))
            .start()
    }

    private fun closeToolDetail() {
        currentActiveTool = null
        val container = findViewById<LinearLayout>(R.id.edit_content_container)
        android.transition.TransitionManager.beginDelayedTransition(
            container,
            android.transition.AutoTransition().apply {
                duration = 240
                interpolator = DecelerateInterpolator(1.4f)
            }
        )

        ivPreviewWallpaper.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(260)
            .setInterpolator(DecelerateInterpolator(1.4f))
            .start()

        panelToolDetail.visibility = View.GONE
        panelMainTools.visibility = View.VISIBLE
        animateToolsEntrance()
    }

    private fun setPresetAngle(targetAngle: Float) {
        sliderAngle.value = targetAngle.coerceIn(-90f, 90f)

        ivAngleNeedleIcon.animate()
            .rotation(targetAngle)
            .setDuration(280)
            .setInterpolator(OvershootInterpolator(1.4f))
            .start()

        tvAngleDisplayValue.text = "${targetAngle.toInt()}°"

        if (layoutAngleSliderContainer.visibility == View.VISIBLE) {
            val container = findViewById<LinearLayout>(R.id.edit_content_container)
            android.transition.TransitionManager.beginDelayedTransition(
                container,
                android.transition.AutoTransition().apply { duration = 180 }
            )
            layoutAngleSliderContainer.visibility = View.GONE
        }

        viewModel.setRotationAngle(targetAngle)
        updateAngleChipsUi(targetAngle)
    }

    private fun observeViewModel() {
        viewModel.category.observe(this) { cat ->
            updateShapeCardsUi(cat)
            updateToolAvailability(cat, animate = true)
            updatePreview(animate = true)
        }

        viewModel.size.observe(this) { size ->
            updateSizeCardsUi(size)
            updatePreview(animate = true)
        }

        viewModel.palette.observe(this) { pal ->
            updatePaletteUi(pal)
            updatePreview(animate = true)
        }

        viewModel.transparency.observe(this) { trans ->
            updateTransparencyUi(trans)
            updatePreview(animate = false)
        }

        viewModel.angle.observe(this) { angle ->
            updateAngleUi(angle)
            updatePreview(animate = true)
        }

        viewModel.contentMode.observe(this) { mode ->
            updateContentUi(mode)
            updatePreview(animate = true)
        }
    }

    private fun updateShapeCardsUi(category: WidgetCategory) {
        val primaryColor = getThemeColor(androidx.appcompat.R.attr.colorPrimary, 0xFF006874.toInt())
        val outlineColor = getThemeColor(com.google.android.material.R.attr.colorOutline, 0xFF6F797A.toInt())
        val surfaceVariant = getThemeColor(com.google.android.material.R.attr.colorSurfaceVariant, 0xFFDBE4E6.toInt())
        val surfaceColor = getThemeColor(com.google.android.material.R.attr.colorSurface, 0xFFF8FDFF.toInt())
        val density = resources.displayMetrics.density

        val shapes = listOf(
            Triple(cardShapeDiagonal, ivIconShapeDiagonal, category == WidgetCategory.DIAGONAL),
            Triple(cardShapeOrganic, ivIconShapeOrganic, category == WidgetCategory.ORGANIC),
            Triple(cardShapeScallop, ivIconShapeScallop, category == WidgetCategory.SCALLOP)
        )

        for ((card, icon, isSelected) in shapes) {
            if (isSelected) {
                card.strokeColor = primaryColor
                card.strokeWidth = (2 * density).toInt()
                card.setCardBackgroundColor(surfaceVariant)
                icon.setColorFilter(primaryColor)
            } else {
                card.strokeColor = outlineColor
                card.strokeWidth = (1 * density).toInt()
                card.setCardBackgroundColor(surfaceColor)
                icon.setColorFilter(getThemeColor(com.google.android.material.R.attr.colorOnSurface))
            }
        }
    }

    private fun updatePaletteUi(palette: ColorPalette) {
        val primaryColor = getThemeColor(androidx.appcompat.R.attr.colorPrimary, 0xFF006874.toInt())
        val outlineColor = getThemeColor(com.google.android.material.R.attr.colorOutline, 0xFF6F797A.toInt())
        val surfaceVariant = getThemeColor(com.google.android.material.R.attr.colorSurfaceVariant, 0xFFDBE4E6.toInt())
        val surfaceColor = getThemeColor(com.google.android.material.R.attr.colorSurface, 0xFFF8FDFF.toInt())
        val density = resources.displayMetrics.density

        val swatches = listOf(
            Triple(swatchDynamic, tvLabelSwatchDynamic, palette == ColorPalette.DYNAMIC),
            Triple(swatchCustomPicker, tvLabelSwatchCustom, palette == ColorPalette.CUSTOM),
            Triple(swatchOlive, tvLabelSwatchOlive, palette == ColorPalette.OLIVE),
            Triple(swatchTeal, tvLabelSwatchTeal, palette == ColorPalette.TEAL),
            Triple(swatchSlate, tvLabelSwatchSlate, palette == ColorPalette.SLATE),
            Triple(swatchAmber, tvLabelSwatchAmber, palette == ColorPalette.AMBER),
            Triple(swatchCrimson, tvLabelSwatchCrimson, palette == ColorPalette.CRIMSON)
        )

        for ((card, label, isSelected) in swatches) {
            if (isSelected) {
                card.strokeColor = primaryColor
                card.strokeWidth = (2 * density).toInt()
                card.setCardBackgroundColor(surfaceVariant)
                label.setTextColor(primaryColor)
            } else {
                card.strokeColor = outlineColor
                card.strokeWidth = (1 * density).toInt()
                card.setCardBackgroundColor(surfaceColor)
                label.setTextColor(getThemeColor(com.google.android.material.R.attr.colorOnSurface))
            }
        }
    }

    private fun updateSizeCardsUi(selectedSize: WidgetSize) {
        val primaryColor = getThemeColor(androidx.appcompat.R.attr.colorPrimary, 0xFF006874.toInt())
        val outlineColor = getThemeColor(com.google.android.material.R.attr.colorOutline, 0xFF6F797A.toInt())
        val surfaceVariant = getThemeColor(com.google.android.material.R.attr.colorSurfaceVariant, 0xFFDBE4E6.toInt())
        val surfaceColor = getThemeColor(com.google.android.material.R.attr.colorSurface, 0xFFF8FDFF.toInt())
        val density = resources.displayMetrics.density

        val cards = listOf(
            cardSize2x2 to WidgetSize.SIZE_2X2,
            cardSize4x2 to WidgetSize.SIZE_4X2,
            cardSize3x3 to WidgetSize.SIZE_3X3,
            cardSize4x3 to WidgetSize.SIZE_4X3,
            cardSize3x2 to WidgetSize.SIZE_3X2,
            cardSize2x3 to WidgetSize.SIZE_2X3,
            cardSize2x4 to WidgetSize.SIZE_2X4
        )

        for ((card, size) in cards) {
            val isSelected = (size == selectedSize)
            if (isSelected) {
                card.strokeColor = primaryColor
                card.strokeWidth = (2 * density).toInt()
                card.setCardBackgroundColor(surfaceVariant)
            } else {
                card.strokeColor = outlineColor
                card.strokeWidth = (1 * density).toInt()
                card.setCardBackgroundColor(surfaceColor)
            }
        }
    }

    private fun updateTransparencyUi(transparency: Int) {
        sliderTransparency.value = transparency.toFloat()
        tvTransparencyValue.text = getString(R.string.transparency_value_format, transparency)
    }

    private fun updateAngleUi(angle: Float) {
        sliderAngle.value = angle.coerceIn(-90f, 90f)
        ivAngleNeedleIcon.rotation = angle
        tvAngleDisplayValue.text = "${angle.toInt()}°"
        updateAngleChipsUi(angle)
    }

    private fun updateAngleChipsUi(currentAngle: Float) {
        val density = resources.displayMetrics.density
        val primaryColor = getThemeColor(androidx.appcompat.R.attr.colorPrimary, 0xFF006874.toInt())
        val surfaceVariant = getThemeColor(com.google.android.material.R.attr.colorSurfaceVariant, 0xFFDBE4E6.toInt())
        val surfaceColor = getThemeColor(com.google.android.material.R.attr.colorSurface, 0xFFF8FDFF.toInt())
        val onSurfaceVariant = getThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF3F484A.toInt())

        val chips = listOf(
            chipAngleNeg60 to -60f,
            chipAngleNeg45 to -45f,
            chipAngleNeg30 to -30f,
            chipAngle0 to 0f,
            chipAnglePos30 to 30f,
            chipAnglePos45 to 45f,
            chipAnglePos60 to 60f
        )

        var foundPreset = false
        val isCustomOpen = layoutAngleSliderContainer.visibility == View.VISIBLE

        if (!isCustomOpen) {
            for ((chip, angleVal) in chips) {
                val isSelected = abs(currentAngle - angleVal) < 0.5f
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

        val isCustomSelected = isCustomOpen || !foundPreset
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

    private fun updateContentUi(mode: WidgetContentMode) {
        val checkId = when (mode) {
            WidgetContentMode.WEATHER -> R.id.btn_content_weather
            WidgetContentMode.CLOCK -> R.id.btn_content_clock
            WidgetContentMode.COMBO -> R.id.btn_content_combo
        }
        if (toggleContent.checkedButtonId != checkId) {
            toggleContent.check(checkId)
        }
    }

    private fun updateToolAvailability(category: WidgetCategory, animate: Boolean) {
        val isDiagonal = (category == WidgetCategory.DIAGONAL)
        val targetAngleVis = if (isDiagonal) View.VISIBLE else View.GONE
        val targetContentVis = if (isDiagonal) View.VISIBLE else View.GONE
        val targetSecondaryVis = if (isDiagonal) View.VISIBLE else View.GONE

        val visibilityChanged = (btnToolAngle.visibility != targetAngleVis)

        if (currentActiveTool == ToolType.ANGLE && !isDiagonal) closeToolDetail()
        if (currentActiveTool == ToolType.CONTENT && !isDiagonal) closeToolDetail()

        if (animate && visibilityChanged) {
            val container = findViewById<LinearLayout>(R.id.edit_content_container)
            android.transition.TransitionManager.beginDelayedTransition(
                container,
                android.transition.TransitionSet().apply {
                    addTransition(android.transition.ChangeBounds())
                    addTransition(android.transition.Fade())
                    duration = 240
                    interpolator = DecelerateInterpolator(1.4f)
                }
            )
        }

        btnToolAngle.visibility = targetAngleVis
        btnToolContent.visibility = targetContentVis
        btnToolSize.visibility = View.GONE
        layoutSizeSecondary.visibility = targetSecondaryVis

        if (isDiagonal && animate && visibilityChanged) {
            listOf(btnToolAngle, btnToolContent).forEachIndexed { idx, tool ->
                tool.alpha = 0f
                tool.scaleX = 0.82f
                tool.scaleY = 0.82f
                tool.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(260)
                    .setStartDelay(50L * idx)
                    .setInterpolator(OvershootInterpolator(1.3f))
                    .start()
            }
        }

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

        setupVisualColorSwatches()
        animateToolsEntrance()
    }

    private fun animateToolsEntrance() {
        val tools = listOf(
            btnToolPalette, btnToolSize, btnToolAngle, btnToolContent, btnToolTransparency
        ).filter { it.visibility == View.VISIBLE }

        for ((index, tool) in tools.withIndex()) {
            tool.alpha = 0f
            tool.translationY = 28f
            tool.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(280)
                .setStartDelay(index * 35L)
                .setInterpolator(OvershootInterpolator(1.2f))
                .start()
        }
    }

    private fun setupVisualColorSwatches() {
        val density = resources.displayMetrics.density
        val insetPx = (8 * density).toInt()

        val palettes = listOf(
            discSwatchOlive to ColorPalette.OLIVE,
            discSwatchTeal to ColorPalette.TEAL,
            discSwatchSlate to ColorPalette.SLATE,
            discSwatchAmber to ColorPalette.AMBER,
            discSwatchCrimson to ColorPalette.CRIMSON
        )
        for ((imageView, pal) in palettes) {
            val bgOval = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(pal.bgColor)
            }
            val centerDot = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(pal.textColor)
            }
            val layerDrawable = LayerDrawable(arrayOf(bgOval, centerDot)).apply {
                setLayerInset(1, insetPx, insetPx, insetPx, insetPx)
            }
            imageView.setImageDrawable(layerDrawable)
        }

        val customPalette = WidgetPreferences.getCustomPalette(this)
        val customBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(customPalette.bgColor)
        }
        val customDot = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(customPalette.textColor)
        }
        discSwatchCustom.setImageDrawable(LayerDrawable(arrayOf(customBg, customDot)).apply {
            setLayerInset(1, insetPx, insetPx, insetPx, insetPx)
        })
    }

    private fun checkAndSavePreset() {
        val category = viewModel.category.value ?: WidgetCategory.DIAGONAL
        val size = viewModel.size.value ?: WidgetSize.SIZE_2X2
        val palette = viewModel.palette.value ?: ColorPalette.DYNAMIC
        val transparency = viewModel.transparency.value ?: 100
        val angle = viewModel.angle.value ?: -45f
        val contentMode = viewModel.contentMode.value ?: WidgetContentMode.WEATHER

        if (isEditMode) {
            val id = editingPresetId ?: return
            val name = editingPresetTitle ?: ""
            WidgetPreferences.updateQuickPreset(
                context = this,
                id = id,
                title = name,
                palette = palette,
                transparency = transparency,
                angle = angle,
                category = category,
                size = size,
                contentMode = contentMode
            )
            Toast.makeText(this, R.string.preset_updated_toast, Toast.LENGTH_SHORT).show()
            val resultIntent = Intent().apply {
                putExtra(EXTRA_PRESET_ID, id)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finishWithTransition()
            return
        }

        // Add Mode: prompt user with styled Material 3 preset dialog
        val suggestions = PresetSuggestionHelper.getPresetNameSuggestions(this, category, palette)
        PresetDialogHelper.showPresetNameDialog(
            context = this,
            iconRes = R.drawable.ic_bookmark_add,
            titleRes = R.string.dialog_add_preset_title,
            subtitleRes = R.string.dialog_add_preset_subtitle,
            initialName = null,
            suggestions = suggestions,
            palette = palette,
            confirmButtonTextRes = R.string.dialog_btn_add,
            validateDuplicate = { name -> WidgetPreferences.isPresetTitleTaken(this, name) },
            onConfirmed = { name ->
                val added = WidgetPreferences.addQuickPreset(
                    context = this,
                    title = name,
                    palette = palette,
                    transparency = transparency,
                    angle = angle,
                    category = category,
                    size = size,
                    contentMode = contentMode
                )
                Toast.makeText(this, R.string.preset_added_toast, Toast.LENGTH_SHORT).show()
                val resultIntent = Intent().apply {
                    putExtra(EXTRA_PRESET_ID, added.id)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finishWithTransition()
            }
        )
    }

    private fun showEditPresetNameDialog() {
        val category = viewModel.category.value ?: WidgetCategory.DIAGONAL
        val palette = viewModel.palette.value ?: ColorPalette.DYNAMIC
        val suggestions = PresetSuggestionHelper.getPresetNameSuggestions(this, category, palette)

        PresetDialogHelper.showPresetNameDialog(
            context = this,
            iconRes = R.drawable.ic_edit,
            titleRes = R.string.dialog_edit_preset_name_title,
            subtitleRes = R.string.dialog_edit_preset_subtitle,
            initialName = editingPresetTitle,
            suggestions = suggestions,
            palette = palette,
            confirmButtonTextRes = R.string.btn_done,
            validateDuplicate = { name ->
                WidgetPreferences.isPresetTitleTaken(this, name, excludeId = editingPresetId)
            },
            onConfirmed = { newName ->
                // User requirement: update title temporarily in memory without toast; only saved on header tick
                editingPresetTitle = newName
            }
        )
    }

    private fun animateSelectionPop(view: View) {
        PresetSuggestionHelper.animateSelectionPop(view)
    }

    private fun getThemeColor(attr: Int, defaultColor: Int = 0): Int {
        val tv = android.util.TypedValue()
        return if (theme.resolveAttribute(attr, tv, true)) {
            if (tv.resourceId != 0) ContextCompat.getColor(this, tv.resourceId) else tv.data
        } else defaultColor
    }
}
