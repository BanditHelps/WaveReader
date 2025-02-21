package com.github.b4ndithelps.wave.data

data class ReaderStyle(
    val fontFamily: String = "Arial",
    val fontSize: Int = 32,
    val lineHeight: Float = 1.6f,
    val textColor: String = "#0000000",
    val backgroundColor: String = "#FFFFFFF",
    val margins: Int = 2,
    val alignment: TextAlignment = TextAlignment.LEFT,
    val theme: Theme = Theme.DARK
) {
    enum class TextAlignment {
        LEFT, RIGHT, JUSTIFY
    }

    enum class Theme { LIGHT, DARK }

    fun toCSS(): String {
        val baseStyles = """
            body {
                margin: 0 !important;
                padding: ${margins}px !important;
                line-height: ${lineHeight} !important;
                font-family: ${fontFamily} !important;
                font-size: ${fontSize}px !important;
                color: ${textColor} !important;
                background-color: ${backgroundColor} !important;
            }
            /* Text content alignment */
            p, article, section {
                margin: 1em 0 !important;
                text-align: ${alignment.name.lowercase()} !important;
                font-family: ${fontFamily} !important;
                font-size: ${fontSize}px !important;
                color: ${textColor} !important;
            }
            /* Preserve decoration alignments */
            .decoration-rw10,
            .media-rw,
            .align-center-rw,
            .ext_ch,
            div[class*="decoration-"],
            div[class*="align-center"] {
                text-align: center !important;
                margin-left: auto !important;
                margin-right: auto !important;
                display: block !important;
            }
            /* Basic image handling */
            img {
                max-width: 100% !important;
                height: auto !important;
                display: block !important;
            }
            /* Specific handling for decoration images */
            img.ornament1,
            .decoration-rw10 img,
            .media-rw img {
                margin: 1em auto !important;
                display: block !important;
            }
            /* Handle tall images */
            img.tall {
                max-height: 80vh !important;
                width: auto !important;
                object-fit: contain !important;
            }
            /* General container rules */
            * {
                max-width: 100% !important;
                box-sizing: border-box !important;
            }
        """.trimIndent()

        return "<style id='reader-base-styles'>$baseStyles</style>"
    }
}
