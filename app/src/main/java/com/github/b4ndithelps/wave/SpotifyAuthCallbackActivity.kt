package com.github.b4ndithelps.wave

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
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
    private var shouldDisplayConfirmation: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spotify_auth_view)

        Log.d("SpotifyAuth", "On Create of Callback Activity")

        // Initialize UI components
        authCompletedCard = findViewById(R.id.authCompletedCard)
        returnToAppButton = findViewById(R.id.returnToAppButton)
        
        // Hide confirmation card by default - will be shown after auth completes
        authCompletedCard.visibility = View.GONE
        
        // Set up return button click listener
        returnToAppButton.setOnClickListener {
            // Launch a fresh SpotifyEpubReaderActivity instead of returning through intent chain
            startNewReaderActivity()
        }

        val app = application as WaveReaderApplication
        spotManager = app.spotifyManagerInstance ?: throw IllegalStateException("SpotifyManager not initialized")

        if (!spotManager.getProcessingCallback()) {
            spotManager.setProcessingCallback(true)
            handleAuthenticationCallback(intent)
        } else {
            showAuthenticationComplete()
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
                    showFailedAuthentication("Code verification failed")
                }
            } else {
                // Handle error scenario
                Log.e("SpotifyAuth", "No authorization code received")
                showFailedAuthentication("No authorization code received")
            }
        } else {
            Log.e("SpotifyAuth", "Invalid redirect URI")
            showFailedAuthentication("Invalid redirect URI")
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

                // Execute the HTTP response on the IO thread
                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }

                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val tokenResponse = JSONObject(responseBody)
                    accessToken = tokenResponse.getString("access_token")
                    refreshToken = tokenResponse.optString("refresh_token", "")
                    expiresIn = tokenResponse.getInt("expires_in")

                    // Store tokens directly in the SpotManager using the existing method
                    val authIntent = Intent().apply {
                        putExtra("access_token", accessToken)
                        putExtra("refresh_token", refreshToken)
                        putExtra("expires_in", expiresIn)
                    }
                    spotManager.processAuthResponse(authIntent)
                    
                    // Show confirmation dialog
                    showAuthenticationComplete()
                } else {
                    Log.e("SpotifyAuth", "Token exchange failed: ${response.code}")
                    showFailedAuthentication("Token exchange failed: ${response.code}")
                }
            } catch (e: Exception) {
                Log.e("SpotifyAuth", "Token exchange error", e)
                showFailedAuthentication("Token exchange error: ${e.message}")
            }
        }
    }

    private fun showAuthenticationComplete() {
        // Show the confirmation card with success message
        runOnUiThread {
            findViewById<TextView>(R.id.authTitleTextView)?.text = "Spotify Authentication Complete"
            findViewById<TextView>(R.id.authMessageTextView)?.text = 
                "Your Spotify account has been successfully connected."
            returnToAppButton.text = "Return to Reader"
            authCompletedCard.visibility = View.VISIBLE
        }
    }
    
    private fun showFailedAuthentication(errorMessage: String) {
        // Show the confirmation card with error message
        runOnUiThread {
            findViewById<TextView>(R.id.authTitleTextView)?.text = "Authentication Failed"
            findViewById<TextView>(R.id.authMessageTextView)?.text = 
                errorMessage
            returnToAppButton.text = "Return to App"
            authCompletedCard.visibility = View.VISIBLE
        }
    }

    private fun startNewReaderActivity() {
        val intent = Intent(this, SpotifyEpubReaderActivity::class.java)
        // Add any necessary extra data
        intent.putExtra("FROM_SPOTIFY_AUTH", true)
        
        // Get the book path from the SpotManager instead of the intent
        val bookPath = spotManager.getCurrentBookPath()
        if (bookPath != null) {
            intent.putExtra("bookPath", bookPath)
            Log.d("SpotifyAuth", "Using stored book path: $bookPath")
        } else {
            // Fallback to intent extras if available
            val intentBookPath = getIntent().getStringExtra("bookPath")
            if (intentBookPath != null) {
                intent.putExtra("bookPath", intentBookPath)
                Log.d("SpotifyAuth", "Using intent book path: $intentBookPath")
            } else {
                Log.w("SpotifyAuth", "No book path available")
            }
        }
        
        // Clear back stack and start fresh
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        // If the auth completed card is visible, start new reader activity
        if (authCompletedCard.visibility == View.VISIBLE) {
            startNewReaderActivity()
        } else {
            // Otherwise proceed with default back behavior
            super.onBackPressed()
        }
    }
}