package com.quick.browser.ui.history

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.quick.browser.R
import com.quick.browser.model.HistoryItem
import com.quick.browser.model.WebPage
import com.quick.browser.util.OfflineArticleSaver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class HistoryAdapter(
    private val onItemClick: (WebPage) -> Unit,
    private val onItemLongClick: (WebPage) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_WEBPAGE = 1
        private const val TAG = "HistoryAdapter"
    }

    private var items = listOf<HistoryItem>()
    private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    private val colorCache = mutableMapOf<String, Int>()

    fun submitList(newItems: List<WebPage>) {
        items = groupWebPagesByTime(newItems.sortedByDescending { it.timestamp })
        notifyDataSetChanged()
    }

    private fun groupWebPagesByTime(webPages: List<WebPage>): List<HistoryItem> {
        val startOfDay = getStartOfDay()
        val startOfWeek = getStartOfWeek()
        
        val groupedItems = mutableListOf<HistoryItem>()
        
        // Group pages by time period
        val todayPages = webPages.filter { it.timestamp >= startOfDay }
        val thisWeekPages = webPages.filter { it.timestamp >= startOfWeek && it.timestamp < startOfDay }
        val olderPages = webPages.filter { it.timestamp < startOfWeek }
        
        // Add Today section
        if (todayPages.isNotEmpty()) {
            groupedItems.add(HistoryItem.Header("Today"))
            todayPages.forEach { groupedItems.add(HistoryItem.WebPageItem(it)) }
        }
        
        // Add Last Week section
        if (thisWeekPages.isNotEmpty()) {
            groupedItems.add(HistoryItem.Header("Last Week"))
            thisWeekPages.forEach { groupedItems.add(HistoryItem.WebPageItem(it)) }
        }
        
        // Add Older section
        if (olderPages.isNotEmpty()) {
            groupedItems.add(HistoryItem.Header("Older"))
            olderPages.forEach { groupedItems.add(HistoryItem.WebPageItem(it)) }
        }
        
        return groupedItems
    }
    
    private fun getStartOfDay(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    private fun getStartOfWeek(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is HistoryItem.Header -> TYPE_HEADER
            is HistoryItem.WebPageItem -> TYPE_WEBPAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_history_header, parent, false)
                HeaderViewHolder(view)
            }
            TYPE_WEBPAGE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_history_card, parent, false)
                HistoryViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is HistoryItem.Header -> {
                (holder as HeaderViewHolder).bind(item.title)
            }
            is HistoryItem.WebPageItem -> {
                (holder as HistoryViewHolder).bind(item.webPage)
            }
        }
    }

    override fun getItemCount() = items.size

    // Header ViewHolder
    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.header_title)
        
        fun bind(title: String) {
            titleText.text = title
        }
    }

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val previewImage: ImageView = itemView.findViewById(R.id.preview_image)
        private val faviconImage: ImageView = itemView.findViewById(R.id.favicon_image)
        private val titleText: TextView = itemView.findViewById(R.id.title_text)
        private val dateText: TextView = itemView.findViewById(R.id.date_text)
        private val offlineIndicator: ImageView = itemView.findViewById(R.id.offline_indicator)
        private val websiteName: TextView = itemView.findViewById(R.id.website_name)
        private val shareButton: ImageView = itemView.findViewById(R.id.share_button)
        private val saveButton: ImageView = itemView.findViewById(R.id.save_button)
        private val deleteOverlay: View = itemView.findViewById(R.id.delete_overlay)
        private val deleteButton: ImageView = itemView.findViewById(R.id.delete_button)

        fun bind(page: WebPage) {
            // Set title
            val displayTitle = when {
                page.title.isEmpty() -> itemView.context.getString(R.string.untitled_page)
                page.title == page.url -> itemView.context.getString(R.string.untitled_page)
                else -> page.title
            }
            titleText.text = displayTitle

            // Set website name (extracted from URL without www, .com, etc.)
            val cleanWebsiteName = extractCleanWebsiteName(page.url)
            websiteName.text = cleanWebsiteName

            // Set date (without year)
            dateText.text = dateFormat.format(Date(page.timestamp))

            // Set save button visibility and icon (always show the download icon)
            saveButton.visibility = View.VISIBLE
            saveButton.setImageResource(R.drawable.ic_download)

            // Set preview image using Glide
            loadPreviewImage(page)

            // Set favicon using Glide
            loadFavicon(page)

            // Set share button click listener
            shareButton.setOnClickListener {
                shareWebPage(page)
            }
            
            // Set save button click listener
            saveButton.setOnClickListener {
                saveWebPageForOffline(page)
            }

            // Set click listener for the card
            itemView.setOnClickListener {
                onItemClick(page)
            }
            
            // Set long click listener for the card
            itemView.setOnLongClickListener {
                showDeleteOption()
                true
            }
            
            // Set delete button click listener
            deleteButton.setOnClickListener {
                hideDeleteOption()
                onItemLongClick(page)
            }
            
            // Set delete overlay click listener to hide delete option
            deleteOverlay.setOnClickListener {
                hideDeleteOption()
            }
        }
        
        private fun showDeleteOption() {
            deleteOverlay.visibility = View.VISIBLE
            deleteButton.visibility = View.VISIBLE
            
            // Add animation
            deleteOverlay.alpha = 0f
            deleteButton.alpha = 0f
            deleteOverlay.animate().alpha(1f).setDuration(200).start()
            deleteButton.animate().alpha(1f).setDuration(200).start()
        }
        
        private fun hideDeleteOption() {
            deleteOverlay.animate().alpha(0f).setDuration(200).withEndAction {
                deleteOverlay.visibility = View.GONE
            }.start()
            deleteButton.animate().alpha(0f).setDuration(200).withEndAction {
                deleteButton.visibility = View.GONE
            }.start()
        }

        private fun extractCleanWebsiteName(url: String): String {
            return try {
                val uri = Uri.parse(url)
                val host = uri.host?.lowercase() ?: return "Unknown"
                
                // Remove www prefix
                val withoutWww = if (host.startsWith("www.")) {
                    host.substring(4)
                } else {
                    host
                }
                
                // Split by dots and take the first part (main domain name)
                val parts = withoutWww.split(".")
                if (parts.isNotEmpty()) {
                    // Capitalize first letter
                    val mainDomain = parts[0]
                    mainDomain.replaceFirstChar { 
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
                    }
                } else {
                    "Unknown"
                }
            } catch (e: Exception) {
                "Unknown"
            }
        }

        private fun shareWebPage(page: WebPage) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, page.url)
                putExtra(Intent.EXTRA_SUBJECT, page.title)
            }
            
            val chooser = Intent.createChooser(shareIntent, "Share webpage")
            itemView.context.startActivity(chooser)
        }
        
        private fun saveWebPageForOffline(page: WebPage) {
            // Create OfflineArticleSaver instance
            val offlineSaver = OfflineArticleSaver(itemView.context)
            
            // Save the article
            offlineSaver.saveArticleForOfflineReading(
                url = page.url,
                scope = CoroutineScope(Dispatchers.Main),
                onSuccess = {
                    // Update the page's offline status
                    page.isAvailableOffline = true
                    
                    // The icon stays the same (download icon) for both saved and unsaved states
                }
            )
        }

        private fun getRandomColorForUrl(url: String): Int {
            // Use cache to ensure consistent colors for the same URL
            return colorCache.getOrPut(url) {
                val hash = url.hashCode()
                val random = Random(hash)
                
                // Generate pleasant colors
                val hue = random.nextFloat() * 360f
                val saturation = 0.5f + random.nextFloat() * 0.3f // 0.5 to 0.8
                val lightness = 0.4f + random.nextFloat() * 0.2f // 0.4 to 0.6
                
                Color.HSVToColor(floatArrayOf(hue, saturation, lightness))
            }
        }

        private fun loadPreviewImage(page: WebPage) {
            // Reset image view properties
            previewImage.scaleType = ImageView.ScaleType.CENTER_CROP
            previewImage.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            
            Log.d(TAG, "Attempting to load preview image for ${page.url}, previewImageUrl: ${page.previewImageUrl}")
            
            if (page.previewImageUrl != null && page.previewImageUrl!!.isNotBlank()) {
                Log.d(TAG, "Loading preview image for ${page.url}: ${page.previewImageUrl}")
                Glide.with(itemView.context)
                    .load(page.previewImageUrl)
                    .placeholder(R.drawable.ic_web_page)
                    .error(R.drawable.ic_web_page)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .addListener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                        override fun onLoadFailed(
                            e: com.bumptech.glide.load.engine.GlideException?,
                            model: Any?,
                            target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            Log.e(TAG, "Failed to load preview image for ${page.url}: ${page.previewImageUrl}", e)
                            // Show a colored background with icon as fallback
                            showFallbackPreview(page)
                            return true // We handled the error
                        }
                        
                        override fun onResourceReady(
                            resource: android.graphics.drawable.Drawable,
                            model: Any,
                            target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                            dataSource: com.bumptech.glide.load.DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            Log.d(TAG, "Successfully loaded preview image for ${page.url}")
                            // Reset background when image is successfully loaded
                            previewImage.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            previewImage.scaleType = ImageView.ScaleType.CENTER_CROP
                            return false
                        }
                    })
                    .into(previewImage)
            } else {
                Log.d(TAG, "No preview image URL for ${page.url}, using fallback")
                showFallbackPreview(page)
            }
        }
        
        private fun showFallbackPreview(page: WebPage) {
            // Set random color background as fallback
            val randomColor = getRandomColorForUrl(page.url)
            previewImage.setBackgroundColor(randomColor)
            previewImage.setImageResource(R.drawable.ic_web_page)
            previewImage.scaleType = ImageView.ScaleType.CENTER
        }

        private fun loadFavicon(page: WebPage) {
            if (page.faviconUrl != null) {
                Glide.with(itemView.context)
                    .load(page.faviconUrl)
                    .placeholder(R.drawable.ic_website)
                    .error(R.drawable.ic_website)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .into(faviconImage)
            } else {
                // Try to construct favicon URL from domain
                try {
                    val uri = Uri.parse(page.url)
                    val domain = uri.host
                    if (domain != null) {
                        val faviconUrl = "https://$domain/favicon.ico"
                        Glide.with(itemView.context)
                            .load(faviconUrl)
                            .placeholder(R.drawable.ic_website)
                            .error(R.drawable.ic_website)
                            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                            .into(faviconImage)
                    } else {
                        faviconImage.setImageResource(R.drawable.ic_website)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error constructing favicon URL", e)
                    faviconImage.setImageResource(R.drawable.ic_website)
                }
            }
        }
    }
}