package com.github.b4ndithelps.wave.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.b4ndithelps.wave.R
import com.github.b4ndithelps.wave.model.SpotifyPlaylist

/**
 * Adapter for displaying Spotify playlists in a RecyclerView
 * Supports pinned and regular playlists with different view types
 */
class SpotifyPlaylistAdapter(
    private var playlists: List<SpotifyPlaylist>,
    private val onPlaylistSelected: (SpotifyPlaylist) -> Unit,
    private val onPinClicked: (SpotifyPlaylist, Boolean) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_PINNED_PLAYLIST = 1
        const val VIEW_TYPE_REGULAR_PLAYLIST = 2
    }

    private var currentPlayingPlaylistId: String? = null
    private var showPinnedHeader = false
    private var showRegularHeader = false

    // Getters for headers visibility
    private fun hasPinnedPlaylists(): Boolean {
        return playlists.any { it.isPinned }
    }

    private fun hasRegularPlaylists(): Boolean {
        return playlists.any { !it.isPinned }
    }

    // Abstract base ViewHolder for common functionality
    abstract inner class BasePlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        protected val playlistCoverImageView: ImageView = itemView.findViewById(R.id.playlistCoverImageView)
        protected val playlistNameTextView: TextView = itemView.findViewById(R.id.playlistNameTextView)
        protected val playlistDetailsTextView: TextView = itemView.findViewById(R.id.playlistDetailsTextView)
        protected val playlistStatusIcon: ImageView = itemView.findViewById(R.id.playlistStatusIcon)
        protected val playlistPinIcon: ImageView = itemView.findViewById(R.id.playlistPinIcon)

        open fun bind(playlist: SpotifyPlaylist) {
            // Set playlist name
            playlistNameTextView.text = playlist.name
            
            // Set playlist details
            val songsText = "${playlist.trackCount} songs"
            val creatorText = if (playlist.creator.isNotEmpty()) {
                " • Created by ${playlist.creator}"
            } else {
                ""
            }
            playlistDetailsTextView.text = "$songsText$creatorText"
            
            // Set the playlist cover image using glide
            Glide.with(itemView.context).load(playlist.imageUrl).into(playlistCoverImageView)

            
            // Set current playing indicator
            if (playlist.id == currentPlayingPlaylistId) {
                playlistStatusIcon.visibility = View.VISIBLE
            } else {
                playlistStatusIcon.visibility = View.GONE
            }
            
            // Set pin icon state
            playlistPinIcon.setImageResource(
                if (playlist.isPinned) R.drawable.ic_check_circle
                else R.drawable.ic_playlist
            )
            
            // Set click listener for playing the playlist
            itemView.setOnClickListener {
                onPlaylistSelected(playlist)
            }
            
            // Set click listener for pinning/unpinning
            playlistPinIcon.setOnClickListener {
                onPinClicked(playlist, !playlist.isPinned)
            }
        }
    }

    // ViewHolder for pinned playlists
    inner class PinnedPlaylistViewHolder(itemView: View) : BasePlaylistViewHolder(itemView) {
        // Add any pinned-specific UI customization
        override fun bind(playlist: SpotifyPlaylist) {
            super.bind(playlist)
            // We could add special styling for pinned playlists here
        }
    }

    // ViewHolder for regular playlists
    inner class RegularPlaylistViewHolder(itemView: View) : BasePlaylistViewHolder(itemView)

    // Header ViewHolder
    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val headerTextView: TextView = itemView.findViewById(R.id.sectionHeaderText)
        
        fun bind(isPinnedHeader: Boolean) {
            headerTextView.text = if (isPinnedHeader) "Pinned Playlists" else "Other Playlists"
        }
    }

    override fun getItemViewType(position: Int): Int {
        val pinnedPlaylists = playlists.filter { it.isPinned }
        val regularPlaylists = playlists.filter { !it.isPinned }
        
        showPinnedHeader = pinnedPlaylists.isNotEmpty()
        showRegularHeader = regularPlaylists.isNotEmpty() && showPinnedHeader
        
        return when {
            // Pinned header
            showPinnedHeader && position == 0 -> VIEW_TYPE_HEADER
            
            // Pinned playlists
            showPinnedHeader && position <= pinnedPlaylists.size -> VIEW_TYPE_PINNED_PLAYLIST
            
            // Regular header (after all pinned playlists)
            showRegularHeader && position == pinnedPlaylists.size + 1 -> VIEW_TYPE_HEADER
            
            // Regular playlists
            else -> VIEW_TYPE_REGULAR_PLAYLIST
        }
    }
    
    // Helper function to get the actual playlist for a given position
    private fun getPlaylistAtPosition(position: Int): SpotifyPlaylist {
        val pinnedPlaylists = playlists.filter { it.isPinned }
        val regularPlaylists = playlists.filter { !it.isPinned }
        val pinnedOffset = if (showPinnedHeader) 1 else 0
        val regularOffset = if (showRegularHeader) 1 else 0
        
        return when {
            // Within pinned playlists
            showPinnedHeader && position <= pinnedPlaylists.size && position > 0 -> 
                pinnedPlaylists[position - pinnedOffset]
            
            // Within regular playlists
            else -> {
                val adjustedPosition = position - pinnedPlaylists.size - pinnedOffset - regularOffset
                regularPlaylists[adjustedPosition]
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_playlist_section_header, parent, false)
                HeaderViewHolder(view)
            }
            VIEW_TYPE_PINNED_PLAYLIST -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_spotify_playlist, parent, false)
                PinnedPlaylistViewHolder(view)
            }
            else -> { // VIEW_TYPE_REGULAR_PLAYLIST
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_spotify_playlist, parent, false)
                RegularPlaylistViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                val isPinnedHeader = position == 0
                holder.bind(isPinnedHeader)
            }
            is BasePlaylistViewHolder -> {
                val playlist = getPlaylistAtPosition(position)
                holder.bind(playlist)
            }
        }
    }

    override fun getItemCount(): Int {
        val pinnedCount = playlists.count { it.isPinned }
        val regularCount = playlists.count { !it.isPinned }
        
        var totalCount = pinnedCount + regularCount
        
        // Add header for pinned playlists if there are any
        if (pinnedCount > 0) totalCount++
        
        // Add header for regular playlists if there are both pinned and regular playlists
        if (pinnedCount > 0 && regularCount > 0) totalCount++
        
        return totalCount
    }

    fun updatePlaylists(newPlaylists: List<SpotifyPlaylist>) {
        playlists = newPlaylists
        notifyDataSetChanged()
    }

    fun setCurrentPlayingPlaylist(playlistId: String?) {
        val oldPlayingPlaylistId = currentPlayingPlaylistId
        currentPlayingPlaylistId = playlistId
        
        // Since our positions are now more complex with headers,
        // it's simplest to just refresh the whole list
        notifyDataSetChanged()
    }
}