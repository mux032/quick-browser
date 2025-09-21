package com.quick.browser.presentation.ui.saved

import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.quick.browser.R
import com.quick.browser.domain.model.SavedArticle
import com.quick.browser.domain.model.SavedArticlesViewStyle
import com.quick.browser.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying saved articles in a RecyclerView with different view styles
 */
class SavedArticlesAdapter(
    private val onItemClick: (SavedArticle) -> Unit,
    private val onDeleteClick: (SavedArticle) -> Unit,
    private val viewModel: SavedArticlesViewModel,
    private val lifecycleOwner: LifecycleOwner
) : ListAdapter<SavedArticle, SavedArticlesAdapter.SavedArticleViewHolder>(SavedArticleDiffCallback()) {

    private var currentViewStyle: SavedArticlesViewStyle = SavedArticlesViewStyle.CARD

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedArticleViewHolder {
        val view = SavedArticlesViewTypeHelper.inflateViewForViewStyle(parent, currentViewStyle)
        return SavedArticleViewHolder(view)
    }

    override fun onBindViewHolder(holder: SavedArticleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemViewType(position: Int): Int {
        // Return view type based on current view style to enable proper view recycling
        return currentViewStyle.ordinal
    }

    fun updateViewStyle(viewStyle: SavedArticlesViewStyle) {
        currentViewStyle = viewStyle
        notifyDataSetChanged()
    }

    inner class SavedArticleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView? = itemView.findViewById(R.id.text_title)
        private val siteNameTextView: TextView? = itemView.findViewById(R.id.text_site_name)
        private val savedDateTextView: TextView? = itemView.findViewById(R.id.text_saved_date)
        private val deleteButton: ImageView? = itemView.findViewById(R.id.button_delete)
        private val previewImage: ImageView? = itemView.findViewById(R.id.preview_image)
        private val faviconImage: ImageView? = itemView.findViewById(R.id.favicon_image)

        fun bind(article: SavedArticle) {
            titleTextView?.text = article.title
            siteNameTextView?.text = article.siteName ?: "Unknown Source"

            // Format saved date to show only month and day
            val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
            val date = Date(article.savedDate)
            savedDateTextView?.text = dateFormat.format(date)

            // Load preview image and favicon using ViewModel
            previewImage?.let { loadPreviewImage(article, it) }
            faviconImage?.let { loadFavicon(article, it) }

            itemView.setOnClickListener {
                onItemClick(article)
            }

            deleteButton?.setOnClickListener {
                onDeleteClick(article)
            }
        }

        private fun loadPreviewImage(article: SavedArticle, imageView: ImageView) {
            // Reset image view properties
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            imageView.setBackgroundColor(Color.TRANSPARENT)

            // Use coroutine to fetch preview image URL from ViewModel
            lifecycleOwner.lifecycleScope.launch {
                try {
                    val previewImageUrl = viewModel.getPreviewImageUrlForArticle(article.url)
                    
                    if (previewImageUrl != null && previewImageUrl.isNotBlank()) {
                        // Load the preview image using Glide
                        withContext(Dispatchers.Main) {
                            Glide.with(itemView.context)
                                .load(previewImageUrl)
                                .placeholder(R.drawable.ic_web_page)
                                .error(R.drawable.ic_web_page)
                                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                                .into(imageView)
                        }
                    } else {
                        // Fallback to constructing URL or showing placeholder
                        showFallbackPreview(imageView)
                    }
                } catch (e: Exception) {
                    Logger.e("SavedArticlesAdapter", "Error loading preview image", e)
                    showFallbackPreview(imageView)
                }
            }
        }

        private fun showFallbackPreview(imageView: ImageView) {
            imageView.setImageResource(R.drawable.ic_web_page)
            imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
            imageView.setBackgroundColor(itemView.context.getColor(R.color.light_gray))
        }

        private fun loadFavicon(article: SavedArticle, imageView: ImageView) {
            // Use coroutine to fetch favicon URL from ViewModel
            lifecycleOwner.lifecycleScope.launch {
                try {
                    val faviconUrl = viewModel.getFaviconUrlForArticle(article.url)
                    
                    if (faviconUrl != null && faviconUrl.isNotBlank()) {
                        // Load the favicon using Glide
                        withContext(Dispatchers.Main) {
                            Glide.with(itemView.context)
                                .load(faviconUrl)
                                .placeholder(R.drawable.ic_website)
                                .error(R.drawable.ic_website)
                                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                                .into(imageView)
                        }
                    } else {
                        // Fallback to constructing favicon URL or showing placeholder
                        showFallbackFavicon(article, imageView)
                    }
                } catch (e: Exception) {
                    Logger.e("SavedArticlesAdapter", "Error loading favicon", e)
                    showFallbackFavicon(article, imageView)
                }
            }
        }

        private fun showFallbackFavicon(article: SavedArticle, imageView: ImageView) {
            // Try to construct favicon URL from domain
            try {
                val uri = Uri.parse(article.url)
                val domain = uri.host
                if (domain != null) {
                    // Try multiple favicon URLs as different sites use different conventions
                    val faviconUrls = listOf(
                        "https://$domain/favicon.ico",
                        "https://www.$domain/favicon.ico",
                        "https://$domain/apple-touch-icon.png",
                        "https://www.$domain/apple-touch-icon.png"
                    )
                    
                    // Try the first URL
                    Glide.with(itemView.context)
                        .load(faviconUrls[0])
                        .placeholder(R.drawable.ic_website)
                        .error(R.drawable.ic_website)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .into(imageView)
                } else {
                    imageView.setImageResource(R.drawable.ic_website)
                }
            } catch (e: Exception) {
                Logger.e("SavedArticlesAdapter", "Error constructing favicon URL", e)
                imageView.setImageResource(R.drawable.ic_website)
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