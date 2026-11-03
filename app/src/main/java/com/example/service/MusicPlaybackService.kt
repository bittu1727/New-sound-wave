package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.MainActivity
import com.example.R
import com.example.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicPlaybackService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null
    private var artLoadJob: Job? = null

    private var mediaPlayer: MediaPlayer? = null
    private var mediaSession: MediaSession? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    private var currentPlayingTrack: Track? = null
    private var isPrepared = false
    private var currentAlbumArtBitmap: Bitmap? = null
    private var currentArtUrl: String? = null

    companion object {
        const val CHANNEL_ID = "soundwave_media_playback_channel_v2"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.example.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.ACTION_RESUME"
        const val ACTION_NEXT = "com.example.ACTION_NEXT"
        const val ACTION_PREV = "com.example.ACTION_PREV"
        const val ACTION_STOP = "com.example.ACTION_STOP"
        const val ACTION_SEEK = "com.example.ACTION_SEEK"
        const val ACTION_SPEED = "com.example.ACTION_SPEED"
        const val ACTION_BASS_BOOST = "com.example.ACTION_BASS_BOOST"

        const val EXTRA_SEEK_MS = "extra_seek_ms"
        const val EXTRA_SPEED = "extra_speed"
        const val EXTRA_BASS_BOOST = "extra_bass_boost"
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        setupMediaSession()
        createNotificationChannel()
        acquireLocks()
    }

    private fun setupMediaSession() {
        mediaSession = MediaSession(this, "SoundWaveMediaSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    resumePlayback()
                }

                override fun onPause() {
                    pausePlayback()
                }

                override fun onSkipToNext() {
                    PlayerController.playNext()
                }

                override fun onSkipToPrevious() {
                    PlayerController.playPrevious()
                }

                override fun onSeekTo(pos: Long) {
                    performSeek(pos)
                }

                override fun onStop() {
                    stopPlayback()
                }
            })
            isActive = true
        }
    }

    private fun acquireLocks() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SoundWave:WakeLock")
            wakeLock?.setReferenceCounted(false)

            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            @Suppress("DEPRECATION")
            wifiLock = wifiManager?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "SoundWave:WifiLock")
            wifiLock?.setReferenceCounted(false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_PLAY -> {
                val track = PlayerController.playbackState.value.currentTrack
                if (track != null) {
                    startTrackPlayback(track)
                }
            }
            ACTION_PAUSE -> {
                pausePlayback()
            }
            ACTION_RESUME -> {
                resumePlayback()
            }
            ACTION_NEXT -> {
                PlayerController.playNext()
            }
            ACTION_PREV -> {
                PlayerController.playPrevious()
            }
            ACTION_STOP -> {
                stopPlayback()
            }
            ACTION_SEEK -> {
                val seekMs = intent.getLongExtra(EXTRA_SEEK_MS, 0L)
                performSeek(seekMs)
            }
            ACTION_SPEED -> {
                val speed = intent.getFloatExtra(EXTRA_SPEED, 1.0f)
                applyPlaybackSpeed(speed)
            }
        }

        return START_NOT_STICKY
    }

    private fun startTrackPlayback(track: Track) {
        currentPlayingTrack = track
        isPrepared = false
        stopProgressUpdates()

        PlayerController.updateState(
            isPlaying = false,
            isLoading = true,
            currentPosMs = 0L,
            durationMs = track.durationMs
        )

        showForegroundNotification(track, isPlaying = false)
        loadAlbumArtworkAsync(track, isPlaying = false)

        releaseMediaPlayer()
        requestAudioFocus()

        try {
            wakeLock?.acquire(3 * 60 * 60 * 1000L) // 3 hours timeout safety
            wifiLock?.acquire()

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(track.streamUrl)
                setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)

                setOnPreparedListener { mp ->
                    isPrepared = true
                    val duration = if (track.isLiveStream) 0L else mp.duration.toLong()
                    mp.start()
                    applyPlaybackSpeed(PlayerController.playbackState.value.speed)

                    PlayerController.updateState(
                        isPlaying = true,
                        isLoading = false,
                        currentPosMs = 0L,
                        durationMs = if (duration > 0) duration else track.durationMs
                    )
                    showForegroundNotification(track, isPlaying = true)
                    startProgressUpdates()
                }

                setOnCompletionListener {
                    stopProgressUpdates()
                    PlayerController.updateState(
                        isPlaying = false,
                        isLoading = false,
                        currentPosMs = it.duration.toLong(),
                        durationMs = it.duration.toLong()
                    )
                    PlayerController.onTrackCompletion()
                }

                setOnErrorListener { _, what, extra ->
                    stopProgressUpdates()
                    PlayerController.updateState(
                        isPlaying = false,
                        isLoading = false,
                        currentPosMs = 0L,
                        durationMs = 0L,
                        errorMessage = "Audio playback error ($what, $extra)"
                    )
                    showForegroundNotification(track, isPlaying = false)
                    true
                }

                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            PlayerController.updateState(
                isPlaying = false,
                isLoading = false,
                currentPosMs = 0L,
                durationMs = 0L,
                errorMessage = "Failed to load audio stream: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    private fun pausePlayback() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        }
        stopProgressUpdates()
        val currentPos = mediaPlayer?.currentPosition?.toLong() ?: 0L
        val duration = mediaPlayer?.duration?.toLong() ?: (currentPlayingTrack?.durationMs ?: 0L)

        PlayerController.updateState(
            isPlaying = false,
            isLoading = false,
            currentPosMs = currentPos,
            durationMs = duration
        )

        currentPlayingTrack?.let {
            showForegroundNotification(it, isPlaying = false)
        }
    }

    private fun resumePlayback() {
        if (requestAudioFocus()) {
            mediaPlayer?.start()
            val currentPos = mediaPlayer?.currentPosition?.toLong() ?: 0L
            val duration = mediaPlayer?.duration?.toLong() ?: (currentPlayingTrack?.durationMs ?: 0L)

            PlayerController.updateState(
                isPlaying = true,
                isLoading = false,
                currentPosMs = currentPos,
                durationMs = duration
            )

            currentPlayingTrack?.let {
                showForegroundNotification(it, isPlaying = true)
            }
            startProgressUpdates()
        }
    }

    private fun performSeek(positionMs: Long) {
        if (mediaPlayer != null && isPrepared && currentPlayingTrack?.isLiveStream != true) {
            mediaPlayer?.seekTo(positionMs.toInt())
            PlayerController.updateState(
                isPlaying = mediaPlayer?.isPlaying == true,
                isLoading = false,
                currentPosMs = positionMs,
                durationMs = mediaPlayer?.duration?.toLong() ?: 0L
            )
            updateMediaSessionState(
                isPlaying = mediaPlayer?.isPlaying == true,
                currentPos = positionMs,
                track = currentPlayingTrack,
                albumArt = currentAlbumArtBitmap
            )
        }
    }

    private fun applyPlaybackSpeed(speed: Float) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mediaPlayer != null && isPrepared) {
                val params = PlaybackParams()
                params.speed = speed
                mediaPlayer?.playbackParams = params
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopPlayback() {
        stopProgressUpdates()
        releaseMediaPlayer()
        releaseLocks()
        abandonAudioFocus()
        PlayerController.updateState(
            isPlaying = false,
            isLoading = false,
            currentPosMs = 0L,
            durationMs = 0L
        )
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releaseMediaPlayer() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                reset()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
            isPrepared = false
        }
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
            if (wifiLock?.isHeld == true) wifiLock?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                if (mediaPlayer?.isPlaying == true) {
                    val pos = mediaPlayer?.currentPosition?.toLong() ?: 0L
                    val dur = if (currentPlayingTrack?.isLiveStream == true) 0L else mediaPlayer?.duration?.toLong() ?: 0L
                    PlayerController.updateState(
                        isPlaying = true,
                        isLoading = false,
                        currentPosMs = pos,
                        durationMs = dur
                    )
                    updateMediaSessionState(
                        isPlaying = true,
                        currentPos = pos,
                        track = currentPlayingTrack,
                        albumArt = currentAlbumArtBitmap
                    )
                }
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun requestAudioFocus(): Boolean {
        val am = audioManager ?: return true
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS -> pausePlayback()
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pausePlayback()
                        AudioManager.AUDIOFOCUS_GAIN -> {
                            if (PlayerController.playbackState.value.isPlaying) {
                                resumePlayback()
                            }
                        }
                    }
                }
                .build()
            audioFocusRequest = req
            am.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS) pausePlayback()
                },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(null)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.music_service_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.music_service_channel_desc)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun loadAlbumArtworkAsync(track: Track, isPlaying: Boolean) {
        if (track.artworkUrl == currentArtUrl && currentAlbumArtBitmap != null) {
            showForegroundNotification(track, isPlaying)
            return
        }

        artLoadJob?.cancel()
        artLoadJob = serviceScope.launch(Dispatchers.IO) {
            val bitmap = try {
                if (track.artworkUrl.isNotEmpty()) {
                    val imageLoader = Coil.imageLoader(applicationContext)
                    val request = ImageRequest.Builder(applicationContext)
                        .data(track.artworkUrl)
                        .allowHardware(false) // Must be a software bitmap for notifications
                        .size(512, 512)
                        .build()
                    val result = (imageLoader.execute(request) as? SuccessResult)?.drawable
                    (result as? BitmapDrawable)?.bitmap
                } else null
            } catch (e: Exception) {
                null
            }

            withContext(Dispatchers.Main) {
                currentAlbumArtBitmap = bitmap
                currentArtUrl = track.artworkUrl
                if (currentPlayingTrack?.id == track.id) {
                    showForegroundNotification(track, isPlaying)
                }
            }
        }
    }

    private fun updateMediaSessionState(isPlaying: Boolean, currentPos: Long, track: Track?, albumArt: Bitmap?) {
        val session = mediaSession ?: return
        val stateBuilder = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_SEEK_TO or
                PlaybackState.ACTION_STOP
            )
            .setState(
                if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                currentPos,
                PlayerController.playbackState.value.speed
            )
        session.setPlaybackState(stateBuilder.build())

        if (track != null) {
            val metaBuilder = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, track.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, track.artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, track.album.ifEmpty { track.genre })
                .putLong(MediaMetadata.METADATA_KEY_DURATION, if (track.isLiveStream) -1L else track.durationMs)
            if (albumArt != null) {
                metaBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, albumArt)
                metaBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, albumArt)
            }
            session.setMetadata(metaBuilder.build())
        }
    }

    private fun showForegroundNotification(track: Track, isPlaying: Boolean) {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_PREV }
        val prevPending = PendingIntent.getService(this, 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val playPauseIntent = Intent(this, MusicPlaybackService::class.java).apply {
            action = if (isPlaying) ACTION_PAUSE else ACTION_RESUME
        }
        val playPausePending = PendingIntent.getService(this, 2, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val nextIntent = Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_NEXT }
        val nextPending = PendingIntent.getService(this, 3, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(this, 4, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val currentPos = mediaPlayer?.currentPosition?.toLong() ?: 0L
        updateMediaSessionState(isPlaying, currentPos, track, currentAlbumArtBitmap)

        // Native Android MediaStyle for rich modern media player card with circular buttons & seekbar
        val mediaStyle = Notification.MediaStyle()
        mediaSession?.sessionToken?.let { mediaStyle.setMediaSession(it) }
        // Compact view shows Prev (0), Play/Pause (1), Next (2)
        mediaStyle.setShowActionsInCompactView(0, 1, 2)

        val builder = Notification.Builder(this, CHANNEL_ID)
            .setStyle(mediaStyle)
            .setSmallIcon(R.drawable.ic_notif_music)
            .setContentTitle(track.title)
            .setContentText(track.artist)
            .setSubText(track.album.ifEmpty { track.genre })
            .setContentIntent(openAppPendingIntent)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setColor(0xFF00E5FF.toInt())
            .setColorized(true)

        currentAlbumArtBitmap?.let {
            builder.setLargeIcon(it)
        }

        // Action 0: Previous
        val prevAction = Notification.Action.Builder(
            R.drawable.ic_notif_prev,
            getString(R.string.previous),
            prevPending
        ).build()
        builder.addAction(prevAction)

        // Action 1: Play / Pause
        val playPauseIcon = if (isPlaying) R.drawable.ic_notif_pause else R.drawable.ic_notif_play
        val playPauseTitle = if (isPlaying) getString(R.string.pause) else getString(R.string.play)
        val playPauseAction = Notification.Action.Builder(
            playPauseIcon,
            playPauseTitle,
            playPausePending
        ).build()
        builder.addAction(playPauseAction)

        // Action 2: Next
        val nextAction = Notification.Action.Builder(
            R.drawable.ic_notif_next,
            getString(R.string.next),
            nextPending
        ).build()
        builder.addAction(nextAction)

        // Action 3: Close / Dismiss
        val closeAction = Notification.Action.Builder(
            R.drawable.ic_notif_close,
            "Close",
            stopPending
        ).build()
        builder.addAction(closeAction)

        val notification = builder.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopPlayback()
        artLoadJob?.cancel()
        mediaSession?.apply {
            isActive = false
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
