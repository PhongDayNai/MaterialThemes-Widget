package com.iatb.materialthemes.ui

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.iatb.materialthemes.R

class UtilityWidgetsActivity : AppCompatActivity() {

    private lateinit var ambientBgView: AmbientMeshBackgroundView
    private lateinit var btnBack: View
    private lateinit var cardComingSoon: GlassBlurCardView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_utility_widgets)

        initViews()
        setupAmbientBackground()
        setupBackNavigation()
    }

    private fun setupAmbientBackground() {
        val snapshot = SharedAmbientBackgroundHolder.getSnapshot()
        if (snapshot != null) {
            ambientBgView.setOrbs(snapshot)
        }
        ambientBgView.post {
            ambientBgView.transitionToNewConstellation(
                durationMs = 950L,
                onUpdate = {
                    cardComingSoon.refreshBlur()
                },
                onComplete = {
                    cardComingSoon.refreshBlur()
                    SharedAmbientBackgroundHolder.saveSnapshot(ambientBgView.getOrbsSnapshot())
                }
            )
        }
    }

    private fun initViews() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.utility_content_container)) { v, insets ->
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

        ambientBgView = findViewById(R.id.ambient_bg_view)
        btnBack = findViewById(R.id.btn_back)
        cardComingSoon = findViewById(R.id.card_coming_soon)
        cardComingSoon.setTargetBackgroundView(ambientBgView)

        findViewById<androidx.core.widget.NestedScrollView>(R.id.scroll_content).setOnScrollChangeListener { _, _, _, _, _ ->
            cardComingSoon.refreshBlur()
        }

        WidgetPreviewHelper.applyPressScaleEffect(btnBack)
        btnBack.setOnClickListener {
            finishWithTransition()
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishWithTransition()
            }
        })
    }

    private fun finishWithTransition() {
        SharedAmbientBackgroundHolder.saveSnapshot(ambientBgView.getOrbsSnapshot())
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onResume() {
        super.onResume()
        cardComingSoon.refreshBlur()
    }

    override fun onPause() {
        super.onPause()
        SharedAmbientBackgroundHolder.saveSnapshot(ambientBgView.getOrbsSnapshot())
    }
}
