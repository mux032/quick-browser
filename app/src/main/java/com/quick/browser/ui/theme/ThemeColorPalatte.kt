package com.quick.browser.ui.theme

import android.content.Context
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.quick.browser.R

/**
 * Defines the available theme colors for the app
 */
enum class ThemeColor(
    val colorName: String,
    @ColorRes val primaryColorRes: Int,
    @ColorRes val primaryDarkColorRes: Int,
    @ColorRes val accentColorRes: Int
) {
    BLUE("Blue", R.color.blue_primary, R.color.blue_primary_dark, R.color.blue_accent),
    GREEN("Green", R.color.green_primary, R.color.green_primary_dark, R.color.green_accent),
    RED("Red", R.color.red_primary, R.color.red_primary_dark, R.color.red_accent),
    PURPLE("Purple", R.color.purple_primary, R.color.purple_primary_dark, R.color.purple_accent),
    LAVENDER("Lavender", R.color.lavender_primary, R.color.lavender_primary_dark, R.color.lavender_accent),
    ORANGE("Orange", R.color.orange_primary, R.color.orange_primary_dark, R.color.orange_accent),
    TEAL("Teal", R.color.teal_primary, R.color.teal_primary_dark, R.color.teal_accent),
    PINK("Pink", R.color.pink_primary, R.color.pink_primary_dark, R.color.pink_accent),
    INDIGO("Indigo", R.color.indigo_primary, R.color.indigo_primary_dark, R.color.indigo_accent),
    CYAN("Cyan", R.color.cyan_primary, R.color.cyan_primary_dark, R.color.cyan_accent),
    AMBER("Amber", R.color.amber_primary, R.color.amber_primary_dark, R.color.amber_accent),
    DEEP_PURPLE("Deep Purple", R.color.deep_purple_primary, R.color.deep_purple_primary_dark, R.color.deep_purple_accent),
    LIGHT_BLUE("Light Blue", R.color.light_blue_primary, R.color.light_blue_primary_dark, R.color.light_blue_accent);

    companion object {
        fun fromName(name: String): ThemeColor {
            return values().find { it.colorName.equals(name, ignoreCase = true) } ?: BLUE
        }

        fun getColorHex(context: Context, @ColorRes colorRes: Int): String {
            val color = ContextCompat.getColor(context, colorRes)
            return String.format("#%06X", 0xFFFFFF and color)
        }
    }
}