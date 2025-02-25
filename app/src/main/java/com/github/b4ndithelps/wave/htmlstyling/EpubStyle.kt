package com.github.b4ndithelps.wave.htmlstyling

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
                    margin: ${margin}px;
                    padding: 0;
                    text-align: ${textAlign.toCssValue()};
                }
                
                p, div {
                    margin-bottom: ${paragraphSpacing}em;
                    line-height: inherit;
                    font-family: inherit;
                    font-size: inherit;
                    color: inherit;
                }
                
                h1, h2, h3, h4, h5, h6 {
                    font-family: inherit;
                    line-height: 1.2;
                    margin-top: 1.5em;
                    margin-bottom: 0.5em;
                    color: inherit;
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
                }
                
                table {
                    border-collapse: collapse;
                    width: 100%;
                    margin: 1em 0;
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
                        margin: ${margin / 2}px;
                    }
                    
                    h1 { font-size: 1.4em; }
                    h2 { font-size: 1.3em; }
                    h3 { font-size: 1.2em; }
                }
            </style>
        """.trimIndent()
    }
}
