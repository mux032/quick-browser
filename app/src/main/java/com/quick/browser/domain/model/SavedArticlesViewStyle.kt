package com.quick.browser.domain.model

/**
 * Enum representing different view styles for saved articles
 */
enum class SavedArticlesViewStyle {
    CARD,           // Current card view (default)
    COMPACT_CARD,   // History-style card with title over image
    COMPACT,        // Small preview image to the left of title
    SUPER_COMPACT   // Just favicon instead of preview image
}