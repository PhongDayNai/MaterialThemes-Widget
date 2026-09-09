package com.iatb.materialthemes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

enum class WidgetCategory {
    DIAGONAL,
    ORGANIC
}

enum class WidgetSize(val labelResId: Int) {
    SIZE_2X2(R.string.size_2x2),
    SIZE_3X2(R.string.size_3x2),
    SIZE_4X2(R.string.size_4x2),
    SIZE_2X3(R.string.size_2x3),
    SIZE_3X3(R.string.size_3x3),
    SIZE_2X4(R.string.size_2x4)
}

class MainViewModel : ViewModel() {

    private val _category = MutableLiveData(WidgetCategory.DIAGONAL)
    val category: LiveData<WidgetCategory> = _category

    private val _size = MutableLiveData(WidgetSize.SIZE_4X2)
    val size: LiveData<WidgetSize> = _size

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

    fun getPreviewLayoutId(): Int {
        val cat = _category.value ?: WidgetCategory.DIAGONAL
        val sz = _size.value ?: WidgetSize.SIZE_4X2

        return when (cat) {
            WidgetCategory.DIAGONAL -> when (sz) {
                WidgetSize.SIZE_2X2 -> R.layout.widget_diagonal_2x2
                WidgetSize.SIZE_3X2 -> R.layout.widget_diagonal_3x2
                WidgetSize.SIZE_4X2 -> R.layout.widget_diagonal_4x2
                WidgetSize.SIZE_2X3 -> R.layout.widget_diagonal_2x3
                WidgetSize.SIZE_3X3 -> R.layout.widget_diagonal_3x3
                WidgetSize.SIZE_2X4 -> R.layout.widget_diagonal_2x4
            }
            WidgetCategory.ORGANIC -> when (sz) {
                WidgetSize.SIZE_2X2 -> R.layout.widget_organic_2x2
                WidgetSize.SIZE_3X2, WidgetSize.SIZE_4X2 -> R.layout.widget_organic_4x2
                WidgetSize.SIZE_2X3 -> R.layout.widget_organic_2x3
                WidgetSize.SIZE_3X3, WidgetSize.SIZE_2X4 -> R.layout.widget_organic_3x3
            }
        }
    }
}
