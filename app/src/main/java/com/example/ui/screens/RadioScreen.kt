package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Track
import com.example.ui.components.RadioCard
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MusicViewModel

@Composable
fun RadioScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val radioStations by viewModel.radioStations.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()

    var showCustomStreamDialog by remember { mutableStateOf(false) }
    var customStreamName by remember { mutableStateOf("") }
    var customStreamUrl by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("radio_screen"),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Live Internet Radio",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "24/7 background audio streams worldwide",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }

                    OutlinedButton(
                        onClick = { showCustomStreamDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("add_custom_station_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Stream", color = NeonCyan, fontSize = 12.sp)
                    }
                }
            }
        }

        // Stations List
        items(radioStations) { station ->
            val isCurrent = playbackState.currentTrack?.id == station.id
            RadioCard(
                station = station,
                isCurrentTrack = isCurrent,
                isPlaying = playbackState.isPlaying,
                onPlayClick = { viewModel.playOrToggleTrack(station, radioStations) },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }
    }

    // Add Custom Stream Dialog
    if (showCustomStreamDialog) {
        AlertDialog(
            onDismissRequest = { showCustomStreamDialog = false },
            title = { Text("Add Live Radio Stream") },
            text = {
                Column {
                    Text(
                        text = "Enter any HTTP/HTTPS live stream URL (MP3, AAC, or Icecast broadcast):",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customStreamName,
                        onValueChange = { customStreamName = it },
                        placeholder = { Text("Station Name (e.g. My Favorite Radio)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customStreamUrl,
                        onValueChange = { customStreamUrl = it },
                        placeholder = { Text("https://stream.example.com/live.mp3") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customStreamUrl.isNotBlank()) {
                            viewModel.playCustomAudioStream(customStreamUrl, customStreamName)
                            showCustomStreamDialog = false
                            customStreamUrl = ""
                            customStreamName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Stream Now", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomStreamDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
