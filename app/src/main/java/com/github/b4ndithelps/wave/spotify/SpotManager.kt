package com.github.b4ndithelps.wave.spotify

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.github.b4ndithelps.wave.model.SpotifyPlaylist
import com.github.b4ndithelps.wave.model.SpotifyTrack
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.android.appremote.api.error.CouldNotFindSpotifyApp
import com.spotify.android.appremote.api.error.NotLoggedInException
import com.spotify.android.appremote.api.error.UserNotAuthorizedException
import com.spotify.protocol.client.Subscription
import com.spotify.protocol.types.Image
import com.spotify.protocol.types.ListItem
import com.spotify.protocol.types.PlayerState
import com.spotify.protocol.types.Track

/**
 * Manages Spotify integration with the app using Spotify App Remote
 * Provides functionality for playback control, playlist management, and more
 */
class SpotManager(context: Context) {
    private var spotifyAppRemote: SpotifyAppRemote? = null
    private val contextRef = context
    private var authenticated: Boolean = false
    private var currentPlayerState: PlayerState? = null
    private var playerStateSubscription: Subscription<PlayerState>? = null
    private var currentTrack: SpotifyTrack? = null
    private var currentPlaylist: SpotifyPlaylist? = null
    private var isPlaying: Boolean = false
    
    // Callback for player state changes to update UI
    private var playerStateChangeCallback: ((isPlaying: Boolean, track: SpotifyTrack?) -> Unit)? = null
    
    // Callback for when album art is loaded
    private var albumArtCallback: ((bitmap: Bitmap) -> Unit)? = null
    
    /**
     * Initiates the authentication process with Spotify
     */
    fun authenticate() {
        val connectionParams = ConnectionParams.Builder(SpotifyAuthConfig.CLIENT_ID)
            .setRedirectUri(SpotifyAuthConfig.REDIRECT_URI)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(contextRef, connectionParams, object: Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                Log.d("SpotManager", "Connected to Spotify Successfully!")
                authenticated = true
                
                // Subscribe to player state changes
                subscribeToPlayerState()
            }

            override fun onFailure(throwable: Throwable) {
                Log.e("SpotManager", throwable.message, throwable)
                
                when (throwable) {
                    is NotLoggedInException, is UserNotAuthorizedException -> {
                        // Handle auth errors - user needs to login to Spotify
                        Log.e("SpotManager", "User needs to log in to Spotify")
                    }
                    is CouldNotFindSpotifyApp -> {
                        // Handle case where Spotify app is not installed
                        Log.e("SpotManager", "Spotify app not installed")
                    }
                    else -> {
                        // Handle other errors
                        Log.e("SpotManager", "Error connecting to Spotify: ${throwable.message}")
                    }
                }
                
                authenticated = false
            }
        })
    }

    /**
     * Disconnects from Spotify App Remote
     */
    fun disconnect() {
        playerStateSubscription?.cancel()
        playerStateSubscription = null
        
        spotifyAppRemote?.let {
            SpotifyAppRemote.disconnect(it)
            spotifyAppRemote = null
            authenticated = false
        }
        
        currentPlayerState = null
        isPlaying = false
    }
    
    /**
     * Sets the callback for player state changes
     */
    fun setPlayerStateChangeListener(callback: (isPlaying: Boolean, track: SpotifyTrack?) -> Unit) {
        playerStateChangeCallback = callback
        
        // Immediately update with current state if available
        if (authenticated && currentTrack != null) {
            callback(isPlaying, currentTrack)
        }
    }
    
    /**
     * Sets the callback for album art loading
     */
    fun setAlbumArtLoadCallback(callback: (bitmap: Bitmap) -> Unit) {
        albumArtCallback = callback
        
        // If we already have a track loaded, get its album art
        currentPlayerState?.track?.let { track ->
            loadAlbumArt(track)
        }
    }
    
    /**
     * Subscribes to player state changes from Spotify
     */
    private fun subscribeToPlayerState() {
        spotifyAppRemote?.playerApi?.subscribeToPlayerState()?.setEventCallback { playerState ->
            currentPlayerState = playerState
            isPlaying = !playerState.isPaused
            
            // Only process changes if the track has changed
            val track = playerState.track
            if (track != null && (currentTrack == null || currentTrack?.id != track.uri)) {
                processTrackChange(track)
            }
            
            // Notify callback of state change
            playerStateChangeCallback?.invoke(isPlaying, currentTrack)
        }?.also {
            playerStateSubscription = it
        }
    }
    
    /**
     * Processes track changes and loads album art
     */
    private fun processTrackChange(track: Track) {
        // Create a SpotifyTrack from the Spotify SDK Track
        currentTrack = SpotifyTrack(
            id = track.uri,
            name = track.name,
            artist = track.artist.name,
            album = track.album.name
        )
        
        // Load album art
        loadAlbumArt(track)
    }
    
    /**
     * Loads album art for the current track
     */
    private fun loadAlbumArt(track: Track) {
        spotifyAppRemote?.imagesApi?.getImage(track.imageUri, Image.Dimension.LARGE)
            ?.setResultCallback { bitmap ->
                albumArtCallback?.invoke(bitmap)
            }
    }
    
    /**
     * Toggles between play and pause
     */
    fun togglePlayPause() {
        if (!authenticated) return
        
        spotifyAppRemote?.playerApi?.let { playerApi ->
            if (isPlaying) {
                playerApi.pause()
            } else {
                playerApi.resume()
            }
        }
    }
    
    /**
     * Plays a specific track by URI
     */
    fun playTrack(trackUri: String) {
        if (!authenticated) return
        
        spotifyAppRemote?.playerApi?.play(trackUri)
    }
    
    /**
     * Skips to the next track
     */
    fun skipToNext() {
        if (!authenticated) return
        
        spotifyAppRemote?.playerApi?.skipNext()
    }
    
    /**
     * Skips to the previous track
     */
    fun skipToPrevious() {
        if (!authenticated) return
        
        spotifyAppRemote?.playerApi?.skipPrevious()
    }
    
    /**
     * Plays a specific playlist
     */
    fun playPlaylist(playlistUri: String) {
        if (!authenticated) return
        
        // Format the URI if it's just an ID
        val fullUri = if (playlistUri.startsWith("spotify:playlist:")) {
            playlistUri
        } else {
            "spotify:playlist:$playlistUri"
        }
        
        spotifyAppRemote?.playerApi?.play(fullUri)
    }
    
    /**
     * Fetches user's playlists using App Remote
     * Note: This is a simpler alternative to using the Web API
     */
    fun fetchPlaylists(callback: (List<SpotifyPlaylist>) -> Unit) {
        if (!authenticated) {
            callback(emptyList())
            return
        }
        
        spotifyAppRemote?.contentApi?.getRecommendedContentItems("playlists")
            ?.setResultCallback { result ->
                val playlists = result.items.map { item ->
                    convertToSpotifyPlaylist(item)
                }
                callback(playlists)
            }
            ?.setErrorCallback { throwable ->
                Log.e("SpotManager", "Error fetching playlists: ${throwable.message}")
                callback(emptyList())
            }
    }
    
    /**
     * Converts a ListItem from Spotify SDK to our app's SpotifyPlaylist model
     */
    private fun convertToSpotifyPlaylist(item: ListItem): SpotifyPlaylist {
        // Extract the playlist ID from the URI
        val uri = item.uri
        val id = if (uri.startsWith("spotify:playlist:")) {
            uri.removePrefix("spotify:playlist:")
        } else {
            uri
        }
        
        return SpotifyPlaylist(
            id = id,
            name = item.title,
            description = item.subtitle ?: "",
            imageUrl = item.imageUri.toString()
        )
    }
    
    /**
     * Returns the current playback state
     */
    fun isPlaying(): Boolean = isPlaying
    
    /**
     * Returns the current track
     */
    fun getCurrentTrack(): SpotifyTrack? = currentTrack
    
    /**
     * Returns whether the manager is authenticated with Spotify
     */
    fun isAuthenticated(): Boolean = authenticated
    
    /**
     * Search for playlists
     * Note: Limited functionality through App Remote. For better search,
     * you would need to implement the Web API
     */
    fun searchPlaylists(query: String, callback: (List<SpotifyPlaylist>) -> Unit) {
        if (!authenticated) {
            callback(emptyList())
            return
        }
        
        // Simple implementation using content API to search
        spotifyAppRemote?.contentApi?.getRecommendedContentItems("playlists")
            ?.setResultCallback { result ->
                val filteredPlaylists = result.items
                    .filter { it.title.contains(query, ignoreCase = true) }
                    .map { convertToSpotifyPlaylist(it) }
                callback(filteredPlaylists)
            }
            ?.setErrorCallback {
                callback(emptyList())
            }
    }
    
    /**
     * Enables/disables repeat mode
     */
    fun setRepeat(on: Boolean) {
        if (!authenticated) return

        val onInt = if (on) 1 else 0
        spotifyAppRemote?.playerApi?.setRepeat(onInt)
    }
    
    /**
     * Enables/disables shuffle mode
     */
    fun setShuffle(on: Boolean) {
        if (!authenticated) return
        
        spotifyAppRemote?.playerApi?.setShuffle(on)
    }
    
    /**
     * Gets the image from the given URI
     */
    fun getImage(imageUri: String, callback: (Bitmap) -> Unit) {
        if (!authenticated) return
        
        val uri = if (imageUri.startsWith("spotify:image:")) {
            Uri.parse(imageUri)
        } else {
            Uri.parse("spotify:image:$imageUri")
        }
    }
}