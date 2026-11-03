package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.model.PlaybackState
import com.example.model.RepeatMode
import com.example.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object PlayerController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var appContext: Context? = null
    private var sleepTimerJob: Job? = null
    private var onTrackPlayedListener: ((Track) -> Unit)? = null

    fun initialize(context: Context, onTrackPlayed: (Track) -> Unit) {
        appContext = context.applicationContext
        onTrackPlayedListener = onTrackPlayed
    }

    fun playTrack(track: Track, newQueue: List<Track> = listOf(track)) {
        val index = newQueue.indexOfFirst { it.id == track.id }.let { if (it >= 0) it else 0 }
        _playbackState.update { current ->
            current.copy(
                currentTrack = track,
                queue = if (newQueue.isNotEmpty()) newQueue else listOf(track),
                queueIndex = index,
                isPlaying = false,
                isLoading = true,
                errorMessage = null,
                currentPositionMs = 0L,
                durationMs = track.durationMs
            )
        }

        onTrackPlayedListener?.invoke(track)
        sendServiceCommand(MusicPlaybackService.ACTION_PLAY)
    }

    fun playFromQueue(index: Int) {
        val currentQueue = _playbackState.value.queue
        if (index in currentQueue.indices) {
            val track = currentQueue[index]
            _playbackState.update {
                it.copy(
                    currentTrack = track,
                    queueIndex = index,
                    isPlaying = false,
                    isLoading = true,
                    errorMessage = null,
                    currentPositionMs = 0L,
                    durationMs = track.durationMs
                )
            }
            onTrackPlayedListener?.invoke(track)
            sendServiceCommand(MusicPlaybackService.ACTION_PLAY)
        }
    }

    fun togglePlayPause() {
        val state = _playbackState.value
        if (state.currentTrack == null) return

        // If currently playing OR currently buffering/loading, pausing is the expected user action
        if (state.isPlaying || state.isLoading) {
            pause()
        } else {
            resume()
        }
    }

    fun pause() {
        _playbackState.update { it.copy(isPlaying = false, isLoading = false) }
        sendServiceCommand(MusicPlaybackService.ACTION_PAUSE)
    }

    fun resume() {
        if (_playbackState.value.currentTrack != null) {
            _playbackState.update { it.copy(isLoading = true, errorMessage = null) }
            sendServiceCommand(MusicPlaybackService.ACTION_RESUME)
        }
    }

    fun playNext() {
        val state = _playbackState.value
        if (state.queue.isEmpty()) return

        if (state.repeatMode == RepeatMode.ONE && state.currentTrack != null) {
            playTrack(state.currentTrack, state.queue)
            return
        }

        val nextIndex = if (state.isShuffle) {
            if (state.queue.size > 1) {
                var rand = (state.queue.indices).random()
                while (rand == state.queueIndex && state.queue.size > 1) {
                    rand = (state.queue.indices).random()
                }
                rand
            } else 0
        } else {
            val next = state.queueIndex + 1
            if (next >= state.queue.size) {
                if (state.repeatMode == RepeatMode.ALL) 0 else return
            } else next
        }

        playFromQueue(nextIndex)
    }

    fun playPrevious() {
        val state = _playbackState.value
        if (state.queue.isEmpty()) return

        // If played more than 3 seconds, restart current track
        if (state.currentPositionMs > 3000L && state.currentTrack != null) {
            seekTo(0L)
            return
        }

        val prevIndex = if (state.isShuffle) {
            (state.queue.indices).random()
        } else {
            val prev = state.queueIndex - 1
            if (prev < 0) {
                if (state.repeatMode == RepeatMode.ALL) state.queue.size - 1 else 0
            } else prev
        }

        playFromQueue(prevIndex)
    }

    fun seekTo(positionMs: Long) {
        val intent = Intent(appContext, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_SEEK
            putExtra(MusicPlaybackService.EXTRA_SEEK_MS, positionMs)
        }
        startServiceIntent(intent)
    }

    fun toggleShuffle() {
        _playbackState.update { it.copy(isShuffle = !it.isShuffle) }
    }

    fun cycleRepeatMode() {
        _playbackState.update { current ->
            val nextMode = when (current.repeatMode) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            }
            current.copy(repeatMode = nextMode)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackState.update { it.copy(speed = speed) }
        val intent = Intent(appContext, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_SPEED
            putExtra(MusicPlaybackService.EXTRA_SPEED, speed)
        }
        startServiceIntent(intent)
    }

    fun setBassBoost(boost: Float) {
        _playbackState.update { it.copy(bassBoost = boost) }
        val intent = Intent(appContext, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_BASS_BOOST
            putExtra(MusicPlaybackService.EXTRA_BASS_BOOST, boost)
        }
        startServiceIntent(intent)
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            _playbackState.update { it.copy(sleepTimerMinutesRemaining = null) }
            return
        }

        _playbackState.update { it.copy(sleepTimerMinutesRemaining = minutes) }
        sleepTimerJob = scope.launch {
            var remaining = minutes
            while (isActive && remaining > 0) {
                delay(60_000L)
                remaining -= 1
                _playbackState.update { it.copy(sleepTimerMinutesRemaining = remaining) }
            }
            pause()
            _playbackState.update { it.copy(sleepTimerMinutesRemaining = null) }
        }
    }

    // Called by MusicPlaybackService to update state
    fun updateState(
        isPlaying: Boolean,
        isLoading: Boolean,
        currentPosMs: Long,
        durationMs: Long,
        errorMessage: String? = null
    ) {
        _playbackState.update {
            it.copy(
                isPlaying = isPlaying,
                isLoading = isLoading,
                currentPositionMs = currentPosMs,
                durationMs = if (durationMs > 0) durationMs else it.durationMs,
                errorMessage = errorMessage
            )
        }
    }

    fun onTrackCompletion() {
        playNext()
    }

    private fun sendServiceCommand(actionStr: String) {
        val intent = Intent(appContext, MusicPlaybackService::class.java).apply {
            action = actionStr
        }
        startServiceIntent(intent)
    }

    private fun startServiceIntent(intent: Intent) {
        val context = appContext ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
