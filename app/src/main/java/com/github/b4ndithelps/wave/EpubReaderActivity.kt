package com.github.b4ndithelps.wave

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.lifecycle.lifecycleScope
import com.github.b4ndithelps.wave.data.AppDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updatePadding
import com.github.b4ndithelps.wave.data.SavedPosition
import com.github.b4ndithelps.wave.htmlstyling.StyleManager
import com.github.b4ndithelps.wave.htmlstyling.ThemeType
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.Resource
import nl.siegmann.epublib.epub.EpubReader
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.util.concurrent.ConcurrentHashMap

class EpubReaderActivity : AppCompatActivity() {

    // Global Components for the activity
    private lateinit var epubWebView: WebView
    private lateinit var epubBook: Book
    private lateinit var gestureDetector: GestureDetector
    private lateinit var styleManager: StyleManager
    private var currentSpineIndex = 0  // Essentially the current page

    // Font Size Stuff
    private lateinit var fontSizeMenu: CardView
    private lateinit var fontSizeSeekBar: SeekBar
    private lateinit var fontSizeText: TextView
    private var isFontMenuVisible = false

    private val MIN_FONT_SIZE = 14f
    private val MAX_FONT_SIZE = 64f
    private val FONT_SIZE_RANGE = MAX_FONT_SIZE - MIN_FONT_SIZE

    // Top and Bottom Menus
    private lateinit var topMenu: View
    private lateinit var bottomMenu: View
    private lateinit var mainLayout: ConstraintLayout

    private var isMenuVisible = false

    // A cache that enables quick access to the images in the ePub
    private val imageCache = ConcurrentHashMap<String, ByteArray>()

    // Page Management
    private var currentPageIndex = 0  // Current page within a spine
    private var totalPages = 1        // Total pages in the current spine
    private var pageHeight = 0        // Height of the "page" in pixels

    private var pagePositionsMap = mutableMapOf<Int, MutableList<Float>>() // Maps a page index to a list of page positions

    private val PAGE_INFO_BAR_HEIGHT_PX by lazy {
        val heightDp = 8 // Approximate height of your info bar in dp
        val density = resources.displayMetrics.density
        (heightDp * density).toInt() // Convert to pixels
    }

    private var savedPosition: SavedPosition? = null
    private var goToLastPageWhenLoaded = false

    private lateinit var pageInfoBar: LinearLayout
    private lateinit var pageNumberText: TextView
    private lateinit var chapterTitleText: TextView
    private lateinit var progressText: TextView


    private var isSystemUIVisible = true

    /**
     * onCreate is called when the activity is first launched. It is used to setyp the menus,
     * configure the webView, and load the book with any modifications.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_epub_reader)

        initializeGUIComponents()
        configureMenus()

        hideSystemUI()

        // Initialize the StyleManager which handles all HTML overrides and user settings
        styleManager = StyleManager(this)
        styleManager.saveTheme(ThemeType.DARK)

        configureWebView()

        hideMenus()

        // Pull in the book path passed to the intent, and attempt to load it
        val bookPath = intent.getStringExtra("bookPath")
        if (bookPath != null) {
            lifecycleScope.launch {
                prepareAndLoadBook(bookPath)
            }
        }
    }


    // =====================================
    //        Initializing Functions
    // =====================================


    /**
     * Will use the Find By Id to locate and populate all of the elements of the activity in one
     * place. Mostly for making it more readable
     */
    private fun initializeGUIComponents() {
        // Essentially a web browser to display HTML
        epubWebView = findViewById(R.id.epubWebView)

        // Font Related Views
        fontSizeMenu = findViewById(R.id.fontSizeMenu)
        fontSizeSeekBar = findViewById(R.id.fontSizeSeekBar)
        fontSizeText = findViewById(R.id.fontSizeText)

        // Menus
        topMenu = findViewById(R.id.topMenu)
        bottomMenu = findViewById(R.id.bottomMenu)
        mainLayout = findViewById(R.id.mainLayout)

        // Page Tracker
        pageInfoBar = findViewById(R.id.page_info_bar)
        pageNumberText = findViewById(R.id.page_number_text)
        chapterTitleText = findViewById(R.id.chapter_title_text)
        progressText = findViewById(R.id.progress_text)

    }

    /**
     * Handles all of the config related to the Top Menu, Bottom Menu, and their respective buttons
     * This currently includes:
     * - Font Size / Slider
     * - Back Button
     */
    private fun configureMenus() {
        // Font Related Initializers
        fontSizeMenu.alpha = 0f
        fontSizeMenu.visibility = View.GONE

        // Set up the on-press handler for the "Font Size" button
        findViewById<Button>(R.id.toggleFontSliderButton).setOnClickListener {
            toggleFontSizeMenu()
        }

        // Set up the on-press handler for the "Back" button
        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish()
        }

        // Font Change Listener - Called when the font slider is moved
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

        // Make the padding dynamic
        topMenu.updatePadding(top = getStatusBarHeight())
    }

    /**
     * Handles the initial setup of the WebView to allow things like HTML overriding, custom JS,
     * image caching, and more
     */
    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun configureWebView() {
        // Default settings for what is essentially a web browser
        epubWebView.settings.apply {
            javaScriptEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            useWideViewPort = true
            loadWithOverviewMode = true
        }

        epubWebView.updatePadding(top = getStatusBarHeight())

        // The following overrides the webViewClient to add in custom Image loading logic. Will
        // enable both image loading and image caching from the ePub. Will also resize them if
        // needed.
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

                pageHeight = epubWebView.height

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
                    
                    // Calculate pages based on content height and viewport height
                    function calculatePages() {
                        const pageHeight = ${pageHeight - PAGE_INFO_BAR_HEIGHT_PX}; // Subtract the info bar height
                        const contentHeight = document.body.scrollHeight;
                        const pageCount = Math.ceil(contentHeight / pageHeight);
                        
                        // Generate list of scroll positions for each page
                        let pagePositions = [];
                        for (let i = 0; i < pageCount; i++) {
                            pagePositions.push(i * pageHeight);
                        }
                        
                        // Send page information back to Android
                        PageCalculator.onPageCalculationComplete(pageCount, pagePositions.join(','));
                    }
                    
                    // Also add padding to the bottom of the content to prevent text from being cut off
                    document.body.style.paddingBottom = "${PAGE_INFO_BAR_HEIGHT_PX}px";
                    
                    // Calculate once everything is loaded
                    window.onload = calculatePages;
                    // Also calculate now in case onload already fired
                    calculatePages();
                    
                """.trimIndent(), null)
            }
        }

        epubWebView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            // Calculate current page based on scroll position
            updateCurrentPageFromScroll(scrollY)
        }


        // Setup the page turning mechanism
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                val x = e.x
                val width = epubWebView.width
                if (x < width / 3) {
                    // Left side - go to previous page
                    goToPreviousPage()  // Use our consolidated function
                } else if (x > 2 * width / 3) {
                    // Right side - go to next page
                    goToNextPage()  // Use our consolidated function
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


        // Begin of the pagification process - turning spines into pages
        epubWebView.addJavascriptInterface(object: Any() {
            @JavascriptInterface
            fun onPageCalculationComplete(pageCount: Int, pagePositions: String) {
                runOnUiThread {
                    totalPages = pageCount
                    val positions = pagePositions.split(",").map { it.toFloat() }
                    pagePositionsMap[currentSpineIndex] = positions.toMutableList()

                    if (goToLastPageWhenLoaded) {
                        // Go to the last page
                        currentPageIndex = totalPages - 1
                        scrollToPage(currentPageIndex)
                        goToLastPageWhenLoaded = false
                    } else if (savedPosition != null && savedPosition!!.spineIndex == currentSpineIndex) {
                        currentPageIndex = savedPosition!!.pageIndex
                        scrollToPage(currentPageIndex)
                    } else {
                        currentPageIndex = 0
                        scrollToPage(0)
                    }

                    updatePageInfo()
                }
            }
        }, "PageCalculator")

    }


    // =====================================
    //          Helper Functions
    // =====================================

    /**
       * Preprocesses HTML content to improve structure and readability
       * Ensures proper indentation and formatting for better pagination
       */
      private fun preprocessHtml(html: String): String {
          return html
      }

    /**
     * Returns the status bar height for use in positioning the dynamic menus.
     */
    private fun getStatusBarHeight(): Int {
        val rectangle = android.graphics.Rect()
        val window: Window = window
        window.decorView.getWindowVisibleDisplayFrame(rectangle)
        return rectangle.top
    }

    /**
     * Creates a WebResourceResponse for an image based on the provided image data and URL.
     *
     * This function determines the appropriate MIME type for the image based on the file extension
     * in the provided URL. It then constructs a `WebResourceResponse` object that can be used
     * by a WebView to display the image.
     *
     * @param imageData The byte array containing the image data.
     * @param url The URL from which the image was (or would have been) loaded. This is used to
     *        determine the image's MIME type based on its file extension.
     * @return A `WebResourceResponse` object representing the image, ready to be used by a WebView.
     *         The `WebResourceResponse` contains the correct MIME type, encoding ("UTF-8"), and a stream
     *         containing the image data.
     *
     * @throws IllegalArgumentException if the url is null or empty.
     *
     * @throws IllegalArgumentException if the imageData is null or empty.
     */
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

    /**
     * A method to search through the resources inside of the EPUB looking for a specific one.
     * i.e Images, CSS, etc.
     */
    private fun findResourceByHref(href: String): Resource? {
        // Search in the EPUB resources
        return epubBook.resources.getAll()?.find { resource ->
            resource.href.endsWith(href, ignoreCase = true)
        }
    }

    /**
     * Prepares book loading by first loading the saved position, then loading the book
     * This ensures the position is available before the book loads
     */
    private suspend fun prepareAndLoadBook(bookPath: String) {
        try {
            // First, load the saved position from the database
            val position = loadSavedPositionSuspend(bookPath)
            
            // Then load the book with the position already known
            loadEpubWithPosition(bookPath, position)
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                epubWebView.loadData(
                    "<html><body><h1>Error: Could not load book</h1></body></html>",
                    "text/html",
                    "UTF-8"
                )
            }
        }
    }
    
    /**
     * Loads saved position from database in a suspending function
     * that will complete before proceeding with book loading
     */
    private suspend fun loadSavedPositionSuspend(bookPath: String): SavedPosition? {
        return withContext(Dispatchers.IO) {
            try {
                val bookDao = AppDatabase.getDatabase(applicationContext).bookDao()
                val bookData = bookDao.getBookByPath(bookPath)
                
                if (bookData != null) {
                    // Return the position data from the database
                    SavedPosition(
                        bookData.currentSpineIndex,
                        bookData.currentPageIndex
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Opens up the book, and opens the first Spine or chapter.
     * Now takes a pre-loaded position parameter to avoid race conditions
     */
    private suspend fun loadEpubWithPosition(bookPath: String, position: SavedPosition?) {
        withContext(Dispatchers.IO) {
            try {
                val epubReader = EpubReader()
                epubBook = epubReader.readEpub(FileInputStream(bookPath))
                
                // Set the saved position
                savedPosition = position
                
                withContext(Dispatchers.Main) {
                    // Get the first chapter (spine element)
                    val spine = epubBook.spine.spineReferences
                    if (spine.isNotEmpty()) {
                        if (savedPosition != null) {
                            currentSpineIndex = savedPosition!!.spineIndex
                        } else {
                            currentSpineIndex = 0
                        }

                        loadSpineItem(currentSpineIndex)
                    } else {
                        // Handle the case where there are no spine elements
                        epubWebView.loadData(
                            "<html><body><h1>Error: No content found</h1></body></html>",
                            "text/html",
                            "UTF-8"
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    epubWebView.loadData(
                        "<html><body><h1>Error: Could not load book</h1></body></html>",
                        "text/html",
                        "UTF-8"
                    )
                }
            }
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

            htmlContent = preprocessHtml(htmlContent)

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
        if (currentPageIndex > 0) {
            currentPageIndex--
            scrollToPage(currentPageIndex)
        } else {
            if (currentSpineIndex > 0) {
                goToPreviousSpine()
            }
        }
    }

    private fun goToNextPage() {
        if (currentPageIndex < totalPages - 1) {
            currentPageIndex++
            scrollToPage(currentPageIndex)
        } else {
            goToNextSpine()
        }
    }

    private fun scrollToPage(pageIndex: Int) {
        if (pagePositionsMap.containsKey(currentSpineIndex)) {
            val positions = pagePositionsMap[currentSpineIndex]
            if (positions != null && pageIndex < positions.size) {
                val yOffset = positions[pageIndex]
                epubWebView.scrollTo(0, yOffset.toInt())
                currentPageIndex = pageIndex
                updatePageInfo()

                // Save the current position
                saveReaderPosition()
            }
        }
    }

    // New function to update page number based on scroll position
    private fun updateCurrentPageFromScroll(scrollY: Int) {
        if (pagePositionsMap.containsKey(currentSpineIndex)) {
            val positions = pagePositionsMap[currentSpineIndex] ?: return

            // Find which page we're currently on based on scroll position
            for (i in positions.indices) {
                // If this is the last position or the scroll is before the next position
                if (i == positions.size - 1 || scrollY < positions[i + 1].toInt()) {
                    if (currentPageIndex != i) {
                        currentPageIndex = i
                        updatePageInfo()
                        saveReaderPosition()
                    }
                    break
                }
            }
        }
    }

    private fun updatePageInfo() {
        // Update page number
        pageNumberText.text = "Page ${currentPageIndex + 1} of $totalPages"

        // Update chapter title
        val spine = epubBook.spine.spineReferences
        val chapterTitle = if (currentSpineIndex < spine.size) {
            spine[currentSpineIndex].resource.title ?: "Chapter ${currentSpineIndex + 1}"
        } else {
            "Chapter ${currentSpineIndex + 1}"
        }
        chapterTitleText.text = chapterTitle

        // Calculate and update overall progress
        val totalSpines = epubBook.spine.spineReferences.size
        val progress = if (totalSpines > 0) {
            val spineWeight = 1.0f / totalSpines
            val currentSpineProgress = if (totalPages > 0) currentPageIndex.toFloat() / totalPages else 0f
            val overallProgress = (currentSpineIndex * spineWeight) + (currentSpineProgress * spineWeight)
            (overallProgress * 100).toInt()
        } else {
            0
        }
        progressText.text = "$progress%"
    }

    private fun goToNextSpine() {
        if (currentSpineIndex < epubBook.spine.spineReferences.size - 1) {
            currentSpineIndex++
            currentPageIndex = 0 // Reset to the first page
            loadSpineItem(currentSpineIndex)
        }
    }

    // Modify goToPreviousSpine() to go to the last page of previous spine
    private fun goToPreviousSpine() {
        if (currentSpineIndex > 0) {
            currentSpineIndex--
            loadSpineItem(currentSpineIndex)

            // Set a flag to indicate we want to go to the last page when loaded
            goToLastPageWhenLoaded = true
        }
    }

    // Save position function using Room database
    private fun saveReaderPosition() {
        val bookPath = intent.getStringExtra("bookPath") ?: return
        val position = SavedPosition(currentSpineIndex, currentPageIndex)

        lifecycleScope.launch {
            try {
                val bookDao = AppDatabase.getDatabase(applicationContext).bookDao()
                
                // First check if book exists in database
                var bookData = bookDao.getBookByPath(bookPath)
                
                if (bookData != null) {
                    // Update just the position fields
                    bookDao.updateReadingPosition(bookPath, position.spineIndex, position.pageIndex)
                } else {
                    // Book doesn't exist yet, create a minimal record
                    bookData = com.github.b4ndithelps.wave.data.BookData(
                        bookPath = bookPath,
                        title = epubBook.title ?: "Unknown Title",
                        currentSpineIndex = position.spineIndex,
                        currentPageIndex = position.pageIndex
                    )
                    bookDao.insert(bookData)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Old method kept for reference, but no longer used
    // Now using loadSavedPositionSuspend which returns the position directly
    @Deprecated("Use loadSavedPositionSuspend instead")
    private fun loadSavedPosition() {
        // This method is no longer used - see loadSavedPositionSuspend
    }

    private fun toggleMenu() {
        if (isMenuVisible) {
            hideMenus()
            hideSystemUI()
            pageInfoBar.visibility = View.VISIBLE
        } else {
            showMenus()
            showSystemUI()
            pageInfoBar.visibility = View.GONE
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

    // Add this function to toggle system UI visibility
    private fun toggleSystemUI() {
        if (isSystemUIVisible) {
            hideSystemUI()
        } else {
            showSystemUI()
        }
    }

    // Function to hide system UI (status and navigation bars)
    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
        isSystemUIVisible = false
    }

    // Function to show system UI
    private fun showSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        }
        isSystemUIVisible = true
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