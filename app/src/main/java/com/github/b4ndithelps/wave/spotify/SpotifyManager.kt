package com.github.b4ndithelps.wave.spotify

import com.github.b4ndithelps.wave.model.SpotifyPlaylist
import com.github.b4ndithelps.wave.model.SpotifyTrack

/**
 * Manages Spotify integration with the application.
 * For now, this uses mock data since actual Spotify API integration will be added later.
 */
class SpotifyManager {
    
    private var isPlaying = false
    private var currentPlaylist: SpotifyPlaylist? = null
    private var currentTrack: SpotifyTrack? = null
    private var currentTrackPosition = 0
    
    // A dummy listener to simulate callbacks
    private var playerStateChangeListener: ((isPlaying: Boolean, track: SpotifyTrack?) -> Unit)? = null
    
    /**
     * Get a list of mock playlists
     */
    fun getPlaylists(): List<SpotifyPlaylist> {
        return listOf(
            SpotifyPlaylist(
                id = "1",
                name = "Chill Reading Mix",
                description = "Perfect ambient tracks for reading",
                trackCount = 20,
                creator = "WaveReader",
                tracks = getMockTracks().subList(0, 5)
            ),
            SpotifyPlaylist(
                id = "2",
                name = "Focus & Concentration",
                description = "Instrumental tracks for deep focus",
                trackCount = 25,
                creator = "WaveReader",
                tracks = getMockTracks().subList(5, 10)
            ),
            SpotifyPlaylist(
                id = "3",
                name = "Classical Reading",
                description = "Classical masterpieces for literature lovers",
                trackCount = 18,
                creator = "WaveReader",
                tracks = getMockTracks().subList(10, 15)
            ),
            SpotifyPlaylist(
                id = "4",
                name = "Lo-Fi Study Beats",
                description = "Relaxing lo-fi beats to read to",
                trackCount = 32,
                creator = "WaveReader",
                tracks = getMockTracks().subList(15, 20)
            ),
            SpotifyPlaylist(
                id = "5",
                name = "Sci-Fi Ambience",
                description = "Futuristic soundscapes for sci-fi reading",
                trackCount = 15,
                creator = "WaveReader",
                tracks = getMockTracks().subList(0, 7)
            )
        )
    }
    
    /**
     * Create mock tracks for demo purposes
     */
    private fun getMockTracks(): List<SpotifyTrack> {
        return listOf(
            SpotifyTrack("101", "Ambient Horizons", "Serenity Sounds", "Calm Reflections"),
            SpotifyTrack("102", "Peaceful Piano", "Relaxing Keys", "Piano Collection"),
            SpotifyTrack("103", "Reading Raindrops", "Nature Sounds", "Natural Elements"),
            SpotifyTrack("104", "Cafe Atmosphere", "Ambient Moods", "Urban Spaces"),
            SpotifyTrack("105", "Night Symphony", "Classical Remix", "Modern Classics"),
            SpotifyTrack("106", "Gentle Strings", "Harmony Orchestra", "String Ensembles"),
            SpotifyTrack("107", "Focus Flow", "Concentration Beats", "Deep Work"),
            SpotifyTrack("108", "Meditation Moment", "Mindful Music", "Present Pause"),
            SpotifyTrack("109", "Forest Whispers", "Natural Ambience", "Woodland Sounds"),
            SpotifyTrack("110", "Ocean Waves", "Coastal Sounds", "Seaside Collection"),
            SpotifyTrack("111", "Bach's Study", "Classical Masters", "Baroque Collection"),
            SpotifyTrack("112", "Mozart's Window", "Vienna Classics", "Classical Genius"),
            SpotifyTrack("113", "Chopin Nocturne", "Piano Classics", "Night Pieces"),
            SpotifyTrack("114", "Vivaldi Seasons", "String Orchestra", "Four Seasons"),
            SpotifyTrack("115", "Beethoven Moonlight", "Piano Sonatas", "Moonlight Collection"),
            SpotifyTrack("116", "Lo-Fi Beats", "Chill Producer", "Study Beats Vol.1"),
            SpotifyTrack("117", "Hip Hop Study", "Lo-Fi Collective", "Homework Beats"),
            SpotifyTrack("118", "Jazzy Reading", "Coffee Shop Jazz", "Smooth Jazz"),
            SpotifyTrack("119", "Rainy Day Lo-Fi", "Mellow Beats", "Rainy Day Collection"),
            SpotifyTrack("120", "Nostalgic Nights", "Retro Wave", "Synthwave Memories")
        )
    }
    
    /**
     * Play a specific playlist
     */
    fun playPlaylist(playlistId: String) {
        val playlist = getPlaylists().find { it.id == playlistId }
        if (playlist != null && playlist.tracks.isNotEmpty()) {
            currentPlaylist = playlist
            currentTrackPosition = 0
            currentTrack = playlist.tracks[currentTrackPosition]
            isPlaying = true
            notifyPlayerStateChanged()
        }
    }
    
    /**
     * Play or pause the current track
     */
    fun togglePlayPause() {
        if (currentTrack != null) {
            isPlaying = !isPlaying
            notifyPlayerStateChanged()
        } else if (currentPlaylist != null && currentPlaylist!!.tracks.isNotEmpty()) {
            // Start playing the first track if nothing is currently playing
            currentTrackPosition = 0
            currentTrack = currentPlaylist!!.tracks[currentTrackPosition]
            isPlaying = true
            notifyPlayerStateChanged()
        }
    }
    
    /**
     * Skip to the next track
     */
    fun skipToNext() {
        currentPlaylist?.let { playlist ->
            if (playlist.tracks.isNotEmpty()) {
                currentTrackPosition = (currentTrackPosition + 1) % playlist.tracks.size
                currentTrack = playlist.tracks[currentTrackPosition]
                isPlaying = true
                notifyPlayerStateChanged()
            }
        }
    }
    
    /**
     * Go back to the previous track
     */
    fun skipToPrevious() {
        currentPlaylist?.let { playlist ->
            if (playlist.tracks.isNotEmpty()) {
                currentTrackPosition = if (currentTrackPosition > 0) {
                    currentTrackPosition - 1
                } else {
                    playlist.tracks.size - 1
                }
                currentTrack = playlist.tracks[currentTrackPosition]
                isPlaying = true
                notifyPlayerStateChanged()
            }
        }
    }
    
    /**
     * Check if music is currently playing
     */
    fun isPlaying(): Boolean = isPlaying
    
    /**
     * Get the currently playing track
     */
    fun getCurrentTrack(): SpotifyTrack? = currentTrack
    
    /**
     * Get the current playlist
     */
    fun getCurrentPlaylist(): SpotifyPlaylist? = currentPlaylist
    
    /**
     * Set a listener for player state changes
     */
    fun setPlayerStateChangeListener(listener: (isPlaying: Boolean, track: SpotifyTrack?) -> Unit) {
        playerStateChangeListener = listener
    }
    
    /**
     * Remove the player state change listener
     */
    fun removePlayerStateChangeListener() {
        playerStateChangeListener = null
    }
    
    /**
     * Notify listeners of player state changes
     */
    private fun notifyPlayerStateChanged() {
        playerStateChangeListener?.invoke(isPlaying, currentTrack)
    }
    
    /**
     * Search playlists by name
     */
    fun searchPlaylists(query: String): List<SpotifyPlaylist> {
        if (query.isBlank()) {
            return getPlaylists()
        }
        
        return getPlaylists().filter {
            it.name.contains(query, ignoreCase = true) || 
            it.description.contains(query, ignoreCase = true)
        }
    }
}