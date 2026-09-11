package com.iatb.materialthemes.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.iatb.materialthemes.R
import com.iatb.materialthemes.data.WidgetPreferences
import com.iatb.materialthemes.render.ShapeWidgetCanvasRenderer
import com.iatb.materialthemes.widget.WidgetAnimationManager
import com.iatb.materialthemes.widget.WidgetUpdateScheduler

class SettingsActivity : AppCompatActivity() {

    private lateinit var ambientBgView: AmbientMeshBackgroundView
    private lateinit var tvCurrentLanguage: TextView
    private lateinit var tvCurrentTempUnit: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        initViews()
        setupAmbientBackground()
        setupListeners()
        updateSettingsValues()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishWithTransition()
            }
        })
    }

    private fun initViews() {
        ambientBgView = findViewById(R.id.ambient_bg_view)
        tvCurrentLanguage = findViewById(R.id.tv_current_language)
        tvCurrentTempUnit = findViewById(R.id.tv_current_temp_unit)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings_content_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val density = resources.displayMetrics.density
            val hPad = (22 * density).toInt()
            v.setPadding(
                hPad + systemBars.left,
                systemBars.top + (14 * density).toInt(),
                hPad + systemBars.right,
                systemBars.bottom + (48 * density).toInt()
            )
            insets
        }

        // Apply fluid touch animations to action cards
        val btnBack = findViewById<View>(R.id.btn_back_settings)
        val itemLang = findViewById<View>(R.id.item_setting_language)
        val itemTemp = findViewById<View>(R.id.item_setting_temp_unit)
        val itemPrivacy = findViewById<View>(R.id.item_setting_privacy)
        val itemShare = findViewById<View>(R.id.item_setting_share)

        WidgetPreviewHelper.applyPressScaleEffect(btnBack)
        WidgetPreviewHelper.applyPressScaleEffect(itemLang)
        WidgetPreviewHelper.applyPressScaleEffect(itemTemp)
        WidgetPreviewHelper.applyPressScaleEffect(itemPrivacy)
        WidgetPreviewHelper.applyPressScaleEffect(itemShare)
    }

    private fun setupAmbientBackground() {
        val snapshot = SharedAmbientBackgroundHolder.getSnapshot()
        if (snapshot != null) {
            ambientBgView.setOrbs(snapshot)
        }
        ambientBgView.post {
            ambientBgView.transitionToNewConstellation(durationMs = 950L) {
                SharedAmbientBackgroundHolder.saveSnapshot(ambientBgView.getOrbsSnapshot())
            }
        }
    }

    private fun setupListeners() {
        findViewById<View>(R.id.btn_back_settings).setOnClickListener {
            finishWithTransition()
        }

        findViewById<View>(R.id.item_setting_language).setOnClickListener {
            showLanguageDialog()
        }

        findViewById<View>(R.id.item_setting_temp_unit).setOnClickListener {
            showTemperatureUnitDialog()
        }

        findViewById<View>(R.id.item_setting_privacy).setOnClickListener {
            Toast.makeText(this, getString(R.string.toast_coming_soon), Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.item_setting_share).setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getString(R.string.settings_share_text))
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.settings_share_title)))
        }
    }

    private fun updateSettingsValues() {
        // 1. Language label
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val currentLang = if (!appLocales.isEmpty) {
            appLocales[0]?.language ?: ""
        } else {
            resources.configuration.locales[0].language
        }
        tvCurrentLanguage.text = if (currentLang.startsWith("vi")) {
            getString(R.string.settings_language_vi)
        } else {
            getString(R.string.settings_language_en)
        }

        // 2. Temperature Unit label
        val unit = WidgetPreferences.getTemperatureUnit(this)
        tvCurrentTempUnit.text = if (unit == "F") {
            getString(R.string.settings_temp_fahrenheit)
        } else {
            getString(R.string.settings_temp_celsius)
        }

        // 3. Version label
        try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            val versionStr = "${pInfo.versionName} (${getString(R.string.app_name)})"
            findViewById<TextView>(R.id.item_setting_version)?.findViewById<TextView>(R.id.tv_app_subtitle)?.text = versionStr
        } catch (_: Exception) {
        }
    }

    private fun showLanguageDialog() {
        val languages = arrayOf(
            getString(R.string.settings_language_en),
            getString(R.string.settings_language_vi)
        )

        val appLocales = AppCompatDelegate.getApplicationLocales()
        val currentLang = if (!appLocales.isEmpty) {
            appLocales[0]?.language ?: ""
        } else {
            resources.configuration.locales[0].language
        }
        val selectedIndex = if (currentLang.startsWith("vi")) 1 else 0

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.settings_language_dialog_title))
            .setSingleChoiceItems(languages, selectedIndex) { dialog, which ->
                dialog.dismiss()
                val targetTag = if (which == 1) "vi" else "en"
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(targetTag))
                ShapeWidgetCanvasRenderer.invalidateCache()
                val updateIntent = Intent(WidgetUpdateScheduler.ACTION_WIDGET_TICK).apply {
                    setPackage(packageName)
                }
                sendBroadcast(updateIntent)
                WidgetAnimationManager.updateAllWidgetsWithProgress(this, 1.0f)
                updateSettingsValues()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showTemperatureUnitDialog() {
        val units = arrayOf(
            getString(R.string.settings_temp_celsius),
            getString(R.string.settings_temp_fahrenheit)
        )
        val currentUnit = WidgetPreferences.getTemperatureUnit(this)
        val selectedIndex = if (currentUnit == "F") 1 else 0

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.settings_temp_unit_title))
            .setSingleChoiceItems(units, selectedIndex) { dialog, which ->
                dialog.dismiss()
                val newUnit = if (which == 1) "F" else "C"
                if (newUnit != currentUnit) {
                    WidgetPreferences.setTemperatureUnit(this, newUnit)
                    ShapeWidgetCanvasRenderer.invalidateCache()
                    val updateIntent = Intent(WidgetUpdateScheduler.ACTION_WIDGET_TICK).apply {
                        setPackage(packageName)
                    }
                    sendBroadcast(updateIntent)
                    WidgetAnimationManager.updateAllWidgetsWithProgress(this, 1.0f)
                    updateSettingsValues()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun finishWithTransition() {
        SharedAmbientBackgroundHolder.saveSnapshot(ambientBgView.getOrbsSnapshot())
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onPause() {
        super.onPause()
        SharedAmbientBackgroundHolder.saveSnapshot(ambientBgView.getOrbsSnapshot())
    }
}
