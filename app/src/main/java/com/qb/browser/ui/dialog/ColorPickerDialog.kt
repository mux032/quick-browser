package com.qb.browser.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.qb.browser.R
import com.qb.browser.ui.adapter.ColorPickerAdapter
import com.qb.browser.ui.theme.ThemeColor

/**
 * Dialog for selecting a theme color with a circular gradient display
 */
class ColorPickerDialog(
    context: Context,
    private val currentColor: ThemeColor,
    private val isDynamicColorEnabled: Boolean,
    private val onColorSelected: (ThemeColor, Boolean) -> Unit
) : Dialog(context) {

    private lateinit var colorAdapter: ColorPickerAdapter
    private var selectedColor: ThemeColor = currentColor
    private var useDynamicColor: Boolean = isDynamicColorEnabled

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_color_picker)

        // Set up RecyclerView with grid layout
        val recyclerView = findViewById<RecyclerView>(R.id.color_recycler_view)
        recyclerView.layoutManager = GridLayoutManager(context, 4)
        
        // Get all available colors
        val colors = ThemeColor.values().toList()
        
        // Create and set adapter
        colorAdapter = ColorPickerAdapter(
            context,
            colors,
            currentColor
        ) { color ->
            selectedColor = color
        }
        recyclerView.adapter = colorAdapter

        // Set up dynamic color switch
        val dynamicColorSwitch = findViewById<SwitchMaterial>(R.id.dynamic_color_switch)
        dynamicColorSwitch.isChecked = isDynamicColorEnabled
        dynamicColorSwitch.setOnCheckedChangeListener { _, isChecked ->
            useDynamicColor = isChecked
            // Disable color selection if dynamic color is enabled
            recyclerView.alpha = if (isChecked) 0.5f else 1.0f
            recyclerView.isEnabled = !isChecked
        }
        
        // Set initial state
        recyclerView.alpha = if (isDynamicColorEnabled) 0.5f else 1.0f
        recyclerView.isEnabled = !isDynamicColorEnabled

        // Set up buttons
        findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            dismiss()
        }

        findViewById<Button>(R.id.btn_apply).setOnClickListener {
            onColorSelected(selectedColor, useDynamicColor)
            dismiss()
        }
    }

    companion object {
        fun show(
            context: Context,
            currentColor: ThemeColor,
            isDynamicColorEnabled: Boolean,
            onColorSelected: (ThemeColor, Boolean) -> Unit
        ) {
            ColorPickerDialog(context, currentColor, isDynamicColorEnabled, onColorSelected).show()
        }
    }
}