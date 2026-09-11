package com.iatb.materialthemes.ui

/**
 * Global singleton that preserves and synchronizes the living ambient background constellation
 * across all screens in the application (Splash, Main/Home, Edit, Presets, PresetEdit).
 * Whenever any screen is opened or resumed, it inherits the current active constellation
 * and seamlessly morphs into new positions, creating a unified living background experience.
 */
object SharedAmbientBackgroundHolder {
    var currentOrbs: List<AmbientMeshBackgroundView.GlowOrb>? = null

    fun saveSnapshot(orbs: List<AmbientMeshBackgroundView.GlowOrb>) {
        currentOrbs = orbs.map { it.copy() }
    }

    fun getSnapshot(): List<AmbientMeshBackgroundView.GlowOrb>? {
        return currentOrbs?.map { it.copy() }
    }

    fun consumeSnapshot(): List<AmbientMeshBackgroundView.GlowOrb>? {
        return currentOrbs?.map { it.copy() }
    }
}
