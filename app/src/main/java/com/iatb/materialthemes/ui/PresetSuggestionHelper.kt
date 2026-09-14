package com.iatb.materialthemes.ui

import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.iatb.materialthemes.R
import com.iatb.materialthemes.WidgetCategory
import com.iatb.materialthemes.data.ColorPalette
import kotlin.math.abs

object PresetSuggestionHelper {

    const val MAX_PRESET_TITLE_LENGTH = 30

    fun getPresetNameSuggestions(context: Context, cat: WidgetCategory, pal: ColorPalette): List<String> {
        val catName = when (cat) {
            WidgetCategory.DIAGONAL -> context.getString(R.string.preset_short_cat_diagonal)
            WidgetCategory.ORGANIC -> context.getString(R.string.preset_short_cat_organic)
            WidgetCategory.SCALLOP -> context.getString(R.string.preset_short_cat_scallop)
        }
        val palName = when (pal) {
            ColorPalette.DYNAMIC -> context.getString(R.string.preset_short_pal_dynamic)
            ColorPalette.OLIVE -> context.getString(R.string.preset_short_pal_olive)
            ColorPalette.TEAL -> context.getString(R.string.preset_short_pal_teal)
            ColorPalette.SLATE -> context.getString(R.string.preset_short_pal_slate)
            ColorPalette.AMBER -> context.getString(R.string.preset_short_pal_amber)
            ColorPalette.CRIMSON -> context.getString(R.string.preset_short_pal_crimson)
            ColorPalette.CUSTOM -> context.getString(R.string.preset_short_pal_custom)
        }
        val pixelStr = context.getString(R.string.preset_expr_pixel)

        val expr1 = when (pal) {
            ColorPalette.DYNAMIC -> context.getString(R.string.preset_expr_dynamic_1)
            ColorPalette.OLIVE -> context.getString(R.string.preset_expr_olive_1)
            ColorPalette.TEAL -> context.getString(R.string.preset_expr_teal_1)
            ColorPalette.SLATE -> context.getString(R.string.preset_expr_slate_1)
            ColorPalette.AMBER -> context.getString(R.string.preset_expr_amber_1)
            ColorPalette.CRIMSON -> context.getString(R.string.preset_expr_crimson_1)
            ColorPalette.CUSTOM -> context.getString(R.string.preset_expr_custom_1)
        }
        val expr2 = when (pal) {
            ColorPalette.DYNAMIC -> context.getString(R.string.preset_expr_dynamic_2)
            ColorPalette.OLIVE -> context.getString(R.string.preset_expr_olive_2)
            ColorPalette.TEAL -> context.getString(R.string.preset_expr_teal_2)
            ColorPalette.SLATE -> context.getString(R.string.preset_expr_slate_2)
            ColorPalette.AMBER -> context.getString(R.string.preset_expr_amber_2)
            ColorPalette.CRIMSON -> context.getString(R.string.preset_expr_crimson_2)
            ColorPalette.CUSTOM -> context.getString(R.string.preset_expr_custom_2)
        }
        val expr3 = when (pal) {
            ColorPalette.DYNAMIC -> context.getString(R.string.preset_expr_dynamic_3)
            ColorPalette.OLIVE -> context.getString(R.string.preset_expr_olive_3)
            ColorPalette.TEAL -> context.getString(R.string.preset_expr_teal_3)
            ColorPalette.SLATE -> context.getString(R.string.preset_expr_slate_3)
            ColorPalette.AMBER -> context.getString(R.string.preset_expr_amber_3)
            ColorPalette.CRIMSON -> context.getString(R.string.preset_expr_crimson_3)
            ColorPalette.CUSTOM -> context.getString(R.string.preset_expr_custom_3)
        }
        val expr4 = when (pal) {
            ColorPalette.DYNAMIC -> context.getString(R.string.preset_expr_dynamic_4)
            ColorPalette.OLIVE -> context.getString(R.string.preset_expr_olive_4)
            ColorPalette.TEAL -> context.getString(R.string.preset_expr_teal_4)
            ColorPalette.SLATE -> context.getString(R.string.preset_expr_slate_4)
            ColorPalette.AMBER -> context.getString(R.string.preset_expr_amber_4)
            ColorPalette.CRIMSON -> context.getString(R.string.preset_expr_crimson_4)
            ColorPalette.CUSTOM -> context.getString(R.string.preset_expr_custom_4)
        }

        val rawList = listOf(
            "$catName $palName",
            expr1,
            expr2,
            expr3,
            expr4,
            "$catName $pixelStr",
            palName
        )

        val result = mutableListOf<String>()
        for (item in rawList) {
            val candidate = item.take(MAX_PRESET_TITLE_LENGTH).trim()
            if (candidate.isNotEmpty() && !result.contains(candidate)) {
                result.add(candidate)
            }
        }
        return result
    }

    data class ChipColorTheme(
        val bgTint: Int,
        val strokeColor: Int,
        val textColor: Int,
        val iconTint: Int
    )

    fun resolveChipColorTheme(context: Context, palette: ColorPalette): ChipColorTheme {
        return when (palette) {
            ColorPalette.OLIVE -> ChipColorTheme(
                bgTint = 0x22343B14.toInt(),
                strokeColor = 0x48343B14.toInt(),
                textColor = 0xFF282E0F.toInt(),
                iconTint = 0xFF343B14.toInt()
            )
            ColorPalette.TEAL -> ChipColorTheme(
                bgTint = 0x2213383B.toInt(),
                strokeColor = 0x4813383B.toInt(),
                textColor = 0xFF0E2A2C.toInt(),
                iconTint = 0xFF13383B.toInt()
            )
            ColorPalette.SLATE -> ChipColorTheme(
                bgTint = 0x201D2124.toInt(),
                strokeColor = 0x441D2124.toInt(),
                textColor = 0xFF16191B.toInt(),
                iconTint = 0xFF1D2124.toInt()
            )
            ColorPalette.AMBER -> ChipColorTheme(
                bgTint = 0x2AF59E0B.toInt(),
                strokeColor = 0x5DF59E0B.toInt(),
                textColor = 0xFF78350F.toInt(),
                iconTint = 0xFFD97706.toInt()
            )
            ColorPalette.CRIMSON -> ChipColorTheme(
                bgTint = 0x24E11D48.toInt(),
                strokeColor = 0x50E11D48.toInt(),
                textColor = 0xFF881337.toInt(),
                iconTint = 0xFFBE123C.toInt()
            )
            ColorPalette.DYNAMIC -> {
                val primary = getThemeColor(context, androidx.appcompat.R.attr.colorPrimary, 0xFF006874.toInt())
                val container = getThemeColor(context, com.google.android.material.R.attr.colorSecondaryContainer, 0xFFCDE7EC.toInt())
                ChipColorTheme(
                    bgTint = container,
                    strokeColor = (primary and 0x00FFFFFF) or 0x33000000,
                    textColor = primary,
                    iconTint = primary
                )
            }
            ColorPalette.CUSTOM -> {
                val customSeed = com.iatb.materialthemes.data.WidgetPreferences.getCustomColor(context)
                val primary = if (customSeed != 0) customSeed else getThemeColor(context, androidx.appcompat.R.attr.colorPrimary, 0xFF006874.toInt())
                ChipColorTheme(
                    bgTint = (primary and 0x00FFFFFF) or 0x24000000,
                    strokeColor = (primary and 0x00FFFFFF) or 0x4D000000,
                    textColor = primary,
                    iconTint = primary
                )
            }
        }
    }

    fun buildSuggestionsLayout(
        context: Context,
        suggestions: List<String>,
        palette: ColorPalette = ColorPalette.DYNAMIC,
        onSelected: (String) -> Unit
    ): View {
        val density = context.resources.displayMetrics.density
        val chipTheme = resolveChipColorTheme(context, palette)
        val scrollChips = HorizontalScrollView(context).apply {
            clipChildren = false
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_NEVER
            isHorizontalScrollBarEnabled = false
        }

        if (suggestions.size <= 2) {
            val singleRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                clipChildren = false
                clipToPadding = false
                val padY = (4 * density).toInt()
                setPadding(0, padY, (12 * density).toInt(), padY)
            }
            for (suggestedName in suggestions) {
                val chip = createSuggestionChip(context, suggestedName, chipTheme) {
                    onSelected(suggestedName)
                }
                val chipParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = (8 * density).toInt()
                }
                singleRow.addView(chip, chipParams)
            }
            scrollChips.addView(singleRow)
        } else {
            val verticalContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                clipChildren = false
                clipToPadding = false
                val padY = (4 * density).toInt()
                setPadding(0, padY, (12 * density).toInt(), padY)
            }
            val row1 = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                clipChildren = false
                clipToPadding = false
            }
            val row2 = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                clipChildren = false
                clipToPadding = false
            }

            val paint = Paint().apply {
                textSize = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP,
                    12.5f,
                    context.resources.displayMetrics
                )
            }
            val (itemsRow1, itemsRow2) = distributeSuggestionsIntoTwoRows(suggestions, density, paint)

            for (name in itemsRow1) {
                val chip = createSuggestionChip(context, name, chipTheme) {
                    onSelected(name)
                }
                val chipParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = (8 * density).toInt()
                }
                row1.addView(chip, chipParams)
            }

            for (name in itemsRow2) {
                val chip = createSuggestionChip(context, name, chipTheme) {
                    onSelected(name)
                }
                val chipParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = (8 * density).toInt()
                }
                row2.addView(chip, chipParams)
            }

            val row1Params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (6 * density).toInt()
            }
            verticalContainer.addView(row1, row1Params)
            verticalContainer.addView(row2)
            scrollChips.addView(verticalContainer)
        }

        return scrollChips
    }

    private fun distributeSuggestionsIntoTwoRows(
        suggestions: List<String>,
        density: Float,
        paint: Paint
    ): Pair<List<String>, List<String>> {
        val n = suggestions.size
        if (n <= 2) {
            return Pair(suggestions, emptyList())
        }

        val fixedWidthPx = 49f * density
        fun calcWidth(text: String): Float = fixedWidthPx + paint.measureText(text)

        val itemWidths = suggestions.map { calcWidth(it) }

        val countRow2 = n / 2
        val countRow1 = n - countRow2

        var bestRow2Indices: List<Int>? = null
        var bestScore = Float.MAX_VALUE

        fun findCombinations(start: Int, chosen: MutableList<Int>) {
            if (chosen.size == countRow2) {
                var w2 = 0f
                for (idx in chosen) {
                    w2 += itemWidths[idx]
                }
                var w1 = 0f
                for (i in 0 until n) {
                    if (!chosen.contains(i)) {
                        w1 += itemWidths[i]
                    }
                }

                val diff = w1 - w2
                val score = if (diff >= 0) diff else abs(diff) * 5f

                if (score < bestScore) {
                    bestScore = score
                    bestRow2Indices = chosen.toList()
                }
                return
            }

            for (i in start until n) {
                chosen.add(i)
                findCombinations(i + 1, chosen)
                chosen.removeAt(chosen.size - 1)
            }
        }

        findCombinations(0, mutableListOf())

        val row2Set = bestRow2Indices?.toSet() ?: emptySet()
        val rawRow1 = mutableListOf<String>()
        val rawRow2 = mutableListOf<String>()

        for (i in 0 until n) {
            if (row2Set.contains(i)) {
                rawRow2.add(suggestions[i])
            } else {
                rawRow1.add(suggestions[i])
            }
        }

        val tailItem: String? = if (countRow1 > countRow2) {
            val minItem = rawRow1.minByOrNull { calcWidth(it) }
            if (minItem != null) {
                rawRow1.remove(minItem)
                minItem
            } else null
        } else null

        val sorted1 = rawRow1.toMutableList()
        val sorted2 = rawRow2.toMutableList()

        val finalRow1 = mutableListOf<String>()
        val finalRow2 = mutableListOf<String>()
        var cumW1 = 0f
        var cumW2 = 0f

        while (sorted1.isNotEmpty() && sorted2.isNotEmpty()) {
            var bestI = 0
            var bestJ = 0
            var bestDiff = Float.MAX_VALUE

            for (i in sorted1.indices) {
                val w1 = calcWidth(sorted1[i])
                for (j in sorted2.indices) {
                    val w2 = calcWidth(sorted2[j])
                    val diff = abs((cumW1 + w1) - (cumW2 + w2))
                    if (diff < bestDiff) {
                        bestDiff = diff
                        bestI = i
                        bestJ = j
                    }
                }
            }

            val item1 = sorted1.removeAt(bestI)
            val item2 = sorted2.removeAt(bestJ)
            finalRow1.add(item1)
            finalRow2.add(item2)
            cumW1 += calcWidth(item1)
            cumW2 += calcWidth(item2)
        }

        finalRow1.addAll(sorted1)
        finalRow2.addAll(sorted2)

        if (tailItem != null) {
            finalRow1.add(tailItem)
        }

        return Pair(finalRow1, finalRow2)
    }

    private fun createSuggestionChip(
        context: Context,
        suggestedName: String,
        theme: ChipColorTheme,
        onSelected: () -> Unit
    ): View {
        val density = context.resources.displayMetrics.density
        val chip = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding((13 * density).toInt(), (8 * density).toInt(), (14 * density).toInt(), (8 * density).toInt())
            val bgPill = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 18 * density
                setColor(theme.bgTint)
                setStroke((1 * density).toInt(), theme.strokeColor)
            }
            background = bgPill
            isClickable = true
            isFocusable = true
        }

        val ivIcon = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams((15 * density).toInt(), (15 * density).toInt()).apply {
                marginEnd = (6 * density).toInt()
            }
            setImageResource(R.drawable.ic_auto_awesome)
            imageTintList = android.content.res.ColorStateList.valueOf(theme.iconTint)
        }

        val tvText = TextView(context).apply {
            text = suggestedName
            textSize = 13f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            setTextColor(theme.textColor)
        }

        chip.addView(ivIcon)
        chip.addView(tvText)

        chip.setOnClickListener {
            animateSelectionPop(chip)
            onSelected()
        }
        return chip
    }

    fun animateSelectionPop(view: View) {
        var parentView = view.parent
        while (parentView is ViewGroup) {
            parentView.clipChildren = false
            parentView.clipToPadding = false
            parentView = parentView.parent
        }

        view.animate().cancel()
        view.pivotX = view.width / 2f
        view.pivotY = view.height / 2f
        view.scaleX = 0.94f
        view.scaleY = 0.94f
        view.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(120)
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(90)
                    .start()
            }
            .start()
    }

    private fun getThemeColor(context: Context, attr: Int, defaultColor: Int = 0): Int {
        val tv = TypedValue()
        return if (context.theme.resolveAttribute(attr, tv, true)) {
            if (tv.resourceId != 0) ContextCompat.getColor(context, tv.resourceId) else tv.data
        } else defaultColor
    }
}
