package com.github.b4ndithelps.wave.htmlstyling


/**
 * Represents the styling for an EPUB document.
 *
 * This data class encapsulates the visual presentation aspects of an EPUB,
 * including text colors, background colors, font properties, text alignment,
 * margins, and theme type. It also provides a method to generate CSS code
 * that can be directly applied to HTML content to achieve the specified
 * style.
 *
 * @property textColor The color of the text (e.g., "#000000" for black).
 * @property backgroundColor The background color of the document (e.g., "#FFFFFF" for white).
 * @property linkColor The color of hyperlinks (e.g., "#0000FF" for blue).
 * @property textSize The base font size for the document in pixels (e.g., 16.0f).
 * @property lineHeight The line height of the text, as a multiplier of the font size (e.g., 1.5f).
 * @property fontFamily The preferred font family for the document (e.g., "Arial").
 * @property paragraphSpacing The spacing between paragraphs, in em units (e.g., 1.0f).
 * @property textAlign The alignment of the text within the document. See [TextAlign].
 * @property margin The margin around the content, in pixels (e.g., 20).
 * @property themeType The theme type (light or dark). See [ThemeType].
 */
data class EpubStyle(
    val textColor: String,
    val backgroundColor: String,
    val linkColor: String,
    var textSize: Float,
    var lineHeight: Float,
    var fontFamily: String,
    var paragraphSpacing: Float,
    var textAlign: TextAlign,
    var margin: Int,
    val themeType: ThemeType) {

    /**
     * Convert the style to CSS that can be easily injected to HTML
     */
    fun toCSS(): String {
        return """
            <style>
                :root {
                    color-scheme: ${if (themeType == ThemeType.DARK) "dark" else "light"};
                }
                
                body {
                    font-family: $fontFamily, system-ui, -apple-system, sans-serif;
                    font-size: ${textSize}px;
                    line-height: ${lineHeight};
                    color: $textColor;
                    background-color: $backgroundColor;
                    margin: ${margin * 2}px ${margin * 1.5}px;
                    padding: 0;
                    text-align: ${textAlign.toCssValue()};
                    hyphens: auto;
                    -webkit-hyphens: auto;
                    -ms-hyphens: auto;
                    word-wrap: break-word;
                    overflow-wrap: break-word;
                }
                
                p, div {
                    margin-bottom: ${paragraphSpacing}em;
                    line-height: inherit;
                    font-family: inherit;
                    font-size: inherit;
                    color: inherit;
                    orphans: 4;
                    widows: 4;
                }
                
                /* Allow proper indentation for paragraphs */
                p {
                    margin-top: 0;
                    margin-bottom: ${paragraphSpacing}em;
                }
                
                h1, h2, h3, h4, h5, h6 {
                    font-family: inherit;
                    line-height: 1.2;
                    margin-top: 1.5em;
                    margin-bottom: 0.5em;
                    color: inherit;
                    page-break-after: avoid;
                    break-after: avoid;
                }
                
                h1 { font-size: 1.6em; }
                h2 { font-size: 1.4em; }
                h3 { font-size: 1.3em; }
                h4 { font-size: 1.2em; }
                h5 { font-size: 1.1em; }
                h6 { font-size: 1em; }
                
                a {
                    color: $linkColor;
                    text-decoration: none;
                }
                
                img {
                    max-width: 100%;
                    height: auto;
                    display: block;
                    margin: 1em auto;
                    page-break-inside: avoid;
                    break-inside: avoid;
                }
                
                ul, ol {
                    padding-left: 2em;
                    margin-bottom: 1em;
                }
                
                li {
                    margin-bottom: 0.5em;
                }
                
                blockquote {
                    border-left: 3px solid #ccc;
                    margin: 1em 0;
                    padding-left: 1em;
                    font-style: italic;
                    color: inherit;
                    page-break-inside: avoid;
                    break-inside: avoid;
                }
                
                code, pre {
                    font-family: 'Courier New', monospace;
                    background-color: ${if (themeType == ThemeType.DARK) "#2d2d2d" else "#f5f5f5"};
                    padding: 0.2em 0.4em;
                    border-radius: 3px;
                    font-size: 0.9em;
                }
                
                pre {
                    padding: 1em;
                    overflow-x: auto;
                    white-space: pre-wrap;
                    page-break-inside: avoid;
                    break-inside: avoid;
                }
                
                table {
                    border-collapse: collapse;
                    width: 100%;
                    margin: 1em 0;
                    page-break-inside: avoid;
                    break-inside: avoid;
                }
                
                table, th, td {
                    border: 1px solid ${if (themeType == ThemeType.DARK) "#444" else "#ddd"};
                }
                
                th, td {
                    padding: 0.5em;
                }
                
                /* Override any existing styles */
                * {
                    max-width: 100% !important;
                    font-size: inherit;
                    font-family: inherit;
                }
                
                /* Responsive design for different screen sizes */
                @media screen and (max-width: 600px) {
                    body {
                        margin: ${margin}px;
                    }
                    
                    h1 { font-size: 1.4em; }
                    h2 { font-size: 1.3em; }
                    h3 { font-size: 1.2em; }
                }
            </style>
        """.trimIndent()
    }
}