package com.github.b4ndithelps.wave.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.github.b4ndithelps.wave.model.SpotifyPlaylist

/**
 * Entity to store pinned Spotify playlists in the database
 */
@Entity(tableName = "pinned_playlists")
data class PinnedPlaylist(
    @PrimaryKey
    val playlistId: String,
    val name: String,
    val description: String,
    val imageUrl: String,
    val trackCount: Int,
    val creator: String,
    val pinnedAt: Long = System.currentTimeMillis()
) {
    /**
     * Convert this database entity to a SpotifyPlaylist model
     */
    fun toSpotifyPlaylist(): SpotifyPlaylist {
        return SpotifyPlaylist(
            id = playlistId,
            name = name,
            description = description,
            imageUrl = imageUrl,
            trackCount = trackCount,
            creator = creator,
            tracks = emptyList(),
            isPinned = true
        )
    }
    
    companion object {
        /**
         * Create a PinnedPlaylist entity from a SpotifyPlaylist model
         */
        fun fromSpotifyPlaylist(playlist: SpotifyPlaylist): PinnedPlaylist {
            return PinnedPlaylist(
                playlistId = playlist.id,
                name = playlist.name,
                description = playlist.description,
                imageUrl = playlist.imageUrl,
                trackCount = playlist.trackCount,
                creator = playlist.creator
            )
        }
    }
}