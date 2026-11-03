package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.GoogleSearchSheet
import com.example.ui.components.MiniPlayer
import com.example.ui.components.NowPlayingSheet
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.RadioScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MusicViewModel
import com.example.ui.viewmodel.NavTab

class MainActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                SoundWaveApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SoundWaveApp(viewModel: MusicViewModel) {
    val context = LocalContext.current
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val showNowPlaying by viewModel.showNowPlayingSheet.collectAsStateWithLifecycle()
    val showGoogleSearch by viewModel.showGoogleSearchSheet.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val toastMsg by viewModel.toastMessage.collectAsStateWithLifecycle()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Request notification permission for Android 13+ background media notification
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled gracefully */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(toastMsg) {
        toastMsg?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // Predictive back handler
    BackHandler(enabled = showNowPlaying || showGoogleSearch || selectedPlaylist != null || activeTab != NavTab.HOME) {
        when {
            showNowPlaying -> viewModel.setShowNowPlayingSheet(false)
            showGoogleSearch -> viewModel.setShowGoogleSearchSheet(false)
            selectedPlaylist != null -> viewModel.selectPlaylist(null)
            activeTab != NavTab.HOME -> viewModel.setActiveTab(NavTab.HOME)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Docked Mini Player
                MiniPlayer(
                    playbackState = playbackState,
                    onExpandClick = { viewModel.setShowNowPlayingSheet(true) }
                )

                // Navigation Bar
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = activeTab == NavTab.HOME,
                        onClick = { viewModel.setActiveTab(NavTab.HOME) },
                        icon = {
                            Icon(
                                imageVector = if (activeTab == NavTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = "Home"
                            )
                        },
                        label = { Text("Home") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = NeonCyan,
                            indicatorColor = NeonCyan,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_home")
                    )

                    NavigationBarItem(
                        selected = activeTab == NavTab.SEARCH,
                        onClick = { viewModel.setActiveTab(NavTab.SEARCH) },
                        icon = {
                            Icon(
                                imageVector = if (activeTab == NavTab.SEARCH) Icons.Filled.Search else Icons.Outlined.Search,
                                contentDescription = "Search"
                            )
                        },
                        label = { Text("Search") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = NeonCyan,
                            indicatorColor = NeonCyan,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_search")
                    )

                    NavigationBarItem(
                        selected = activeTab == NavTab.RADIO,
                        onClick = { viewModel.setActiveTab(NavTab.RADIO) },
                        icon = {
                            Icon(
                                imageVector = if (activeTab == NavTab.RADIO) Icons.Filled.Radio else Icons.Outlined.Radio,
                                contentDescription = "Radio"
                            )
                        },
                        label = { Text("Radio") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = NeonCyan,
                            indicatorColor = NeonCyan,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_radio")
                    )

                    NavigationBarItem(
                        selected = activeTab == NavTab.LIBRARY,
                        onClick = { viewModel.setActiveTab(NavTab.LIBRARY) },
                        icon = {
                            Icon(
                                imageVector = if (activeTab == NavTab.LIBRARY) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic,
                                contentDescription = "Library"
                            )
                        },
                        label = { Text("Library") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = NeonCyan,
                            indicatorColor = NeonCyan,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_library")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                NavTab.HOME -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToSearch = { viewModel.setActiveTab(NavTab.SEARCH) }
                )
                NavTab.SEARCH -> SearchScreen(
                    viewModel = viewModel
                )
                NavTab.RADIO -> RadioScreen(
                    viewModel = viewModel
                )
                NavTab.LIBRARY -> LibraryScreen(
                    viewModel = viewModel
                )
            }
        }
    }

    // Full screen Now Playing Sheet
    if (showNowPlaying && playbackState.currentTrack != null) {
        val currentTrack = playbackState.currentTrack!!
        val isFav = favorites.any { it.id == currentTrack.id }
        NowPlayingSheet(
            playbackState = playbackState,
            isFavorite = isFav,
            onFavoriteToggle = { viewModel.toggleFavorite(currentTrack) },
            onDismiss = { viewModel.setShowNowPlayingSheet(false) }
        )
    }

    // Google Search & Web Stream Sheet
    if (showGoogleSearch) {
        GoogleSearchSheet(
            onPlayCustomUrl = { url, title ->
                viewModel.playCustomAudioStream(url, title)
            },
            onDismiss = { viewModel.setShowGoogleSearchSheet(false) }
        )
    }
}
