package com.github.b4ndithelps.wave.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Data Access Object for pinned playlists
 */
@Dao
interface PinnedPlaylistDao {
    
    /**
     * Get all pinned playlists, ordered by when they were pinned (most recent first)
     */
    @Query("SELECT * FROM pinned_playlists ORDER BY pinnedAt DESC")
    suspend fun getAllPinnedPlaylists(): List<PinnedPlaylist>
    
    /**
     * Check if a playlist is pinned
     */
    @Query("SELECT COUNT(*) FROM pinned_playlists WHERE playlistId = :playlistId")
    suspend fun isPlaylistPinned(playlistId: String): Int
    
    /**
     * Pin a playlist
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun pinPlaylist(playlist: PinnedPlaylist)
    
    /**
     * Unpin a playlist
     */
    @Query("DELETE FROM pinned_playlists WHERE playlistId = :playlistId")
    suspend fun unpinPlaylist(playlistId: String)
    
    /**
     * Delete all pinned playlists
     */
    @Query("DELETE FROM pinned_playlists")
    suspend fun deleteAllPinnedPlaylists()
}