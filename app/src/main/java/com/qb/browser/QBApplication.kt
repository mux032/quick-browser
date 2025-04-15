package com.qb.browser

import android.app.Application
import com.qb.browser.viewmodel.BubbleViewModel
import com.qb.browser.viewmodel.WebViewModel

/**
 * Application class for initializing app-wide dependencies
 */
class QBApplication : Application() {
    
    // ViewModels accessible throughout the app
    lateinit var bubbleViewModel: BubbleViewModel
    lateinit var webViewModel: WebViewModel
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize ViewModels
        bubbleViewModel = BubbleViewModel(this)
        webViewModel = WebViewModel(this)
    }
}