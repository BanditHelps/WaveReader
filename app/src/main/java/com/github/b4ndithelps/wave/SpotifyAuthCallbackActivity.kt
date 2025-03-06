package com.github.b4ndithelps.wave

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.github.b4ndithelps.wave.spotify.SpotManager
import com.github.b4ndithelps.wave.spotify.SpotifyAuthConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class SpotifyAuthCallbackActivity : AppCompatActivity() {
    private lateinit var spotManager: SpotManager
    private lateinit var authCompletedCard: CardView
    private lateinit var returnToAppButton: Button
    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var expiresIn: Int = 0
    private var shouldDisplayConfirmation: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spotify_auth_view)

        Log.d("SpotifyAuth", "On Create of Callback Activity")

        // Get display confirmation preference from intent
        shouldDisplayConfirmation = intent.getBooleanExtra("DISPLAY_CONFIRMATION", false)
        Log.d("SpotifyAuth", "Should display confirmation: $shouldDisplayConfirmation")

        // Initialize UI components
        authCompletedCard = findViewById(R.id.authCompletedCard)
        returnToAppButton = findViewById(R.id.returnToAppButton)
        
        // Hide confirmation card by default - will be shown after auth completes if needed
        authCompletedCard.visibility = View.GONE
        
        // Set up return button click listener
        returnToAppButton.setOnClickListener {
            finishWithSuccess()
        }

        val app = application as WaveReaderApplication
        spotManager = app.spotifyManagerInstance ?: throw IllegalStateException("SpotifyManager not initialized")

        if (!spotManager.getProcessingCallback()) {
            spotManager.setProcessingCallback(true)
            handleAuthenticationCallback(intent)
        } else {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun handleAuthenticationCallback(intent: Intent) {
        val uri = intent.data

        if (uri != null && uri.toString().startsWith(SpotifyAuthConfig.REDIRECT_URI)) {
            val code = uri.getQueryParameter("code")
            val state = uri.getQueryParameter("state")

            if (code != null) {
                val codeVerifier = spotManager.getCodeVerifier()

                if (codeVerifier != null) {
                    // Exchange the auth code for an actual token
                    exchangeCodeForTokens(code, codeVerifier)
                } else {
                    Log.e("SpotifyAuth", "Code verifier not found")
                    finishWithCancellation()
                }
            } else {
                // Handle error scenario
                Log.e("SpotifyAuth", "No authorization code received")
                finishWithCancellation()
            }
        } else {
            Log.e("SpotifyAuth", "Invalid redirect URI")
            finishWithCancellation()
        }
    }

    private fun exchangeCodeForTokens(authorizationCode: String, codeVerifier: String) {
        lifecycleScope.launch {
            try {
                val client = OkHttpClient()

                val requestBody = FormBody.Builder()
                    .add("grant_type", "authorization_code")
                    .add("code", authorizationCode)
                    .add("redirect_uri", SpotifyAuthConfig.REDIRECT_URI)
                    .add("client_id", SpotifyAuthConfig.CLIENT_ID)
                    .add("code_verifier", codeVerifier)
                    .build()

                val request = Request.Builder()
                    .url(SpotifyAuthConfig.AUTH_TOKEN_URL)
                    .post(requestBody)
                    .build()

                // Execute the HTTP response on the IO thread cause doing it on the main
                // makes Android really mad
                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }

                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val tokenResponse = JSONObject(responseBody)
                    accessToken = tokenResponse.getString("access_token")
                    refreshToken = tokenResponse.optString("refresh_token", null)
                    expiresIn = tokenResponse.getInt("expires_in")

                    // Process tokens and show completion dialog if required
//                    if (shouldDisplayConfirmation) {
//                        showAuthenticationComplete()
//                    } else {
//                        // Immediately return with success
//                        finishWithSuccess()
//                    }
                    showAuthenticationComplete()
                } else {
                    Log.e("SpotifyAuth", "Token exchange failed: ${response.code}")
                    finishWithCancellation()
                }
            } catch (e: Exception) {
                Log.e("SpotifyAuth", "Token exchange error", e)
                finishWithCancellation()
            }
        }
    }

    private fun showAuthenticationComplete() {
        // Show the completion card
        runOnUiThread {
            authCompletedCard.visibility = View.VISIBLE
        }
    }

    // Call this method when the user clicks the return button
    private fun finishWithSuccess() {
        if (accessToken != null) {
            // Create an intent to send back to the main activity
            val resultIntent = Intent().apply {
                putExtra("access_token", accessToken)
                putExtra("refresh_token", refreshToken)
                putExtra("expires_in", expiresIn)
            }
            setResult(RESULT_OK, resultIntent)
        } else {
            setResult(RESULT_CANCELED)
        }
        finish()
    }

    // In case of errors or cancellations, finish with RESULT_CANCELED
    private fun finishWithCancellation() {
        setResult(RESULT_CANCELED)
        finish()
    }

    @Override
    override fun onBackPressed() {
        // If the auth completed card is visible, finish with success
        if (authCompletedCard.visibility == View.VISIBLE) {
            finishWithSuccess()
        } else {
            // Otherwise proceed with default back behavior
            super.onBackPressed()
        }
    }
}