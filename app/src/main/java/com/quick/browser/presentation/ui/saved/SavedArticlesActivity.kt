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
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.quick.browser.R
import com.quick.browser.databinding.ActivitySavedArticlesBinding
import com.quick.browser.domain.model.SavedArticle
import com.quick.browser.domain.model.SavedArticlesViewStyle
import com.quick.browser.domain.service.ISettingsService
import com.quick.browser.presentation.ui.reader.OfflineReaderActivity
import com.quick.browser.presentation.ui.saved.viewmodel.TagViewModel
import com.quick.browser.utils.Logger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Activity to display saved articles for offline reading
 */
@AndroidEntryPoint
class SavedArticlesActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsService: ISettingsService

    private lateinit var binding: ActivitySavedArticlesBinding
    private lateinit var adapter: SavedArticlesAdapter
    private lateinit var tagsAdapter: TagsAdapter
    private var isSearchBarExplicitlyOpened = false

    private val viewModel: SavedArticlesViewModel by viewModels()
    private val tagViewModel: TagViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_QBrowser)
        super.onCreate(savedInstanceState)
        binding = ActivitySavedArticlesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.statusBarColor = ContextCompat.getColor(this, R.color.colorPrimary)

        // Set up toolbar
        setupToolbar()

        setupRecyclerView()
        setupTagsRecyclerView()
        observeViewModel()
        observeTagViewModel()

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

        // Handle back press
        setupOnBackPressed()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false) // Hide back button
        supportActionBar?.setTitle(R.string.saved_articles)

        // Ensure toolbar sits below the status bar on all devices
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { v, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.updatePadding(top = statusBarHeight)
            insets
        }

        // Set up custom toolbar buttons
        binding.toolbar.findViewById<android.widget.ImageButton>(R.id.toolbar_search)?.setOnClickListener {
            showSearchBar()
        }

        // Set up burger menu button to open side panel
        binding.toolbar.findViewById<android.widget.ImageButton>(R.id.toolbar_menu)?.setOnClickListener {
            binding.drawerLayout.openDrawer(binding.navView)
        }
    }

    private fun setupRecyclerView() {
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

        binding.recyclerViewSavedArticles.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewSavedArticles.adapter = adapter
    }

    private fun setupTagsRecyclerView() {
        tagsAdapter = TagsAdapter(
            onItemClick = { tag ->
                // Set current tag and load articles for this tag
                viewModel.setCurrentTag(tag)
                binding.drawerLayout.closeDrawer(binding.navView)
            }
        )

        binding.sidePanel.tagsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SavedArticlesActivity)
            adapter = tagsAdapter
        }
    }

    private fun observeViewModel() {
        // Observe the UI state from the ViewModel
        viewModel.uiState.onEach { uiState ->
            // Check if the UI state is Loading, Success, or Error
            if (uiState.isLoading) {
                // Show loading indicator if needed
            } else if (uiState.error != null) {
                // Show error message
                Logger.e(TAG, "Error loading saved articles: ${uiState.error}")
            } else {
                // Update adapter with saved articles
                adapter.submitList(uiState.articles)
                supportActionBar?.title = uiState.currentTag?.name ?: getString(R.string.saved_articles)
            }
        }.launchIn(lifecycleScope)
    }

    private fun observeTagViewModel() {
        // Observe tags
        tagViewModel.uiState.onEach { uiState ->
            // Update tags list
            tagsAdapter.submitList(uiState.tags)

            // Check if there's an error to display
            if (uiState.error != null) {
                Toast.makeText(this@SavedArticlesActivity, uiState.error, Toast.LENGTH_SHORT).show()
                tagViewModel.clearMessages()
            }

            // Check if there's a success message to display
            if (uiState.successMessage != null) {
                Toast.makeText(this@SavedArticlesActivity, uiState.successMessage, Toast.LENGTH_SHORT).show()
                tagViewModel.clearMessages()
            }
        }.launchIn(lifecycleScope)
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Hide keyboard when Enter is pressed but keep search bar visible
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.searchView.windowToken, 0)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    viewModel.loadArticles()
                } else {
                    // Perform search with the query
                    viewModel.searchSavedArticles(newText)
                }
                return true
            }
        })

        // Handle search view close event
        binding.searchView.setOnCloseListener {
            viewModel.loadArticles()
            false
        }

        // Handle IME options (Enter key)
        val searchEditText = binding.searchView.findViewById<TextView>(androidx.appcompat.R.id.search_src_text)
        searchEditText?.imeOptions = EditorInfo.IME_ACTION_SEARCH
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadArticles()
            // Stop the refresh animation after a short delay
            binding.swipeRefreshLayout.postDelayed({
                binding.swipeRefreshLayout.isRefreshing = false
            }, 1000)
        }

        // Set refresh colors
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.colorPrimary,
            R.color.colorAccent,
            R.color.secondaryColor
        )
    }

    private fun showSearchBar() {
        // Mark that the search bar was explicitly opened
        isSearchBarExplicitlyOpened = true

        // Show the search card
        binding.searchCard.visibility = View.VISIBLE

        // Post the focus and keyboard show to ensure the view is properly laid out
        binding.searchCard.post {
            // Focus on the search view and show keyboard
            binding.searchView.requestFocus()
            binding.searchView.isIconified = false

            // Show keyboard with a delay to ensure proper layout
            binding.searchView.post {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(binding.searchView, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    private fun closeSearchBar() {
        // Reset the explicit open flag
        isSearchBarExplicitlyOpened = false

        // Hide the search card
        binding.searchCard.visibility = View.GONE

        // Clear search query
        binding.searchView.setQuery("", false)

        // Hide keyboard
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchView.windowToken, 0)

        viewModel.loadArticles()
    }

    private fun hideSearchBar() {
        // Hide keyboard
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchView.windowToken, 0)

        // Only hide the search bar if it's not focused (user explicitly closed it) or if it wasn't explicitly opened
        if ((!binding.searchView.hasFocus() && !isSearchBarExplicitlyOpened) || binding.searchView.query.isNullOrEmpty()) {
            binding.searchCard.visibility = View.GONE
            isSearchBarExplicitlyOpened = false
            // Clear search query
            binding.searchView.setQuery("", false)
            viewModel.loadArticles()
        }
    }

    private fun showAddTagDialog() {
        AddTagDialogFragment().show(supportFragmentManager, "AddTagDialogFragment")
    }

    private fun updateViewStyle(viewStyle: SavedArticlesViewStyle) {
        adapter.updateViewStyle(viewStyle)
        // Save the preference
        settingsService.setSavedArticlesViewStyle(viewStyle.name)
    }

    private fun loadSavedViewStyle() {
        val savedStyleName = settingsService.getSavedArticlesViewStyle()
        val currentViewStyle = try {
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
            val layoutParams = binding.searchCard.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            layoutParams.bottomMargin = if (keypadHeight > screenHeight * 0.15) {
                // Keyboard is visible, position above it
                keypadHeight + 32 // Add some padding
            } else {
                // Keyboard is hidden, use default margin
                32
            }
            binding.searchCard.layoutParams = layoutParams

            // If keyboard is hidden AND search view is not focused AND search bar wasn't explicitly opened, hide it
            if (keypadHeight < screenHeight * 0.15 && !binding.searchView.hasFocus() && !isSearchBarExplicitlyOpened) {
                if (binding.searchCard.visibility == View.VISIBLE && binding.searchView.query.isNullOrEmpty()) {
                    binding.searchCard.visibility = View.GONE
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

    private fun setupOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // If search bar is visible, close it instead of closing the activity
                if (binding.searchCard.visibility == View.VISIBLE) {
                    closeSearchBar()
                } else if (binding.drawerLayout.isDrawerOpen(binding.navView)) {
                    // If drawer is open, close it
                    binding.drawerLayout.closeDrawer(binding.navView)
                } else if (viewModel.uiState.value.currentTag != null) {
                    // If we're viewing a folder, go back to all articles
                    viewModel.setCurrentTag(null)
                } else {
                    // If none of the above, perform the default back press action
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun setupSidePanel() {
        // Set up close button in side panel
        binding.sidePanel.closeButton.setOnClickListener {
            binding.drawerLayout.closeDrawer(binding.navView)
        }

        // Set up add tag button
        binding.sidePanel.addTagButton.setOnClickListener {
            showAddTagDialog()
        }

        // Set up all articles item
        binding.sidePanel.allArticlesItem.setOnClickListener {
            // Show all articles
            viewModel.setCurrentTag(null)
            binding.drawerLayout.closeDrawer(binding.navView)
        }

        // Set up view style selection
        binding.sidePanel.cardViewItem.setOnClickListener {
            updateViewStyle(SavedArticlesViewStyle.CARD)
            binding.drawerLayout.closeDrawer(binding.navView)
        }

        binding.sidePanel.compactCardItem.setOnClickListener {
            updateViewStyle(SavedArticlesViewStyle.COMPACT_CARD)
            binding.drawerLayout.closeDrawer(binding.navView)
        }

        binding.sidePanel.compactItem.setOnClickListener {
            updateViewStyle(SavedArticlesViewStyle.COMPACT)
            binding.drawerLayout.closeDrawer(binding.navView)
        }

        binding.sidePanel.superCompactItem.setOnClickListener {
            updateViewStyle(SavedArticlesViewStyle.SUPER_COMPACT)
            binding.drawerLayout.closeDrawer(binding.navView)
        }
    }

    companion object {
        private const val TAG = "SavedArticlesActivity"
    }
}