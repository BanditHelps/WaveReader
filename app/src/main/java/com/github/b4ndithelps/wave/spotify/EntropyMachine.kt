package com.github.b4ndithelps.wave.spotify

import android.net.Uri
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Used to generate random codes for Spotify Authentication
 */
class EntropyMachine {

    /**
     * Function to generate a high-entropy code verifier
     */
    fun generateCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val codeVerifierBytes = ByteArray(64) // 86 character base64 string
        secureRandom.nextBytes(codeVerifierBytes)
        return Base64.encodeToString(codeVerifierBytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    /**
     * Function to generate code challenge from the verifier using SHA-256
     */
    fun generateCodeChallenge(codeVerifier: String): String {
        val bytes = codeVerifier.toByteArray(Charsets.US_ASCII)
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val digest = messageDigest.digest(bytes)
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    /**
     * Build the Spotify authorization URL with PKCE parameters
     */
    fun buildSpotifyAuthUrl(
        clientId: String,
        redirectUri: String,
        scope: String,
        state: String,
        codeChallenge: String
    ): Uri {
        return Uri.parse("https://accounts.spotify.com/authorize").buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("scope", scope)
            .appendQueryParameter("state", state)
            .appendQueryParameter("code_challenge", codeChallenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .build()
    }
}