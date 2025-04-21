package com.qb.browser.manager

import android.content.Context
import android.graphics.Point
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * BubblePositionManager handles the positioning and layout of bubbles on the screen.
 * It manages bubble positions, handles edge snapping, and ensures bubbles are arranged
 * in an organized manner.
 *
 * Features:
 * - Smart positioning for new bubbles
 * - Edge snapping for better UX
 * - Screen boundary enforcement
 * - Position state management
 */
class BubblePositionManager(context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val screenSize = Point()
    private val _positions = MutableStateFlow<Map<String, Point>>(emptyMap())
    val positions: StateFlow<Map<String, Point>> = _positions

    companion object {
        private const val BUBBLE_SIZE = 64 // dp
        private const val EDGE_MARGIN = 16 // dp
        private const val SNAP_THRESHOLD = 32 // dp
        private const val SPACING = 80 // dp
    }

    init {
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getSize(screenSize)
    }

    /**
     * Calculates initial position for a new bubble
     * @param bubbleId The ID of the bubble
     * @return Point containing x,y coordinates
     */
    fun calculateInitialPosition(bubbleId: String): Point {
        val existingPositions = _positions.value
        val index = existingPositions.size
        
        // Calculate position in a circular pattern
        val angle = (index * (2.0 * Math.PI / 5.0))
        val centerX = screenSize.x / 2
        val centerY = screenSize.y / 2
        val radius = SPACING * 2
        
        val x = (centerX + radius * Math.cos(angle)).toInt()
        val y = (centerY + radius * Math.sin(angle)).toInt()
        
        val position = constrainToScreen(Point(x, y))
        updatePosition(bubbleId, position)
        
        return position
    }

    /**
     * Updates position of an existing bubble
     * @param bubbleId The ID of the bubble
     * @param point New position
     * @return Final position after constraints
     */
    fun updatePosition(bubbleId: String, point: Point): Point {
        val snappedPoint = snapToEdge(point)
        val finalPoint = constrainToScreen(snappedPoint)
        
        val currentPositions = _positions.value.toMutableMap()
        currentPositions[bubbleId] = finalPoint
        _positions.value = currentPositions
        
        return finalPoint
    }

    /**
     * Removes position tracking for a bubble
     * @param bubbleId The ID of the bubble
     */
    fun removePosition(bubbleId: String) {
        val currentPositions = _positions.value.toMutableMap()
        currentPositions.remove(bubbleId)
        _positions.value = currentPositions
    }

    /**
     * Snaps position to screen edge if close enough
     * @param point Position to check for snapping
     * @return Snapped position
     */
    private fun snapToEdge(point: Point): Point {
        val x = when {
            point.x < SNAP_THRESHOLD -> EDGE_MARGIN
            point.x > screenSize.x - SNAP_THRESHOLD -> screenSize.x - EDGE_MARGIN - BUBBLE_SIZE
            else -> point.x
        }

        val y = when {
            point.y < SNAP_THRESHOLD -> EDGE_MARGIN
            point.y > screenSize.y - SNAP_THRESHOLD -> screenSize.y - EDGE_MARGIN - BUBBLE_SIZE
            else -> point.y
        }

        return Point(x, y)
    }

    /**
     * Ensures position stays within screen bounds
     * @param point Position to constrain
     * @return Constrained position
     */
    private fun constrainToScreen(point: Point): Point {
        val x = point.x.coerceIn(EDGE_MARGIN, screenSize.x - EDGE_MARGIN - BUBBLE_SIZE)
        val y = point.y.coerceIn(EDGE_MARGIN, screenSize.y - EDGE_MARGIN - BUBBLE_SIZE)
        return Point(x, y)
    }

    /**
     * Gets current position of a bubble
     * @param bubbleId The ID of the bubble
     * @return Current position or null if not found
     */
    fun getPosition(bubbleId: String): Point? {
        return _positions.value[bubbleId]
    }
}