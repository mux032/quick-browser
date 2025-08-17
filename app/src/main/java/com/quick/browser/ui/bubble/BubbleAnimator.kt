package com.quick.browser.ui.bubble

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator

/**
 * Handles all animations for the bubble UI
 */
class BubbleAnimator(private val context: Context) {

    /**
     * Animate expanding a view with a scale and fade effect
     */
    fun animateExpand(view: View, onEnd: (() -> Unit)? = null) {
        view.alpha = 0f
        view.scaleX = 0.8f
        view.scaleY = 0.8f
        view.visibility = View.VISIBLE
        
        val animSet = AnimatorSet()
        val alphaAnim = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        val scaleXAnim = ObjectAnimator.ofFloat(view, "scaleX", 0.8f, 1f)
        val scaleYAnim = ObjectAnimator.ofFloat(view, "scaleY", 0.8f, 1f)
        
        animSet.playTogether(alphaAnim, scaleXAnim, scaleYAnim)
        animSet.duration = ANIMATION_DURATION_MEDIUM
        animSet.interpolator = OvershootInterpolator(1.2f)
        
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onEnd?.invoke()
            }
        })
        
        animSet.start()
    }
    
    /**
     * Animate collapsing a view with a scale and fade effect
     */
    fun animateCollapse(view: View, onEnd: (() -> Unit)? = null) {
        val animSet = AnimatorSet()
        val alphaAnim = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)
        val scaleXAnim = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.8f)
        val scaleYAnim = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.8f)
        
        animSet.playTogether(alphaAnim, scaleXAnim, scaleYAnim)
        animSet.duration = ANIMATION_DURATION_MEDIUM
        animSet.interpolator = AccelerateInterpolator()
        
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                view.visibility = View.GONE
                onEnd?.invoke()
            }
        })
        
        animSet.start()
    }
    
    /**
     * Animate collapsing expanded container to bubble with smooth transition
     * This creates a gradual transition similar to expansion but in reverse
     */
    fun animateCollapseTobubble(
        expandedContainer: View,
        urlBarContainer: View,
        bubbleContainer: View,
        onEnd: (() -> Unit)? = null
    ) {
        // Phase 1: Start collapsing the expanded container
        val collapseAnimSet = AnimatorSet()
        val alphaAnim = ObjectAnimator.ofFloat(expandedContainer, "alpha", 1f, 0f)
        val scaleXAnim = ObjectAnimator.ofFloat(expandedContainer, "scaleX", 1f, 0.3f)
        val scaleYAnim = ObjectAnimator.ofFloat(expandedContainer, "scaleY", 1f, 0.3f)
        
        collapseAnimSet.playTogether(alphaAnim, scaleXAnim, scaleYAnim)
        collapseAnimSet.duration = ANIMATION_DURATION_MEDIUM
        collapseAnimSet.interpolator = AccelerateInterpolator()
        
        collapseAnimSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                // Hide URL bar immediately when animation starts to prevent flash
                urlBarContainer.visibility = View.GONE
            }
            
            override fun onAnimationEnd(animation: Animator) {
                // Hide expanded container completely
                expandedContainer.visibility = View.GONE
                
                // Reset expanded container properties for next time
                expandedContainer.scaleX = 1f
                expandedContainer.scaleY = 1f
                expandedContainer.alpha = 1f
                
                // Phase 2: Show bubble container with scale-up animation
                bubbleContainer.alpha = 0f
                bubbleContainer.scaleX = 0.3f
                bubbleContainer.scaleY = 0.3f
                bubbleContainer.visibility = View.VISIBLE
                
                // Animate bubble appearing with smooth scale-up effect
                val bubbleAppearAnim = AnimatorSet()
                val bubbleAlphaAnim = ObjectAnimator.ofFloat(bubbleContainer, "alpha", 0f, 1f)
                val bubbleScaleXAnim = ObjectAnimator.ofFloat(bubbleContainer, "scaleX", 0.3f, 1f)
                val bubbleScaleYAnim = ObjectAnimator.ofFloat(bubbleContainer, "scaleY", 0.3f, 1f)
                
                bubbleAppearAnim.playTogether(bubbleAlphaAnim, bubbleScaleXAnim, bubbleScaleYAnim)
                bubbleAppearAnim.duration = ANIMATION_DURATION_MEDIUM
                bubbleAppearAnim.interpolator = OvershootInterpolator(1.2f)
                
                bubbleAppearAnim.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        onEnd?.invoke()
                    }
                })
                
                bubbleAppearAnim.start()
            }
        })
        
        collapseAnimSet.start()
    }
    
    /**
     * Animate expanding from bubble to expanded container with smooth transition
     * This creates a gradual transition similar to collapse but in reverse
     */
    fun animateExpandFromBubble(
        bubbleContainer: View,
        urlBarContainer: View,
        expandedContainer: View,
        onEnd: (() -> Unit)? = null
    ) {
        // Phase 1: Start shrinking the bubble container
        val bubbleCollapseAnim = AnimatorSet()
        val bubbleAlphaAnim = ObjectAnimator.ofFloat(bubbleContainer, "alpha", 1f, 0f)
        val bubbleScaleXAnim = ObjectAnimator.ofFloat(bubbleContainer, "scaleX", 1f, 0.3f)
        val bubbleScaleYAnim = ObjectAnimator.ofFloat(bubbleContainer, "scaleY", 1f, 0.3f)
        
        bubbleCollapseAnim.playTogether(bubbleAlphaAnim, bubbleScaleXAnim, bubbleScaleYAnim)
        bubbleCollapseAnim.duration = ANIMATION_DURATION_MEDIUM
        bubbleCollapseAnim.interpolator = AccelerateInterpolator()
        
        bubbleCollapseAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // Hide bubble container and show URL bar
                bubbleContainer.visibility = View.GONE
                urlBarContainer.visibility = View.VISIBLE
                
                // Reset bubble container properties for next time
                bubbleContainer.scaleX = 1f
                bubbleContainer.scaleY = 1f
                bubbleContainer.alpha = 1f
                
                // Phase 2: Show expanded container with scale-up animation
                expandedContainer.alpha = 0f
                expandedContainer.scaleX = 0.3f
                expandedContainer.scaleY = 0.3f
                expandedContainer.visibility = View.VISIBLE
                
                // Animate expanded container appearing with smooth scale-up effect
                val expandAnimSet = AnimatorSet()
                val expandAlphaAnim = ObjectAnimator.ofFloat(expandedContainer, "alpha", 0f, 1f)
                val expandScaleXAnim = ObjectAnimator.ofFloat(expandedContainer, "scaleX", 0.3f, 1f)
                val expandScaleYAnim = ObjectAnimator.ofFloat(expandedContainer, "scaleY", 0.3f, 1f)
                
                expandAnimSet.playTogether(expandAlphaAnim, expandScaleXAnim, expandScaleYAnim)
                expandAnimSet.duration = ANIMATION_DURATION_MEDIUM
                expandAnimSet.interpolator = OvershootInterpolator(1.1f)
                
                expandAnimSet.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        onEnd?.invoke()
                    }
                })
                
                expandAnimSet.start()
            }
        })
        
        bubbleCollapseAnim.start()
    }
    
    /**
     * Animate bubble appearing with a scale up effect
     */
    fun animateAppear(view: View, onEnd: (() -> Unit)? = null) {
        view.alpha = 0f
        view.scaleX = 0f
        view.scaleY = 0f
        
        val animSet = AnimatorSet()
        val alphaAnim = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        val scaleXAnim = ObjectAnimator.ofFloat(view, "scaleX", 0f, 1f)
        val scaleYAnim = ObjectAnimator.ofFloat(view, "scaleY", 0f, 1f)
        
        animSet.playTogether(alphaAnim, scaleXAnim, scaleYAnim)
        animSet.duration = ANIMATION_DURATION_LONG
        animSet.interpolator = BounceInterpolator()
        
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onEnd?.invoke()
            }
        })
        
        animSet.start()
    }
    
    /**
     * Animate bubble disappearing with a scale down effect
     */
    fun animateDisappear(view: View, onEnd: (() -> Unit)? = null) {
        val animSet = AnimatorSet()
        val alphaAnim = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)
        val scaleXAnim = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0f)
        val scaleYAnim = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0f)
        
        animSet.playTogether(alphaAnim, scaleXAnim, scaleYAnim)
        animSet.duration = ANIMATION_DURATION_MEDIUM
        animSet.interpolator = AccelerateInterpolator()
        
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onEnd?.invoke()
            }
        })
        
        animSet.start()
    }
    
    /**
     * Animate a bounce effect on a view
     * @param isExpand true for expansion bounce, false for collapse
     */
    fun animateBounce(view: View, isExpand: Boolean) {
        // Use a more moderate scale to prevent clipping
        val fromScale = if (isExpand) 1f else 1.05f
        val toScale = if (isExpand) 1.05f else 1f
        
        val animSet = AnimatorSet()
        val scaleXAnim = ObjectAnimator.ofFloat(view, "scaleX", fromScale, toScale)
        val scaleYAnim = ObjectAnimator.ofFloat(view, "scaleY", fromScale, toScale)
        
        animSet.playTogether(scaleXAnim, scaleYAnim)
        animSet.duration = ANIMATION_DURATION_SHORT
        animSet.interpolator = if (isExpand) OvershootInterpolator(1.5f) else DecelerateInterpolator()
        
        // Animate back to original size after the bounce
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (isExpand) {
                    val returnAnim = AnimatorSet()
                    val returnScaleXAnim = ObjectAnimator.ofFloat(view, "scaleX", toScale, 1f)
                    val returnScaleYAnim = ObjectAnimator.ofFloat(view, "scaleY", toScale, 1f)
                    
                    returnAnim.playTogether(returnScaleXAnim, returnScaleYAnim)
                    returnAnim.duration = ANIMATION_DURATION_SHORT
                    returnAnim.interpolator = DecelerateInterpolator()
                    returnAnim.start()
                }
            }
        })
        
        animSet.start()
    }
    
    /**
     * Animate a pulse effect (grow and shrink) on a view
     * @param repeatCount number of times to repeat the pulse, -1 for infinite
     */
    fun animatePulse(view: View, repeatCount: Int = 1) {
        val scaleXAnim = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.2f, 1f)
        val scaleYAnim = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.2f, 1f)
        
        val animSet = AnimatorSet()
        animSet.playTogether(scaleXAnim, scaleYAnim)
        animSet.duration = ANIMATION_DURATION_MEDIUM
        animSet.interpolator = AccelerateDecelerateInterpolator()
        
        if (repeatCount != 0) {
            scaleXAnim.repeatCount = repeatCount
            scaleYAnim.repeatCount = repeatCount
        }
        
        animSet.start()
    }
    
    /**
     * Animate a view moving from one position to another
     */
    fun animateMove(view: View, fromX: Float, fromY: Float, toX: Float, toY: Float, onEnd: (() -> Unit)? = null) {
        view.translationX = fromX
        view.translationY = fromY
        
        val translateXAnim = ObjectAnimator.ofFloat(view, "translationX", fromX, toX)
        val translateYAnim = ObjectAnimator.ofFloat(view, "translationY", fromY, toY)
        
        val animSet = AnimatorSet()
        animSet.playTogether(translateXAnim, translateYAnim)
        animSet.duration = ANIMATION_DURATION_MEDIUM
        animSet.interpolator = DecelerateInterpolator()
        
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onEnd?.invoke()
            }
        })
        
        animSet.start()
    }
    
    /**
     * Animate a view shaking (for error feedback)
     */
    fun animateShake(view: View) {
        val animator = ObjectAnimator.ofFloat(view, "translationX", 0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f)
        animator.duration = ANIMATION_DURATION_MEDIUM
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
    }
    
    /**
     * Animate a tab being added to the tab list
     */
    fun animateTabAdded(view: View) {
        view.alpha = 0f
        view.translationY = -50f
        
        val animSet = AnimatorSet()
        val alphaAnim = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        val translateAnim = ObjectAnimator.ofFloat(view, "translationY", -50f, 0f)
        
        animSet.playTogether(alphaAnim, translateAnim)
        animSet.duration = ANIMATION_DURATION_MEDIUM
        animSet.interpolator = DecelerateInterpolator()
        animSet.start()
    }
    
    /**
     * Animate a tab being removed from the tab list
     */
    fun animateTabRemoved(view: View, onEnd: (() -> Unit)? = null) {
        val animSet = AnimatorSet()
        val alphaAnim = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)
        val translateAnim = ObjectAnimator.ofFloat(view, "translationX", 0f, view.width.toFloat())
        
        animSet.playTogether(alphaAnim, translateAnim)
        animSet.duration = ANIMATION_DURATION_MEDIUM
        animSet.interpolator = AccelerateInterpolator()
        
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onEnd?.invoke()
            }
        })
        
        animSet.start()
    }
    
    /**
     * Animate expanded bubble closing with graceful scale-down effect
     * This provides visual feedback that the bubble was intentionally closed, not crashed
     */
    fun animateExpandedBubbleClose(
        urlBarContainer: View,
        expandedContainer: View,
        bubbleContainer: View,
        onEnd: (() -> Unit)? = null
    ) {
        // Create simultaneous animations for both URL bar and expanded container
        val urlBarAnimSet = AnimatorSet()
        val urlBarAlphaAnim = ObjectAnimator.ofFloat(urlBarContainer, "alpha", 1f, 0f)
        val urlBarScaleXAnim = ObjectAnimator.ofFloat(urlBarContainer, "scaleX", 1f, 0f)
        val urlBarScaleYAnim = ObjectAnimator.ofFloat(urlBarContainer, "scaleY", 1f, 0f)
        
        urlBarAnimSet.playTogether(urlBarAlphaAnim, urlBarScaleXAnim, urlBarScaleYAnim)
        urlBarAnimSet.duration = ANIMATION_DURATION_MEDIUM
        urlBarAnimSet.interpolator = AccelerateInterpolator()
        
        val expandedAnimSet = AnimatorSet()
        val expandedAlphaAnim = ObjectAnimator.ofFloat(expandedContainer, "alpha", 1f, 0f)
        val expandedScaleXAnim = ObjectAnimator.ofFloat(expandedContainer, "scaleX", 1f, 0f)
        val expandedScaleYAnim = ObjectAnimator.ofFloat(expandedContainer, "scaleY", 1f, 0f)
        
        expandedAnimSet.playTogether(expandedAlphaAnim, expandedScaleXAnim, expandedScaleYAnim)
        expandedAnimSet.duration = ANIMATION_DURATION_MEDIUM
        expandedAnimSet.interpolator = AccelerateInterpolator()
        
        // Start both animations simultaneously
        val masterAnimSet = AnimatorSet()
        masterAnimSet.playTogether(urlBarAnimSet, expandedAnimSet)
        
        masterAnimSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // Hide the containers after animation completes
                urlBarContainer.visibility = View.GONE
                expandedContainer.visibility = View.GONE
                bubbleContainer.visibility = View.INVISIBLE
                
                // Reset properties for potential future use
                urlBarContainer.alpha = 1f
                urlBarContainer.scaleX = 1f
                urlBarContainer.scaleY = 1f
                expandedContainer.alpha = 1f
                expandedContainer.scaleX = 1f
                expandedContainer.scaleY = 1f
                
                onEnd?.invoke()
            }
        })
        
        masterAnimSet.start()
    }
    
    // ======================================
    // Settings Panel Animations
    // ======================================
    
    /**
     * Animate settings panel appearing with dropdown effect
     * 
     * @param panel The settings panel view to animate
     * @param fromButton The button that triggered the panel (for positioning)
     * @param onEnd Callback to invoke when animation completes
     */
    fun animateSettingsPanelShow(panel: View, fromButton: View? = null, onEnd: (() -> Unit)? = null) {
        // Set initial state
        panel.alpha = 0f
        panel.scaleX = 0.3f
        panel.scaleY = 0.3f
        panel.translationY = -50f
        panel.visibility = View.VISIBLE
        
        // Set pivot point based on button position if provided
        fromButton?.let { button ->
            panel.pivotX = button.x + (button.width / 2f)
            panel.pivotY = 0f
        } ?: run {
            // Default to top-right corner
            panel.pivotX = panel.width * 0.9f
            panel.pivotY = 0f
        }
        
        val animSet = AnimatorSet()
        val alphaAnim = ObjectAnimator.ofFloat(panel, "alpha", 0f, 1f)
        val scaleXAnim = ObjectAnimator.ofFloat(panel, "scaleX", 0.3f, 1f)
        val scaleYAnim = ObjectAnimator.ofFloat(panel, "scaleY", 0.3f, 1f)
        val translateYAnim = ObjectAnimator.ofFloat(panel, "translationY", -50f, 0f)
        
        animSet.playTogether(alphaAnim, scaleXAnim, scaleYAnim, translateYAnim)
        animSet.duration = ANIMATION_DURATION_MEDIUM
        animSet.interpolator = OvershootInterpolator(1.1f)
        
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onEnd?.invoke()
            }
        })
        
        animSet.start()
    }
    
    /**
     * Animate settings panel hiding with upward collapse effect
     * 
     * @param panel The settings panel view to animate
     * @param onEnd Callback to invoke when animation completes
     */
    fun animateSettingsPanelHide(panel: View, onEnd: (() -> Unit)? = null) {
        // Set pivot to maintain dropdown effect
        panel.pivotX = panel.width * 0.9f
        panel.pivotY = 0f
        
        val animSet = AnimatorSet()
        val alphaAnim = ObjectAnimator.ofFloat(panel, "alpha", 1f, 0f)
        val scaleXAnim = ObjectAnimator.ofFloat(panel, "scaleX", 1f, 0.3f)
        val scaleYAnim = ObjectAnimator.ofFloat(panel, "scaleY", 1f, 0.3f)
        val translateYAnim = ObjectAnimator.ofFloat(panel, "translationY", 0f, -30f)
        
        animSet.playTogether(alphaAnim, scaleXAnim, scaleYAnim, translateYAnim)
        animSet.duration = ANIMATION_DURATION_SHORT
        animSet.interpolator = AccelerateInterpolator()
        
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                panel.visibility = View.GONE
                // Reset properties for next time
                panel.translationY = 0f
                panel.scaleX = 1f
                panel.scaleY = 1f
                panel.alpha = 1f
                onEnd?.invoke()
            }
        })
        
        animSet.start()
    }
    
    // ======================================
    // Mode Transition Animations
    // ======================================
    
    /**
     * Animate transition to read mode with content fade effect
     * 
     * @param webView The web view to fade out
     * @param readModeContainer The read mode container to fade in
     * @param onEnd Callback to invoke when animation completes
     */
    fun animateToReadMode(webView: View, readModeContainer: View, onEnd: (() -> Unit)? = null) {
        // Prepare read mode container
        readModeContainer.alpha = 0f
        readModeContainer.translationY = 50f
        readModeContainer.visibility = View.VISIBLE
        
        // Phase 1: Fade out web view
        val webViewFadeOut = ObjectAnimator.ofFloat(webView, "alpha", 1f, 0f)
        webViewFadeOut.duration = ANIMATION_DURATION_SHORT
        webViewFadeOut.interpolator = AccelerateInterpolator()
        
        webViewFadeOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                webView.visibility = View.GONE
                
                // Phase 2: Fade in read mode container
                val readModeAnimSet = AnimatorSet()
                val alphaAnim = ObjectAnimator.ofFloat(readModeContainer, "alpha", 0f, 1f)
                val translateYAnim = ObjectAnimator.ofFloat(readModeContainer, "translationY", 50f, 0f)
                
                readModeAnimSet.playTogether(alphaAnim, translateYAnim)
                readModeAnimSet.duration = ANIMATION_DURATION_MEDIUM
                readModeAnimSet.interpolator = DecelerateInterpolator()
                
                readModeAnimSet.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        onEnd?.invoke()
                    }
                })
                
                readModeAnimSet.start()
            }
        })
        
        webViewFadeOut.start()
    }
    
    /**
     * Animate transition from read mode back to web view
     * 
     * @param readModeContainer The read mode container to fade out
     * @param webView The web view to fade in
     * @param onEnd Callback to invoke when animation completes
     */
    fun animateFromReadMode(readModeContainer: View, webView: View, onEnd: (() -> Unit)? = null) {
        // Phase 1: Fade out read mode container
        val readModeFadeOut = AnimatorSet()
        val alphaAnim = ObjectAnimator.ofFloat(readModeContainer, "alpha", 1f, 0f)
        val translateYAnim = ObjectAnimator.ofFloat(readModeContainer, "translationY", 0f, -50f)
        
        readModeFadeOut.playTogether(alphaAnim, translateYAnim)
        readModeFadeOut.duration = ANIMATION_DURATION_SHORT
        readModeFadeOut.interpolator = AccelerateInterpolator()
        
        readModeFadeOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                readModeContainer.visibility = View.GONE
                readModeContainer.translationY = 0f
                
                // Phase 2: Fade in web view
                webView.alpha = 0f
                webView.visibility = View.VISIBLE
                
                val webViewFadeIn = ObjectAnimator.ofFloat(webView, "alpha", 0f, 1f)
                webViewFadeIn.duration = ANIMATION_DURATION_MEDIUM
                webViewFadeIn.interpolator = DecelerateInterpolator()
                
                webViewFadeIn.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        onEnd?.invoke()
                    }
                })
                
                webViewFadeIn.start()
            }
        })
        
        readModeFadeOut.start()
    }
    
    /**
     * Animate transition to summary mode with slide effect  
     * 
     * @param webView The web view to slide out
     * @param summaryContainer The summary container to slide in
     * @param onEnd Callback to invoke when animation completes
     */
    fun animateToSummaryMode(webView: View, summaryContainer: View, onEnd: (() -> Unit)? = null) {
        // Prepare summary container
        summaryContainer.alpha = 0f
        summaryContainer.translationX = webView.width.toFloat()
        summaryContainer.visibility = View.VISIBLE
        
        // Create simultaneous slide animations
        val animSet = AnimatorSet()
        
        // Web view slides out to the left
        val webViewSlideOut = ObjectAnimator.ofFloat(webView, "translationX", 0f, -webView.width.toFloat())
        val webViewFadeOut = ObjectAnimator.ofFloat(webView, "alpha", 1f, 0.5f)
        
        // Summary container slides in from the right
        val summarySlideIn = ObjectAnimator.ofFloat(summaryContainer, "translationX", webView.width.toFloat(), 0f)
        val summaryFadeIn = ObjectAnimator.ofFloat(summaryContainer, "alpha", 0f, 1f)
        
        animSet.playTogether(webViewSlideOut, webViewFadeOut, summarySlideIn, summaryFadeIn)
        animSet.duration = ANIMATION_DURATION_MEDIUM
        animSet.interpolator = DecelerateInterpolator()
        
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                webView.visibility = View.GONE
                webView.translationX = 0f
                webView.alpha = 1f
                onEnd?.invoke()
            }
        })
        
        animSet.start()
    }
    
    /**
     * Animate transition from summary mode back to web view
     * 
     * @param summaryContainer The summary container to slide out
     * @param webView The web view to slide in
     * @param onEnd Callback to invoke when animation completes
     */
    fun animateFromSummaryMode(summaryContainer: View, webView: View, onEnd: (() -> Unit)? = null) {
        // Prepare web view
        webView.alpha = 0.5f
        webView.translationX = -webView.width.toFloat()
        webView.visibility = View.VISIBLE
        
        // Create simultaneous slide animations
        val animSet = AnimatorSet()
        
        // Summary container slides out to the right
        val summarySlideOut = ObjectAnimator.ofFloat(summaryContainer, "translationX", 0f, summaryContainer.width.toFloat())
        val summaryFadeOut = ObjectAnimator.ofFloat(summaryContainer, "alpha", 1f, 0f)
        
        // Web view slides in from the left
        val webViewSlideIn = ObjectAnimator.ofFloat(webView, "translationX", -webView.width.toFloat(), 0f)
        val webViewFadeIn = ObjectAnimator.ofFloat(webView, "alpha", 0.5f, 1f)
        
        animSet.playTogether(summarySlideOut, summaryFadeOut, webViewSlideIn, webViewFadeIn)
        animSet.duration = ANIMATION_DURATION_MEDIUM
        animSet.interpolator = DecelerateInterpolator()
        
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                summaryContainer.visibility = View.GONE
                summaryContainer.translationX = 0f
                summaryContainer.alpha = 1f
                onEnd?.invoke()
            }
        })
        
        animSet.start()
    }
    
    // ======================================
    // Resize Animations
    // ======================================
    
    /**
     * Animate bubble container resizing with smooth scaling
     * 
     * @param container The container to resize
     * @param fromWidth Starting width
     * @param fromHeight Starting height
     * @param toWidth Target width
     * @param toHeight Target height
     * @param onEnd Callback to invoke when animation completes
     */
    fun animateResize(
        container: View, 
        fromWidth: Int, 
        fromHeight: Int, 
        toWidth: Int, 
        toHeight: Int,
        onEnd: (() -> Unit)? = null
    ) {
        val widthAnimator = ValueAnimator.ofInt(fromWidth, toWidth)
        val heightAnimator = ValueAnimator.ofInt(fromHeight, toHeight)
        
        val animSet = AnimatorSet()
        animSet.playTogether(widthAnimator, heightAnimator)
        animSet.duration = ANIMATION_DURATION_MEDIUM
        animSet.interpolator = DecelerateInterpolator()
        
        widthAnimator.addUpdateListener { animator ->
            val animatedWidth = animator.animatedValue as Int
            val layoutParams = container.layoutParams
            layoutParams.width = animatedWidth
            container.layoutParams = layoutParams
        }
        
        heightAnimator.addUpdateListener { animator ->
            val animatedHeight = animator.animatedValue as Int
            val layoutParams = container.layoutParams
            layoutParams.height = animatedHeight
            container.layoutParams = layoutParams
        }
        
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onEnd?.invoke()
            }
        })
        
        animSet.start()
    }
    
    /**
     * Animate resize handles appearing with scale effect
     * 
     * @param handles List of resize handle views to animate
     * @param onEnd Callback to invoke when animation completes
     */
    fun animateResizeHandlesShow(handles: List<View>, onEnd: (() -> Unit)? = null) {
        val animators = mutableListOf<Animator>()
        
        handles.forEachIndexed { index, handle ->
            // Stagger the animations slightly
            val delay = index * 50L
            
            handle.alpha = 0f
            handle.scaleX = 0f
            handle.scaleY = 0f
            handle.visibility = View.VISIBLE
            
            val animSet = AnimatorSet()
            val alphaAnim = ObjectAnimator.ofFloat(handle, "alpha", 0f, 1f)
            val scaleXAnim = ObjectAnimator.ofFloat(handle, "scaleX", 0f, 1f)
            val scaleYAnim = ObjectAnimator.ofFloat(handle, "scaleY", 0f, 1f)
            
            animSet.playTogether(alphaAnim, scaleXAnim, scaleYAnim)
            animSet.duration = ANIMATION_DURATION_SHORT
            animSet.startDelay = delay
            animSet.interpolator = OvershootInterpolator(1.2f)
            
            animators.add(animSet)
        }
        
        // Start all animations
        animators.forEach { it.start() }
        
        // Call onEnd after the last animation completes
        animators.lastOrNull()?.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onEnd?.invoke()
            }
        })
    }
    
    /**
     * Animate resize handles hiding with scale effect
     * 
     * @param handles List of resize handle views to animate
     * @param onEnd Callback to invoke when animation completes
     */
    fun animateResizeHandlesHide(handles: List<View>, onEnd: (() -> Unit)? = null) {
        val animators = mutableListOf<Animator>()
        
        handles.forEachIndexed { index, handle ->
            // Stagger the animations slightly
            val delay = index * 30L
            
            val animSet = AnimatorSet()
            val alphaAnim = ObjectAnimator.ofFloat(handle, "alpha", 1f, 0f)
            val scaleXAnim = ObjectAnimator.ofFloat(handle, "scaleX", 1f, 0f)
            val scaleYAnim = ObjectAnimator.ofFloat(handle, "scaleY", 1f, 0f)
            
            animSet.playTogether(alphaAnim, scaleXAnim, scaleYAnim)
            animSet.duration = ANIMATION_DURATION_SHORT
            animSet.startDelay = delay
            animSet.interpolator = AccelerateInterpolator()
            
            animSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    handle.visibility = View.GONE
                    // Reset properties for next time
                    handle.alpha = 1f
                    handle.scaleX = 1f
                    handle.scaleY = 1f
                }
            })
            
            animators.add(animSet)
        }
        
        // Start all animations
        animators.forEach { it.start() }
        
        // Call onEnd after the last animation completes
        animators.lastOrNull()?.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onEnd?.invoke()
            }
        })
    }
    
    /**
     * Animate toolbar sliding in/out based on scroll direction
     * 
     * @param toolbar The toolbar view to animate
     * @param show Whether to show (true) or hide (false) the toolbar
     * @param onEnd Callback to invoke when animation completes
     */
    fun animateToolbarSlide(toolbar: View, show: Boolean, onEnd: (() -> Unit)? = null) {
        val targetTranslationY = if (show) 0f else toolbar.height.toFloat()
        
        val animator = ObjectAnimator.ofFloat(toolbar, "translationY", toolbar.translationY, targetTranslationY)
        animator.duration = ANIMATION_DURATION_MEDIUM
        animator.interpolator = if (show) DecelerateInterpolator() else AccelerateInterpolator()
        
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // Ensure toolbar is fully visible or hidden
                if (!show) {
                    toolbar.visibility = View.GONE
                } else {
                    toolbar.visibility = View.VISIBLE
                }
                onEnd?.invoke()
            }
        })
        
        // Make sure toolbar is visible before starting show animation
        if (show) {
            toolbar.visibility = View.VISIBLE
        }
        
        animator.start()
    }
    
    // ======================================
    // Progress and Loading Animations
    // ======================================
    
    /**
     * Animate progress indicator with pulsing effect
     * 
     * @param progressView The progress view to animate
     * @param show Whether to show or hide the progress
     * @param onEnd Callback to invoke when animation completes
     */
    fun animateProgressIndicator(progressView: View, show: Boolean, onEnd: (() -> Unit)? = null) {
        if (show) {
            progressView.alpha = 0f
            progressView.scaleX = 0.5f
            progressView.scaleY = 0.5f
            progressView.visibility = View.VISIBLE
            
            val animSet = AnimatorSet()
            val alphaAnim = ObjectAnimator.ofFloat(progressView, "alpha", 0f, 1f)
            val scaleXAnim = ObjectAnimator.ofFloat(progressView, "scaleX", 0.5f, 1f)
            val scaleYAnim = ObjectAnimator.ofFloat(progressView, "scaleY", 0.5f, 1f)
            
            animSet.playTogether(alphaAnim, scaleXAnim, scaleYAnim)
            animSet.duration = ANIMATION_DURATION_SHORT
            animSet.interpolator = DecelerateInterpolator()
            
            animSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onEnd?.invoke()
                }
            })
            
            animSet.start()
        } else {
            val animSet = AnimatorSet()
            val alphaAnim = ObjectAnimator.ofFloat(progressView, "alpha", 1f, 0f)
            val scaleXAnim = ObjectAnimator.ofFloat(progressView, "scaleX", 1f, 0.5f)
            val scaleYAnim = ObjectAnimator.ofFloat(progressView, "scaleY", 1f, 0.5f)
            
            animSet.playTogether(alphaAnim, scaleXAnim, scaleYAnim)
            animSet.duration = ANIMATION_DURATION_SHORT
            animSet.interpolator = AccelerateInterpolator()
            
            animSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    progressView.visibility = View.GONE
                    // Reset properties
                    progressView.alpha = 1f
                    progressView.scaleX = 1f
                    progressView.scaleY = 1f
                    onEnd?.invoke()
                }
            })
            
            animSet.start()
        }
    }
    
    /**
     * Animate content loading with skeleton effect
     * 
     * @param containerView The container view to show loading effect in
     * @param show Whether to show or hide loading effect
     * @param onEnd Callback to invoke when animation completes
     */
    fun animateContentLoading(containerView: View, show: Boolean, onEnd: (() -> Unit)? = null) {
        if (show) {
            // Create shimmer/pulse effect
            val animator = ObjectAnimator.ofFloat(containerView, "alpha", 1f, 0.3f, 1f)
            animator.duration = ANIMATION_DURATION_LONG
            animator.repeatCount = ValueAnimator.INFINITE
            animator.interpolator = AccelerateDecelerateInterpolator()
            animator.start()
            
            containerView.tag = animator // Store animator reference for later cancellation
        } else {
            // Cancel existing animation if any
            (containerView.tag as? ObjectAnimator)?.cancel()
            containerView.tag = null
            
            // Fade to full opacity
            val animator = ObjectAnimator.ofFloat(containerView, "alpha", containerView.alpha, 1f)
            animator.duration = ANIMATION_DURATION_SHORT
            animator.interpolator = DecelerateInterpolator()
            
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onEnd?.invoke()
                }
            })
            
            animator.start()
        }
    }
    
    companion object {
        const val ANIMATION_DURATION_SHORT = 150L
        const val ANIMATION_DURATION_MEDIUM = 300L
        const val ANIMATION_DURATION_LONG = 500L
    }
}