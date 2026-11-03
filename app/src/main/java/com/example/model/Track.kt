package com.example.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val artworkUrl: String = "",
    val streamUrl: String,
    val durationMs: Long = 0L,
    val genre: String = "Music",
    val source: String = "Online",
    val isLiveStream: Boolean = false
) {
    val formattedDuration: String
        get() {
            if (isLiveStream) return "LIVE"
            if (durationMs <= 0L) return "--:--"
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}
