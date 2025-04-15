package com.qb.browser.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.qb.browser.R
import com.qb.browser.model.Bubble

class TabsAdapter(
    private val context: Context,
    private val onTabSelected: (String) -> Unit,
    private val onTabClosed: (String) -> Unit
) : ListAdapter<Bubble, TabsAdapter.TabViewHolder>(BUBBLE_COMPARATOR) {

    private var selectedTabId: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.bubble_tab_item, parent, false)
        return TabViewHolder(view)
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        val bubble = getItem(position)
        holder.bind(bubble, bubble.id == selectedTabId)

        holder.itemView.setOnClickListener {
            onTabSelected(bubble.id)
            selectedTabId = bubble.id
            notifyDataSetChanged()
        }

        holder.closeButton.setOnClickListener {
            onTabClosed(bubble.id)
        }
    }

    inner class TabViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.tab_title)
        private val urlText: TextView = itemView.findViewById(R.id.tab_url)
        private val faviconImage: ImageView = itemView.findViewById(R.id.tab_favicon)
        val closeButton: ImageView = itemView.findViewById(R.id.tab_close)

        fun bind(bubble: Bubble, isSelected: Boolean) {
            titleText.text = bubble.title.ifEmpty { context.getString(R.string.untitled_page) }
            urlText.text = bubble.url
            
            bubble.favicon?.let {
                faviconImage.setImageBitmap(it)
            } ?: run {
                faviconImage.setImageResource(R.drawable.ic_globe)
            }
            
            itemView.isSelected = isSelected
            itemView.setBackgroundResource(
                if (isSelected) R.drawable.tab_background_selected
                else R.drawable.tab_background_normal
            )
        }
    }

    companion object {
        private val BUBBLE_COMPARATOR = object : DiffUtil.ItemCallback<Bubble>() {
            override fun areItemsTheSame(oldItem: Bubble, newItem: Bubble): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Bubble, newItem: Bubble): Boolean {
                return oldItem == newItem
            }
        }
    }
}