package com.iatb.materialthemes

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.iatb.materialthemes.ui.AmbientMeshBackgroundView
import com.iatb.materialthemes.ui.CircularArcProgressView
import com.iatb.materialthemes.ui.SplashBackgroundTransitionHolder

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var ambientBg: AmbientMeshBackgroundView
    private lateinit var arcProgress: CircularArcProgressView
    private lateinit var iconCard: FrameLayout
    private lateinit var ivIcon: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        initViews()
        ambientBg.startAmbientAnimation(durationMs = 5000L)
        startEntranceAnimations()
    }

    private fun initViews() {
        ambientBg = findViewById(R.id.splash_ambient_bg)
        arcProgress = findViewById(R.id.splash_arc_progress)
        iconCard = findViewById(R.id.splash_icon_card)
        ivIcon = findViewById(R.id.splash_iv_icon)
        tvTitle = findViewById(R.id.splash_tv_title)
        tvSubtitle = findViewById(R.id.splash_tv_subtitle)
        tvStatus = findViewById(R.id.splash_tv_status)
    }

    private fun startEntranceAnimations() {
        // Initial states
        iconCard.alpha = 0f
        iconCard.scaleX = 0.7f
        iconCard.scaleY = 0.7f

        tvTitle.alpha = 0f
        tvTitle.translationY = 24f

        tvSubtitle.alpha = 0f
        tvSubtitle.translationY = 24f

        arcProgress.alpha = 0f
        arcProgress.scaleX = 0.85f
        arcProgress.scaleY = 0.85f

        tvStatus.alpha = 0f

        // 1. Icon pop entrance
        iconCard.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(650)
            .setInterpolator(OvershootInterpolator(1.3f))
            .start()

        // 2. Title & Subtitle fade up
        tvTitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(550)
            .setStartDelay(180)
            .setInterpolator(DecelerateInterpolator(1.4f))
            .start()

        tvSubtitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(550)
            .setStartDelay(260)
            .setInterpolator(DecelerateInterpolator(1.4f))
            .start()

        // 3. Arc Progress entrance and run progress
        arcProgress.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(600)
            .setStartDelay(320)
            .setInterpolator(OvershootInterpolator(1.1f))
            .withEndAction {
                tvStatus.animate().alpha(1f).setDuration(300).start()
                // Run circular arc progress from 0% to 100%
                arcProgress.animateProgress(
                    from = 0f,
                    to = 1f,
                    durationMs = 1800L,
                    onEnd = {
                        navigateToMain()
                    }
                )
            }
            .start()
    }

    private fun navigateToMain() {
        // Freeze ambient orbs in place and capture exact runtime snapshot
        ambientBg.stopAmbientAnimation()
        SplashBackgroundTransitionHolder.snapshotOrbs = ambientBg.getOrbsSnapshot()

        if (!isFinishing && !isDestroyed) {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_FROM_SPLASH, true)
            }
            startActivity(intent)
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }
}
