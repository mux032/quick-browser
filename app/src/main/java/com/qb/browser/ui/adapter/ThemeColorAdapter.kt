package com.qb.browser.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.qb.browser.R
import com.qb.browser.ui.theme.ThemeColor

/**
 * Adapter for the theme color selection recycler view
 */
class ThemeColorAdapter(
    private val context: Context,
    private val colors: List<ThemeColor>,
    private var selectedColorName: String,
    private val onColorSelected: (ThemeColor) -> Unit
) : RecyclerView.Adapter<ThemeColorAdapter.ColorViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_theme_color, parent, false)
        return ColorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        val color = colors[position]
        holder.bind(color)
    }

    override fun getItemCount(): Int = colors.size

    fun updateSelectedColor(colorName: String) {
        val oldSelectedPosition = colors.indexOfFirst { it.colorName == selectedColorName }
        val newSelectedPosition = colors.indexOfFirst { it.colorName == colorName }
        
        selectedColorName = colorName
        
        if (oldSelectedPosition >= 0) {
            notifyItemChanged(oldSelectedPosition)
        }
        if (newSelectedPosition >= 0) {
            notifyItemChanged(newSelectedPosition)
        }
    }

    inner class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val colorCircle: CardView = itemView.findViewById(R.id.color_circle)
        private val colorName: TextView = itemView.findViewById(R.id.color_name)
        private val checkIcon: ImageView = itemView.findViewById(R.id.check_icon)

        fun bind(themeColor: ThemeColor) {
            // Set the color of the circle
            val color = ContextCompat.getColor(context, themeColor.primaryColorRes)
            colorCircle.setCardBackgroundColor(color)
            
            // Set the color name
            colorName.text = themeColor.colorName
            
            // Show check icon if this color is selected
            val isSelected = themeColor.colorName == selectedColorName
            checkIcon.visibility = if (isSelected) View.VISIBLE else View.GONE
            
            // Set click listener
            itemView.setOnClickListener {
                val oldSelectedColor = selectedColorName
                selectedColorName = themeColor.colorName
                
                // Update the UI
                val oldPosition = colors.indexOfFirst { it.colorName == oldSelectedColor }
                if (oldPosition >= 0) {
                    notifyItemChanged(oldPosition)
                }
                notifyItemChanged(adapterPosition)
                
                // Notify the listener
                onColorSelected(themeColor)
            }
        }
    }
}