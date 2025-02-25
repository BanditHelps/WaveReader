package com.github.b4ndithelps.wave.htmlstyling

class HtmlProcessor {
    fun processHtml(htmlContent: String, style: EpubStyle): String {
        var processed = htmlContent

        // Ensure we have proper HTML structure
        if (!processed.contains("<html", ignoreCase = true)) {
            processed = "<html><head></head><body>$processed</body></html>"
        }

        // Insert CSS into the head
        processed = if (processed.contains("<head>", ignoreCase = true)) {
            processed.replace("<head>", "<head>${style.toCSS()}", ignoreCase = true)
        } else if (processed.contains("<html", ignoreCase = true)) {
            processed.replace("<html", "<html><head>${style.toCSS()}</head>", ignoreCase = true)
        } else {
            "<html><head>${style.toCSS()}</head><body>$processed</body></html>"
        }

        // Ensure viewport meta tag for responsiveness
        if (!processed.contains("viewport", ignoreCase = true)) {
            processed = processed.replace("<head>",
                "<head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">",
                ignoreCase = true)
        }

        return processed
    }
}