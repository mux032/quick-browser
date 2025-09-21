package com.quick.browser.presentation.ui.saved

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
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
import com.quick.browser.domain.model.SavedArticlesViewStyle
import com.quick.browser.presentation.ui.reader.OfflineReaderActivity
import com.quick.browser.service.SettingsService
import com.quick.browser.utils.Logger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Activity to display saved articles for offline reading
 */
@AndroidEntryPoint
class SavedArticlesActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsService: SettingsService

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SavedArticlesAdapter
    private lateinit var searchView: SearchView
    private lateinit var searchCard: MaterialCardView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private var isSearchBarExplicitlyOpened = false
    private var currentViewStyle: SavedArticlesViewStyle = SavedArticlesViewStyle.CARD

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
        
        // Load saved view style preference
        loadSavedViewStyle()
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
            },
            viewModel = viewModel,
            lifecycleOwner = this
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
                    Logger.e(TAG, "Error loading saved articles: ${uiState.error}")
                } else {
                    // Update adapter with saved articles
                    adapter.submitList(uiState.articles)
                }
            }
        }
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
                    // Show all saved articles when search query is empty
                    observeViewModel()
                } else {
                    // Perform search with the query
                    viewModel.searchSavedArticles(newText)
                }
                return true
            }
        })

        // Handle search view close event
        searchView.setOnCloseListener {
            // Show all saved articles when search view is closed
            observeViewModel()
            false
        }

        // Handle IME options (Enter key)
        val searchEditText = searchView.findViewById<TextView>(androidx.appcompat.R.id.search_src_text)
        searchEditText?.imeOptions = EditorInfo.IME_ACTION_SEARCH
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
        
        // Set up view style selection - we need to find the included layout first
        val sidePanelInclude = navView.findViewById<View>(R.id.side_panel)
        if (sidePanelInclude is ViewGroup) {
            // Find the view style items within the included layout
            val cardViewItem = sidePanelInclude.findViewById<android.widget.LinearLayout>(R.id.card_view_item)
            val compactCardViewItem = sidePanelInclude.findViewById<android.widget.LinearLayout>(R.id.compact_card_item)
            val compactViewItem = sidePanelInclude.findViewById<android.widget.LinearLayout>(R.id.compact_item)
            val superCompactViewItem = sidePanelInclude.findViewById<android.widget.LinearLayout>(R.id.super_compact_item)
            
            cardViewItem.setOnClickListener {
                updateViewStyle(SavedArticlesViewStyle.CARD)
                drawerLayout.closeDrawer(navView)
            }
            
            compactCardViewItem.setOnClickListener {
                updateViewStyle(SavedArticlesViewStyle.COMPACT_CARD)
                drawerLayout.closeDrawer(navView)
            }
            
            compactViewItem.setOnClickListener {
                updateViewStyle(SavedArticlesViewStyle.COMPACT)
                drawerLayout.closeDrawer(navView)
            }
            
            superCompactViewItem.setOnClickListener {
                updateViewStyle(SavedArticlesViewStyle.SUPER_COMPACT)
                drawerLayout.closeDrawer(navView)
            }
        }
    }

    private fun updateViewStyle(viewStyle: SavedArticlesViewStyle) {
        currentViewStyle = viewStyle
        adapter.updateViewStyle(viewStyle)
        
        // Save the preference
        settingsService.setSavedArticlesViewStyle(viewStyle.name)
    }

    private fun loadSavedViewStyle() {
        val savedStyleName = settingsService.getSavedArticlesViewStyle()
        currentViewStyle = try {
            SavedArticlesViewStyle.valueOf(savedStyleName)
        } catch (e: IllegalArgumentException) {
            SavedArticlesViewStyle.CARD // Default to card view
        }
        adapter.updateViewStyle(currentViewStyle)
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

    private fun openArticleInReaderMode(article: SavedArticle) {
        val intent = Intent(this, OfflineReaderActivity::class.java).apply {
            putExtra(OfflineReaderActivity.EXTRA_ARTICLE_URL, article.url)
        }
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_saved_articles, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                showSearchBar()
                true
            }
            else -> super.onOptionsItemSelected(item)
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

    companion object {
        private const val TAG = "SavedArticlesActivity"
    }
}