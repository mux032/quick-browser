package com.quick.browser.presentation.ui.saved

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigation.NavigationView
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
    private lateinit var searchView: SearchView
    private lateinit var searchCard: MaterialCardView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private var isSearchBarExplicitlyOpened = false

    private val viewModel: SavedArticlesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_articles)

        // Set up toolbar
        setupToolbar()
        
        // Set up drawer layout and navigation view
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        setupRecyclerView()
        observeViewModel()
        
        // Initialize views
        searchView = findViewById(R.id.search_view)
        searchCard = findViewById(R.id.search_card)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)
        
        // Set up search functionality
        setupSearchView()
        
        // Set up swipe refresh
        setupSwipeRefresh()
        
        // Listen for keyboard visibility changes
        setupKeyboardVisibilityListener()
        
        // Set up side panel interactions
        setupSidePanel()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false) // Hide back button
        supportActionBar?.setTitle(R.string.saved_articles)
        
        // Ensure toolbar sits below the status bar on all devices
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.updatePadding(top = statusBarHeight)
            insets
        }
        
        // Set up custom toolbar buttons
        val searchButton = toolbar.findViewById<android.widget.ImageButton>(R.id.toolbar_search)
        searchButton?.setOnClickListener {
            showSearchBar()
        }

        // Set up burger menu button to open side panel
        val menuButton = toolbar.findViewById<android.widget.ImageButton>(R.id.toolbar_menu)
        menuButton?.setOnClickListener {
            drawerLayout.openDrawer(navView)
        }
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return true
    }
    
    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Hide keyboard when Enter is pressed but keep search bar visible
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(searchView.windowToken, 0)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    // Show all saved articles
                    observeViewModel()
                } else {
                    // Search saved articles
                    viewModel.searchSavedArticles(newText)
                }
                return true
            }
        })
        
        // Handle close button click
        searchView.setOnCloseListener {
            closeSearchBar()
            true
        }
        
        // Also handle Enter key from the search view's text field
        searchView.findViewById<androidx.appcompat.widget.SearchView.SearchAutoComplete>(
            androidx.appcompat.R.id.search_src_text
        ).setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // Hide keyboard when Enter is pressed but keep search bar visible
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(searchView.windowToken, 0)
                true
            } else {
                false
            }
        }
    }
    
    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            // Refresh the saved articles data
            observeViewModel()

            // Stop the refresh animation after a short delay
            swipeRefreshLayout.postDelayed({
                swipeRefreshLayout.isRefreshing = false
            }, 1000)
        }

        // Set refresh colors
        swipeRefreshLayout.setColorSchemeResources(
            R.color.colorPrimary,
            R.color.colorAccent,
            R.color.secondaryColor
        )
    }
    
    private fun showSearchBar() {
        // Mark that the search bar was explicitly opened
        isSearchBarExplicitlyOpened = true
        
        // Show the search card
        searchCard.visibility = View.VISIBLE
        
        // Post the focus and keyboard show to ensure the view is properly laid out
        searchCard.post {
            // Focus on the search view and show keyboard
            searchView.requestFocus()
            searchView.isIconified = false
            
            // Show keyboard with a delay to ensure proper layout
            searchView.post {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }
    
    private fun closeSearchBar() {
        // Reset the explicit open flag
        isSearchBarExplicitlyOpened = false
        
        // Hide the search card
        searchCard.visibility = View.GONE
        
        // Clear search query
        searchView.setQuery("", false)
        
        // Hide keyboard
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchView.windowToken, 0)
        
        // Show all saved articles
        observeViewModel()
    }
    
    private fun hideSearchBar() {
        // Hide keyboard
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchView.windowToken, 0)
        
        // Only hide the search bar if it's not focused (user explicitly closed it) or if it wasn't explicitly opened
        if ((!searchView.hasFocus() && !isSearchBarExplicitlyOpened) || searchView.query.isNullOrEmpty()) {
            searchCard.visibility = View.GONE
            isSearchBarExplicitlyOpened = false
            // Clear search query
            searchView.setQuery("", false)
            // Show all saved articles
            observeViewModel()
        }
    }
    
    private fun setupSidePanel() {
        // Set up close button in side panel
        val closeButton = navView.findViewById<android.widget.ImageButton>(R.id.close_button)
        closeButton.setOnClickListener {
            drawerLayout.closeDrawer(navView)
        }
        
        // Set up add folder button
        val addFolderButton = navView.findViewById<android.widget.ImageButton>(R.id.add_folder_button)
        addFolderButton.setOnClickListener {
            // TODO: Implement add folder functionality
        }
        
        // Set up all articles item
        val allArticlesItem = navView.findViewById<android.widget.LinearLayout>(R.id.all_articles_item)
        allArticlesItem.setOnClickListener {
            // TODO: Implement show all articles functionality
            drawerLayout.closeDrawer(navView)
        }
    }
    
    private fun setupKeyboardVisibilityListener() {
        // Listen for keyboard visibility changes
        window.decorView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            window.decorView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = window.decorView.height
            val keypadHeight = screenHeight - rect.bottom
            
            // Update search card bottom margin to position it above keyboard
            val layoutParams = searchCard.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            layoutParams.bottomMargin = if (keypadHeight > screenHeight * 0.15) {
                // Keyboard is visible, position above it
                keypadHeight + 32 // Add some padding
            } else {
                // Keyboard is hidden, use default margin
                32
            }
            searchCard.layoutParams = layoutParams
            
            // If keyboard is hidden AND search view is not focused AND search bar wasn't explicitly opened, hide it
            if (keypadHeight < screenHeight * 0.15 && !searchView.hasFocus() && !isSearchBarExplicitlyOpened) {
                if (searchCard.visibility == View.VISIBLE && searchView.query.isNullOrEmpty()) {
                    searchCard.visibility = View.GONE
                }
            }
        }
    }
    
    override fun onBackPressed() {
        // If search bar is visible, close it instead of closing the activity
        if (searchCard.visibility == View.VISIBLE) {
            closeSearchBar()
        } else {
            super.onBackPressed()
        }
    }
}