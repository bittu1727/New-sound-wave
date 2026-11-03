package com.example.data.repository

import com.example.data.api.OnlineMusicApi
import com.example.data.local.FavoriteDao
import com.example.data.local.FavoriteTrackEntity
import com.example.data.local.HistoryDao
import com.example.data.local.HistoryTrackEntity
import com.example.data.local.PlaylistDao
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistTrackCrossRef
import com.example.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MusicRepository(
    private val api: OnlineMusicApi,
    private val favoriteDao: FavoriteDao,
    private val playlistDao: PlaylistDao,
    private val historyDao: HistoryDao
) {
    suspend fun searchSongs(query: String): List<Track> {
        return api.searchSongs(query)
    }

    fun getAllFavorites(): Flow<List<Track>> {
        return favoriteDao.getAllFavorites().map { entities ->
            entities.map { it.toTrack() }
        }
    }

    fun isFavorite(trackId: String): Flow<Boolean> {
        return favoriteDao.isFavorite(trackId)
    }

    suspend fun toggleFavorite(track: Track, currentFavorite: Boolean) {
        if (currentFavorite) {
            favoriteDao.deleteFavorite(track.id)
        } else {
            favoriteDao.insertFavorite(FavoriteTrackEntity.fromTrack(track))
        }
    }

    fun getRecentlyPlayed(): Flow<List<Track>> {
        return historyDao.getRecentlyPlayed().map { entities ->
            entities.map { it.toTrack() }
        }
    }

    suspend fun recordPlayed(track: Track) {
        historyDao.insertHistory(HistoryTrackEntity.fromTrack(track))
    }

    suspend fun clearHistory() {
        historyDao.clearHistory()
    }

    fun getPlaylists(): Flow<List<PlaylistEntity>> {
        return playlistDao.getAllPlaylists()
    }

    suspend fun createPlaylist(name: String, description: String = ""): Long {
        return playlistDao.createPlaylist(PlaylistEntity(name = name, description = description))
    }

    suspend fun deletePlaylist(id: Long) {
        playlistDao.deletePlaylist(id)
    }

    fun getPlaylistTracks(playlistId: Long): Flow<List<Track>> {
        return playlistDao.getTracksForPlaylist(playlistId).map { crossRefs ->
            crossRefs.map { it.toTrack() }
        }
    }

    suspend fun addTrackToPlaylist(playlistId: Long, track: Track) {
        val crossRef = PlaylistTrackCrossRef(
            playlistId = playlistId,
            trackId = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            artworkUrl = track.artworkUrl,
            streamUrl = track.streamUrl,
            durationMs = track.durationMs,
            genre = track.genre,
            source = track.source,
            isLiveStream = track.isLiveStream
        )
        playlistDao.addTrackToPlaylist(crossRef)
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    // Curated 24/7 Live Online Radio Stations
    fun getCuratedRadioStations(): List<Track> {
        return listOf(
            Track(
                id = "radio_lofi",
                title = "Lo-Fi Beats 24/7",
                artist = "Chillhop & Study Vibes",
                album = "Global Live Stream",
                artworkUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&q=80",
                streamUrl = "https://streams.ilovemusic.de/iloveradio17.mp3",
                durationMs = 0L,
                genre = "Lo-Fi",
                source = "Radio",
                isLiveStream = true
            ),
            Track(
                id = "radio_groove_salad",
                title = "Groove Salad Ambient",
                artist = "SomaFM Chillout",
                album = "San Francisco, CA",
                artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&q=80",
                streamUrl = "https://ice1.somafm.com/groovesalad-128-mp3",
                durationMs = 0L,
                genre = "Chillout",
                source = "Radio",
                isLiveStream = true
            ),
            Track(
                id = "radio_synthwave",
                title = "DEF CON Radio",
                artist = "Music for Hackers & Geeks",
                album = "DEF CON Live Stream",
                artworkUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&q=80",
                streamUrl = "https://ice1.somafm.com/defcon-128-mp3",
                durationMs = 0L,
                genre = "Synthwave",
                source = "Radio",
                isLiveStream = true
            ),
            Track(
                id = "radio_drone_zone",
                title = "Drone Zone Space",
                artist = "Atmospheric Space & Ambient",
                album = "Deep Soundscapes",
                artworkUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&q=80",
                streamUrl = "https://ice1.somafm.com/dronezone-128-mp3",
                durationMs = 0L,
                genre = "Ambient",
                source = "Radio",
                isLiveStream = true
            ),
            Track(
                id = "radio_secret_agent",
                title = "Secret Agent Lounge",
                artist = "The Spy Soundtrack",
                album = "Vintage Surf & Lounge",
                artworkUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&q=80",
                streamUrl = "https://ice1.somafm.com/secretagent-128-mp3",
                durationMs = 0L,
                genre = "Lounge",
                source = "Radio",
                isLiveStream = true
            ),
            Track(
                id = "radio_indie_pop",
                title = "Indie Pop Rocks!",
                artist = "Underground Alternative",
                album = "Indie Tracks",
                artworkUrl = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=600&q=80",
                streamUrl = "https://ice1.somafm.com/indiepop-128-mp3",
                durationMs = 0L,
                genre = "Indie Rock",
                source = "Radio",
                isLiveStream = true
            ),
            Track(
                id = "radio_jazz_lush",
                title = "Lush Smooth Jazz",
                artist = "Sensuous Vocals & Smooth Melodies",
                album = "Mellow Jazz Hour",
                artworkUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&q=80",
                streamUrl = "https://ice1.somafm.com/lush-128-mp3",
                durationMs = 0L,
                genre = "Jazz",
                source = "Radio",
                isLiveStream = true
            ),
            Track(
                id = "radio_celtic",
                title = "Thistle Acoustic",
                artist = "Celtic & Acoustic Traditions",
                album = "Celtic Soundscapes",
                artworkUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=600&q=80",
                streamUrl = "https://ice1.somafm.com/thistle-128-mp3",
                durationMs = 0L,
                genre = "Acoustic",
                source = "Radio",
                isLiveStream = true
            )
        )
    }

    // High quality initial trending tracks across major genres
    fun getInitialTrendingTracks(): List<Track> {
        return listOf(
            Track(
                id = "trend_1",
                title = "Midnight City Lights",
                artist = "Neon Horizon",
                album = "Synthwave Odyssey",
                artworkUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&q=80",
                streamUrl = "https://cdn.pixabay.com/download/audio/2022/05/27/audio_1808fbf07a.mp3?filename=lofi-study-112191.mp3",
                durationMs = 146000L,
                genre = "Lo-Fi",
                source = "Trending"
            ),
            Track(
                id = "trend_2",
                title = "Cosmic Dreams",
                artist = "Starlight Echo",
                album = "Infinite Horizons",
                artworkUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&q=80",
                streamUrl = "https://cdn.pixabay.com/download/audio/2022/01/18/audio_d0a13f69d2.mp3?filename=electronic-future-beats-117997.mp3",
                durationMs = 162000L,
                genre = "Electronic",
                source = "Trending"
            ),
            Track(
                id = "trend_3",
                title = "Coffee & Rain",
                artist = "Cozy Afternoon",
                album = "Rainy Day Chills",
                artworkUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=600&q=80",
                streamUrl = "https://cdn.pixabay.com/download/audio/2022/03/15/audio_c8c8a73467.mp3?filename=chill-abstract-intention-12099.mp3",
                durationMs = 125000L,
                genre = "Chillout",
                source = "Trending"
            ),
            Track(
                id = "trend_4",
                title = "Summer Breeze",
                artist = "Solar Groove",
                album = "Golden Hour Session",
                artworkUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&q=80",
                streamUrl = "https://cdn.pixabay.com/download/audio/2022/11/06/audio_9676e10265.mp3?filename=uplifting-day-126040.mp3",
                durationMs = 138000L,
                genre = "Pop",
                source = "Trending"
            ),
            Track(
                id = "trend_5",
                title = "Deep Space Pulse",
                artist = "CyberVibe",
                album = "Cyberpunk 2099",
                artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&q=80",
                streamUrl = "https://cdn.pixabay.com/download/audio/2022/10/14/audio_9939f792cb.mp3?filename=tuesday-glitch-122413.mp3",
                durationMs = 153000L,
                genre = "EDM",
                source = "Trending"
            ),
            Track(
                id = "trend_6",
                title = "Acoustic Serenade",
                artist = "Timber & Strings",
                album = "Campfire Memories",
                artworkUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=600&q=80",
                streamUrl = "https://cdn.pixabay.com/download/audio/2021/08/04/audio_bb630cc098.mp3?filename=acoustic-guitars-ambient-uplifting-11219.mp3",
                durationMs = 132000L,
                genre = "Acoustic",
                source = "Trending"
            )
        )
    }
}
