package com.github.b4ndithelps.wave.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

// A file that simply defines the functions available to everything else

@Dao
interface BookDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: BookData)

    @Update
    suspend fun update(book: BookData)

    @Query("SELECT * FROM books WHERE bookPath = :bookPath")
    suspend fun getBookByPath(bookPath: String): BookData?

    @Query("SELECT * FROM books")
    suspend fun getAllBooks(): List<BookData>

    // Query to delete a book from the DB
    @Query("DELETE FROM books WHERE bookPath = :bookPath")
    suspend fun deleteBook(bookPath: String)
    
    // Update reading position for a book
    @Query("UPDATE books SET current_spine_index = :spineIndex, current_page_index = :pageIndex WHERE bookPath = :bookPath")
    suspend fun updateReadingPosition(bookPath: String, spineIndex: Int, pageIndex: Int)
}