package com.iatb.materialthemes.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.iatb.materialthemes.R
import com.iatb.materialthemes.data.DynamicThemeExtractor
import com.iatb.materialthemes.data.WidgetPreferences

object ColorWheelPickerDialog {

    fun show(
        context: Context,
        initialColor: Int = WidgetPreferences.getCustomColor(context),
        onColorSelected: (Int) -> Unit
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_color_picker, null)
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
            .setView(view)
            .create()

        val colorWheel: ColorWheelView = view.findViewById(R.id.color_wheel_view)
        val ivSeed: ImageView = view.findViewById(R.id.iv_seed_color)
        val chipTone1: TextView = view.findViewById(R.id.chip_tone_primary)
        val chipTone2: TextView = view.findViewById(R.id.chip_tone_secondary)
        val chipTone3: TextView = view.findViewById(R.id.chip_tone_text)
        val btnCancel: MaterialButton = view.findViewById(R.id.btn_picker_cancel)
        val btnSelect: MaterialButton = view.findViewById(R.id.btn_picker_select)

        var chosenColor = initialColor
        val density = context.resources.displayMetrics.density

        fun updateHarmonyPreviews(color: Int) {
            val tones = DynamicThemeExtractor.createHarmoniousTonesFromColor(color)

            val seedBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
            }
            ivSeed.setImageDrawable(seedBg)

            fun makePill(bgColor: Int): GradientDrawable {
                return GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 18f * density
                    setColor(bgColor)
                }
            }

            chipTone1.background = makePill(tones.bgColor)
            chipTone1.setTextColor(tones.textColor)

            chipTone2.background = makePill(tones.secondaryBgColor)
            chipTone2.setTextColor(tones.textColor)

            chipTone3.background = makePill(tones.tertiaryBgColor)
            chipTone3.setTextColor(tones.textColor)
        }

        colorWheel.setColor(initialColor)
        updateHarmonyPreviews(initialColor)

        colorWheel.onColorChanged = { color ->
            chosenColor = color
            updateHarmonyPreviews(color)
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSelect.setOnClickListener {
            WidgetPreferences.setCustomColor(context, chosenColor)
            onColorSelected(chosenColor)
            dialog.dismiss()
        }

        dialog.show()
    }
}
