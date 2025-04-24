package com.qb.browser.ui.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.qb.browser.R
import com.qb.browser.ui.theme.ThemeColor

/**
 * Adapter for the color picker RecyclerView
 */
class ColorPickerAdapter(
    private val context: Context,
    private val colors: List<ThemeColor>,
    private var selectedColor: ThemeColor,
    private val onColorSelected: (ThemeColor) -> Unit
) : RecyclerView.Adapter<ColorPickerAdapter.ColorViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.color_picker_item, parent, false)
        return ColorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        val color = colors[position]
        holder.bind(color, color == selectedColor)
    }

    override fun getItemCount(): Int = colors.size

    fun updateSelectedColor(color: ThemeColor) {
        val oldSelectedPosition = colors.indexOf(selectedColor)
        selectedColor = color
        val newSelectedPosition = colors.indexOf(selectedColor)
        
        if (oldSelectedPosition >= 0) {
            notifyItemChanged(oldSelectedPosition)
        }
        if (newSelectedPosition >= 0) {
            notifyItemChanged(newSelectedPosition)
        }
    }

    inner class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val colorCard: MaterialCardView = itemView.findViewById(R.id.color_card)
        private val colorGradient: ImageView = itemView.findViewById(R.id.color_gradient)
        private val colorCheck: ImageView = itemView.findViewById(R.id.color_check)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val color = colors[position]
                    updateSelectedColor(color)
                    onColorSelected(color)
                }
            }
        }

        fun bind(color: ThemeColor, isSelected: Boolean) {
            // Create gradient drawable
            val primaryColor = ContextCompat.getColor(context, color.primaryColorRes)
            val accentColor = ContextCompat.getColor(context, color.accentColorRes)
            
            val gradient = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(primaryColor, accentColor)
            )
            gradient.cornerRadius = 0f
            colorGradient.setImageDrawable(gradient)
            
            // Update selection state
            colorCheck.visibility = if (isSelected) View.VISIBLE else View.GONE
            colorCard.strokeColor = if (isSelected) {
                ContextCompat.getColor(context, R.color.colorAccent)
            } else {
                Color.TRANSPARENT
            }
        }
    }
}