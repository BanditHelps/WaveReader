package com.github.b4ndithelps.wave.htmlstyling

enum class TextAlign {
    LEFT, CENTER, RIGHT, JUSTIFY;

    fun toCssValue(): String {
        return name.lowercase()
    }
}