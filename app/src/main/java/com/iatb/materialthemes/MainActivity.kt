package com.iatb.materialthemes

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.iatb.materialthemes.widget.DiagonalWidgetProvider
import com.iatb.materialthemes.widget.OrganicWidgetProvider
import com.iatb.materialthemes.widget.ScallopWidgetProvider

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var previewContainer: FrameLayout
    private lateinit var toggleCategory: MaterialButtonToggleGroup
    private lateinit var toggleSize: MaterialButtonToggleGroup
    private lateinit var btnPin: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        initViews()
        setupWindowInsets()
        setupListeners()
        observeViewModel()
    }

    private fun initViews() {
        previewContainer = findViewById(R.id.preview_container)
        toggleCategory = findViewById(R.id.toggle_category)
        toggleSize = findViewById(R.id.toggle_size)
        btnPin = findViewById(R.id.btn_pin)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_scroll)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupListeners() {
        toggleCategory.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btn_category_diagonal -> viewModel.setCategory(WidgetCategory.DIAGONAL)
                    R.id.btn_category_organic -> viewModel.setCategory(WidgetCategory.ORGANIC)
                    R.id.btn_category_scallop -> viewModel.setCategory(WidgetCategory.SCALLOP)
                }
            }
        }

        toggleSize.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btn_size_2x2 -> viewModel.setSize(WidgetSize.SIZE_2X2)
                    R.id.btn_size_3x2 -> viewModel.setSize(WidgetSize.SIZE_3X2)
                    R.id.btn_size_4x2 -> viewModel.setSize(WidgetSize.SIZE_4X2)
                    R.id.btn_size_2x3 -> viewModel.setSize(WidgetSize.SIZE_2X3)
                    R.id.btn_size_3x3 -> viewModel.setSize(WidgetSize.SIZE_3X3)
                    R.id.btn_size_2x4 -> viewModel.setSize(WidgetSize.SIZE_2X4)
                }
            }
        }

        btnPin.setOnClickListener {
            pinCurrentWidget()
        }
    }

    private fun observeViewModel() {
        viewModel.category.observe(this) {
            updatePreview()
        }

        viewModel.size.observe(this) {
            updatePreview()
        }
    }

    private fun updatePreview() {
        previewContainer.removeAllViews()
        val layoutId = viewModel.getPreviewLayoutId()
        val view = LayoutInflater.from(this).inflate(layoutId, previewContainer, false)

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        previewContainer.addView(view, params)
    }

    private fun pinCurrentWidget() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                val providerClass = when (viewModel.category.value) {
                    WidgetCategory.DIAGONAL -> DiagonalWidgetProvider::class.java
                    WidgetCategory.ORGANIC -> OrganicWidgetProvider::class.java
                    WidgetCategory.SCALLOP -> ScallopWidgetProvider::class.java
                    null -> DiagonalWidgetProvider::class.java
                }
                val provider = ComponentName(this, providerClass)
                appWidgetManager.requestPinAppWidget(provider, null, null)
                Toast.makeText(this, getString(R.string.pin_widget_success), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.pin_widget_not_supported), Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, getString(R.string.pin_widget_not_supported), Toast.LENGTH_LONG).show()
        }
    }
}