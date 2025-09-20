package com.quick.browser.presentation.ui.saved

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.quick.browser.R
import com.quick.browser.domain.model.SavedArticle
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying saved articles in a RecyclerView
 */
class SavedArticlesAdapter(
    private val onItemClick: (SavedArticle) -> Unit,
    private val onDeleteClick: (SavedArticle) -> Unit
) : ListAdapter<SavedArticle, SavedArticlesAdapter.SavedArticleViewHolder>(SavedArticleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedArticleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_saved_article, parent, false)
        return SavedArticleViewHolder(view)
    }

    override fun onBindViewHolder(holder: SavedArticleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SavedArticleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.text_title)
        private val siteNameTextView: TextView = itemView.findViewById(R.id.text_site_name)
        private val savedDateTextView: TextView = itemView.findViewById(R.id.text_saved_date)
        private val deleteButton: ImageView = itemView.findViewById(R.id.button_delete)
        private val previewImage: ImageView = itemView.findViewById(R.id.preview_image)
        private val faviconImage: ImageView = itemView.findViewById(R.id.favicon_image)

        fun bind(article: SavedArticle) {
            titleTextView.text = article.title
            siteNameTextView.text = article.siteName ?: "Unknown Source"

            // Format saved date to show only month and day
            val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
            val date = Date(article.savedDate)
            savedDateTextView.text = dateFormat.format(date)

            // Set preview image (using a placeholder for now)
            previewImage.setImageResource(R.drawable.ic_web_page)
            
            // Set favicon (using a placeholder for now)
            faviconImage.setImageResource(R.drawable.ic_website)

            itemView.setOnClickListener {
                onItemClick(article)
            }

            deleteButton.setOnClickListener {
                onDeleteClick(article)
            }
        }
    }

    class SavedArticleDiffCallback : DiffUtil.ItemCallback<SavedArticle>() {
        override fun areItemsTheSame(oldItem: SavedArticle, newItem: SavedArticle): Boolean {
            return oldItem.url == newItem.url
        }

        override fun areContentsTheSame(oldItem: SavedArticle, newItem: SavedArticle): Boolean {
            return oldItem == newItem
        }
    }
}