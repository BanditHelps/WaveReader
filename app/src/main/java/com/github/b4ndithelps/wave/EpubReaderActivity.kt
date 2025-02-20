package com.github.b4ndithelps.wave

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.webkit.WebView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.epub.EpubReader
import java.io.FileInputStream

class EpubReaderActivity : AppCompatActivity() {

    private lateinit var epubWebView: WebView
    private lateinit var epubBook: Book
    private var currentSpineIndex = 0
    private lateinit var gestureDetector: GestureDetector


    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_epub_reader)

        epubWebView = findViewById(R.id.epubWebView)
        epubWebView.settings.javaScriptEnabled = true

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
                    }
                    return true
                }
            })

        epubWebView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
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
                epubWebView.loadData("<html><body><h1>Error: No content found</h1></body></html>", "text/html", "UTF-8")
            }


        } catch (e: Exception) {
            e.printStackTrace()
            epubWebView.loadData("<html><body><h1>Error: Could not load book</h1></body></html>", "text/html", "UTF-8")
        }
    }

    private fun loadSpineItem(index: Int) {
        val spine = epubBook.spine.spineReferences
        if (index in spine.indices) {
            val resource = spine[index].resource
            val htmlContent = String(resource.data, Charsets.UTF_8)
            epubWebView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
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
}