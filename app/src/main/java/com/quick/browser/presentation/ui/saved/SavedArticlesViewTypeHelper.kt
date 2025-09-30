package com.quick.browser.presentation.ui.saved

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import com.quick.browser.R
import com.quick.browser.domain.model.SavedArticlesViewStyle

/**
 * Helper class to manage different view types for saved articles
 */
object SavedArticlesViewTypeHelper {

    /**
     * Get the layout resource ID for the given view style
     */
    @LayoutRes
    fun getLayoutResForViewStyle(viewStyle: SavedArticlesViewStyle): Int {
        return when (viewStyle) {
            SavedArticlesViewStyle.CARD -> R.layout.item_saved_article
            SavedArticlesViewStyle.COMPACT_CARD -> R.layout.item_saved_article_compact_card
            SavedArticlesViewStyle.COMPACT -> R.layout.item_saved_article_compact
            SavedArticlesViewStyle.SUPER_COMPACT -> R.layout.item_saved_article_super_compact
        }
    }

    /**
     * Inflate a view for the given view style
     */
    fun inflateViewForViewStyle(
        parent: ViewGroup,
        viewStyle: SavedArticlesViewStyle
    ): View {
        val layoutRes = getLayoutResForViewStyle(viewStyle)
        return LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
    }
}