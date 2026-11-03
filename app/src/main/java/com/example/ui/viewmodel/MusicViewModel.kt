package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.OnlineMusicApi
import com.example.data.local.MusicDatabase
import com.example.data.local.PlaylistEntity
import com.example.data.repository.MusicRepository
import com.example.model.PlaybackState
import com.example.model.Track
import com.example.service.PlayerController
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NavTab {
    HOME,
    SEARCH,
    RADIO,
    LIBRARY
}

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MusicDatabase.getDatabase(application)
    private val repository = MusicRepository(
        api = OnlineMusicApi(),
        favoriteDao = database.favoriteDao(),
        playlistDao = database.playlistDao(),
        historyDao = database.historyDao()
    )

    val playbackState: StateFlow<PlaybackState> = PlayerController.playbackState

    val favorites: StateFlow<List<Track>> = repository.getAllFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayed: StateFlow<List<Track>> = repository.getRecentlyPlayed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistEntity>> = repository.getPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPlaylist = MutableStateFlow<PlaylistEntity?>(null)
    val selectedPlaylist = _selectedPlaylist.asStateFlow()

    private val _selectedPlaylistTracks = MutableStateFlow<List<Track>>(emptyList())
    val selectedPlaylistTracks = _selectedPlaylistTracks.asStateFlow()

    private val _activeTab = MutableStateFlow(NavTab.HOME)
    val activeTab = _activeTab.asStateFlow()

    private val _showNowPlayingSheet = MutableStateFlow(false)
    val showNowPlayingSheet = _showNowPlayingSheet.asStateFlow()

    private val _showGoogleSearchSheet = MutableStateFlow(false)
    val showGoogleSearchSheet = _showGoogleSearchSheet.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Track>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _selectedGenre = MutableStateFlow("All")
    val selectedGenre = _selectedGenre.asStateFlow()

    private val _trendingTracks = MutableStateFlow<List<Track>>(emptyList())
    val trendingTracks = _trendingTracks.asStateFlow()

    private val _radioStations = MutableStateFlow<List<Track>>(emptyList())
    val radioStations = _radioStations.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage = _toastMessage.asStateFlow()

    private var searchJob: Job? = null

    init {
        PlayerController.initialize(application) { track ->
            viewModelScope.launch {
                repository.recordPlayed(track)
            }
        }

        _trendingTracks.value = repository.getInitialTrendingTracks()
        _radioStations.value = repository.getCuratedRadioStations()

        // Preload initial search results for top music
        performSearch("Top Hits 2024")
    }

    fun setActiveTab(tab: NavTab) {
        _activeTab.value = tab
    }

    fun setShowNowPlayingSheet(show: Boolean) {
        _showNowPlayingSheet.value = show
    }

    fun setShowGoogleSearchSheet(show: Boolean) {
        _showGoogleSearchSheet.value = show
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            val results = repository.searchSongs(query)
            _searchResults.value = results
            _isSearching.value = false
        }
    }

    fun performSearch(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            val results = repository.searchSongs(query)
            _searchResults.value = results
            _isSearching.value = false
        }
    }

    fun filterByGenre(genre: String) {
        _selectedGenre.value = genre
        if (genre == "All") {
            performSearch("Top Hits")
        } else {
            performSearch(genre)
        }
    }

    fun playTrack(track: Track, queue: List<Track> = listOf(track)) {
        PlayerController.playTrack(track, queue)
    }

    fun playCustomAudioStream(url: String, title: String) {
        if (url.isBlank()) {
            _toastMessage.value = "Please enter a valid audio URL"
            return
        }
        val customTrack = Track(
            id = "custom_" + System.currentTimeMillis(),
            title = if (title.isNotBlank()) title else "Custom Web Stream",
            artist = "Google / Web Stream",
            album = "Online Source",
            artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&q=80",
            streamUrl = url.trim(),
            durationMs = 0L,
            genre = "Custom",
            source = "Web Search",
            isLiveStream = true
        )
        PlayerController.playTrack(customTrack, listOf(customTrack))
        _toastMessage.value = "Streaming custom web audio..."
        _showGoogleSearchSheet.value = false
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            val isFav = favorites.value.any { it.id == track.id }
            repository.toggleFavorite(track, isFav)
            _toastMessage.value = if (isFav) "Removed from Favorites" else "Added to Favorites"
        }
    }

    fun createPlaylist(name: String, description: String = "") {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createPlaylist(name.trim(), description.trim())
            _toastMessage.value = "Playlist \"$name\" created"
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(id)
            if (_selectedPlaylist.value?.playlistId == id) {
                _selectedPlaylist.value = null
                _selectedPlaylistTracks.value = emptyList()
            }
            _toastMessage.value = "Playlist deleted"
        }
    }

    fun selectPlaylist(playlist: PlaylistEntity?) {
        _selectedPlaylist.value = playlist
        if (playlist != null) {
            viewModelScope.launch {
                repository.getPlaylistTracks(playlist.playlistId).collect {
                    _selectedPlaylistTracks.value = it
                }
            }
        } else {
            _selectedPlaylistTracks.value = emptyList()
        }
    }

    fun addTrackToPlaylist(playlistId: Long, playlistName: String, track: Track) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, track)
            _toastMessage.value = "Added to $playlistName"
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: String) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, trackId)
            _toastMessage.value = "Removed from playlist"
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _toastMessage.value = "Playback history cleared"
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
