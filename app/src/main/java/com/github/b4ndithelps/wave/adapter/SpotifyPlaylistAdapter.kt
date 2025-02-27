package com.github.b4ndithelps.wave.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.b4ndithelps.wave.R
import com.github.b4ndithelps.wave.model.SpotifyPlaylist

class SpotifyPlaylistAdapter(
    private var playlists: List<SpotifyPlaylist>,
    private val onPlaylistSelected: (SpotifyPlaylist) -> Unit
) : RecyclerView.Adapter<SpotifyPlaylistAdapter.PlaylistViewHolder>() {

    private var currentPlayingPlaylistId: String? = null

    inner class PlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val playlistCoverImageView: ImageView = itemView.findViewById(R.id.playlistCoverImageView)
        private val playlistNameTextView: TextView = itemView.findViewById(R.id.playlistNameTextView)
        private val playlistDetailsTextView: TextView = itemView.findViewById(R.id.playlistDetailsTextView)
        private val playlistStatusIcon: ImageView = itemView.findViewById(R.id.playlistStatusIcon)

        fun bind(playlist: SpotifyPlaylist) {
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
            
            // Set the playlist cover image
            // In a real implementation, use an image loading library like Glide
            // Glide.with(itemView.context).load(playlist.imageUrl).into(playlistCoverImageView)
            
            // Set current playing indicator
            if (playlist.id == currentPlayingPlaylistId) {
                playlistStatusIcon.visibility = View.VISIBLE
            } else {
                playlistStatusIcon.visibility = View.GONE
            }
            
            // Set click listener
            itemView.setOnClickListener {
                onPlaylistSelected(playlist)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_spotify_playlist, parent, false)
        return PlaylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        holder.bind(playlists[position])
    }

    override fun getItemCount(): Int = playlists.size

    fun updatePlaylists(newPlaylists: List<SpotifyPlaylist>) {
        playlists = newPlaylists
        notifyDataSetChanged()
    }

    fun setCurrentPlayingPlaylist(playlistId: String?) {
        val oldPlayingPlaylistId = currentPlayingPlaylistId
        currentPlayingPlaylistId = playlistId
        
        // Update UI for old playing playlist
        oldPlayingPlaylistId?.let { oldId ->
            val oldIndex = playlists.indexOfFirst { it.id == oldId }
            if (oldIndex >= 0) {
                notifyItemChanged(oldIndex)
            }
        }
        
        // Update UI for new playing playlist
        playlistId?.let { newId ->
            val newIndex = playlists.indexOfFirst { it.id == newId }
            if (newIndex >= 0) {
                notifyItemChanged(newIndex)
            }
        }
    }
}