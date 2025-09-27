package com.quick.browser.presentation.ui.history

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.quick.browser.R
import com.quick.browser.databinding.ActivityHistoryBinding
import com.quick.browser.domain.repository.HistoryRepository
import com.quick.browser.domain.usecase.GetHistoryUseCase
import com.quick.browser.domain.usecase.SearchHistoryUseCase
import com.quick.browser.presentation.ui.browser.OfflineArticleSaver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class HistoryActivity : AppCompatActivity() {

    @Inject
    lateinit var offlineArticleSaver: OfflineArticleSaver

    @Inject
    lateinit var getHistoryUseCase: GetHistoryUseCase

    @Inject
    lateinit var searchHistoryUseCase: SearchHistoryUseCase

    @Inject
    lateinit var historyRepository: HistoryRepository

    companion object {
        private const val TAG = "HistoryActivity"
        const val EXTRA_SELECTED_URL = "selected_url"
    }

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var historyViewModel: HistoryViewModel
    private lateinit var historyAdapter: HistoryAdapter
    private var isSearchBarExplicitlyOpened = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_QBrowser)
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.statusBarColor = ContextCompat.getColor(this, R.color.colorPrimary)

        // Set up toolbar
        setupToolbar()

        // Initialize ViewModel
        historyViewModel = ViewModelProvider(this)[HistoryViewModel::class.java]

        // Set up RecyclerView
        setupRecyclerView()

        // Set up search functionality
        setupSearchView()

        // Set up swipe refresh
        setupSwipeRefresh()

        // Observe history data
        observeHistoryData()

        // Listen for keyboard visibility changes
        setupKeyboardVisibilityListener()

        // Handle back press
        setupOnBackPressed()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false) // Hide back button
        supportActionBar?.setDisplayShowTitleEnabled(false) // Hide default title

        // Ensure toolbar sits below the status bar on all devices
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { v, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.updatePadding(top = statusBarHeight)
            insets
        }

        // Set up custom toolbar buttons
        binding.toolbar.findViewById<ImageButton>(R.id.toolbar_search).setOnClickListener {
            showSearchBar()
        }

        binding.toolbar.findViewById<ImageButton>(R.id.toolbar_delete).setOnClickListener {
            showDeleteOptionsDialog()
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(
            onItemClick = { webPage ->
                returnSelectedUrl(webPage.url)
            },
            onItemLongClick = { webPage ->
                historyViewModel.deletePage(webPage)
                Toast.makeText(this, "\"${webPage.title}\" deleted from history", Toast.LENGTH_SHORT).show()
            },
            offlineArticleSaver = offlineArticleSaver
        )

        binding.historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = this@HistoryActivity.historyAdapter
        }
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
                    // Show all history
                    observeHistoryData()
                } else {
                    // Search history
                    searchHistory(newText)
                }
                return true
            }
        })

        // Handle close button click
        binding.searchView.setOnCloseListener {
            closeSearchBar()
            true
        }

        // Also handle Enter key from the search view's text field
        binding.searchView.findViewById<androidx.appcompat.widget.SearchView.SearchAutoComplete>(
            androidx.appcompat.R.id.search_src_text
        ).setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // Hide keyboard when Enter is pressed but keep search bar visible
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.searchView.windowToken, 0)
                true
            } else {
                false
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            // Refresh the history data
            observeHistoryData()

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

    private fun searchHistory(query: String) {
        historyViewModel.searchHistory(query)
        // Observe the search results directly from the use case
        searchHistoryUseCase(query).observe(this) { pages ->
            if (pages.isEmpty()) {
                binding.historyRecyclerView.visibility = View.GONE
                binding.emptyHistoryView.visibility = View.VISIBLE
            } else {
                binding.historyRecyclerView.visibility = View.VISIBLE
                binding.emptyHistoryView.visibility = View.GONE
                historyAdapter.submitList(pages)
            }
        }
    }

    private fun observeHistoryData() {
        // Observe all pages from the use case
        getHistoryUseCase().observe(this) { pages ->
            if (pages.isEmpty()) {
                binding.historyRecyclerView.visibility = View.GONE
                binding.emptyHistoryView.visibility = View.VISIBLE
            } else {
                binding.historyRecyclerView.visibility = View.VISIBLE
                binding.emptyHistoryView.visibility = View.GONE
                historyAdapter.submitList(pages)
            }
        }
    }

    private fun returnSelectedUrl(url: String) {
        val resultIntent = Intent().apply {
            putExtra(EXTRA_SELECTED_URL, url)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
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

        // Show all history
        observeHistoryData()
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
            // Show all history
            observeHistoryData()
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

    private fun showDeleteOptionsDialog() {
        // Create custom dialog
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_options, null)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.delete_options_radio_group)
        val cancelButton = dialogView.findViewById<Button>(R.id.button_cancel)
        val deleteButton = dialogView.findViewById<Button>(R.id.button_delete)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Set default selection
        radioGroup.check(R.id.radio_delete_last_hour)

        // Set up button listeners
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        deleteButton.setOnClickListener {
            val selectedRadioButtonId = radioGroup.checkedRadioButtonId
            when (selectedRadioButtonId) {
                R.id.radio_delete_last_hour -> {
                    // Delete history from last hour
                    val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000)
                    CoroutineScope(Dispatchers.IO).launch {
                        historyRepository.deleteLastHourPages(oneHourAgo)
                    }
                    Toast.makeText(this, "Last hour history deleted", Toast.LENGTH_SHORT).show()
                }
                R.id.radio_delete_today_yesterday -> {
                    // Delete history from today and yesterday
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.DAY_OF_YEAR, -1) // Yesterday
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val yesterdayStart = calendar.timeInMillis

                    CoroutineScope(Dispatchers.IO).launch {
                        historyRepository.deleteTodayPages(yesterdayStart)
                    }
                    Toast.makeText(this, "Today and yesterday history deleted", Toast.LENGTH_SHORT).show()
                }
                R.id.radio_delete_everything -> {
                    // Delete all history
                    CoroutineScope(Dispatchers.IO).launch {
                        historyRepository.deleteAllPages()
                    }
                    Toast.makeText(this, "All history deleted", Toast.LENGTH_SHORT).show()
                }
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // If search bar is visible, close it instead of closing the activity
                if (binding.searchCard.visibility == View.VISIBLE) {
                    closeSearchBar()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }
}