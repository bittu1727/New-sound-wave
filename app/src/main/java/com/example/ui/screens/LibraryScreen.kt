package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.PlaylistEntity
import com.example.model.Track
import com.example.ui.components.TrackItem
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VibrantPink
import com.example.ui.viewmodel.MusicViewModel

@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Favorites", "Playlists", "History")

    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsStateWithLifecycle()
    val selectedPlaylistTracks by viewModel.selectedPlaylistTracks.collectAsStateWithLifecycle()

    var showNewPlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var newPlaylistDesc by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("library_screen")
    ) {
        // Top Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Music Library",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )

            if (selectedTabIndex == 1 && selectedPlaylist == null) {
                Button(
                    onClick = { showNewPlaylistDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    modifier = Modifier.testTag("create_playlist_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            } else if (selectedTabIndex == 2 && recentlyPlayed.isNotEmpty()) {
                IconButton(onClick = { viewModel.clearHistory() }) {
                    Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clear History", tint = TextSecondary)
                }
            }
        }

        // Playlist Detail View or Main Tabs
        if (selectedPlaylist != null) {
            val playlist = selectedPlaylist!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.selectPlaylist(null) }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "${selectedPlaylistTracks.size} tracks",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }

                    IconButton(onClick = { viewModel.deletePlaylist(playlist.playlistId) }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Playlist", tint = VibrantPink)
                    }
                }

                if (selectedPlaylistTracks.isNotEmpty()) {
                    Button(
                        onClick = { viewModel.playTrack(selectedPlaylistTracks.first(), selectedPlaylistTracks) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Play All Tracks", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    if (selectedPlaylistTracks.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "This playlist is empty.\nSearch any song and add it here!",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                    items(selectedPlaylistTracks) { pTrack ->
                        val isCurrent = playbackState.currentTrack?.id == pTrack.id
                        val isFav = favorites.any { it.id == pTrack.id }
                        TrackItem(
                            track = pTrack,
                            isCurrentTrack = isCurrent,
                            isPlaying = playbackState.isPlaying,
                            onClick = { viewModel.playTrack(pTrack, selectedPlaylistTracks) },
                            isFavorite = isFav,
                            onFavoriteToggle = { viewModel.toggleFavorite(pTrack) },
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        } else {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = NeonCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = NeonCyan
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) NeonCyan else TextSecondary
                            )
                        }
                    )
                }
            }

            // Tab Content
            when (selectedTabIndex) {
                0 -> { // Favorites
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp).let {
                            PaddingValues(
                                start = it.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                                top = it.calculateTopPadding(),
                                end = it.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                                bottom = 120.dp
                            )
                        }
                    ) {
                        if (favorites.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(40.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = VibrantPink.copy(alpha = 0.5f),
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No favorite songs yet",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Tap the heart on any song to save it here for fast playback.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            item {
                                Button(
                                    onClick = { viewModel.playTrack(favorites.first(), favorites) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                                ) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Play All Favorites (${favorites.size})", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }

                            items(favorites) { fTrack ->
                                val isCurrent = playbackState.currentTrack?.id == fTrack.id
                                TrackItem(
                                    track = fTrack,
                                    isCurrentTrack = isCurrent,
                                    isPlaying = playbackState.isPlaying,
                                    onClick = { viewModel.playTrack(fTrack, favorites) },
                                    isFavorite = true,
                                    onFavoriteToggle = { viewModel.toggleFavorite(fTrack) },
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                1 -> { // Playlists
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp).let {
                            PaddingValues(
                                start = it.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                                top = it.calculateTopPadding(),
                                end = it.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                                bottom = 120.dp
                            )
                        }
                    ) {
                        if (playlists.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(40.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QueueMusic,
                                        contentDescription = null,
                                        tint = ElectricViolet.copy(alpha = 0.5f),
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No custom playlists yet",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Create custom playlists to organize your favorite online music.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            items(playlists) { pl ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { viewModel.selectPlaylist(pl) }
                                        .testTag("playlist_item_${pl.playlistId}"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = ElectricViolet,
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(imageVector = Icons.Default.QueueMusic, contentDescription = null, tint = Color.White)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = pl.name,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                            )
                                            if (pl.description.isNotEmpty()) {
                                                Text(
                                                    text = pl.description,
                                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> { // History
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp).let {
                            PaddingValues(
                                start = it.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                                top = it.calculateTopPadding(),
                                end = it.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                                bottom = 120.dp
                            )
                        }
                    ) {
                        if (recentlyPlayed.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(40.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = TextSecondary.copy(alpha = 0.5f),
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No history recorded yet",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Any song or radio you play will automatically appear here.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            items(recentlyPlayed) { hTrack ->
                                val isCurrent = playbackState.currentTrack?.id == hTrack.id
                                val isFav = favorites.any { it.id == hTrack.id }
                                TrackItem(
                                    track = hTrack,
                                    isCurrentTrack = isCurrent,
                                    isPlaying = playbackState.isPlaying,
                                    onClick = { viewModel.playTrack(hTrack, recentlyPlayed) },
                                    isFavorite = isFav,
                                    onFavoriteToggle = { viewModel.toggleFavorite(hTrack) },
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // New Playlist Dialog
    if (showNewPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showNewPlaylistDialog = false },
            title = { Text("Create New Playlist") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        placeholder = { Text("Playlist Name (e.g. Chill Beats)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPlaylistDesc,
                        onValueChange = { newPlaylistDesc = it },
                        placeholder = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            viewModel.createPlaylist(newPlaylistName, newPlaylistDesc)
                            showNewPlaylistDialog = false
                            newPlaylistName = ""
                            newPlaylistDesc = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Create", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewPlaylistDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
