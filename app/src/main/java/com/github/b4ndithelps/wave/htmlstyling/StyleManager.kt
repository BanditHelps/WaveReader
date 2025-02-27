package com.github.b4ndithelps.wave.htmlstyling

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Main style manager class to handle ePub styling
 */
class StyleManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "epub_style_prefs"
        private const val KEY_STYLE_TYPE = "style_type"
        private const val KEY_TEXT_SIZE = "text_size"
        private const val KEY_LINE_HEIGHT = "line_height"
        private const val KEY_FONT_FAMILY = "font_family"
        private const val KEY_PARAGRAPH_SPACING = "paragraph_spacing"
        private const val KEY_TEXT_ALIGN = "text_align"
        private const val KEY_MARGIN = "margin"
        private const val KEY_THEME = "theme"
    }

    val currentStyle: EpubStyle
        get() {
            val styleType = prefs.getString(KEY_STYLE_TYPE, ThemeType.LIGHT.name) ?: ThemeType.LIGHT.name
            return when (styleType) {
                ThemeType.DARK.name -> getDarkStyle()
                ThemeType.SEPIA.name -> getSepiaStyle()
                else -> getLightStyle()
            }.apply {
                // Apply user customizations from preferences
                textSize = prefs.getFloat(KEY_TEXT_SIZE, textSize)
                lineHeight = prefs.getFloat(KEY_LINE_HEIGHT, lineHeight)
                fontFamily = prefs.getString(KEY_FONT_FAMILY, fontFamily) ?: fontFamily
                paragraphSpacing = prefs.getFloat(KEY_PARAGRAPH_SPACING, paragraphSpacing)
                textAlign = TextAlign.valueOf(prefs.getString(KEY_TEXT_ALIGN, textAlign.name) ?: textAlign.name)
                margin = prefs.getInt(KEY_MARGIN, margin)
            }
        }

    // Predefined themes with enhanced book-like styling
    private fun getLightStyle() = EpubStyle(
        textColor = "#333333",
        backgroundColor = "#FFFBF2", // Slightly off-white for better eye comfort
        linkColor = "#1E88E5",
        textSize = 18f,
        lineHeight = 1.6f, // Increased line height for better readability
        fontFamily = "Georgia, serif", // More book-like serif font
        paragraphSpacing = 1.2f,
        textAlign = TextAlign.JUSTIFY,
        margin = 20, // Increased margins for more comfortable reading
        themeType = ThemeType.LIGHT
    )

    private fun getDarkStyle() = EpubStyle(
        textColor = "#E0E0E0",
        backgroundColor = "#121212",
        linkColor = "#64B5F6",
        textSize = 18f,
        lineHeight = 1.6f, // Increased line height for better readability
        fontFamily = "Palatino, Georgia, serif", // More book-like serif font
        paragraphSpacing = 1.2f,
        textAlign = TextAlign.JUSTIFY,
        margin = 20, // Increased margins for more comfortable reading
        themeType = ThemeType.DARK
    )

    private fun getSepiaStyle() = EpubStyle(
        textColor = "#5B4636",
        backgroundColor = "#F8F1E3",
        linkColor = "#9C6644",
        textSize = 18f,
        lineHeight = 1.6f, // Increased line height for better readability
        fontFamily = "Palatino, Georgia, serif", // Traditional book font
        paragraphSpacing = 1.2f,
        textAlign = TextAlign.JUSTIFY,
        margin = 20, // Increased margins for more comfortable reading
        themeType = ThemeType.SEPIA
    )

    // Save changes to preferences
    fun saveTheme(themeType: ThemeType) {
        prefs.edit {
            putString(KEY_STYLE_TYPE, themeType.name)
        }
    }

    fun saveFontFamily(fontFamily: String) {
        prefs.edit {
            putString(KEY_FONT_FAMILY, fontFamily)
        }
    }

    fun saveTextSize(size: Float) {
        prefs.edit {
            putFloat(KEY_TEXT_SIZE, size)
        }
    }

    fun saveLineHeight(height: Float) {
        prefs.edit {
            putFloat(KEY_LINE_HEIGHT, height)
        }
    }

    fun saveParagraphSpacing(spacing: Float) {
        prefs.edit {
            putFloat(KEY_PARAGRAPH_SPACING, spacing)
        }
    }

    fun saveTextAlign(align: TextAlign) {
        prefs.edit {
            putString(KEY_TEXT_ALIGN, align.name)
        }
    }

    fun saveMargin(margin: Int) {
        prefs.edit {
            putInt(KEY_MARGIN, margin)
        }
    }
}