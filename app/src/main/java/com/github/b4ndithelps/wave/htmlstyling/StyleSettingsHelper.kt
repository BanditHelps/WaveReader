package com.github.b4ndithelps.wave.htmlstyling

class StyleSettingsHelper(private val styleManager: StyleManager) {
    fun getAvailableThemes(): List<ThemeType> {
        return ThemeType.values().toList()
    }

    fun getAvailableFonts(): List<String> {
        return listOf(
            "sans-serif",
            "serif",
            "monospace",
            "cursive",
            "sans-serif-light",
            "sans-serif-thin",
            "sans-serif-condensed",
            "sans-serif-medium"
        )
    }

    fun getAvailableTextAlignments(): List<TextAlign> {
        return TextAlign.values().toList()
    }

    fun applyTheme(themeType: ThemeType) {
        styleManager.saveTheme(themeType)
    }

    fun applyFont(fontFamily: String) {
        styleManager.saveFontFamily(fontFamily)
    }

    fun applyTextSize(size: Float) {
        styleManager.saveTextSize(size)
    }

    fun applyLineHeight(height: Float) {
        styleManager.saveLineHeight(height)
    }

    fun applyTextAlign(align: TextAlign) {
        styleManager.saveTextAlign(align)
    }

    fun applyMargin(margin: Int) {
        styleManager.saveMargin(margin)
    }

    fun applyParagraphSpacing(spacing: Float) {
        styleManager.saveParagraphSpacing(spacing)
    }
}