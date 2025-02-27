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
                    margin: ${margin * 2.5}px ${margin * 2}px; /* Increased margins for better readability */
                    padding: 0;
                    text-align: ${textAlign.toCssValue()};
                    hyphens: auto;
                    -webkit-hyphens: auto;
                    -ms-hyphens: auto;
                    word-wrap: break-word;
                    overflow-wrap: break-word;
                }
                
                /* Base styles for text elements */
                p, div {
                    margin-bottom: ${paragraphSpacing}em;
                    line-height: inherit;
                    font-family: inherit;
                    font-size: inherit;
                    color: inherit;
                    orphans: 4;
                    widows: 4;
                }
                
                /* Paragraph styling with text-indent for book-like appearance */
                p {
                    margin-top: 0;
                    margin-bottom: ${paragraphSpacing}em;
                    text-indent: 1.5em; /* Paragraph indentation */
                    letter-spacing: 0.01em; /* Slightly increased letter spacing */
                }
                
                /* Don't indent first paragraph after a heading */
                h1 + p, h2 + p, h3 + p, h4 + p, h5 + p, h6 + p, div.chapter-start p:first-child {
                    text-indent: 0;
                }
                
                /* New style for chapter opening paragraphs */
                p.chapter-first {
                    text-indent: 0;
                }
                
                /* First letter styling for chapter openings (optional) */
                p.chapter-first:first-letter {
                    font-size: 1.2em;
                    font-weight: bold;
                }
                
                p:has(em) {
                    padding-top: 0.8em;
                    padding-bottom: 0.8em;
                }
                
                /* Heading styles */
                h1, h2, h3, h4, h5, h6 {
                    font-family: inherit;
                    line-height: 1.2;
                    margin-top: 2em; /* Increased top margin */
                    margin-bottom: 0.8em;
                    color: inherit;
                    page-break-after: avoid;
                    break-after: avoid;
                    text-align: center; /* Center headings by default */
                    letter-spacing: 0.03em; /* Slightly increased letter spacing for headings */
                }
                
                h1 { font-size: 1.7em; }
                h2 { font-size: 1.5em; }
                h3 { font-size: 1.3em; }
                h4 { font-size: 1.2em; }
                h5 { font-size: 1.1em; }
                h6 { font-size: 1em; }
                
                /* Link styling */
                a {
                    color: $linkColor;
                    text-decoration: none;
                    border-bottom: 1px dotted $linkColor; /* Subtle underline */
                }
                
                /* Image styling */
                img {
                    max-width: 90%; /* Slightly reduced to avoid touching edges */
                    height: auto;
                    display: block;
                    margin: 1.5em auto; /* Increased vertical margin */
                    page-break-inside: avoid;
                    break-inside: avoid;
                    border-radius: 3px; /* Slightly rounded corners */
                    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1); /* Subtle shadow */
                }
                
                /* Lists */
                ul, ol {
                    padding-left: 2.5em; /* Increased padding */
                    margin-bottom: 1.2em;
                    margin-top: 1em;
                }
                
                li {
                    margin-bottom: 0.6em; /* Increased spacing between list items */
                    line-height: 1.5;
                }
                
                /* Blockquote styling */
                blockquote {
                    border-left: 3px solid ${if (themeType == ThemeType.DARK) "#555" else "#ccc"};
                    margin: 1.5em 2em 1.5em 1em; /* Increased margins */
                    padding: 0.8em 0 0.8em 1.2em;
                    font-style: italic;
                    color: inherit;
                    page-break-inside: avoid;
                    break-inside: avoid;
                    background-color: ${if (themeType == ThemeType.DARK) "rgba(50, 50, 50, 0.3)" else "rgba(245, 245, 245, 0.5)"};
                    border-radius: 0 3px 3px 0;
                }
                
                /* Code blocks */
                code, pre {
                    font-family: 'Courier New', monospace;
                    background-color: ${if (themeType == ThemeType.DARK) "#2d2d2d" else "#f5f5f5"};
                    padding: 0.2em 0.4em;
                    border-radius: 3px;
                    font-size: 0.9em;
                }
                
                pre {
                    padding: 1.2em; /* Increased padding */
                    margin: 1.5em 0;
                    overflow-x: auto;
                    white-space: pre-wrap;
                    page-break-inside: avoid;
                    break-inside: avoid;
                    border-radius: 5px;
                    border: 1px solid ${if (themeType == ThemeType.DARK) "#444" else "#ddd"};
                }
                
                /* Table styling */
                table {
                    border-collapse: collapse;
                    width: 100%;
                    margin: 1.5em 0; /* Increased margin */
                    page-break-inside: avoid;
                    break-inside: avoid;
                }
                
                table, th, td {
                    border: 1px solid ${if (themeType == ThemeType.DARK) "#444" else "#ddd"};
                }
                
                th, td {
                    padding: 0.7em; /* Increased padding */
                }
                
                th {
                    background-color: ${if (themeType == ThemeType.DARK) "#333" else "#f0f0f0"};
                }
                
                /* Horizontal rule styling */
                hr {
                    border: none;
                    height: 1px;
                    background-color: ${if (themeType == ThemeType.DARK) "#444" else "#ddd"};
                    margin: 2em 0;
                }
                
                /* Section breaks */
                .section-break {
                    text-align: center;
                    margin: 2em 0;
                }
                
                .section-break::before {
                    content: "* * *";
                    display: inline-block;
                    letter-spacing: 1em;
                    margin-left: 1em;
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
                        margin: ${margin * 1.5}px ${margin}px;
                    }
                    
                    h1 { font-size: 1.5em; }
                    h2 { font-size: 1.4em; }
                    h3 { font-size: 1.2em; }
                }
            </style>
        """.trimIndent()
    }
}