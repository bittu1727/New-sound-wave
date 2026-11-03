package com.example.data.api

import com.example.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class OnlineMusicApi(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    suspend fun searchSongs(query: String, limit: Int = 40): List<Track> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "https://itunes.apple.com/search?term=$encoded&media=music&entity=song&limit=$limit"

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "SoundWave-MusicApp/1.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val responseBody = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(responseBody)
            val results = json.optJSONArray("results") ?: return@withContext emptyList()

            val tracks = mutableListOf<Track>()
            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue
                val streamUrl = item.optString("previewUrl")
                if (streamUrl.isNullOrBlank()) continue

                val trackId = item.optLong("trackId", System.currentTimeMillis() + i).toString()
                val trackName = item.optString("trackName", "Unknown Track")
                val artistName = item.optString("artistName", "Unknown Artist")
                val collectionName = item.optString("collectionName", "Single")
                val rawArtwork = item.optString("artworkUrl100", "")
                // Upgrade artwork to high resolution 600x600
                val highResArtwork = if (rawArtwork.isNotEmpty()) {
                    rawArtwork.replace("100x100bb.jpg", "600x600bb.jpg")
                } else ""
                val durationMs = item.optLong("trackTimeMillis", 30000L)
                val genre = item.optString("primaryGenreName", "Music")

                tracks.add(
                    Track(
                        id = trackId,
                        title = trackName,
                        artist = artistName,
                        album = collectionName,
                        artworkUrl = highResArtwork,
                        streamUrl = streamUrl,
                        durationMs = durationMs,
                        genre = genre,
                        source = "Search",
                        isLiveStream = false
                    )
                )
            }
            tracks
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
