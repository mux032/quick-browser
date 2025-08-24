package com.quick.browser.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.quick.browser.R
import com.quick.browser.domain.model.SavedArticle
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity to display saved articles for offline reading
 */
@AndroidEntryPoint
class SavedArticlesActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SavedArticlesAdapter
    
    private val viewModel: SavedArticlesViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_articles)
        
        setupToolbar()
        setupRecyclerView()
        observeViewModel()
    }
    
    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.saved_articles)
    }
    
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recycler_view_saved_articles)
        adapter = SavedArticlesAdapter(
            onItemClick = { article ->
                // Handle article click - open in reader mode
                openArticleInReaderMode(article)
            },
            onDeleteClick = { article ->
                // Handle delete click
                viewModel.deleteArticle(article)
            }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }
    
    private fun observeViewModel() {
        viewModel.savedArticles.observe(this) { articles ->
            adapter.submitList(articles)
        }
    }
    
    private fun openArticleInReaderMode(article: SavedArticle) {
        val intent = Intent(this, OfflineReaderActivity::class.java).apply {
            putExtra(OfflineReaderActivity.EXTRA_ARTICLE_TITLE, article.title)
            putExtra(OfflineReaderActivity.EXTRA_ARTICLE_CONTENT, article.content)
            putExtra(OfflineReaderActivity.EXTRA_ARTICLE_BYLINE, article.author)
            putExtra(OfflineReaderActivity.EXTRA_ARTICLE_SITE_NAME, article.siteName)
            putExtra(OfflineReaderActivity.EXTRA_ARTICLE_PUBLISH_DATE, article.publishDate)
        }
        startActivity(intent)
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}