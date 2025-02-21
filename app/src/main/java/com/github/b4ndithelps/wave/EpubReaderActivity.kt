package com.github.b4ndithelps.wave

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.geometry.Rect
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updatePadding
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.Resource
import nl.siegmann.epublib.epub.EpubReader
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

class EpubReaderActivity : AppCompatActivity() {

    private lateinit var epubWebView: WebView
    private lateinit var epubBook: Book
    private var currentSpineIndex = 0
    private lateinit var gestureDetector: GestureDetector

    private lateinit var topMenu: View
    private lateinit var bottomMenu: View
    private lateinit var mainLayout: ConstraintLayout

    private var isMenuVisible = false

    private val imageCache = ConcurrentHashMap<String, ByteArray>()

    // CSS Styles for responsive content
    private val responsiveStyles = """
        <style>
            body {
                margin: 0;
                padding: 16px;
                line-height: 1.6;
            }
            img {
                max-width: 100% !important;
                height: auto !important;
                display: block;
                margin: 1em auto;
            }
            /* Handle very tall images */
            img.tall {
                max-height: 80vh !important;
                width: auto !important;
                object-fit: contain;
            }
            /* Basic text styles */
            p {
                margin: 1em 0;
            }
            /* Ensure content fits screen width */
            * {
                max-width: 100% !important;
                box-sizing: border-box;
            }
        </style>
    """.trimIndent()


    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_epub_reader)

        epubWebView = findViewById(R.id.epubWebView)
        epubWebView.settings.apply {
            javaScriptEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            useWideViewPort = true
            loadWithOverviewMode = true
        }

        // Tweaks to the webview client as to handle image loading
        epubWebView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest
            ): WebResourceResponse? {
                val url = request.url.toString()

                // Check if it is an image req
                if (url.lowercase().matches(Regex(".+\\.(jpg|jpeg|png|gif|svg)"))) {
                    // See if it is cached first
                    val cachedImage = imageCache[url]
                    if (cachedImage != null) {
                        return createImageResponse(cachedImage, url)
                    }

                    // If not in the cache, try to find it in the epub resources
                    try {
                        val imageName = url.substring(url.lastIndexOf("/") + 1)
                        val resource = findResourceByHref(imageName)

                        if (resource != null) {
                            val imageData = resource.data
                            imageCache[url] = imageData
                            return createImageResponse(imageData, url)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Inject JavaScript to handle the tall stuff
                view?.evaluateJavascript("""
                    document.querySelectorAll('img').forEach(img => {
                        img.onload = function() {
                            if (this.naturalHeight > this.naturalWidth * 1.5) {
                                this.classList.add('tall');
                            }
                        }
                        // Handle already loaded images
                        if (img.complete && img.naturalHeight > img.naturalWidth * 1.5) {
                            img.classList.add('tall');
                        }
                    });
                """.trimIndent(), null)
            }
        }

        topMenu = findViewById(R.id.topMenu)
        bottomMenu = findViewById(R.id.bottomMenu)
        mainLayout = findViewById(R.id.mainLayout)

        val statusBarHeight = getStatusBarHeight()

        epubWebView.updatePadding(top = statusBarHeight)

        topMenu.updatePadding(top = statusBarHeight)

        val backButton: Button = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        val bookPath = intent.getStringExtra("bookPath")
        if (bookPath != null) {
            loadEpub(bookPath)
        }

        gestureDetector = GestureDetector(this, object : GestureDetector
        .SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                val x = e.x
                val width = epubWebView.width
                if (x < width / 3) {
                    // Left side
                    goToPreviousPage()
                } else if (x > 2 * width / 3) {
                    // Right side
                    goToNextPage()
                } else {
                    toggleMenu()
                }
                return true
            }
        })

        epubWebView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
        hideMenus()
    }

    private fun getStatusBarHeight(): Int {
        val rectangle = android.graphics.Rect()
        val window: Window = window
        window.decorView.getWindowVisibleDisplayFrame(rectangle)
        return rectangle.top
    }

    private fun createImageResponse(imageData: ByteArray, url: String): WebResourceResponse {
        val mimeType = when {
            url.endsWith(".jpg", true) || url.endsWith(".jpeg", true) -> "image/jpeg"
            url.endsWith(".png", true) -> "image/png"
            url.endsWith(".gif", true) -> "image/gif"
            url.endsWith(".svg", true) -> "image/svg+xml"
            else -> "image/jpeg"
        }

        return WebResourceResponse(
            mimeType,
            "UTF-8",
            ByteArrayInputStream(imageData)
        )
    }

    private fun findResourceByHref(href: String): Resource? {
        // Search in the EPUB resources
        return epubBook.resources.getAll()?.find { resource ->
            resource.href.endsWith(href, ignoreCase = true)
        }
    }


    private fun loadEpub(bookPath: String) {
        try {
            val epubReader = EpubReader()
            epubBook = epubReader.readEpub(FileInputStream(bookPath))

            // Get the first chapter (spine element)
            val spine = epubBook.spine.spineReferences
            if (spine.isNotEmpty()) {
                currentSpineIndex = 0
                loadSpineItem(currentSpineIndex)
            } else {
                // Handle the case where there are no spine elements
                epubWebView.loadData(
                    "<html><body><h1>Error: No content found</h1></body></html>",
                    "text/html",
                    "UTF-8"
                )
            }


        } catch (e: Exception) {
            e.printStackTrace()
            epubWebView.loadData(
                "<html><body><h1>Error: Could not load book</h1></body></html>",
                "text/html",
                "UTF-8"
            )
        }
    }

    /**
     * This is where the actual HTML is loaded into the webview.
     * Further HTML styling gets injected in here before it is passed to the webview.
     */
    private fun loadSpineItem(index: Int) {
        val spine = epubBook.spine.spineReferences
        if (index in spine.indices) {
            val resource = spine[index].resource
            var htmlContent = String(resource.data, Charsets.UTF_8)

            // Insert responsive styles to the HTML Head
            htmlContent = if (htmlContent.contains("<head>", ignoreCase = true)) {
                htmlContent.replace("<head>", "<head>$responsiveStyles", ignoreCase = true)
            } else {
                "<html><head>$responsiveStyles</head><body>$htmlContent</body></html>"
            }

            // Setting the baseURL here for the image references
            epubWebView.loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null)
        }
    }


//    private fun replaceImageReferencesWithBase64(htmlContent: String): String {
//        var modifiedHtml = htmlContent
//        val imgRegex = Regex("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>")
//        val matches = imgRegex.findAll(htmlContent)
//
//        for (match in matches) {
//            val imgTag = match.value
//            var imgSrc = match.groupValues[1]
//
//            // Remove the relative location if it has one
//            imgSrc = imgSrc.removePrefix("../")
//
//            // Find the image resource in the EPUB
//            val imageResource = epubBook.resources.getByHref(imgSrc)
//            if (imageResource != null) {
//                val base64Image = encodeImageToBase64(imageResource)
//                val dataUri = "data:${imageResource.mediaType.name};base64,$base64Image"
//                val newImgTag = imgTag.replace(imgSrc, dataUri)
//                modifiedHtml = modifiedHtml.replace(imgTag, newImgTag)
//            }
//        }
//        return modifiedHtml
//    }

    private fun encodeImageToBase64(imageResource: Resource): String {
        val imageData = imageResource.data
        return Base64.getEncoder().encodeToString(imageData)
    }

    private fun goToPreviousPage() {
        if (currentSpineIndex > 0) {
            currentSpineIndex--
            loadSpineItem(currentSpineIndex)
        }
    }

    private fun goToNextPage() {
        val spine = epubBook.spine.spineReferences
        if (currentSpineIndex < spine.size - 1) {
            currentSpineIndex++
            loadSpineItem(currentSpineIndex)
        }
    }

    private fun toggleMenu() {
        if (isMenuVisible) {
            hideMenus()
        } else {
            showMenus()
        }
        isMenuVisible = !isMenuVisible
    }

    private fun showMenus() {
        topMenu.visibility = View.VISIBLE
        bottomMenu.visibility = View.VISIBLE

        // Animation cause we tryhard like that
        val topMenuAnimator = ObjectAnimator.ofFloat(topMenu, "translationY", -topMenu.height.toFloat(), 0f)
        topMenuAnimator.duration = 300 // ms
        topMenuAnimator.start()

        val bottomMenuAnimator = ObjectAnimator.ofFloat(bottomMenu, "translationY", bottomMenu.height.toFloat(), 0f)
        bottomMenuAnimator.duration = 300 // ms
        bottomMenuAnimator.start()

    }

    private fun hideMenus() {
        // Top menu animation
        val topMenuAnimator = ObjectAnimator.ofFloat(topMenu, "translationY", 0f, -topMenu.height.toFloat())
        topMenuAnimator.duration = 300 // ms
        topMenuAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                topMenu.visibility = View.GONE
            }
        })
        topMenuAnimator.start()


        // Bottom Menu Animation
        val bottomMenuAnimator = ObjectAnimator.ofFloat(bottomMenu, "translationY", 0f, bottomMenu.height.toFloat())
        bottomMenuAnimator.duration = 300 // ms
        bottomMenuAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                bottomMenu.visibility = View.GONE
            }
        })
        bottomMenuAnimator.start()

    }
}