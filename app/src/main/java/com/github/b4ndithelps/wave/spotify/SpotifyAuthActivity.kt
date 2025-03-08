package com.github.b4ndithelps.wave.spotify

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import com.github.b4ndithelps.wave.SpotifyAuthCallbackActivity
import com.github.b4ndithelps.wave.WaveReaderApplication

/**
 * SpotifyAuthActivity is responsible for initiating the Spotify authentication flow using Custom Tabs.
 *
 * This activity handles the following:
 * 1. Loading the Spotify authorization URL from the SpotifyManager.
 * 2. Launching a Custom Tab to display the Spotify login/consent page.
 * 3. Setting up a PendingIntent to handle the authentication callback.
 * 4. Handling the result of the Custom Tab flow in `onActivityResult`.
 * 5. Returning the authentication result to the calling activity.
 *
 * The authentication flow involves:
 *   - Starting this activity.
 *   - The activity launches a Custom Tab.
 *   - The user interacts with the Spotify page within the Custom Tab.
 *   - Upon successful authentication or cancellation, the Custom Tab triggers the `SpotifyAuthCallbackActivity`.
 *   - `SpotifyAuthCallbackActivity` communicates the result back to `SpotifyAuthActivity`.
 *   - This activity receives the result in `onActivityResult` and forwards it to the calling component.
 *   - Finally this activity finishes itself.
 *
 * Important Considerations:
 *   - This activity relies on the `SpotifyManager` being properly initialized within the `WaveReaderApplication`.
 *   - It uses `CustomTabsIntent` for a seamless in-app browser experience.
 *   - Proper session management is handled by setting flags like `FLAG_ACTIVITY_NO_HISTORY` and `FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS`.
 *   - The callback is handled by `SpotifyAuthCallbackActivity`, which expects specific extras in the intent to display a confirmation dialog and close the tab.
 *
 */
class SpotifyAuthActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as WaveReaderApplication
        val spotManager = app.spotifyManagerInstance ?: throw IllegalStateException("SpotifyManager not initialized")

        val authUrl = spotManager.loadAuthUrl()
        Log.d("SpotifyAuth", "Oncreate of auth activity")

        if (authUrl != null) {
            // Create the intent for the callback activity with confirmation dialog
            val callbackIntent = Intent(this, SpotifyAuthCallbackActivity::class.java)
            callbackIntent.putExtra("SHOULD_CLOSE_TAB", true)
            callbackIntent.putExtra("DISPLAY_CONFIRMATION", true)

            val pendingIntent = PendingIntent.getActivity(
                this,
                SpotifyAuthConfig.REQUEST_CODE,
                callbackIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Build and launch the custom tab with proper session management
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setShareState(CustomTabsIntent.SHARE_STATE_OFF) // Disable sharing option
                .build()

            // Set the pending intent for when auth completes
            customTabsIntent.intent.putExtra(CustomTabsIntent.EXTRA_SESSION, pendingIntent)
            
            // Add flags to ensure proper session management
            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            
            Log.d("SpotifyAuth", "launching new intent")
            customTabsIntent.launchUrl(this, authUrl)
        } else {
            Log.e("SpotifyAuth", "No Authentication Url found!")
            setResult(RESULT_CANCELED)
            finish()
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