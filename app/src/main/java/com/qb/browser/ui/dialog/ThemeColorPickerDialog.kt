package com.qb.browser.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qb.browser.R
import com.qb.browser.ui.adapter.ThemeColorAdapter
import com.qb.browser.ui.theme.ThemeColor
import com.qb.browser.util.SettingsManager

/**
 * Dialog for selecting a theme color
 */
class ThemeColorPickerDialog(
    context: Context,
    private val onColorSelected: (ThemeColor) -> Unit
) : Dialog(context) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ThemeColorAdapter
    private lateinit var buttonCancel: Button
    private lateinit var buttonApply: Button
    
    private val settingsManager = SettingsManager.getInstance(context)
    private var selectedColor: ThemeColor = ThemeColor.fromName(settingsManager.getThemeColor())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val view = LayoutInflater.from(context).inflate(R.layout.theme_color_picker_dialog, null)
        setContentView(view)
        
        // Initialize views
        recyclerView = view.findViewById(R.id.recycler_view_colors)
        buttonCancel = view.findViewById(R.id.button_cancel)
        buttonApply = view.findViewById(R.id.button_apply)
        
        // Set up the recycler view
        recyclerView.layoutManager = GridLayoutManager(context, 4)
        adapter = ThemeColorAdapter(
            context,
            ThemeColor.values().toList(),
            settingsManager.getThemeColor()
        ) { color ->
            selectedColor = color
        }
        recyclerView.adapter = adapter
        
        // Set up button listeners
        buttonCancel.setOnClickListener {
            dismiss()
        }
        
        buttonApply.setOnClickListener {
            onColorSelected(selectedColor)
            dismiss()
        }
    }
    
    companion object {
        fun show(context: Context, onColorSelected: (ThemeColor) -> Unit) {
            ThemeColorPickerDialog(context, onColorSelected).show()
        }
    }
}