package com.iatb.materialthemes.ui

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.app.Activity
import android.content.Intent
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.CornerFamily
import com.iatb.materialthemes.MainViewModel
import com.iatb.materialthemes.R
import com.iatb.materialthemes.WidgetCategory
import com.iatb.materialthemes.WidgetSize
import com.iatb.materialthemes.data.ColorPalette
import com.iatb.materialthemes.data.DynamicThemeExtractor
import com.iatb.materialthemes.data.QuickPreset
import com.iatb.materialthemes.data.WidgetContentMode
import com.iatb.materialthemes.data.WidgetPreferences
import kotlin.math.abs

class WidgetPresetsActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var previewContainer: FrameLayout
    private var previewAnimator: ValueAnimator? = null

    private lateinit var rvPresets: RecyclerView
    private var presetsAdapter: PresetsAdapter? = null

    private lateinit var btnApplyHeader: ImageButton
    private lateinit var containerFloatingTab: LinearLayout
    private lateinit var badgeStickyTab: MaterialCardView
    private lateinit var btnBadgeAdd: ImageButton
    private lateinit var containerBadgeEdit: FrameLayout
    private lateinit var btnBadgeEdit: ImageButton
    private lateinit var containerBadgeDelete: FrameLayout
    private lateinit var btnBadgeDelete: ImageButton
    private lateinit var btnBadgeDrag: View
    private var editButtonAnimator: ValueAnimator? = null
    private var deleteButtonAnimator: ValueAnimator? = null

    // Reference to the actual configuration applied to home screen widgets
    private var appliedCategory: WidgetCategory = WidgetCategory.DIAGONAL
    private var appliedPalette: ColorPalette = ColorPalette.DYNAMIC
    private var appliedTransparency: Int = 100
    private var appliedAngle: Float = -45f
    private var appliedPresetId: String? = null

    // Currently selected preset in this screen
    private var selectedPreset: QuickPreset? = null

    private val presetEditLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val returnedId = result.data?.getStringExtra(WidgetPresetEditActivity.EXTRA_PRESET_ID)
            val prevSelectedId = selectedPreset?.id
            refreshPresetsList()
            val updated = WidgetPreferences.getQuickPresets(this).filter { it.isEnabled }
            val target = (if (returnedId != null) updated.firstOrNull { it.id == returnedId } else null)
                ?: (if (prevSelectedId != null) updated.firstOrNull { it.id == prevSelectedId } else null)
                ?: updated.lastOrNull()
            if (target != null) {
                selectPreset(target, animate = true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_widget_presets)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        viewModel.initFromPreferences(this)

        // Capture actual applied configuration from Preferences
        appliedCategory = WidgetPreferences.getCategory(this)
        appliedPalette = WidgetPreferences.getColorPalette(this)
        appliedTransparency = WidgetPreferences.getTransparency(this)
        appliedAngle = WidgetPreferences.getRotationAngle(this)
        appliedPresetId = WidgetPreferences.getAppliedPresetId(this)

        initViews()
        setupStickyBadgeDrag()
        setupPresetsRecyclerView()
        observeViewModel()

        val cur = selectedPreset
        if (cur != null) {
            showEditButton(animate = false)
            val shouldDelete = !isPresetApplied(cur)
            if (shouldDelete) {
                showDeleteButton(animate = false)
            }
        }
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

        btnApplyHeader = findViewById(R.id.btn_apply_preset_header)
        containerFloatingTab = findViewById(R.id.container_floating_tab)
        badgeStickyTab = findViewById(R.id.badge_sticky_tab)
        btnBadgeAdd = findViewById(R.id.btn_badge_add)
        containerBadgeEdit = findViewById(R.id.container_badge_edit)
        btnBadgeEdit = findViewById(R.id.btn_badge_edit)
        containerBadgeDelete = findViewById(R.id.container_badge_delete)
        btnBadgeDelete = findViewById(R.id.btn_badge_delete)
        btnBadgeDrag = findViewById(R.id.btn_badge_drag)

        // Case 1: Add Mode (Opens fresh new canvas, independent of selected preset)
        btnBadgeAdd.setOnClickListener {
            animateSelectionPop(btnBadgeAdd)
            val intent = Intent(this, WidgetPresetEditActivity::class.java).apply {
                putExtra(WidgetPresetEditActivity.EXTRA_IS_EDIT, false)
            }
            presetEditLauncher.launch(intent)
        }

        btnBadgeEdit.setOnClickListener {
            animateSelectionPop(btnBadgeEdit)
            val preset = selectedPreset ?: return@setOnClickListener
            val intent = Intent(this, WidgetPresetEditActivity::class.java).apply {
                putExtra(WidgetPresetEditActivity.EXTRA_IS_EDIT, true)
                putExtra(WidgetPresetEditActivity.EXTRA_PRESET_ID, preset.id)
                putExtra(WidgetPresetEditActivity.EXTRA_PRESET_TITLE, preset.getLocalizedTitle(this@WidgetPresetsActivity))
                putExtra(WidgetPresetEditActivity.EXTRA_CATEGORY, preset.category.name)
                putExtra(WidgetPresetEditActivity.EXTRA_PALETTE, preset.palette.name)
                putExtra(WidgetPresetEditActivity.EXTRA_TRANSPARENCY, preset.transparency)
                putExtra(WidgetPresetEditActivity.EXTRA_ANGLE, preset.rotationAngle)
                putExtra(WidgetPresetEditActivity.EXTRA_SIZE, preset.size.name)
                putExtra(WidgetPresetEditActivity.EXTRA_CONTENT_MODE, preset.contentMode.name)
            }
            presetEditLauncher.launch(intent)
        }

        btnBadgeDelete.setOnClickListener {
            animateSelectionPop(btnBadgeDelete)
            confirmDeleteCurrentSelectedPreset()
        }

        // Header button: explicitly apply selected style to active home screen widget
        btnApplyHeader.setOnClickListener {
            animateSelectionPop(btnApplyHeader)
            val preset = selectedPreset ?: return@setOnClickListener

            viewModel.setColorPalette(preset.palette)
            viewModel.setTransparency(preset.transparency)
            viewModel.setCategory(preset.category)
            viewModel.setSize(preset.size)
            viewModel.setContentMode(preset.contentMode)
            if (preset.category == WidgetCategory.DIAGONAL) {
                viewModel.setRotationAngle(preset.rotationAngle)
            }
            viewModel.saveAndApply(this)

            appliedCategory = preset.category
            appliedPalette = preset.palette
            appliedTransparency = preset.transparency
            appliedAngle = preset.rotationAngle
            appliedPresetId = preset.id
            WidgetPreferences.setAppliedPresetId(this, appliedPresetId)

            Toast.makeText(this, R.string.quick_config_saved, Toast.LENGTH_SHORT).show()
            btnApplyHeader.visibility = View.GONE
            hideDeleteButton(animate = true)
            bounceStickyBadge()
            presetsAdapter?.notifyDataSetChanged()
        }
    }

    private fun bounceStickyBadge() {
        badgeStickyTab.animate().cancel()
        badgeStickyTab.scaleX = 0.93f
        badgeStickyTab.scaleY = 0.93f
        badgeStickyTab.animate()
            .scaleX(1.06f)
            .scaleY(1.06f)
            .setDuration(160)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                badgeStickyTab.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(240)
                    .setInterpolator(OvershootInterpolator(2.4f))
                    .start()
            }
            .start()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupStickyBadgeDrag() {
        val density = resources.displayMetrics.density

        // Round only the LEFT side of the badge, keep right side flat
        badgeStickyTab.shapeAppearanceModel = badgeStickyTab.shapeAppearanceModel.toBuilder()
            .setTopLeftCorner(CornerFamily.ROUNDED, 24 * density)
            .setBottomLeftCorner(CornerFamily.ROUNDED, 24 * density)
            .setTopRightCorner(CornerFamily.ROUNDED, 0f)
            .setBottomRightCorner(CornerFamily.ROUNDED, 0f)
            .build()

        val fingerprintView = btnBadgeDrag.findViewById<AnimatedFingerprintView>(R.id.fingerprint_view)

        var downRawY = 0f
        var initialTranslationY = 0f
        var isDragging = false
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop

        // Drag is handled exclusively via the circular fingerprint button outside the badge
        btnBadgeDrag.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawY = event.rawY
                    initialTranslationY = containerFloatingTab.translationY
                    isDragging = false
                    fingerprintView?.expand()
                    // Tactile physical feedback: scale down entire floating unit smoothly while dragging
                    containerFloatingTab.animate().scaleX(0.90f).scaleY(0.90f).setDuration(120).start()
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = event.rawY - downRawY
                    if (!isDragging && abs(deltaY) > touchSlop) {
                        isDragging = true
                    }
                    if (isDragging) {
                        val parentView = containerFloatingTab.parent as View
                        val topMargin = (containerFloatingTab.layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin ?: 0
                        val minY = -topMargin.toFloat() + 56 * density
                        val maxY = (parentView.height - containerFloatingTab.height - topMargin - 40 * density).coerceAtLeast(minY)
                        val targetY = (initialTranslationY + deltaY).coerceIn(minY, maxY)
                        containerFloatingTab.translationY = targetY
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    fingerprintView?.collapse()
                    // Restore size with smooth spring/bounce
                    containerFloatingTab.animate().scaleX(1.0f).scaleY(1.0f).setDuration(220)
                        .setInterpolator(OvershootInterpolator(1.4f)).start()
                    true
                }
                else -> false
            }
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

        // Default selection matching currently applied settings
        if (selectedPreset == null) {
            val matching = if (appliedPresetId != null) {
                list.firstOrNull { it.id == appliedPresetId }
            } else {
                list.firstOrNull { isPresetMatchingApplied(it) }
            } ?: list.firstOrNull()
            if (matching != null) {
                if (appliedPresetId == null && isPresetMatchingApplied(matching)) {
                    appliedPresetId = matching.id
                    WidgetPreferences.setAppliedPresetId(this, matching.id)
                }
                selectPreset(matching, animate = false)
            }
        }
    }

    private fun isPresetApplied(preset: QuickPreset): Boolean {
        if (appliedPresetId != null) {
            return preset.id == appliedPresetId
        }
        return isPresetMatchingApplied(preset)
    }

    private fun isPresetMatchingApplied(preset: QuickPreset): Boolean {
        val catMatch = preset.category == appliedCategory
        val palMatch = preset.palette == appliedPalette
        val transMatch = preset.transparency == appliedTransparency
        val angleMatch = if (appliedCategory == WidgetCategory.DIAGONAL) {
            abs(preset.rotationAngle - appliedAngle) < 0.5f
        } else {
            true
        }
        return catMatch && palMatch && transMatch && angleMatch
    }

    private fun selectPreset(preset: QuickPreset, animate: Boolean) {
        selectedPreset = preset
        viewModel.setColorPalette(preset.palette)
        viewModel.setTransparency(preset.transparency)
        viewModel.setCategory(preset.category)
        viewModel.setSize(preset.size)
        viewModel.setContentMode(preset.contentMode)
        if (preset.category == WidgetCategory.DIAGONAL) {
            viewModel.setRotationAngle(preset.rotationAngle)
        }
        presetsAdapter?.notifyDataSetChanged()
        updatePreview(animate = animate)

        // Show edit button whenever a preset from the list is selected
        showEditButton(animate = animate)

        // If preset is already applied, hide tick icon on header and delete button
        val isApplied = isPresetApplied(preset)
        if (isApplied) {
            btnApplyHeader.visibility = View.GONE
            hideDeleteButton(animate = animate)
        } else {
            btnApplyHeader.visibility = View.VISIBLE
            showDeleteButton(animate = animate)
        }

        if (animate) {
            bounceStickyBadge()
        }
    }

    private fun showEditButton(animate: Boolean) {
        val density = resources.displayMetrics.density
        val targetHeightPx = (52 * density).toInt()

        if (containerBadgeEdit.visibility == View.VISIBLE && containerBadgeEdit.layoutParams.height >= targetHeightPx) {
            return
        }
        editButtonAnimator?.cancel()

        if (!animate) {
            containerBadgeEdit.visibility = View.VISIBLE
            containerBadgeEdit.layoutParams.height = targetHeightPx
            containerBadgeEdit.requestLayout()
            btnBadgeEdit.alpha = 1f
            btnBadgeEdit.scaleX = 1f
            btnBadgeEdit.scaleY = 1f
            btnBadgeEdit.translationY = 0f
            return
        }

        containerBadgeEdit.visibility = View.VISIBLE
        val currentHeight = containerBadgeEdit.layoutParams.height.coerceAtLeast(0)
        val startFraction = if (targetHeightPx > 0) currentHeight.toFloat() / targetHeightPx else 0f

        // Tactile overshoot bounce on the badge card
        badgeStickyTab.animate().cancel()
        badgeStickyTab.scaleX = 0.94f
        badgeStickyTab.scaleY = 0.94f
        badgeStickyTab.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(160)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                badgeStickyTab.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(220)
                    .setInterpolator(OvershootInterpolator(2.4f))
                    .start()
            }
            .start()

        editButtonAnimator = ValueAnimator.ofFloat(startFraction, 1f).apply {
            duration = 400L
            interpolator = OvershootInterpolator(1.8f)
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                containerBadgeEdit.layoutParams.height = (targetHeightPx * fraction).toInt().coerceAtLeast(0)
                containerBadgeEdit.requestLayout()

                btnBadgeEdit.alpha = fraction.coerceIn(0f, 1f)
                btnBadgeEdit.scaleX = (0.3f + 0.7f * fraction).coerceAtLeast(0f)
                btnBadgeEdit.scaleY = (0.3f + 0.7f * fraction).coerceAtLeast(0f)
                btnBadgeEdit.translationY = (1f - fraction) * (-16f * density)
            }
            start()
        }
    }

    private fun hideEditButton(animate: Boolean) {
        if (containerBadgeEdit.visibility == View.GONE || containerBadgeEdit.layoutParams.height == 0) {
            return
        }
        editButtonAnimator?.cancel()
        val density = resources.displayMetrics.density
        val targetHeightPx = (52 * density).toInt()

        if (!animate) {
            containerBadgeEdit.layoutParams.height = 0
            containerBadgeEdit.visibility = View.GONE
            btnBadgeEdit.alpha = 0f
            return
        }

        val currentHeight = containerBadgeEdit.layoutParams.height
        val startFraction = if (targetHeightPx > 0) currentHeight.toFloat() / targetHeightPx else 1f

        editButtonAnimator = ValueAnimator.ofFloat(startFraction, 0f).apply {
            duration = 320L
            interpolator = DecelerateInterpolator(1.5f)
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                containerBadgeEdit.layoutParams.height = (targetHeightPx * fraction).toInt().coerceAtLeast(0)
                containerBadgeEdit.requestLayout()

                btnBadgeEdit.alpha = fraction.coerceIn(0f, 1f)
                btnBadgeEdit.scaleX = (0.3f + 0.7f * fraction).coerceAtLeast(0f)
                btnBadgeEdit.scaleY = (0.3f + 0.7f * fraction).coerceAtLeast(0f)
                btnBadgeEdit.translationY = (1f - fraction) * (-16f * density)
            }
            doOnEnd {
                if (containerBadgeEdit.layoutParams.height == 0) {
                    containerBadgeEdit.visibility = View.GONE
                }
            }
            start()
        }
    }

    private fun showDeleteButton(animate: Boolean) {
        val density = resources.displayMetrics.density
        val targetHeightPx = (52 * density).toInt()

        if (containerBadgeDelete.visibility == View.VISIBLE && containerBadgeDelete.layoutParams.height >= targetHeightPx) {
            return
        }
        deleteButtonAnimator?.cancel()

        if (!animate) {
            containerBadgeDelete.visibility = View.VISIBLE
            containerBadgeDelete.layoutParams.height = targetHeightPx
            containerBadgeDelete.requestLayout()
            btnBadgeDelete.alpha = 1f
            btnBadgeDelete.scaleX = 1f
            btnBadgeDelete.scaleY = 1f
            btnBadgeDelete.translationY = 0f
            return
        }

        containerBadgeDelete.visibility = View.VISIBLE
        val currentHeight = containerBadgeDelete.layoutParams.height.coerceAtLeast(0)
        val startFraction = if (targetHeightPx > 0) currentHeight.toFloat() / targetHeightPx else 0f

        // Tactile overshoot bounce on the badge card
        badgeStickyTab.animate().cancel()
        badgeStickyTab.scaleX = 0.94f
        badgeStickyTab.scaleY = 0.94f
        badgeStickyTab.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(160)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                badgeStickyTab.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(220)
                    .setInterpolator(OvershootInterpolator(2.4f))
                    .start()
            }
            .start()

        deleteButtonAnimator = ValueAnimator.ofFloat(startFraction, 1f).apply {
            duration = 400L
            interpolator = OvershootInterpolator(1.8f)
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                containerBadgeDelete.layoutParams.height = (targetHeightPx * fraction).toInt().coerceAtLeast(0)
                containerBadgeDelete.requestLayout()

                btnBadgeDelete.alpha = fraction.coerceIn(0f, 1f)
                btnBadgeDelete.scaleX = (0.3f + 0.7f * fraction).coerceAtLeast(0f)
                btnBadgeDelete.scaleY = (0.3f + 0.7f * fraction).coerceAtLeast(0f)
                btnBadgeDelete.translationY = (1f - fraction) * (-16f * density)
            }
            start()
        }
    }

    private fun hideDeleteButton(animate: Boolean) {
        if (containerBadgeDelete.visibility == View.GONE || containerBadgeDelete.layoutParams.height == 0) {
            return
        }
        deleteButtonAnimator?.cancel()
        val density = resources.displayMetrics.density
        val targetHeightPx = (52 * density).toInt()

        if (!animate) {
            containerBadgeDelete.layoutParams.height = 0
            containerBadgeDelete.visibility = View.GONE
            btnBadgeDelete.alpha = 0f
            return
        }

        val currentHeight = containerBadgeDelete.layoutParams.height
        val startFraction = if (targetHeightPx > 0) currentHeight.toFloat() / targetHeightPx else 1f

        deleteButtonAnimator = ValueAnimator.ofFloat(startFraction, 0f).apply {
            duration = 320L
            interpolator = DecelerateInterpolator(1.5f)
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                containerBadgeDelete.layoutParams.height = (targetHeightPx * fraction).toInt()
                containerBadgeDelete.requestLayout()

                btnBadgeDelete.alpha = fraction
                btnBadgeDelete.scaleX = 0.4f + 0.6f * fraction
                btnBadgeDelete.scaleY = 0.4f + 0.6f * fraction
                btnBadgeDelete.translationY = (1f - fraction) * (-16f * density)
            }
            doOnEnd {
                if (containerBadgeDelete.layoutParams.height == 0) {
                    containerBadgeDelete.visibility = View.GONE
                }
            }
            start()
        }
    }

    private fun confirmDeleteCurrentSelectedPreset() {
        val preset = selectedPreset ?: return
        val presetTitle = preset.getLocalizedTitle(this)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_delete_preset_title)
            .setMessage(getString(R.string.dialog_delete_preset_message, presetTitle))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                WidgetPreferences.deleteQuickPreset(this, preset.id)
                Toast.makeText(this, R.string.preset_deleted_toast, Toast.LENGTH_SHORT).show()
                refreshPresetsList()

                val remaining = WidgetPreferences.getQuickPresets(this).filter { it.isEnabled }
                val newSelection = if (appliedPresetId != null) {
                    remaining.firstOrNull { it.id == appliedPresetId }
                } else {
                    remaining.firstOrNull { isPresetMatchingApplied(it) }
                } ?: remaining.firstOrNull()
                if (newSelection != null) {
                    selectPreset(newSelection, animate = true)
                } else {
                    selectedPreset = null
                    hideEditButton(animate = true)
                    hideDeleteButton(animate = true)
                }
            }
            .setNegativeButton(R.string.dialog_btn_cancel, null)
            .show()
    }

    private fun observeViewModel() {
        viewModel.category.observe(this) { updatePreview(animate = true) }
        viewModel.size.observe(this) { updatePreview(animate = true) }
        viewModel.palette.observe(this) { updatePreview(animate = true) }
        viewModel.transparency.observe(this) { updatePreview(animate = false) }
        viewModel.angle.observe(this) { updatePreview(animate = true) }
        viewModel.contentMode.observe(this) { updatePreview(animate = true) }
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

    private fun animateSelectionPop(view: View) {
        view.animate().cancel()
        view.scaleX = 0.88f
        view.scaleY = 0.88f
        view.animate()
            .scaleX(1.14f)
            .scaleY(1.14f)
            .setDuration(130)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(200)
                    .setInterpolator(OvershootInterpolator(2.4f))
                    .start()
            }
            .start()
    }

    private inner class PresetsAdapter(
        private var items: List<QuickPreset>
    ) : RecyclerView.Adapter<PresetsAdapter.PresetViewHolder>() {

        private var dynamicColorCached: Pair<Int, Int>? = null

        fun updateItems(newItems: List<QuickPreset>) {
            items = newItems
            dynamicColorCached = null
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

            holder.tvName.text = preset.getLocalizedTitle(this@WidgetPresetsActivity)
            val paletteName = getString(preset.palette.labelResId)
            val opacityText = getString(R.string.transparency_value_format, preset.transparency)
            holder.tvSubtitle.text = "$paletteName • $opacityText"

            // Dual-tone swatch preview
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

            // Selection state based on active selectedPreset
            val isSelected = selectedPreset?.id == preset.id ||
                    (selectedPreset == null && isPresetApplied(preset))

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

            // Tapping a preset updates preview and selection WITHOUT applying immediately
            holder.card.setOnClickListener {
                animateSelectionPop(holder.card)
                selectPreset(preset, animate = true)
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
