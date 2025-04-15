package com.qb.browser.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qb.browser.R
import com.qb.browser.model.WebPage
import com.qb.browser.service.BubbleService
import com.qb.browser.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var historyViewModel: HistoryViewModel
    private lateinit var adapter: HistoryAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        
        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.history)
        
        // Initialize views
        recyclerView = findViewById(R.id.recycler_view)
        emptyView = findViewById(R.id.empty_view)
        
        // Initialize ViewModel
        historyViewModel = ViewModelProvider(this)[HistoryViewModel::class.java]
        
        // Set up RecyclerView
        setupRecyclerView()
        
        // Observe history data
        observeHistoryData()
        
        // Set up clear history button
        findViewById<Button>(R.id.btn_clear_history).setOnClickListener {
            showClearHistoryDialog()
        }
    }
    
    private fun setupRecyclerView() {
        adapter = HistoryAdapter(
            onItemClick = { webPage ->
                openPageInBubble(webPage.url)
            },
            onDeleteClick = { webPage ->
                historyViewModel.deletePage(webPage)
            }
        )
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            addItemDecoration(DividerItemDecoration(this@HistoryActivity, DividerItemDecoration.VERTICAL))
            adapter = this@HistoryActivity.adapter
        }
    }
    
    private fun observeHistoryData() {
        historyViewModel.getAllPages().observe(this) { pages ->
            if (pages.isEmpty()) {
                recyclerView.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                emptyView.visibility = View.GONE
                adapter.submitList(pages)
            }
        }
    }
    
    private fun openPageInBubble(url: String) {
        val intent = Intent(this, BubbleService::class.java).apply {
            action = BubbleService.ACTION_OPEN_URL
            putExtra(BubbleService.EXTRA_URL, url)
        }
        startService(intent)
        finish()
    }
    
    private fun showClearHistoryDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.clear_history)
            .setMessage(R.string.clear_history_confirmation)
            .setPositiveButton(R.string.yes) { _, _ ->
                historyViewModel.clearAllHistory()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    // RecyclerView Adapter for History items
    private class HistoryAdapter(
        private val onItemClick: (WebPage) -> Unit,
        private val onDeleteClick: (WebPage) -> Unit
    ) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
        
        private var items = listOf<WebPage>()
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        
        fun submitList(newItems: List<WebPage>) {
            items = newItems.sortedByDescending { it.timestamp }
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_history, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val page = items[position]
            
            holder.titleTextView.text = page.title
            holder.urlTextView.text = page.url
            holder.dateTextView.text = dateFormat.format(Date(page.timestamp))
            
            // Set Offline indicator if available offline
            holder.offlineIndicator.visibility = if (page.isAvailableOffline) View.VISIBLE else View.GONE
            
            holder.itemView.setOnClickListener {
                onItemClick(page)
            }
            
            holder.deleteButton.setOnClickListener {
                onDeleteClick(page)
            }
        }
        
        override fun getItemCount() = items.size
        
        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val titleTextView: TextView = itemView.findViewById(R.id.text_title)
            val urlTextView: TextView = itemView.findViewById(R.id.text_url)
            val dateTextView: TextView = itemView.findViewById(R.id.text_date)
            val offlineIndicator: View = itemView.findViewById(R.id.offline_indicator)
            val deleteButton: View = itemView.findViewById(R.id.btn_delete)
        }
    }
}
