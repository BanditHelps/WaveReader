package com.github.b4ndithelps.wave

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wave_reader.adapter.BookCoverAdapter
import com.github.b4ndithelps.wave.data.AppDatabase
import com.github.b4ndithelps.wave.data.BookData
import com.github.b4ndithelps.wave.model.Book
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.siegmann.epublib.epub.EpubReader
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Also known as the library activity. This is where the user manages their collection.
 * It is also where the user will be able to access the global settings of the App
 */
class HomeActivity : AppCompatActivity(), BookCoverAdapter.OnItemClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BookCoverAdapter
    private var actionMode: ActionMode? = null

    private val bookList = mutableListOf<Book>()
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        // Locate all the elements we will need
        val toolbar : MaterialToolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.bookCoversRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        setSupportActionBar(toolbar)

        // This recycler shows all of the books in an expandable, scrollable grid
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        adapter = BookCoverAdapter(bookList, this)
        recyclerView.adapter = adapter

        db = AppDatabase.getDatabase(this)

        loadBooksFromDatabase()
    }

    /**
     * When any book in the library is clicked, this method will be called, opening up a new
     * reader, using the file path of the book being opened.
     */
    override fun onItemClick(book: Book) {
        val intent = Intent(this, SpotifyEpubReaderActivity::class.java)
        intent.putExtra("bookPath", book.filePath)
        startActivity(intent)
    }

    override fun onItemLongClick(position: Int): Boolean {
        if (actionMode == null) {
            actionMode = startActionMode(actionModeCallback)
            return true
        } else {
            return false
        }
    }

    override fun onSelectionModeChanged(selectedCount: Int) {
        actionMode?.title = "$selectedCount selected"

        if (selectedCount == 0 && actionMode != null) {
            actionMode?.finish()
        }
    }

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu?): Boolean {
            mode.menuInflater.inflate(R.menu.menu_context_book_selection, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.action_delete -> {
                    deleteSelectedBooks()
                    mode.finish()
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            adapter.exitSelectionMode()
            actionMode = null
        }
    }

    private fun deleteSelectedBooks() {
        val selectedBooks = adapter.getSelectedItems()

        adapter.removeSelectedItems()

        // Delete from the db
        CoroutineScope(Dispatchers.IO).launch {
            selectedBooks.forEach {
                db.bookDao().deleteBook(it.filePath)
            }
        }
    }

    // =====================================
    //           Menu Functions
    // =====================================

    /**
     * Top Menu bar container
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    /**
     * Button Handlers for the Top Menu. Includes the following:
     * - Import New Book
     * - Refresh Library
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_import -> {
                openFilePicker()
                true
            }
            R.id.action_refresh -> {
                // Refresh Library button
                bookList.clear()
                loadBooksFromDatabase()
                true
            } else -> super.onOptionsItemSelected(item)
        }
    }


    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // I believe this is making sure that the url is valid
        uri?.let {
            val fileType = contentResolver.getType(it)
            if (fileType == "application/pdf" || fileType == "application/epub+zip") {
                processFile(it)
            } else {
                Toast.makeText(this, "Invalid File Type!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Uses the defined Activity Contract "getContent" to open the file picker, and initiate the
     * process chain.
     */
    private fun openFilePicker() {
        getContent.launch("*/*")
    }


    /**
     * Gets the name and file location of the provided file path, and copies it to the internal
     * storage of the application. Then fires addBookToLibrary().
     */
    private fun processFile(uri: Uri) {
        // Find the file name
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val fileName = it.getString(nameIndex)

                // Copy file to the internal storage
                val file = copyFileToInternalStorage(uri, fileName)

                addBookToLibrary(file, fileName)
            }
        }
    }

    /**
     * A simple method that copies the contents of a file path to the internal storage of the app
     */
    private fun copyFileToInternalStorage(uri: Uri, fileName: String): File {
        val inputStream = contentResolver.openInputStream(uri)
        val file = File(filesDir, fileName)
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        return file
    }

    /**
     * Main method for handling the book library addition options. Does the following:
     * - Acquire the cover image
     * - Construct the book data object
     * - Add to the book database
     */
    private fun addBookToLibrary(file: File, fileName: String) {
        // Assigns the value Cover Image to either the result of getCoverImageFromEpub() or null
        val coverImage = if (fileName.endsWith(".epub")) {
            getCoverImageFromEpub(file)
        } else {
            null
        }

        // Add the book and the starting information to the database
        CoroutineScope(Dispatchers.IO).launch {
            val bookData = BookData(bookPath = file.absolutePath)

            if (fileName.endsWith(".epub")) {
                val epubBook = EpubReader().readEpub(FileInputStream(file))

                bookData.title = epubBook.metadata.titles.firstOrNull() ?: "Unknown Title"
                bookData.authors = epubBook.metadata.authors.joinToString(", ") { it.toString() }
                bookData.totalPages = epubBook.spine.spineReferences.size

                // Do a little cleaning of the file name cause it be nasty sometimes
                val coverImageFilePath = "cover_${bookData.title}_${bookData.authors}.jpg"
                bookData.coverImageFilename = coverImageFilePath.sanitizeFileName()

                // Attempt to save the cover image to internal storage. If it fails, let someone know
                if (!saveCoverImageToStorage(this@HomeActivity, bookData.coverImageFilename.toString(), coverImage)) {
                    Toast.makeText(this@HomeActivity, "Error Saving Cover Image to disk", Toast.LENGTH_SHORT).show()
                }
            } else {
                bookData.title = fileName
                bookData.authors = "Unknown"
                bookData.totalPages = 0
            }
            db.bookDao().insert(bookData)
            // Update the UI on the main thread
            withContext(Dispatchers.Main) {
                loadBooksFromDatabase()
            }
        }
    }

    /**
     * Queries all of the books in the database, and reconstructs the library with them.
     */
    private fun loadBooksFromDatabase() {
        CoroutineScope(Dispatchers.IO).launch {
            val bookDataList = db.bookDao().getAllBooks()
            val newBookList = mutableListOf<Book>()
            bookDataList.forEach{ bookData ->
                val coverImageBitmap = if (bookData.coverImageFilename != null) {
                    loadCoverImageFromStorage(this@HomeActivity, bookData.coverImageFilename)
                } else {
                    null
                }

                // Define the Book Object that is going in the cards of the library
                val book = Book(
                    bookData.bookPath,
                    bookData.title ?: "Unknown",
                    bookData.bookPath,
                    coverImageBitmap
                )

                newBookList.add(book)
            }
            withContext(Dispatchers.Main) {
                bookList.clear()
                bookList.addAll(newBookList)
                adapter.notifyDataSetChanged()
            }
        }
    }


    /**
     * Method to determine the cover image from an epub file. Attempts to first use the built in
     * EPUB library to acquire it, but will execute more advanced search features for improperly
     * configured EPUBs
     */
    private fun getCoverImageFromEpub(file: File): Bitmap? {
        return try {
            val book = EpubReader().readEpub(FileInputStream(file))

            // First, attempt the built in (correctly formatted) epub
            val coverImage = book.coverImage
            if (coverImage != null) {
                return BitmapFactory.decodeStream(coverImage.inputStream)
            }

            // If the cover image is not set up correctly, this section essentially brute forces the
            // EPUB in order to find the image.
            val coverImageResourceStream = extractCoverImage(file)
            if (coverImageResourceStream != null) {
                return BitmapFactory.decodeStream(coverImageResourceStream)
            }

            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * This will unzip and locate the cover image inside of the EPUB. Will search the internal HTML
     * for the cover-image property, and when it finds it, uses the image path in the href to
     * locate the file path of the image.
     */
    private fun extractCoverImage(epubFile: File): InputStream? {
        val zip = ZipFile(epubFile)

        try {
            // First find the content.opf file
            val contentEntry = zip.entries().asSequence()
                .find { it.name.endsWith("content.opf") }
                ?: return null

            // Parse the content.opf XML
            val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val document = zip.getInputStream(contentEntry).use { stream ->
                documentBuilder.parse(stream)
            }

            // Find the item with cover-image property
            val items = document.getElementsByTagName("item")
            var coverImagePath: String? = null

            for (i in 0 until items.length) {
                val item = items.item(i) as Element
                val properties = item.getAttribute("properties")
                if (properties.contains("cover-image")) {
                    coverImagePath = item.getAttribute("href")
                    break
                }
            }

            if (coverImagePath == null) return null

            // Handle relative paths
            val contentDir = File(contentEntry.name).parent ?: ""
            val normalizedPath = when {
                coverImagePath.startsWith("/") -> coverImagePath.substring(1)
                contentDir.isNotEmpty() -> "$contentDir/$coverImagePath"
                else -> coverImagePath
            }.replace('\\', '/')

            // Read the cover image into memory
            val imageEntry = zip.entries().asSequence()
                .find { it.name.replace('\\', '/') == normalizedPath }
                ?: return null


            // Read the entire image into a byte array and return a ByteArrayInputStream
            return zip.getInputStream(imageEntry).use { stream ->
                val bytes = stream.readBytes()
                ByteArrayInputStream(bytes)
            }
        } finally {
            zip.close()
        }
    }

    /**
     * Caches the cover images into the internal storage as jpg files to speed up retrieval.
     */
    private fun saveCoverImageToStorage(context: Context, bookId: String, coverImageBitmap: Bitmap?): Boolean {
        if (coverImageBitmap == null) {
            return false
        }

        // Get the app's internal storage directory
        val directory = context.filesDir

        // Create a file object to store the image in
        val outFile = File(directory, bookId)

        try {
            // Crate a FileOutputStream to write to the actual file
            FileOutputStream(outFile).use { outputStream ->
                ByteArrayOutputStream().use { byteArrayOutputStream ->
                    // Compress the bitmap to JPEG
                    coverImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)

                    // Write the compressed data to the outputStream, aka the file
                    outputStream.write(byteArrayOutputStream.toByteArray())
                    outputStream.flush()
                }
            }
            return true

        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Used to pull cached images out of the internal storage for the library.
     */
    private fun loadCoverImageFromStorage(context: Context, fileName: String?): Bitmap? {
        if (fileName == null) {
            return null
        }

        val directory = context.filesDir
        val file = File(directory, fileName)

        if (!file.exists()) {
            return null
        }

        return try {
            FileInputStream(file).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Makes file names not as disgusting, especially with these books
     */
    private fun String.sanitizeFileName(): String {
        return this
            // Replace any character that isn't alphanumeric, space, hyphen, or underscore
            .replace(Regex("[^a-zA-Z0-9\\s\\-_.]"), "")
            // Replace multiple spaces with single space
            .replace(Regex("\\s+"), " ")
            // Trim spaces from start and end
            .trim()
    }
}