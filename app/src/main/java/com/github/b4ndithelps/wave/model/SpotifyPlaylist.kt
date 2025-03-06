package com.github.b4ndithelps.wave.model

data class SpotifyPlaylist(
    val id: String,
    val name: String,
    val description: String = "",
    val imageUrl: String = "",
    val trackCount: Int = 0,
    val creator: String = "",
    val tracks: List<SpotifyTrack> = emptyList(),
    val isPinned: Boolean = false
)