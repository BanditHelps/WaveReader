package com.example.wave_reader.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.b4ndithelps.wave.R
import com.github.b4ndithelps.wave.model.Book


/**
 * Adapter for displaying a list of book covers in a RecyclerView.
 *
 * <p>This adapter handles the display of book covers, including loading cover images
 * from the {@link Book} data or using a placeholder if no cover is available. It also
 * manages selection mode, allowing users to select multiple books.</p>
 *
 * <p><b>Functionality:</b></p>
 * <ul>
 *     <li>Displays book cover images.</li>
 *     <li>Handles loading placeholder images when no cover is available.</li>
 *     <li>Manages selection mode for multiple book selection.</li>
 *     <li>Notifies a listener about item clicks and long clicks.</li>
 *     <li>Provides methods to toggle selection, enter/exit selection mode,
 *         retrieve selected items, and remove selected items.</li>
 * </ul>
 *
 * @param books    The mutable list of {@link Book} objects to be displayed.
 * @param listener The {@link OnItemClickListener} to handle item clicks and long clicks.
 */
class BookCoverAdapter(private val books: MutableList<Book>, private val listener: OnItemClickListener) : RecyclerView.Adapter<BookCoverAdapter.BookCoverViewHolder>() {

    private var selectionMode = false

    interface OnItemClickListener {
        fun onItemClick(book: Book)
        fun onItemLongClick(position: Int): Boolean
        fun onSelectionModeChanged(selectedCount: Int)
    }

    class BookCoverViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val bookCoverImageView: ImageView = itemView.findViewById(R.id.bookCoverImageView)
        val selectionIndicator: ImageView = itemView.findViewById(R.id.selectionIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookCoverViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_book_cover, parent, false)
        return BookCoverViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: BookCoverViewHolder, position: Int) {
        val currentBook = books[position]

        // Set the cover image
        if (currentBook.coverImage != null) {
            holder.bookCoverImageView.setImageBitmap(currentBook.coverImage)
        } else {
            // Load a placeholder image if there is no cover
            Glide.with(holder.itemView.context)
                .load("https://via.placeholder.com/120x180?text=No+Cover")
                .into(holder.bookCoverImageView)
        }

        // Update the selection indicator if the book is selected
        holder.selectionIndicator.visibility = if (currentBook.isSelected) View.VISIBLE else View.GONE

        // Set up the listener for clicking that book
        holder.itemView.setOnClickListener {
            if (selectionMode) {
                toggleSelection(position)
                listener.onSelectionModeChanged(getSelectedItemCount())
            } else {
                listener.onItemClick(currentBook)
            }
        }

        holder.itemView.setOnLongClickListener {
            if (!selectionMode) {
                selectionMode = true
                toggleSelection(position)
                listener.onItemLongClick(position)
                listener.onSelectionModeChanged(getSelectedItemCount())
                return@setOnLongClickListener true
            }
            false
        }
    }

    override fun getItemCount(): Int {
        return books.size
    }

    fun toggleSelection(position: Int) {
        books[position].isSelected = !books[position].isSelected
        notifyItemChanged(position)
    }

    fun enterSelectionMode() {
        selectionMode = true
    }

    fun exitSelectionMode() {
        selectionMode = false
        books.forEach { it.isSelected = false}
        notifyDataSetChanged()
    }

    fun isInSelectionMode() = selectionMode

    fun getSelectedItems(): List<Book> {
        return books.filter { it.isSelected }
    }

    fun getSelectedItemCount(): Int {
        return books.count { it.isSelected }
    }

    fun removeSelectedItems() {
        val toRemove = books.filter { it.isSelected }
        books.removeAll(toRemove)
        notifyDataSetChanged()
    }

    fun getSelectedPositions(): List<Int> {
        return books.mapIndexedNotNull { index, book ->
            if (book.isSelected) index else null
        }
    }
}