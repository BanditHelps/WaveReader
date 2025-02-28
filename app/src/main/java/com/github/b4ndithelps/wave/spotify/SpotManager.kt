package com.github.b4ndithelps.wave.spotify

import android.content.Context
import android.util.Log
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote

class SpotManager(context: Context) {
    private var spotifyAppRemote: SpotifyAppRemote? = null
    private val contextRef = context
    private var authenticated: Boolean = false

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
            }

            override fun onFailure(throwable: Throwable) {
                Log.e("SpotManager", throwable.message, throwable)
            }
        })
    }

    fun disconnect() {
        spotifyAppRemote?.let {
            SpotifyAppRemote.disconnect(it)
            spotifyAppRemote = null
            authenticated = false
        }
    }

    fun pressPlay() {
        if (authenticated) {
//            spotifyAppRemote?.playerApi?.let {
//
//            }

        }
    }
}