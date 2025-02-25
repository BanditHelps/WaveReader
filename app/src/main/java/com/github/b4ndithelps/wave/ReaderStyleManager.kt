package com.github.b4ndithelps.wave

import android.webkit.WebView
import com.github.b4ndithelps.wave.data.ReaderStyle

class ReaderStyleManager(private val webView: WebView) {
    var currentStyle = ReaderStyle()
        private set

    fun updateFontFamily(fontFamily: String) {
        currentStyle = currentStyle.copy(fontFamily = fontFamily)
        refreshStyles()
    }

    fun updateFontSize(size: Int) {
        currentStyle = currentStyle.copy(fontSize = size)
        refreshStyles()
    }

    fun updateTheme(theme: ReaderStyle.Theme) {
        currentStyle = when (theme) {
            ReaderStyle.Theme.LIGHT -> currentStyle.copy(
                backgroundColor = "#FFFFFF",
                textColor = "#000000"
            )
            ReaderStyle.Theme.DARK -> currentStyle.copy(
                backgroundColor = "#1A1A1A",
                textColor = "#FFFFFF"
            )
        }
        refreshStyles()
    }

    fun updateTextAlignment(alignment: ReaderStyle.TextAlignment) {
        currentStyle = currentStyle.copy(alignment = alignment)
        refreshStyles()
    }

    fun updateLineHeight(lineHeight: Float) {
        currentStyle = currentStyle.copy(lineHeight = lineHeight)
        refreshStyles()
    }

    fun updateMargins(margins: Int) {
        currentStyle = currentStyle.copy(margins = margins)
        refreshStyles()
    }

    fun refreshStyles() {
        val javascript = """
            (function() {
                // Function to ensure our styles take precedence
                function ensureStylesPrecedence() {
                    // Remove any existing custom styles
                    const existingStyle = document.getElementById('reader-styles');
                    if (existingStyle) existingStyle.remove();
                    
                    // Create new style element
                    const style = document.createElement('style');
                    style.id = 'reader-styles';
                    
                    // Make our styles more specific and important
                    style.textContent = `
                        body, body * {
                            margin: 0 !important;
                            padding: ${currentStyle.margins}px !important;
                            line-height: ${currentStyle.lineHeight} !important;
                            font-family: ${currentStyle.fontFamily} !important;
                            font-size: ${currentStyle.fontSize}px !important;
                            color: ${currentStyle.textColor} !important;
                        }
                        body {
                            background-color: ${currentStyle.backgroundColor} !important;
                        }
                        p, div, span, article, section {
                            margin: 1em 0 !important;
                        }
                    `;
                    
                    // Append style to the end of head to ensure it's last
                    document.head.appendChild(style);
                    
                    // Optional: Disable any other stylesheets
//                    Array.from(document.styleSheets).forEach(sheet => {
//                        if (sheet.ownerNode.id !== 'reader-styles' && 
//                            sheet.ownerNode.id !== 'reader-base-styles') {
//                            sheet.disabled = true;
//                        }
//                    });
                }
                
                // Call immediately and after a short delay to ensure it runs
                ensureStylesPrecedence();
                setTimeout(ensureStylesPrecedence, 50);
            })();
        """.trimIndent()

        webView.evaluateJavascript(javascript, null)
    }

}