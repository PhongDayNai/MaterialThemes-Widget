package com.iatb.materialthemes

import android.app.Application
import android.app.WallpaperManager
import android.content.ComponentCallbacks
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.iatb.materialthemes.data.DynamicThemeExtractor
import com.iatb.materialthemes.widget.DiagonalWidgetProvider
import com.iatb.materialthemes.widget.OrganicWidgetProvider
import com.iatb.materialthemes.widget.ScallopWidgetProvider

class MaterialThemesApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Listen for real-time wallpaper color changes (API 27+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                val wm = getSystemService(WallpaperManager::class.java)
                    ?: WallpaperManager.getInstance(this)
                wm?.addOnColorsChangedListener({ colors, _ ->
                    DynamicThemeExtractor.onWallpaperColorsChanged(colors)
                    DiagonalWidgetProvider.updateAllWidgets(this)
                    OrganicWidgetProvider.updateAllWidgets(this)
                    ScallopWidgetProvider.updateAllWidgets(this)
                }, Handler(Looper.getMainLooper()))
            } catch (_: Exception) {
            }
        }

        // 2. Listen for real-time system theme / configuration changes (Monet, Dark/Light mode)
        registerComponentCallbacks(object : ComponentCallbacks {
            override fun onConfigurationChanged(newConfig: Configuration) {
                DynamicThemeExtractor.invalidateCache()
                DiagonalWidgetProvider.updateAllWidgets(this@MaterialThemesApp)
                OrganicWidgetProvider.updateAllWidgets(this@MaterialThemesApp)
                ScallopWidgetProvider.updateAllWidgets(this@MaterialThemesApp)
            }

            override fun onLowMemory() {}
        })
    }
}
