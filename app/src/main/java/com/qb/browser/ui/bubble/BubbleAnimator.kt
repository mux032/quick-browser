package com.qb.browser.ui.bubble

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
    
    companion object {
        const val ANIMATION_DURATION_SHORT = 150L
        const val ANIMATION_DURATION_MEDIUM = 300L
        const val ANIMATION_DURATION_LONG = 500L
    }
}