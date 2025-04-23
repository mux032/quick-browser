package com.qb.browser.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.qb.browser.R
import com.qb.browser.model.OfflinePage
import com.qb.browser.util.OfflinePageManager
import com.qb.browser.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class OfflinePageActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var textNoPages: TextView
    private lateinit var progressLoading: ProgressBar
    private lateinit var fabSaveCurrentPage: FloatingActionButton
    
    private lateinit var offlinePagesAdapter: OfflinePagesAdapter
    private val offlinePageManager by lazy { OfflinePageManager.getInstance(this) }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offline_page)
        
        // Initialize views
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.recyclerview_offline_pages)
        textNoPages = findViewById(R.id.text_no_pages)
        progressLoading = findViewById(R.id.progress_loading)
        fabSaveCurrentPage = findViewById(R.id.fab_save_current_page)
        
        // Set up toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Set up RecyclerView
        offlinePagesAdapter = OfflinePagesAdapter(
            onItemClick = { page -> openOfflinePage(page) },
            onOptionsClick = { page, view -> showPageOptions(page, view) }
        )
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@OfflinePageActivity)
            adapter = offlinePagesAdapter
            addItemDecoration(
                androidx.recyclerview.widget.DividerItemDecoration(
                    this@OfflinePageActivity,
                    LinearLayoutManager.VERTICAL
                )
            )
        }
        
        // Set up floating action button
        fabSaveCurrentPage.setOnClickListener {
            // The FAB is for demo purposes - in a real app, pages are saved from WebViewActivity
            Toast.makeText(
                this,
                "Please save pages from web view using the menu option",
                Toast.LENGTH_LONG
            ).show()
        }
        
        // Load offline pages
        loadOfflinePages()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh the list when resuming the activity
        loadOfflinePages()
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_offline_pages, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_delete_all -> {
                confirmDeleteAll()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    /**
     * Convert OfflinePageManager.OfflinePage to model.OfflinePage
     */
    private fun convertOfflinePage(managerPage: OfflinePageManager.OfflinePage): OfflinePage {
        return OfflinePage(
            id = managerPage.id,
            url = managerPage.url,
            title = managerPage.title,
            timestamp = managerPage.timestamp,
            filePath = managerPage.filePath,
            thumbnailPath = managerPage.thumbnailPath,
            size = managerPage.size
        )
    }
    
    private fun loadOfflinePages() {
        progressLoading.visibility = View.VISIBLE
        
        lifecycleScope.launch(Dispatchers.IO) {
            val managerPages = offlinePageManager.getAllOfflinePages()
            // Convert from manager page type to model page type
            val pages = managerPages.map { convertOfflinePage(it) }
            
            withContext(Dispatchers.Main) {
                progressLoading.visibility = View.GONE
                
                if (pages.isEmpty()) {
                    textNoPages.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    textNoPages.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    offlinePagesAdapter.submitList(pages)
                }
            }
        }
    }
    
    private fun openOfflinePage(page: OfflinePage) {
        val pageFile = offlinePageManager.loadOfflinePage(page.id)
        if (pageFile != null) {
            // Create FileProvider URI instead of direct file URI for Android 7+ compatibility
            val fileUri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                pageFile
            )
            
            val intent = Intent(this, WebViewActivity::class.java).apply {
                // Set the file URI as the data
                data = fileUri
                // Flag to grant read permission to the receiving activity
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                
                // Add necessary extras
                putExtra(Constants.EXTRA_IS_OFFLINE, true)
                putExtra(Constants.EXTRA_PAGE_ID, page.id)
                putExtra(Constants.EXTRA_PAGE_TITLE, page.title)
            }
            startActivity(intent)
        } else {
            Toast.makeText(this, R.string.error_loading_page, Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showPageOptions(page: OfflinePage, view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.menu_offline_page_item, popup.menu)
        
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_view -> {
                    openOfflinePage(page)
                    true
                }
                R.id.action_delete -> {
                    confirmDelete(page)
                    true
                }
                R.id.action_share -> {
                    sharePage(page)
                    true
                }
                else -> false
            }
        }
        
        popup.show()
    }
    
    private fun confirmDelete(page: OfflinePage) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_page)
            .setMessage(getString(R.string.delete_page_confirmation, page.title))
            .setPositiveButton(R.string.delete) { _, _ ->
                deletePage(page)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun deletePage(page: OfflinePage) {
        progressLoading.visibility = View.VISIBLE
        
        lifecycleScope.launch(Dispatchers.IO) {
            val success = offlinePageManager.deletePage(page.id)
            
            withContext(Dispatchers.Main) {
                progressLoading.visibility = View.GONE
                
                if (success) {
                    Toast.makeText(
                        this@OfflinePageActivity,
                        R.string.page_deleted,
                        Toast.LENGTH_SHORT
                    ).show()
                    loadOfflinePages()
                } else {
                    Toast.makeText(
                        this@OfflinePageActivity,
                        R.string.error_deleting_page,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun confirmDeleteAll() {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_all_pages)
            .setMessage(R.string.delete_all_confirmation)
            .setPositiveButton(R.string.delete_all_button) { _, _ ->
                deleteAllPages()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun deleteAllPages() {
        progressLoading.visibility = View.VISIBLE
        
        lifecycleScope.launch(Dispatchers.IO) {
            val managerPages = offlinePageManager.getAllOfflinePages()
            var successCount = 0
            
            for (page in managerPages) {
                val success = offlinePageManager.deletePage(page.id)
                if (success) successCount++
            }
            
            withContext(Dispatchers.Main) {
                progressLoading.visibility = View.GONE
                
                Toast.makeText(
                    this@OfflinePageActivity,
                    getString(R.string.pages_deleted, successCount),
                    Toast.LENGTH_SHORT
                ).show()
                
                loadOfflinePages()
            }
        }
    }
    
    private fun sharePage(page: OfflinePage) {
        // We could implement sharing the offline page as a file
        Toast.makeText(this, R.string.sharing_not_implemented, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Adapter for the offline pages list
     */
    inner class OfflinePagesAdapter(
        private val onItemClick: (OfflinePage) -> Unit,
        private val onOptionsClick: (OfflinePage, View) -> Unit
    ) : androidx.recyclerview.widget.ListAdapter<OfflinePage, OfflinePagesAdapter.ViewHolder>(
        object : androidx.recyclerview.widget.DiffUtil.ItemCallback<OfflinePage>() {
            override fun areItemsTheSame(oldItem: OfflinePage, newItem: OfflinePage): Boolean {
                return oldItem.id == newItem.id
            }
            
            override fun areContentsTheSame(oldItem: OfflinePage, newItem: OfflinePage): Boolean {
                return oldItem == newItem
            }
        }
    ) {
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val textPageTitle: TextView = itemView.findViewById(R.id.text_page_title)
            private val textPageUrl: TextView = itemView.findViewById(R.id.text_page_url)
            private val textSavedDate: TextView = itemView.findViewById(R.id.text_saved_date)
            private val imagePageThumbnail: ImageView = itemView.findViewById(R.id.image_page_thumbnail)
            private val buttonOptions: ImageButton = itemView.findViewById(R.id.button_options)
            
            fun bind(page: OfflinePage) {
                textPageTitle.text = page.title
                textPageUrl.text = page.url
                
                // Format the date
                val date = Date(page.timestamp)
                val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                textSavedDate.text = getString(R.string.saved_on_date, dateFormat.format(date))
                
                // Set click listeners
                itemView.setOnClickListener { onItemClick(page) }
                buttonOptions.setOnClickListener { v -> onOptionsClick(page, v) }
                
                // Load thumbnail if exists
                if (page.thumbnailPath != null) {
                    imagePageThumbnail.setImageURI(Uri.parse(page.thumbnailPath))
                } else {
                    imagePageThumbnail.setImageResource(R.drawable.ic_web_page)
                }
            }
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_offline_page, parent, false)
            return ViewHolder(itemView)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }
    
    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, OfflinePageActivity::class.java)
        }
    }
}