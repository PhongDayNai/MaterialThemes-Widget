package com.iatb.materialthemes.ui

/**
 * Backward-compatible bridge delegating to SharedAmbientBackgroundHolder.
 */
object SplashBackgroundTransitionHolder {
    var snapshotOrbs: List<AmbientMeshBackgroundView.GlowOrb>?
        get() = SharedAmbientBackgroundHolder.currentOrbs
        set(value) {
            SharedAmbientBackgroundHolder.currentOrbs = value?.map { it.copy() }
        }

    fun consumeSnapshot(): List<AmbientMeshBackgroundView.GlowOrb>? {
        return SharedAmbientBackgroundHolder.consumeSnapshot()
    }
}
