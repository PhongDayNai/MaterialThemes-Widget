package com.iatb.materialthemes.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import com.iatb.materialthemes.WidgetCategory
import com.iatb.materialthemes.WidgetSize
import com.iatb.materialthemes.data.WidgetPreferences

object ActiveWidgetManager {

    data class ActiveWidgetInfo(
        val appWidgetId: Int,
        val category: WidgetCategory,
        val size: WidgetSize
    )

    private data class ProviderRegistration(
        val providerClass: Class<*>,
        val category: WidgetCategory,
        val defaultSize: WidgetSize?
    )

    private val REGISTERED_PROVIDERS = listOf(
        ProviderRegistration(DiagonalWidgetProvider::class.java, WidgetCategory.DIAGONAL, WidgetSize.SIZE_2X2),
        ProviderRegistration(DiagonalWideWidgetProvider::class.java, WidgetCategory.DIAGONAL, WidgetSize.SIZE_4X2),
        ProviderRegistration(Diagonal4x3WidgetProvider::class.java, WidgetCategory.DIAGONAL, WidgetSize.SIZE_4X3),
        ProviderRegistration(OrganicWidgetProvider::class.java, WidgetCategory.ORGANIC, WidgetSize.SIZE_2X2),
        ProviderRegistration(OrganicWideWidgetProvider::class.java, WidgetCategory.ORGANIC, WidgetSize.SIZE_4X2),
        ProviderRegistration(Organic4x3WidgetProvider::class.java, WidgetCategory.ORGANIC, WidgetSize.SIZE_4X3),
        ProviderRegistration(ScallopWidgetProvider::class.java, WidgetCategory.SCALLOP, WidgetSize.SIZE_2X2),
        ProviderRegistration(ScallopWideWidgetProvider::class.java, WidgetCategory.SCALLOP, WidgetSize.SIZE_4X2),
        ProviderRegistration(Scallop4x3WidgetProvider::class.java, WidgetCategory.SCALLOP, WidgetSize.SIZE_4X3)
    )

    /**
     * Inspects AppWidgetManager to find all widgets currently placed on the user's home screen.
     * Uses saved size for each widget if available, or dynamically resolves it.
     */
    fun getAllActiveWidgets(context: Context): List<ActiveWidgetInfo> {
        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return emptyList()
        val result = mutableListOf<ActiveWidgetInfo>()

        for (reg in REGISTERED_PROVIDERS) {
            val component = ComponentName(context, reg.providerClass)
            val ids = try {
                appWidgetManager.getAppWidgetIds(component)
            } catch (_: Exception) {
                IntArray(0)
            }

            for (id in ids) {
                val options = try {
                    appWidgetManager.getAppWidgetOptions(id)
                } catch (_: Exception) {
                    null
                }
                val hasDimensions = options != null && (
                    options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) > 0 ||
                    options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0) > 0 ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        !options.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES).isNullOrEmpty())
                )
                val resolvedSize = if (hasDimensions) {
                    val size = resolveWidgetSize(context, options, reg.defaultSize ?: WidgetSize.SIZE_2X2)
                    WidgetPreferences.setWidgetSavedSize(context, id, size)
                    size
                } else {
                    WidgetPreferences.getWidgetSavedSize(context, id)
                        ?: reg.defaultSize
                        ?: WidgetSize.SIZE_2X2
                }
                result.add(ActiveWidgetInfo(id, reg.category, resolvedSize))
            }
        }
        return result
    }

    /**
     * Syncs the active home screen widget's category and size into WidgetPreferences.
     * Reads saved size if present, avoiding erroneous overwrites.
     */
    fun syncActiveWidget(context: Context): ActiveWidgetInfo? {
        val allActive = getAllActiveWidgets(context)
        if (allActive.isEmpty()) {
            WidgetPreferences.setHasActiveHomeScreenWidget(context, false)
            return null
        }

        val lastActiveId = WidgetPreferences.getLastActiveWidgetId(context)
        val chosen = allActive.firstOrNull { it.appWidgetId == lastActiveId } ?: allActive.first()

        WidgetPreferences.setCategory(context, chosen.category)
        WidgetPreferences.setSize(context, chosen.size)
        WidgetPreferences.setLastActiveWidgetId(context, chosen.appWidgetId)
        WidgetPreferences.setHasActiveHomeScreenWidget(context, true)
        return chosen
    }

    /**
     * Directly records a widget as the currently active home screen widget.
     */
    fun recordActiveWidget(
        context: Context,
        appWidgetId: Int,
        category: WidgetCategory,
        size: WidgetSize
    ) {
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            WidgetPreferences.setLastActiveWidgetId(context, appWidgetId)
            WidgetPreferences.setWidgetSavedSize(context, appWidgetId, size)
        }
        WidgetPreferences.setCategory(context, category)
        WidgetPreferences.setSize(context, size)
        WidgetPreferences.setHasActiveHomeScreenWidget(context, true)
    }

    /**
     * Invoked by widget providers when onAppWidgetOptionsChanged fires (user resized widget on home screen).
     */
    fun recordOptionsChanged(
        context: Context,
        providerClass: Class<*>,
        appWidgetId: Int,
        options: Bundle?
    ) {
        val reg = REGISTERED_PROVIDERS.firstOrNull { it.providerClass == providerClass } ?: return
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val actualOptions = options ?: try {
            appWidgetManager?.getAppWidgetOptions(appWidgetId)
        } catch (_: Exception) {
            null
        }
        val size = resolveWidgetSize(context, actualOptions, reg.defaultSize ?: WidgetSize.SIZE_2X2)
        recordActiveWidget(context, appWidgetId, reg.category, size)
    }

    /**
     * Invoked by widget providers when onUpdate runs with active widget IDs.
     * Preserves existing saved size so periodic minute ticks never overwrite user-resized dimensions.
     */
    fun recordProviderUpdate(
        context: Context,
        providerClass: Class<*>,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return
        val reg = REGISTERED_PROVIDERS.firstOrNull { it.providerClass == providerClass } ?: return
        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
        val lastId = appWidgetIds.last()

        val options = try {
            appWidgetManager.getAppWidgetOptions(lastId)
        } catch (_: Exception) {
            null
        }
        val hasDimensions = options != null && (
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) > 0 ||
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0) > 0 ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !options.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES).isNullOrEmpty())
        )
        val size = if (hasDimensions) {
            val resolved = resolveWidgetSize(context, options, reg.defaultSize ?: WidgetSize.SIZE_2X2)
            WidgetPreferences.setWidgetSavedSize(context, lastId, resolved)
            resolved
        } else {
            WidgetPreferences.getWidgetSavedSize(context, lastId)
                ?: reg.defaultSize
                ?: WidgetSize.SIZE_2X2
        }
        recordActiveWidget(context, lastId, reg.category, size)
    }

    /**
     * Resolves the WidgetSize from an AppWidgetOptions Bundle.
     * Properly handles portrait and landscape orientations, Android 12+ OPTION_APPWIDGET_SIZES,
     * and maps dimensions to the appropriate grid cells.
     */
    fun resolveWidgetSize(
        context: Context,
        options: Bundle?,
        defaultSize: WidgetSize = WidgetSize.SIZE_2X2
    ): WidgetSize {
        if (options == null) return defaultSize

        val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        // Android 12+ (API 31+) provides OPTION_APPWIDGET_SIZES
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val sizes = options.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
            if (!sizes.isNullOrEmpty()) {
                val chosenSize = if (isLandscape) {
                    sizes.maxByOrNull { it.width } ?: sizes.first()
                } else {
                    sizes.minByOrNull { it.width } ?: sizes.first()
                }
                if (chosenSize.width > 0 && chosenSize.height > 0) {
                    return resolveWidgetSizeFromDimensions(chosenSize.width.toInt(), chosenSize.height.toInt())
                }
            }
        }

        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)

        if (minWidth <= 0 && maxWidth <= 0 && minHeight <= 0 && maxHeight <= 0) {
            return defaultSize
        }

        // On portrait: width is minWidth (portrait width), height is maxHeight (portrait height).
        // On landscape: width is maxWidth (landscape width), height is minHeight (landscape height).
        val widthDp = if (isLandscape) {
            if (maxWidth > 0) maxWidth else minWidth
        } else {
            if (minWidth > 0) minWidth else maxWidth
        }

        val heightDp = if (isLandscape) {
            if (minHeight > 0) minHeight else maxHeight
        } else {
            if (maxHeight > 0) maxHeight else minHeight
        }

        return resolveWidgetSizeFromDimensions(widthDp, heightDp)
    }

    /**
     * Maps width (dp) and height (dp) to the closest matching WidgetSize:
     * - 2 cols x 2 rows -> 2x2
     * - 3 cols x 2 rows -> 3x2
     * - 4 cols x 2 rows -> 4x2
     * - 2 cols x 3 rows -> 2x3
     * - 3 cols x 3 rows -> 3x3
     * - 4 cols x 3 rows -> 4x3
     * - 2 cols x 4 rows -> 2x4
     */
    fun resolveWidgetSizeFromDimensions(widthDp: Int, heightDp: Int): WidgetSize {
        val cols = when {
            widthDp >= 270 -> 4
            widthDp >= 205 -> 3
            else -> 2
        }
        val rows = when {
            heightDp >= 340 -> 4
            heightDp >= 250 -> 3
            else -> 2
        }

        return when {
            cols == 4 && rows >= 3 -> WidgetSize.SIZE_4X3
            cols == 4 -> WidgetSize.SIZE_4X2
            cols == 3 && rows >= 3 -> WidgetSize.SIZE_3X3
            cols == 3 -> WidgetSize.SIZE_3X2
            rows >= 4 -> WidgetSize.SIZE_2X4
            rows == 3 -> WidgetSize.SIZE_2X3
            else -> WidgetSize.SIZE_2X2
        }
    }
}
