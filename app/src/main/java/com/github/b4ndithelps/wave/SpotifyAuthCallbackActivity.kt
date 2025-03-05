package com.github.b4ndithelps.wave

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.b4ndithelps.wave.spotify.SpotManager
import com.github.b4ndithelps.wave.spotify.SpotifyAuthConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class SpotifyAuthCallbackActivity : AppCompatActivity() {
    private lateinit var spotManager: SpotManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("SpotifyAuth", "On Create of Callback Activity")

        val app = application as WaveReaderApplication
        spotManager = app.spotifyManagerInstance ?: throw IllegalStateException("SpotifyManager not initialized")

        if (!spotManager.getProcessingCallback()) {
            spotManager.setProcessingCallback(true)
            handleAuthenticationCallback(intent)
        } else {
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
                    val accessToken = tokenResponse.getString("access_token")
                    val refreshToken = tokenResponse.optString("refresh_token", null)
                    val expiresIn = tokenResponse.getInt("expires_in")

                    // Process tokens
                    processTokenResponse(accessToken, refreshToken, expiresIn)
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

    private fun processTokenResponse(accessToken: String, refreshToken: String?, expiresIn: Int) {
        // Create an intent to send back to the main activity
        val resultIntent = Intent().apply {
            putExtra("access_token", accessToken)
            putExtra("refresh_token", refreshToken)
            putExtra("expires_in", expiresIn)
        }

        finishWithResult(resultIntent)
//        setResult(RESULT_OK, resultIntent)
    }

    // Call this method to finish the activity and pass the results back
    private fun finishWithResult(resultIntent: Intent) {
        setResult(RESULT_OK, resultIntent)

        finish()
    }

    // In case of errors or cancellations, finish with RESULT_CANCELED
    private fun finishWithCancellation() {
        setResult(RESULT_CANCELED)
        finish()
    }
}