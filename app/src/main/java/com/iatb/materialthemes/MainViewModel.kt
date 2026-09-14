package com.iatb.materialthemes

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.iatb.materialthemes.data.ColorPalette
import com.iatb.materialthemes.data.WidgetContentMode
import com.iatb.materialthemes.data.WidgetPreferences
import com.iatb.materialthemes.widget.DiagonalWidgetProvider

enum class WidgetCategory {
    DIAGONAL,
    ORGANIC,
    SCALLOP
}

enum class WidgetSize(val labelResId: Int) {
    SIZE_2X2(R.string.size_2x2),
    SIZE_3X2(R.string.size_3x2),
    SIZE_4X2(R.string.size_4x2),
    SIZE_2X3(R.string.size_2x3),
    SIZE_3X3(R.string.size_3x3),
    SIZE_2X4(R.string.size_2x4),
    SIZE_4X3(R.string.size_4x3)
}

class MainViewModel : ViewModel() {

    private val _category = MutableLiveData(WidgetCategory.DIAGONAL)
    val category: LiveData<WidgetCategory> = _category

    private val _size = MutableLiveData(WidgetSize.SIZE_2X2)
    val size: LiveData<WidgetSize> = _size

    private val _angle = MutableLiveData(-45f)
    val angle: LiveData<Float> = _angle

    private val _palette = MutableLiveData(ColorPalette.OLIVE)
    val palette: LiveData<ColorPalette> = _palette

    private val _contentMode = MutableLiveData(WidgetContentMode.WEATHER)
    val contentMode: LiveData<WidgetContentMode> = _contentMode

    private val _transparency = MutableLiveData(100)
    val transparency: LiveData<Int> = _transparency

    fun initFromPreferences(context: Context) {
        _category.value = WidgetPreferences.getCategory(context)
        _size.value = WidgetPreferences.getSize(context)
        _angle.value = WidgetPreferences.getRotationAngle(context)
        _palette.value = WidgetPreferences.getColorPalette(context)
        _contentMode.value = WidgetPreferences.getContentMode(context)
        _transparency.value = WidgetPreferences.getTransparency(context)
    }

    fun setCategory(newCategory: WidgetCategory) {
        if (_category.value != newCategory) {
            _category.value = newCategory
        }
    }

    fun setSize(newSize: WidgetSize) {
        if (_size.value != newSize) {
            _size.value = newSize
        }
    }

    fun setRotationAngle(newAngle: Float) {
        if (_angle.value != newAngle) {
            _angle.value = newAngle
        }
    }

    fun setColorPalette(newPalette: ColorPalette) {
        if (_palette.value != newPalette) {
            _palette.value = newPalette
        }
    }

    fun setContentMode(newMode: WidgetContentMode) {
        if (_contentMode.value != newMode) {
            _contentMode.value = newMode
        }
    }

    fun setTransparency(newTransparency: Int) {
        if (_transparency.value != newTransparency) {
            _transparency.value = newTransparency.coerceIn(0, 100)
        }
    }

    fun saveAndApply(context: Context) {
        _category.value?.let { WidgetPreferences.setCategory(context, it) }
        _size.value?.let { WidgetPreferences.setSize(context, it) }
        _angle.value?.let { WidgetPreferences.setRotationAngle(context, it) }
        _palette.value?.let { WidgetPreferences.setColorPalette(context, it) }
        _contentMode.value?.let { WidgetPreferences.setContentMode(context, it) }
        _transparency.value?.let { WidgetPreferences.setTransparency(context, it) }

        com.iatb.materialthemes.data.DynamicThemeExtractor.invalidateCache()

        DiagonalWidgetProvider.updateAllWidgets(context)
        com.iatb.materialthemes.widget.OrganicWidgetProvider.updateAllWidgets(context)
        com.iatb.materialthemes.widget.ScallopWidgetProvider.updateAllWidgets(context)
    }
}
