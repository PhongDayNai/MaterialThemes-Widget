package com.iatb.materialthemes.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.iatb.materialthemes.R
import com.iatb.materialthemes.data.ColorPalette

object PresetDialogHelper {

    /**
     * Displays a modern Material 3 Bottom Sheet for creating or renaming a preset.
     * Ergonomic, slides smoothly from bottom, and stays above soft keyboard.
     */
    fun showPresetNameDialog(
        context: Context,
        @DrawableRes iconRes: Int = R.drawable.ic_bookmark_add,
        @StringRes titleRes: Int = R.string.dialog_add_preset_title,
        @StringRes subtitleRes: Int = R.string.dialog_add_preset_subtitle,
        initialName: String? = null,
        suggestions: List<String> = emptyList(),
        palette: ColorPalette = ColorPalette.DYNAMIC,
        @StringRes confirmButtonTextRes: Int = R.string.dialog_btn_add,
        validateDuplicate: (name: String) -> Boolean = { false },
        onConfirmed: (name: String) -> Unit
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.sheet_preset_name, null)
        val dialog = BottomSheetDialog(context)
        dialog.setContentView(view)

        // Keep fully expanded and resize gracefully with keyboard
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        val frameIcon: FrameLayout = view.findViewById(R.id.frame_dialog_icon)
        val ivIcon: ImageView = view.findViewById(R.id.iv_dialog_icon)
        val tvTitle: TextView = view.findViewById(R.id.tv_dialog_title)
        val tvSubtitle: TextView = view.findViewById(R.id.tv_dialog_subtitle)
        val btnClose: ImageView = view.findViewById(R.id.btn_dialog_close)
        val tilName: TextInputLayout = view.findViewById(R.id.til_preset_name)
        val etName: TextInputEditText = view.findViewById(R.id.et_preset_name)
        val layoutSuggestions: View = view.findViewById(R.id.layout_suggestions_section)
        val containerSuggestionsChips: FrameLayout = view.findViewById(R.id.container_suggestions_chips)
        val ivSparkle: ImageView = view.findViewById(R.id.iv_suggestions_sparkle)
        val btnCancel: MaterialButton = view.findViewById(R.id.btn_dialog_cancel)
        val btnSave: MaterialButton = view.findViewById(R.id.btn_dialog_save)

        // Palette harmonic theme
        val chipTheme = PresetSuggestionHelper.resolveChipColorTheme(context, palette)
        frameIcon.backgroundTintList = ColorStateList.valueOf(chipTheme.bgTint)
        ivIcon.setImageResource(iconRes)
        ivIcon.imageTintList = ColorStateList.valueOf(chipTheme.textColor)
        ivSparkle.imageTintList = ColorStateList.valueOf(chipTheme.iconTint)

        tvTitle.setText(titleRes)
        tvSubtitle.setText(subtitleRes)
        btnSave.setText(confirmButtonTextRes)
        btnSave.backgroundTintList = ColorStateList.valueOf(chipTheme.iconTint)

        initialName?.let {
            etName.setText(it)
            if (it.isNotEmpty()) {
                etName.setSelection(it.length.coerceAtMost(PresetSuggestionHelper.MAX_PRESET_TITLE_LENGTH))
            }
        }

        // Suggestions setup with palette harmonization
        if (suggestions.isNotEmpty()) {
            layoutSuggestions.visibility = View.VISIBLE
            val suggestionsView = PresetSuggestionHelper.buildSuggestionsLayout(context, suggestions, palette) { selectedName ->
                etName.setText(selectedName)
                etName.setSelection(selectedName.length)
                tilName.error = null
            }
            containerSuggestionsChips.removeAllViews()
            containerSuggestionsChips.addView(suggestionsView)
        } else {
            layoutSuggestions.visibility = View.GONE
        }

        // Clear error when user modifies text
        etName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (tilName.error != null) {
                    tilName.error = null
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val name = etName.text?.toString()?.trim().orEmpty()
            if (name.isEmpty()) {
                tilName.error = context.getString(R.string.preset_name_empty_warning)
                etName.requestFocus()
                return@setOnClickListener
            }
            if (name.length > PresetSuggestionHelper.MAX_PRESET_TITLE_LENGTH) {
                tilName.error = context.getString(R.string.preset_name_too_long_warning, PresetSuggestionHelper.MAX_PRESET_TITLE_LENGTH)
                etName.requestFocus()
                return@setOnClickListener
            }
            if (validateDuplicate(name)) {
                tilName.error = context.getString(R.string.preset_duplicate_name_warning, name)
                etName.requestFocus()
                return@setOnClickListener
            }

            dialog.dismiss()
            onConfirmed(name)
        }

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundColor(Color.TRANSPARENT)
            var parent = view.parent
            while (parent is ViewGroup) {
                parent.clipChildren = false
                parent.clipToPadding = false
                parent = parent.parent
            }
        }

        dialog.show()
    }

    /**
     * Displays a modern Material 3 Bottom Sheet for informational alerts (e.g. Preset already exists).
     */
    fun showAlertInfoDialog(
        context: Context,
        @DrawableRes iconRes: Int = R.drawable.ic_collections_bookmark,
        @StringRes titleRes: Int = R.string.preset_duplicate_title,
        message: String,
        @StringRes buttonTextRes: Int = R.string.btn_done,
        onDismiss: (() -> Unit)? = null
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.sheet_preset_confirm, null)
        val dialog = BottomSheetDialog(context)
        dialog.setContentView(view)

        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true

        val ivIcon: ImageView = view.findViewById(R.id.iv_confirm_icon)
        val tvTitle: TextView = view.findViewById(R.id.tv_confirm_title)
        val tvMessage: TextView = view.findViewById(R.id.tv_confirm_message)
        val btnClose: ImageView = view.findViewById(R.id.btn_confirm_close)
        val btnCancel: MaterialButton = view.findViewById(R.id.btn_confirm_cancel)
        val btnPositive: MaterialButton = view.findViewById(R.id.btn_confirm_positive)

        ivIcon.setImageResource(iconRes)
        tvTitle.setText(titleRes)
        tvMessage.text = message

        btnClose.setOnClickListener {
            dialog.dismiss()
            onDismiss?.invoke()
        }

        btnCancel.visibility = View.GONE
        btnPositive.setText(buttonTextRes)
        btnPositive.setOnClickListener {
            dialog.dismiss()
            onDismiss?.invoke()
        }

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundColor(Color.TRANSPARENT)
        }

        dialog.show()
    }

    /**
     * Displays a modern Material 3 Bottom Sheet for confirmation actions (e.g. Delete preset).
     */
    fun showConfirmDialog(
        context: Context,
        @DrawableRes iconRes: Int = R.drawable.ic_delete,
        @StringRes titleRes: Int = R.string.dialog_delete_preset_title,
        message: String,
        @StringRes positiveButtonTextRes: Int = R.string.action_delete,
        isDestructive: Boolean = false,
        onConfirmed: () -> Unit
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.sheet_preset_confirm, null)
        val dialog = BottomSheetDialog(context)
        dialog.setContentView(view)

        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true

        val frameIcon: FrameLayout = view.findViewById(R.id.frame_confirm_icon)
        val ivIcon: ImageView = view.findViewById(R.id.iv_confirm_icon)
        val tvTitle: TextView = view.findViewById(R.id.tv_confirm_title)
        val tvMessage: TextView = view.findViewById(R.id.tv_confirm_message)
        val btnClose: ImageView = view.findViewById(R.id.btn_confirm_close)
        val btnCancel: MaterialButton = view.findViewById(R.id.btn_confirm_cancel)
        val btnPositive: MaterialButton = view.findViewById(R.id.btn_confirm_positive)

        ivIcon.setImageResource(iconRes)
        tvTitle.setText(titleRes)
        tvMessage.text = message
        btnPositive.setText(positiveButtonTextRes)

        if (isDestructive) {
            frameIcon.backgroundTintList = ColorStateList.valueOf(0x26DC2626.toInt())
            ivIcon.imageTintList = ColorStateList.valueOf(0xFFDC2626.toInt())
            btnPositive.backgroundTintList = ColorStateList.valueOf(0xFFDC2626.toInt())
            btnPositive.setTextColor(Color.WHITE)
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnPositive.setOnClickListener {
            dialog.dismiss()
            onConfirmed()
        }

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundColor(Color.TRANSPARENT)
        }

        dialog.show()
    }
}
