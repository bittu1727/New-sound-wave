package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Track
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VibrantPink

@Composable
fun TrackItem(
    track: Track,
    isCurrentTrack: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onFavoriteToggle: (() -> Unit)? = null,
    isFavorite: Boolean = false,
    onAddToPlaylistClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("track_item_${track.id}"),
        color = if (isCurrentTrack) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Artwork / Cover
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (track.artworkUrl.isNotEmpty()) {
                    AsyncImage(
                        model = track.artworkUrl,
                        contentDescription = "Cover for ${track.title}",
                        modifier = Modifier.size(54.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // If currently playing, show miniature equalizer overlay
                if (isCurrentTrack) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AudioVisualizer(
                            isPlaying = isPlaying,
                            barCount = 4,
                            barWidth = 3.dp,
                            barSpacing = 2.dp,
                            maxHeight = 22.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Title & Artist
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isCurrentTrack) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isCurrentTrack) NeonCyan else TextPrimary,
                        fontSize = 15.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${track.artist}${if (track.album.isNotEmpty()) " • " + track.album else ""}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 13.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Duration / Live Tag
            if (track.isLiveStream) {
                Surface(
                    color = VibrantPink,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text = "LIVE",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else {
                Text(
                    text = track.formattedDuration,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            // Quick Actions / Menu
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("track_menu_button_${track.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Track options",
                        tint = TextSecondary
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    if (onFavoriteToggle != null) {
                        DropdownMenuItem(
                            text = { Text(if (isFavorite) "Remove Favorite" else "Favorite") },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (isFavorite) VibrantPink else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onFavoriteToggle()
                            }
                        )
                    }
                    if (onAddToPlaylistClick != null) {
                        DropdownMenuItem(
                            text = { Text("Add to Playlist") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PlaylistAdd,
                                    contentDescription = null,
                                    tint = ElectricViolet
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onAddToPlaylistClick()
                            }
                        )
                    }
                }
            }
        }
    }
}
