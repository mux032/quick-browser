package com.qb.browser.ui.theme

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.qb.browser.util.SettingsManager

/**
 * Manages theme application throughout the app
 */
class ThemeManager private constructor(private val context: Context) {
    
    private val settingsManager = SettingsManager.getInstance(context)
    
    companion object {
        @Volatile
        private var instance: ThemeManager? = null
        
        fun getInstance(context: Context): ThemeManager {
            return instance ?: synchronized(this) {
                instance ?: ThemeManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    /**
     * Apply the current theme to an activity
     */
    fun applyTheme(activity: Activity) {
        val isDarkTheme = settingsManager.isDarkThemeEnabled()
        
        // Set the status bar and navigation bar colors
        val window = activity.window
        val primaryDarkColorRes = settingsManager.getCurrentThemePrimaryDarkColorResId()
        val primaryDarkColor = ContextCompat.getColor(context, primaryDarkColorRes)
        window.statusBarColor = primaryDarkColor
        window.navigationBarColor = primaryDarkColor
        
        // Set the action bar color if it exists
        if (activity is AppCompatActivity) {
            activity.supportActionBar?.let { actionBar ->
                val primaryColorRes = settingsManager.getCurrentThemePrimaryColorResId()
                val primaryColor = ContextCompat.getColor(context, primaryColorRes)
                actionBar.setBackgroundDrawable(ColorDrawable(primaryColor))
            }
        }
    }
    
    /**
     * Apply theme color to a view
     */
    fun applyThemeToView(view: View, applyToBackground: Boolean = false) {
        val primaryColorRes = settingsManager.getCurrentThemePrimaryColorResId()
        val primaryColor = ContextCompat.getColor(context, primaryColorRes)
        
        if (applyToBackground) {
            view.setBackgroundColor(primaryColor)
        }
        
        when (view) {
            is FloatingActionButton -> {
                view.backgroundTintList = ColorStateList.valueOf(primaryColor)
            }
            is ImageView -> {
                val drawable = view.drawable
                if (drawable != null) {
                    val wrappedDrawable = DrawableCompat.wrap(drawable)
                    DrawableCompat.setTint(wrappedDrawable, primaryColor)
                    view.setImageDrawable(wrappedDrawable)
                }
            }
            else -> {
                ViewCompat.setBackgroundTintList(view, ColorStateList.valueOf(primaryColor))
            }
        }
    }
    
    /**
     * Get the current theme's primary color
     */
    @ColorInt
    fun getPrimaryColor(): Int {
        val primaryColorRes = settingsManager.getCurrentThemePrimaryColorResId()
        return ContextCompat.getColor(context, primaryColorRes)
    }
    
    /**
     * Get the current theme's primary dark color
     */
    @ColorInt
    fun getPrimaryDarkColor(): Int {
        val primaryDarkColorRes = settingsManager.getCurrentThemePrimaryDarkColorResId()
        return ContextCompat.getColor(context, primaryDarkColorRes)
    }
    
    /**
     * Get the current theme's accent color
     */
    @ColorInt
    fun getAccentColor(): Int {
        val accentColorRes = settingsManager.getCurrentThemeAccentColorResId()
        return ContextCompat.getColor(context, accentColorRes)
    }
    
    /**
     * Apply theme color to a drawable
     */
    fun applyThemeToDrawable(drawable: android.graphics.drawable.Drawable): android.graphics.drawable.Drawable {
        val wrappedDrawable = DrawableCompat.wrap(drawable).mutate()
        DrawableCompat.setTint(wrappedDrawable, getPrimaryColor())
        return wrappedDrawable
    }
}