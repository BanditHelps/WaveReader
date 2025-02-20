package com.github.b4ndithelps.wave

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wave_reader.adapter.BookCoverAdapter
import com.github.b4ndithelps.wave.model.Book
import com.google.android.material.appbar.MaterialToolbar
import nl.siegmann.epublib.epub.EpubReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class HomeActivity : AppCompatActivity(), BookCoverAdapter.OnItemClickListener {
    private lateinit var adapter: BookCoverAdapter
    private val bookList = mutableListOf<Book>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        val toolbar : MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val recyclerView: RecyclerView = findViewById(R.id.bookCoversRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 3)

//        bookList.add(Book("Book 1", "https://via.placeholder.com/120x180?text=Book+1"))

        adapter = BookCoverAdapter(bookList, this)
        recyclerView.adapter = adapter

        loadBooksFromInternalStorage()

    }

    // When the book is clicked, pass it's file path over to the reader activity.
    override fun onItemClick(book: Book) {
        val intent = Intent(this, EpubReaderActivity::class.java)
        intent.putExtra("bookPath", book.filePath)
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_import -> {
                openFilePicker()
                true
            }
            R.id.action_refresh -> {
                // Refresh Library button
                bookList.clear()
                loadBooksFromInternalStorage()
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

    private fun openFilePicker() {
        getContent.launch("*/*")
    }

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

    private fun copyFileToInternalStorage(uri: Uri, fileName: String): File {
        val inputStream = contentResolver.openInputStream(uri)
        val file = File(filesDir, fileName)
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        return file
    }

    private fun addBookToLibrary(file: File, fileName: String) {
        val coverImage = if (fileName.endsWith(".epub")) {
            getCoverImageFromEpub(file)
        } else {
            null
        }

        // Create a new book object
        val book = Book(fileName, file.absolutePath, coverImage)
        bookList.add(book)
        adapter.notifyDataSetChanged()
    }

    private fun loadBooksFromInternalStorage() {
        val files = filesDir.listFiles()
        files?.forEach { file ->
            if (file.isFile && file.name.endsWith(".pdf") || file.name.endsWith(".epub")) {
                val coverImage = if (file.name.endsWith(".epub")) {
                    getCoverImageFromEpub(file)
                } else {
                    null
                }

                val book = Book(file.name, file.absolutePath, coverImage)
                bookList.add(book)
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun getCoverImageFromEpub(file: File): Bitmap? {
        return try {
            val book = EpubReader().readEpub(FileInputStream(file))
            val coverImage = book.coverImage
            if (coverImage != null) {
                BitmapFactory.decodeStream(coverImage.inputStream)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}