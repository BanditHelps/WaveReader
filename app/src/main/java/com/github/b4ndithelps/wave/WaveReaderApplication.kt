package com.github.b4ndithelps.wave

import android.app.Application
import android.content.Context
import com.github.b4ndithelps.wave.spotify.SpotManager

// A global state to hold the spotify authentication info.
// This is used to prevent issues with reauthenticating every time
// a book is opened.
class WaveReaderApplication : Application() {
    var spotifyManagerInstance: SpotManager? = null

    companion object {
        fun get(context: Context): WaveReaderApplication {
            return context.applicationContext as WaveReaderApplication
        }
    }
}