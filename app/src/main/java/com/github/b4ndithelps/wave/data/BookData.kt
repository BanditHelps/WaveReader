package com.github.b4ndithelps.wave.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "books")
data class BookData(
    @PrimaryKey val bookPath: String,
    @ColumnInfo(name = "title") var title: String = "",
    @ColumnInfo(name = "authors") var authors: String = "",
    @ColumnInfo(name = "total_pages") var totalPages: Int = 0,
    @ColumnInfo(name = "current_page") var currentPage: Int = 0,
    @ColumnInfo(name = "progress_percentage") var progressPercentage: Double = 0.0,
    @ColumnInfo(name = "is_read") var isRead: Boolean = false,
    @ColumnInfo(name = "times_read") var timesRead: Int = 0,
    @ColumnInfo(name = "rating") var rating: Int = 0,                                // Out of 5
    @ColumnInfo(name = "total_time_read") var totalTimeRead: Long = 0,               // In Seconds
    @ColumnInfo(name = "cover_image_filename") var coverImageFilename: String? = ""
)
