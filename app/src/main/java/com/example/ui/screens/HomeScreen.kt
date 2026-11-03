package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.model.Track
import com.example.ui.components.RadioCard
import com.example.ui.components.TrackItem
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VibrantPink
import com.example.ui.viewmodel.MusicViewModel

@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val trendingTracks by viewModel.trendingTracks.collectAsStateWithLifecycle()
    val radioStations by viewModel.radioStations.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val selectedGenre by viewModel.selectedGenre.collectAsStateWithLifecycle()

    val genres = listOf("All", "Pop", "Lo-Fi", "Electronic", "Rock", "Chillout", "EDM", "Acoustic")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // App Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = NeonCyan,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "SoundWave",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                                letterSpacing = (-0.5).sp
                            )
                        )
                        Text(
                            text = "Online Stream & Background Music",
                            style = MaterialTheme.typography.labelSmall.copy(color = NeonCyan)
                        )
                    }
                }

                // Google Search button shortcut
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { viewModel.setShowGoogleSearchSheet(true) }
                        .testTag("home_google_search_shortcut")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Google Search",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                }
            }
        }

        // Hero Feature Banner
        item {
            val featuredTrack = trendingTracks.firstOrNull()
            if (featuredTrack != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .height(170.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .testTag("hero_banner"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = featuredTrack.artworkUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xF00A0E17),
                                            Color(0xD00F172A),
                                            Color(0x551E293B)
                                        )
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                color = ElectricViolet,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "FEATURED TRACK",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = featuredTrack.title,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${featuredTrack.artist} • ${featuredTrack.genre}",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFCBD5E1)),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = { viewModel.playTrack(featuredTrack, trendingTracks) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                modifier = Modifier.testTag("hero_play_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Play Now",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Genre Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                genres.forEach { genre ->
                    val isSelected = selectedGenre == genre
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.filterByGenre(genre) },
                        label = {
                            Text(
                                text = genre,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan,
                            selectedLabelColor = Color.Black,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("filter_chip_$genre")
                    )
                }
            }
        }

        // Recently Played (if any)
        if (recentlyPlayed.isNotEmpty()) {
            item {
                Text(
                    text = "Recently Played",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ),
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recentlyPlayed.take(8)) { rTrack ->
                        Column(
                            modifier = Modifier
                                .width(110.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.playTrack(rTrack, recentlyPlayed) }
                                .testTag("recent_track_${rTrack.id}"),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                if (rTrack.artworkUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = rTrack.artworkUrl,
                                        contentDescription = rTrack.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = rTrack.title,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = TextPrimary
                            )
                            Text(
                                text = rTrack.artist,
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Trending Tracks Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Trending Online Tracks",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "Search More",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.clickable { onNavigateToSearch() }
                )
            }
        }

        items(trendingTracks) { track ->
            val isCurrent = playbackState.currentTrack?.id == track.id
            val isFav = favorites.any { it.id == track.id }
            TrackItem(
                track = track,
                isCurrentTrack = isCurrent,
                isPlaying = playbackState.isPlaying,
                onClick = { viewModel.playTrack(track, trendingTracks) },
                isFavorite = isFav,
                onFavoriteToggle = { viewModel.toggleFavorite(track) },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            )
        }

        // Curated Live Radio Stations
        item {
            Text(
                text = "Live Radio & Ambient Streams",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                ),
                modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 12.dp)
            )
        }

        items(radioStations.take(3)) { station ->
            val isCurrent = playbackState.currentTrack?.id == station.id
            RadioCard(
                station = station,
                isCurrentTrack = isCurrent,
                isPlaying = playbackState.isPlaying,
                onPlayClick = { viewModel.playTrack(station, radioStations) },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }
    }
}
