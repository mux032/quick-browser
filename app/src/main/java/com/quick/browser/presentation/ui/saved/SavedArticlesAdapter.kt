package com.quick.browser.presentation.ui.saved

import android.graphics.Color
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
import com.quick.browser.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying saved articles in a RecyclerView
 */
class SavedArticlesAdapter(
    private val onItemClick: (SavedArticle) -> Unit,
    private val onDeleteClick: (SavedArticle) -> Unit,
    private val viewModel: SavedArticlesViewModel,
    private val lifecycleOwner: LifecycleOwner
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

            // Load preview image and favicon using ViewModel
            loadPreviewImage(article)
            loadFavicon(article)

            itemView.setOnClickListener {
                onItemClick(article)
            }

            deleteButton.setOnClickListener {
                onDeleteClick(article)
            }
        }

        private fun loadPreviewImage(article: SavedArticle) {
            // Reset image view properties
            previewImage.scaleType = ImageView.ScaleType.CENTER_CROP
            previewImage.setBackgroundColor(Color.TRANSPARENT)

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
                                .addListener(object : RequestListener<Drawable> {
                                    override fun onLoadFailed(
                                        e: GlideException?,
                                        model: Any?,
                                        target: Target<Drawable>,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        // Show fallback when loading fails
                                        showFallbackPreview()
                                        return true // We handled the error
                                    }

                                    override fun onResourceReady(
                                        resource: Drawable,
                                        model: Any,
                                        target: Target<Drawable>,
                                        dataSource: DataSource,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        // Reset background when image is successfully loaded
                                        previewImage.setBackgroundColor(Color.TRANSPARENT)
                                        previewImage.scaleType = ImageView.ScaleType.CENTER_CROP
                                        return false
                                    }
                                })
                                .into(previewImage)
                        }
                    } else {
                        // Fallback to constructing URL or showing placeholder
                        showFallbackPreview()
                    }
                } catch (e: Exception) {
                    Logger.e("SavedArticlesAdapter", "Error loading preview image", e)
                    showFallbackPreview()
                }
            }
        }

        private fun showFallbackPreview() {
            previewImage.setImageResource(R.drawable.ic_web_page)
            previewImage.scaleType = ImageView.ScaleType.CENTER_INSIDE
            previewImage.setBackgroundColor(itemView.context.getColor(R.color.light_gray))
        }

        private fun loadFavicon(article: SavedArticle) {
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
                                .into(faviconImage)
                        }
                    } else {
                        // Fallback to constructing favicon URL or showing placeholder
                        showFallbackFavicon(article)
                    }
                } catch (e: Exception) {
                    Logger.e("SavedArticlesAdapter", "Error loading favicon", e)
                    showFallbackFavicon(article)
                }
            }
        }

        private fun showFallbackFavicon(article: SavedArticle) {
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
                        .into(faviconImage)
                } else {
                    faviconImage.setImageResource(R.drawable.ic_website)
                }
            } catch (e: Exception) {
                Logger.e("SavedArticlesAdapter", "Error constructing favicon URL", e)
                faviconImage.setImageResource(R.drawable.ic_website)
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