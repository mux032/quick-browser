package com.quick.browser.presentation.ui.components

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.quick.browser.R
import com.quick.browser.service.SettingsService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Base activity class that applies consistent theming across all activities
 */
@AndroidEntryPoint
open class BaseActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsService: SettingsService

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_QBrowser)

        // 2. Then call super
        super.onCreate(savedInstanceState)
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        // Apply status bar color after content view is set
        applyStatusBarColor()
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        // Apply status bar color after content view is set
        applyStatusBarColor()
    }

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        super.setContentView(view, params)
        // Apply status bar color after content view is set
        applyStatusBarColor()
    }

    private fun applyStatusBarColor() {
        @Suppress("DEPRECATION")
        window.statusBarColor = ContextCompat.getColor(this, R.color.colorPrimary)
    }
}