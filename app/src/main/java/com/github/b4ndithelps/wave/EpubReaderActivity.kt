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
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updatePadding
import com.github.b4ndithelps.wave.htmlstyling.StyleManager
import com.github.b4ndithelps.wave.htmlstyling.ThemeType
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.Resource
import nl.siegmann.epublib.epub.EpubReader
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.util.concurrent.ConcurrentHashMap

class EpubReaderActivity : AppCompatActivity() {

    private lateinit var epubWebView: WebView
    private lateinit var epubBook: Book
    private var currentSpineIndex = 0
    private lateinit var gestureDetector: GestureDetector
    private lateinit var styleManager: StyleManager

    // Font Size Stuff
    private lateinit var fontSizeMenu: CardView
    private lateinit var fontSizeSeekBar: SeekBar
    private lateinit var fontSizeText: TextView
    private var isFontMenuVisible = false

    private val MIN_FONT_SIZE = 14f
    private val MAX_FONT_SIZE = 64f
    private val FONT_SIZE_RANGE = MAX_FONT_SIZE - MIN_FONT_SIZE


    private lateinit var topMenu: View
    private lateinit var bottomMenu: View
    private lateinit var mainLayout: ConstraintLayout

    private var isMenuVisible = false

    private val imageCache = ConcurrentHashMap<String, ByteArray>()

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_epub_reader)

        // initialize the font views
        fontSizeMenu = findViewById(R.id.fontSizeMenu)
        fontSizeSeekBar = findViewById(R.id.fontSizeSeekBar)
        fontSizeText = findViewById(R.id.fontSizeText)

        fontSizeMenu.alpha = 0f
        fontSizeMenu.visibility = View.GONE

        findViewById<Button>(R.id.toggleSliderButton).setOnClickListener {
            toggleFontSizeMenu()
        }

        // Font Change listener
        fontSizeSeekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekbar: SeekBar?, progress: Int, fromUser: Boolean) {
                val fontSize = MIN_FONT_SIZE + (progress / 100f) * FONT_SIZE_RANGE
                fontSliderGUIUpdate()
                styleManager.saveTextSize(fontSize)
                loadSpineItem(currentSpineIndex)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Not needed
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Not Needed
            }
        })

        epubWebView = findViewById(R.id.epubWebView)
        epubWebView.settings.apply {
            javaScriptEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            useWideViewPort = true
            loadWithOverviewMode = true
        }

        styleManager = StyleManager(this)
        styleManager.saveTheme(ThemeType.DARK)

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
                htmlContent.replace("<head>", "<head>${styleManager.currentStyle.toCSS()}", ignoreCase = true)
            } else {
                "<html><head>${styleManager.currentStyle.toCSS()}</head><body>$htmlContent</body></html>"
            }

            // Setting the baseURL here for the image references
            epubWebView.loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null)
        }
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

    private fun toggleFontSizeMenu() {
        if (isFontMenuVisible) {
            // Hide menu with animation
            fontSizeMenu.animate()
                .alpha(0f)
                .translationY(100f)
                .setDuration(300)
                .withEndAction {
                    fontSizeMenu.visibility = View.GONE
                }
                .start()
        } else {
            // Show menu with animation
            fontSliderGUIUpdate()
            fontSizeMenu.visibility = View.VISIBLE
            fontSizeMenu.translationY = 100f
            fontSizeMenu.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .start()
        }

        isFontMenuVisible = !isFontMenuVisible
    }

    /**
     * A method used to provide feedback to the font slider to make it display the correct percentage.
     * - Mode 0: Initial update to just load the existing stored value
     * - Mode 1: Update when the slider is moved
     */
    private fun fontSliderGUIUpdate() {
        val currentFontSize = styleManager.currentStyle.textSize

        val progress = ((currentFontSize - MIN_FONT_SIZE) / FONT_SIZE_RANGE * 100).toInt().coerceIn(0, 100)

        fontSizeSeekBar.progress = progress

        // Format to one decimal place for cleaner display
        val formattedSize = String.format("%.1f", currentFontSize)
        fontSizeText.text = "Font Size: $formattedSize px"

    }

}