package com.example.wave_reader.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.b4ndithelps.wave.R
import com.github.b4ndithelps.wave.model.Book


class BookCoverAdapter(private val books: List<Book>) : RecyclerView.Adapter<BookCoverAdapter.BookCoverViewHolder>() {

    class BookCoverViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val bookCoverImageView: ImageView = itemView.findViewById(R.id.bookCoverImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookCoverViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_book_cover, parent, false)
        return BookCoverViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: BookCoverViewHolder, position: Int) {
        val currentBook = books[position]
        if (currentBook.coverImage != null) {
            holder.bookCoverImageView.setImageBitmap(currentBook.coverImage)
        } else {
            // Load a placeholder image if there is no cover
            Glide.with(holder.itemView.context)
                .load("https://via.placeholder.com/120x180?text=No+Cover")
                .into(holder.bookCoverImageView)
        }
    }

    override fun getItemCount(): Int {
        return books.size
    }
}