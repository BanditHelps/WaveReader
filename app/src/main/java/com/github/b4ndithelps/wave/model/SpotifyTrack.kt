package com.github.b4ndithelps.wave.model

data class SpotifyTrack(
    val id: String,
    val name: String,
    val artist: String,
    val album: String = "",
    val albumImageUrl: String = "",
    val durationMs: Long = 0
)