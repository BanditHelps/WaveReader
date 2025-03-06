package com.github.b4ndithelps.wave.spotify

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import com.github.b4ndithelps.wave.CustomTabsCloser
import com.github.b4ndithelps.wave.SpotifyAuthCallbackActivity
import com.github.b4ndithelps.wave.WaveReaderApplication

class SpotifyAuthActivity : AppCompatActivity() {

    private lateinit var customTabsCloseBroadcastReceiver: CustomTabsCloser
    private val exportFlag: Int = 2

    @SuppressLint("WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as WaveReaderApplication
        val spotManager = app.spotifyManagerInstance ?: throw IllegalStateException("SpotifyManager not initialized")

        customTabsCloseBroadcastReceiver = CustomTabsCloser()
        val intentFilter = IntentFilter(CustomTabsCloser.ACTION_CLOSE_TABS)
        registerReceiver(customTabsCloseBroadcastReceiver, intentFilter, exportFlag)

        val authUrl = spotManager.loadAuthUrl()
        Log.d("SpotifyAuth", "Oncreate of auth activity")

        if (authUrl != null) {
            // Create the intent for the callback activity
            val callbackIntent = Intent(this, SpotifyAuthCallbackActivity::class.java)
            callbackIntent.putExtra("SHOULD_CLOSE_TAB", true)

            val pendingIntent = PendingIntent.getActivity(
                this,
                SpotifyAuthConfig.REQUEST_CODE,
                callbackIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Build and launch the custom tab
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()

            // Attach the pending intent to the custom tab
            customTabsIntent.intent.putExtra(CustomTabsIntent.EXTRA_SESSION, pendingIntent)
            Log.d("SpotifyAuth", "launching new intent")

            customTabsIntent.launchUrl(this, authUrl)
        } else {
            Log.e("SpotifyAuth", "No Authentication Url found!")
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(customTabsCloseBroadcastReceiver)
        } catch (e: Exception) {
            Log.w("SpotifyAuth", "Error unregistering receiver", e)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("SpotifyAuth", "Got to result")
        if (requestCode == SpotifyAuthConfig.REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                Log.d("SpotifyAuth", "Got result!")
                val resultIntent = Intent().apply {
                    putExtras(data.extras ?: Bundle())
                }
                setResult(RESULT_OK, resultIntent)
            } else {
                Log.d("SpotifyAuth", "Cancelling in activity result!")
                setResult(RESULT_CANCELED)
            }
        } else {
            Log.d("SpotifyAuth", "Got cancellation!")
            setResult(RESULT_CANCELED)
        }
        finish()
    }


}