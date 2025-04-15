package com.qb.browser.ui

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
        val fromScale = if (isExpand) 1f else 1.1f
        val toScale = if (isExpand) 1.1f else 1f
        
        val animSet = AnimatorSet()
        val scaleXAnim = ObjectAnimator.ofFloat(view, "scaleX", fromScale, toScale)
        val scaleYAnim = ObjectAnimator.ofFloat(view, "scaleY", fromScale, toScale)
        
        animSet.playTogether(scaleXAnim, scaleYAnim)
        animSet.duration = ANIMATION_DURATION_SHORT
        animSet.interpolator = if (isExpand) OvershootInterpolator(2f) else DecelerateInterpolator()
        
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