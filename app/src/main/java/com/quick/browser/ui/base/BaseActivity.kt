package com.quick.browser.ui.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.quick.browser.R
import com.quick.browser.manager.SettingsManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Base activity class that applies consistent theming across all activities
 */
@AndroidEntryPoint
open class BaseActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsManager: SettingsManager

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

    override fun setContentView(view: android.view.View?) {
        super.setContentView(view)
        // Apply status bar color after content view is set
        applyStatusBarColor()
    }

    override fun setContentView(view: android.view.View?, params: android.view.ViewGroup.LayoutParams?) {
        super.setContentView(view, params)
        // Apply status bar color after content view is set
        applyStatusBarColor()
    }

    private fun applyStatusBarColor() {
        @Suppress("DEPRECATION")
        window.statusBarColor = ContextCompat.getColor(this, R.color.colorPrimary)
    }
}