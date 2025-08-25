package com.quick.browser.presentation.ui.saved

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.quick.browser.R
import com.quick.browser.domain.model.SavedArticle
import com.quick.browser.presentation.ui.reader.OfflineReaderActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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
        // Observe the UI state from the ViewModel
        lifecycleScope.launch {
            viewModel.uiState.collect { uiState ->
                // Check if the UI state is Loading, Success, or Error
                if (uiState.isLoading) {
                    // Show loading indicator if needed
                } else if (uiState.error != null) {
                    // Show error message
                } else {
                    // Submit the articles to the adapter
                    adapter.submitList(uiState.articles)
                }
            }
        }
    }

    private fun openArticleInReaderMode(article: SavedArticle) {
        val intent = Intent(this, OfflineReaderActivity::class.java).apply {
            putExtra(OfflineReaderActivity.Companion.EXTRA_ARTICLE_TITLE, article.title)
            putExtra(OfflineReaderActivity.Companion.EXTRA_ARTICLE_CONTENT, article.content)
            putExtra(OfflineReaderActivity.Companion.EXTRA_ARTICLE_BYLINE, article.author)
            putExtra(OfflineReaderActivity.Companion.EXTRA_ARTICLE_SITE_NAME, article.siteName)
            putExtra(OfflineReaderActivity.Companion.EXTRA_ARTICLE_PUBLISH_DATE, article.publishDate)
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