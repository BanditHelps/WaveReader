package com.github.b4ndithelps.wave.spotify

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import com.github.b4ndithelps.wave.model.SpotifyPlaylist
import com.github.b4ndithelps.wave.model.SpotifyTrack
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.android.appremote.api.error.CouldNotFindSpotifyApp
import com.spotify.android.appremote.api.error.NotLoggedInException
import com.spotify.android.appremote.api.error.UserNotAuthorizedException
import com.spotify.protocol.client.Subscription
import com.spotify.protocol.types.Image
import com.spotify.protocol.types.PlayerState
import com.spotify.protocol.types.Track
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url
import java.io.InputStream
import java.net.URL

/**
 * Spotify Web API data models
 */
data class SpotifyUser(
    val id: String,
    val display_name: String
)

data class SpotifyPagingObject<T>(
    val href: String,
    val items: List<T>,
    val limit: Int,
    val next: String?,
    val offset: Int,
    val previous: String?,
    val total: Int
)

data class SpotifyImage(
    val height: Int?,
    val width: Int?,
    val url: String
)

data class SpotifyPlaylistResponse(
    val id: String,
    val name: String,
    val description: String,
    val images: List<SpotifyImage>,
    val owner: SpotifyUser,
    val tracks: SpotifyPagingObject<SpotifyPlaylistTrack>
)

data class SpotifyPlaylistsResponse(
    val href: String,
    val items: List<SpotifyPlaylistResponse>,
    val limit: Int,
    val next: String?,
    val offset: Int,
    val previous: String?,
    val total: Int
)

data class SpotifyArtist(
    val id: String,
    val name: String
)

data class SpotifyAlbum(
    val id: String,
    val name: String,
    val images: List<SpotifyImage>
)

data class SpotifyTrackResponse(
    val id: String,
    val name: String,
    val artists: List<SpotifyArtist>,
    val album: SpotifyAlbum,
    val duration_ms: Long,
    val uri: String
)

data class SpotifyPlaylistTrack(
    val added_at: String,
    val track: SpotifyTrackResponse
)

data class SpotifyPlaylistTracksResponse(
    val href: String,
    val items: List<SpotifyPlaylistTrack>,
    val limit: Int,
    val next: String?,
    val offset: Int,
    val previous: String?,
    val total: Int
)

data class SpotifySearchResponse(
    val playlists: SpotifyPagingObject<SpotifyPlaylistResponse>?
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: Int,
    @SerializedName("refresh_token") val refreshToken: String?
)

data class PlayerItem(
    val uri: String
)

/**
 * Spotify Web API interface
 */
interface SpotifyService {
    @GET("me/playlists")
    fun getUserPlaylists(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Call<SpotifyPlaylistsResponse>
    
    @GET("playlists/{playlist_id}")
    fun getPlaylist(
        @Path("playlist_id") playlistId: String
    ): Call<SpotifyPlaylistResponse>
    
    @GET("playlists/{playlist_id}/tracks")
    fun getPlaylistTracks(
        @Path("playlist_id") playlistId: String,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): Call<SpotifyPlaylistTracksResponse>
    
    @GET("search")
    fun search(
        @Query("q") query: String,
        @Query("type") type: String,
        @Query("limit") limit: Int = 20
    ): Call<SpotifySearchResponse>
    
    @GET
    fun downloadImage(@Url url: String): Call<ResponseBody>
}

/**
 * Manages Spotify integration with the app using both Spotify App Remote and Web API
 * Provides functionality for playback control, playlist management, and more
 */
class SpotManager(private val context: Context) {
    private var refreshToken: String? = null
    private var currentBookPath: String? = null // Stores the current book path
    private var spotifyAppRemote: SpotifyAppRemote? = null
    private var authenticated: Boolean = false
    private var webApiAuthenticated: Boolean = false
    private var accessToken: String? = null
    private var currentPlayerState: PlayerState? = null
    private var playerStateSubscription: Subscription<PlayerState>? = null
    private var currentTrack: SpotifyTrack? = null
    private var currentPlaylist: SpotifyPlaylist? = null
    private var isPlaying: Boolean = false
    private var authAttempted: Boolean = false
    private var codeVerifier: String? = null
    private var authUrl: Uri? = null

    private var isProcessingAuthResponse: Boolean = false // tracks the callback progress
    
    // Retrofit client for Web API
    private lateinit var spotifyService: SpotifyService
    
    // Callback for player state changes to update UI
    private var playerStateChangeCallback: ((isPlaying: Boolean, track: SpotifyTrack?) -> Unit)? = null
    
    // Callback for when album art is loaded
    private var albumArtCallback: ((bitmap: Bitmap) -> Unit)? = null

    fun storeCodeVerifier(codeVerifier: String) {
        this.codeVerifier = codeVerifier
    }

    fun getCodeVerifier(): String? {
        return codeVerifier
    }

    fun setProcessingCallback(progress: Boolean) {
        isProcessingAuthResponse = progress
    }

    fun getProcessingCallback(): Boolean {
        return isProcessingAuthResponse
    }

    /**
     * Initiates the authentication process with Spotify
     * This will handle both App Remote and Web API authentication
     */
    fun authenticate() {
        // First authenticate with App Remote for playback control
        authenticateAppRemote()
        
        // Then authenticate with Web API for enhanced data access
        authenticateWebApi()
    }

    fun attemptedAuth(): Boolean {
        return authAttempted
    }

    fun setAuthAttempted() {
        authAttempted = true
    }

    // =============================
    //      App Remote Section
    // =============================
    
    /**
     * AppRemote - Authenticates with Spotify App Remote for playback control
     */
    private fun authenticateAppRemote() {
        val connectionParams = ConnectionParams.Builder(SpotifyAuthConfig.CLIENT_ID)
            .setRedirectUri(SpotifyAuthConfig.REDIRECT_URI)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(context, connectionParams, object: Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                Log.d("SpotManager", "Connected to Spotify App Remote Successfully!")
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
     * AppRemote - Sets the callback for player state changes
     */
    fun setPlayerStateChangeListener(callback: (isPlaying: Boolean, track: SpotifyTrack?) -> Unit) {
        playerStateChangeCallback = callback

        // Immediately update with current state if available
        if (authenticated && currentTrack != null) {
            callback(isPlaying, currentTrack)
        }
    }

    /**
     * AppRemote - Sets the callback for album art loading
     */
    fun setAlbumArtLoadCallback(callback: (bitmap: Bitmap) -> Unit) {
        albumArtCallback = callback

        // If we already have a track loaded, get its album art
        currentPlayerState?.track?.let { track ->
            loadAlbumArt(track)
        }
    }

    /**
     * AppRemote - Subscribes to player state changes from Spotify.
     * Makes it so that the player updates automatically when a new song is being played
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
     * AppRemote - Processes track changes and loads album art.
     * Called from the playerStateChanged event
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
     * AppRemote - Loads album art for the current track
     */
    private fun loadAlbumArt(track: Track) {
        spotifyAppRemote?.imagesApi?.getImage(track.imageUri, Image.Dimension.LARGE)
            ?.setResultCallback { bitmap ->
                albumArtCallback?.invoke(bitmap)
            }
    }

    /**
     * AppRemote - Toggles between play and pause
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
     * AppRemote - Plays a specific track by URI
     */
    fun playTrack(trackUri: String) {
        if (!authenticated) return

        // Format the URI if it's just an ID
        val fullUri = if (trackUri.startsWith("spotify:track:")) {
            trackUri
        } else {
            "spotify:track:$trackUri"
        }

        spotifyAppRemote?.playerApi?.play(fullUri)
    }

    /**
     * AppRemote - Skips to the next track
     */
    fun skipToNext() {
        if (!authenticated) return

        spotifyAppRemote?.playerApi?.skipNext()
    }

    /**
     * AppRemote - Skips to the previous track
     */
    fun skipToPrevious() {
        if (!authenticated) return

        spotifyAppRemote?.playerApi?.skipPrevious()
    }

    /**
     * AppRemote - Plays a specific playlist
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



    // ==========================
    //      Web API Section
    // ==========================
    
    /**
     * WebAPI - Authenticates with Spotify Web API for enhanced data access
     */
    private fun authenticateWebApi() {
        val request = AuthorizationRequest.Builder(
            SpotifyAuthConfig.CLIENT_ID,
            AuthorizationResponse.Type.TOKEN,
            SpotifyAuthConfig.REDIRECT_URI
        )
        .setScopes(SpotifyAuthConfig.SCOPES.split(" ").toTypedArray())
        .build()
        
        // Open the authentication page in Custom Tabs
        val intent = CustomTabsIntent.Builder().build()
        val uri = Uri.parse(request.toUri().toString())
        intent.launchUrl(context, uri)
    }
    
    /**
     * Process authentication response
     * Call this method from your activity's onNewIntent or similar method
     */
    /**
     * Process authentication response
     * Call this method with the intent containing token information
     */
    fun processAuthResponse(intent: Intent) {
        try {
            // Extract tokens directly from the intent
            val accessToken = intent.getStringExtra("access_token")
            val refreshToken = intent.getStringExtra("refresh_token")
            val expiresIn = intent.getIntExtra("expires_in", 0)

            if (accessToken != null) {
                this.accessToken = accessToken
                this.refreshToken = refreshToken
                webApiAuthenticated = true

                Log.d("SpotManager", "Authenticated with Web API successfully!")

                // Initialize Retrofit client with the token
                initializeWebApiClient(accessToken)
            } else {
                Log.e("SpotManager", "No access token received")
                webApiAuthenticated = false
            }
        } catch (e: Exception) {
            Log.e("SpotManager", "Error processing auth response", e)
            webApiAuthenticated = false
        }
    }
    
    /**
     * Initializes the Retrofit client for Spotify Web API
     */
    private fun initializeWebApiClient(token: String) {
        // Create an interceptor to add the auth token to requests
        val authInterceptor = Interceptor { chain ->
            val newRequest = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
            chain.proceed(newRequest)
        }
        
        // Add logging for debug builds
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        // Create OkHttpClient with the interceptor
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
        
        // Create Gson converter that handles null values
        val gson = GsonBuilder()
            .serializeNulls()
            .create()
        
        // Create Retrofit instance
        val retrofit = Retrofit.Builder()
            .baseUrl(SpotifyAuthConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        
        // Create the service
        spotifyService = retrofit.create(SpotifyService::class.java)
    }

    /**
     * Disconnects from Spotify App Remote and Web API
     */
    fun disconnect() {
        playerStateSubscription?.cancel()
        playerStateSubscription = null
        
        spotifyAppRemote?.let {
            SpotifyAppRemote.disconnect(it)
            spotifyAppRemote = null
            authenticated = false
        }
        
        // Clear Web API auth
        accessToken = null
        webApiAuthenticated = false
        
        currentPlayerState = null
        isPlaying = false
    }
    
    /**
     * WebAPI - Fetches user's playlists using Web API for enhanced data
     */
    suspend fun fetchPlaylists(): List<SpotifyPlaylist> {
        return withContext(Dispatchers.IO) {
            try {
                if (!webApiAuthenticated || accessToken == null) {
                    return@withContext emptyList<SpotifyPlaylist>()
                }
                
                val response = spotifyService.getUserPlaylists().execute()
                if (response.isSuccessful) {
                    val playlistResponse = response.body()
                    val playlists = playlistResponse?.items?.map { apiPlaylist ->
                        SpotifyPlaylist(
                            id = apiPlaylist.id,
                            name = apiPlaylist.name,
                            description = apiPlaylist.description,
                            imageUrl = if (apiPlaylist.images.isNotEmpty()) apiPlaylist.images[0].url else "",
                            trackCount = apiPlaylist.tracks.total,
                            creator = apiPlaylist.owner.display_name
                        )
                    } ?: emptyList()
                    return@withContext playlists
                } else {
                    Log.e("SpotManager", "Error fetching playlists: ${response.code()} ${response.message()}")
                    return@withContext emptyList<SpotifyPlaylist>()
                }
            } catch (e: Exception) {
                Log.e("SpotManager", "Exception fetching playlists", e)
                return@withContext emptyList<SpotifyPlaylist>()
            }
        }
    }
    
    /**
     * Fetch a specific playlist's details using Web API
     */
    suspend fun getPlaylist(playlistId: String): SpotifyPlaylist? {
        return withContext(Dispatchers.IO) {
            try {
                if (!webApiAuthenticated || accessToken == null) {
                    return@withContext null
                }
                
                val response = spotifyService.getPlaylist(playlistId).execute()
                if (response.isSuccessful) {
                    val apiPlaylist = response.body() ?: return@withContext null
                    
                    // Get tracks for the playlist
                    val tracks = getPlaylistTracks(playlistId)
                    
                    return@withContext SpotifyPlaylist(
                        id = apiPlaylist.id,
                        name = apiPlaylist.name,
                        description = apiPlaylist.description,
                        imageUrl = if (apiPlaylist.images.isNotEmpty()) apiPlaylist.images[0].url else "",
                        trackCount = apiPlaylist.tracks.total,
                        creator = apiPlaylist.owner.display_name,
                        tracks = tracks
                    )
                } else {
                    Log.e("SpotManager", "Error fetching playlist: ${response.code()} ${response.message()}")
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e("SpotManager", "Exception fetching playlist", e)
                return@withContext null
            }
        }
    }
    
    /**
     * Get all tracks for a playlist, handling pagination
     */
    private suspend fun getPlaylistTracks(playlistId: String): List<SpotifyTrack> {
        return withContext(Dispatchers.IO) {
            val allTracks = mutableListOf<SpotifyTrack>()
            try {
                var offset = 0
                var hasMore = true
                
                while (hasMore) {
                    val response = spotifyService.getPlaylistTracks(playlistId, 100, offset).execute()
                    if (response.isSuccessful) {
                        val tracksResponse = response.body()
                        val tracks = tracksResponse?.items?.map { item ->
                            val track = item.track
                            SpotifyTrack(
                                id = track.uri,
                                name = track.name,
                                artist = track.artists.joinToString(", ") { it.name },
                                album = track.album.name,
                                albumImageUrl = if (track.album.images.isNotEmpty()) track.album.images[0].url else "",
                                durationMs = track.duration_ms
                            )
                        } ?: emptyList()
                        
                        allTracks.addAll(tracks)
                        
                        // Check if there are more tracks to fetch
                        if (tracksResponse?.next != null) {
                            offset += 100
                        } else {
                            hasMore = false
                        }
                    } else {
                        Log.e("SpotManager", "Error fetching playlist tracks: ${response.code()} ${response.message()}")
                        hasMore = false
                    }
                }
            } catch (e: Exception) {
                Log.e("SpotManager", "Exception fetching playlist tracks", e)
            }
            
            return@withContext allTracks
        }
    }
    
    /**
     * Search for playlists using Web API
     */
    suspend fun searchPlaylists(query: String): List<SpotifyPlaylist> {
        return withContext(Dispatchers.IO) {
            try {
                if (!webApiAuthenticated || accessToken == null) {
                    return@withContext emptyList<SpotifyPlaylist>()
                }
                
                val response = spotifyService.search(query, "playlist").execute()
                if (response.isSuccessful) {
                    val searchResponse = response.body()
                    val playlists = searchResponse?.playlists?.items?.map { apiPlaylist ->
                        SpotifyPlaylist(
                            id = apiPlaylist.id,
                            name = apiPlaylist.name,
                            description = apiPlaylist.description,
                            imageUrl = if (apiPlaylist.images.isNotEmpty()) apiPlaylist.images[0].url else "",
                            trackCount = apiPlaylist.tracks.total,
                            creator = apiPlaylist.owner.display_name
                        )
                    } ?: emptyList()
                    return@withContext playlists
                } else {
                    Log.e("SpotManager", "Error searching playlists: ${response.code()} ${response.message()}")
                    return@withContext emptyList<SpotifyPlaylist>()
                }
            } catch (e: Exception) {
                Log.e("SpotManager", "Exception searching playlists", e)
                return@withContext emptyList<SpotifyPlaylist>()
            }
        }
    }
    
    /**
     * Download album art image from the Web API
     */
    suspend fun downloadImage(imageUrl: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream: InputStream = URL(imageUrl).openStream()
                return@withContext BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                Log.e("SpotManager", "Error downloading image", e)
                return@withContext null
            }
        }
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
     * Returns whether the manager is authenticated with Spotify App Remote
     */
    fun isRemoteAuthenticated(): Boolean = authenticated
    
    /**
     * Returns whether the manager is authenticated with Spotify Web API
     */
    fun isWebApiAuthenticated(): Boolean = webApiAuthenticated
    
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

    fun saveAuthUrl(authUrl: Uri) {
        this.authUrl = authUrl
    }

    fun loadAuthUrl(): Uri? {
        return authUrl
    }

    /**
     * Store current book path so it can be retrieved later
     */
    fun setCurrentBookPath(path: String?) {
        this.currentBookPath = path
    }
    
    /**
     * Get the current book path
     */
    fun getCurrentBookPath(): String? {
        return currentBookPath
    }
}