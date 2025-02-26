package com.github.b4ndithelps.wave.model

import android.graphics.Bitmap

data class Book(
    val id: String,
    val title: String,
    val filePath: String,
    val coverImage: Bitmap? = null,
    var isSelected: Boolean = false)