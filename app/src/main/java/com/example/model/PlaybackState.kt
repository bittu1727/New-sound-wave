package com.example.model

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

data class PlaybackState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<Track> = emptyList(),
    val queueIndex: Int = -1,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val speed: Float = 1.0f,
    val bassBoost: Float = 0.0f,
    val sleepTimerMinutesRemaining: Int? = null,
    val errorMessage: String? = null
) {
    val progressFraction: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val formattedCurrentTime: String
        get() {
            if (currentTrack?.isLiveStream == true) return "LIVE"
            val totalSeconds = currentPositionMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }

    val formattedDurationTime: String
        get() {
            if (currentTrack?.isLiveStream == true) return "STREAM"
            if (durationMs <= 0) return "--:--"
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}
