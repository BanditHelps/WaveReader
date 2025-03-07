package com.github.b4ndithelps.wave.spotify

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.Date

/**
 * Storage class for Spotify authentication tokens
 * Handles securely persisting and retrieving tokens for the app
 */
class SpotAuthStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "spotify_auth_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at" // Absolute timestamp when token expires
    }
    
    /**
     * Saves Spotify authentication tokens and calculates expiration time
     */
    fun saveTokens(accessToken: String, refreshToken: String?, expiresIn: Int) {
        // Calculate absolute expiration time by adding expiresIn (seconds) to current time
        val expiresAt = Date().time + (expiresIn * 1000L)
        
        prefs.edit {
            putString(KEY_ACCESS_TOKEN, accessToken)
            refreshToken?.let { putString(KEY_REFRESH_TOKEN, it) }
            putLong(KEY_EXPIRES_AT, expiresAt)
        }
    }
    
    /**
     * Gets the stored access token if it exists
     */
    fun getAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }
    
    /**
     * Gets the stored refresh token if it exists
     */
    fun getRefreshToken(): String? {
        return prefs.getString(KEY_REFRESH_TOKEN, null)
    }
    
    /**
     * Gets the expiration timestamp
     */
    fun getExpiresAt(): Long {
        return prefs.getLong(KEY_EXPIRES_AT, 0L)
    }
    
    /**
     * Checks if the stored access token has expired
     * @return true if token has expired or doesn't exist, false if it's still valid
     */
    fun isTokenExpired(): Boolean {
        val expiresAt = getExpiresAt()
        return expiresAt == 0L || Date().time >= expiresAt
    }
    
    /**
     * Checks if we have valid credentials stored
     * @return true if we have a non-expired access token or a refresh token
     */
    fun hasValidCredentials(): Boolean {
        return !isTokenExpired() || getRefreshToken() != null
    }
    
    /**
     * Clears all stored tokens
     */
    fun clearTokens() {
        prefs.edit {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_EXPIRES_AT)
        }
    }
}