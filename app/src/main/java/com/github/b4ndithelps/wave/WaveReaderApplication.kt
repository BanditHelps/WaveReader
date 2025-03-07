package com.github.b4ndithelps.wave

import android.app.Application
import android.content.Context
import android.util.Log
import com.github.b4ndithelps.wave.spotify.SpotManager

// A global state to hold the spotify authentication info.
// This is used to prevent issues with reauthenticating every time
// a book is opened.
class WaveReaderApplication : Application() {
    var spotifyManagerInstance: SpotManager? = null
    
    override fun onCreate() {
        super.onCreate()
        // Initialize SpotManager when the application starts
        initializeSpotifyManager()
    }
    
    /**
     * Initializes the Spotify Manager with application context
     * This ensures that auth tokens can persist between activities and app restarts
     */
    private fun initializeSpotifyManager() {
        try {
            Log.d("WaveReaderApp", "Initializing Spotify Manager")
            spotifyManagerInstance = SpotManager(applicationContext)
        } catch (e: Exception) {
            Log.e("WaveReaderApp", "Error initializing Spotify Manager", e)
        }
    }

    companion object {
        fun get(context: Context): WaveReaderApplication {
            return context.applicationContext as WaveReaderApplication
        }
    }
}