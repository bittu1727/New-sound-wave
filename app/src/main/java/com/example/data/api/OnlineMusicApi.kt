package com.example.data.api

import android.util.Base64
import com.example.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class OnlineMusicApi(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    /**
     * Searches for full-length online songs (3-6+ minutes) with high audio quality.
     */
    suspend fun searchSongs(query: String, limit: Int = 40): List<Track> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        // 1. Primary: Search Full-Length songs from global catalog
        val fullTracks = searchFullLengthTracks(query, limit)
        if (fullTracks.isNotEmpty()) {
            return@withContext fullTracks
        }

        // 2. Secondary fallback: iTunes audio search
        searchItunesFallback(query, limit)
    }

    private fun searchFullLengthTracks(query: String, limit: Int): List<Track> {
        return try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&_marker=0&cc=in&includeMetaTags=1&p=1&n=$limit&q=$encoded"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()

            val responseBody = response.body?.string() ?: return emptyList()
            val json = JSONObject(responseBody)
            val results = json.optJSONArray("results") ?: return emptyList()

            val tracks = mutableListOf<Track>()
            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue

                val encUrl = item.optString("encrypted_media_url", "")
                val streamUrl = if (encUrl.isNotEmpty()) decryptMediaUrl(encUrl) else null
                if (streamUrl.isNullOrBlank()) continue

                val rawId = item.optString("id", System.currentTimeMillis().toString() + i)
                val rawTitle = item.optString("song", "Unknown Track")
                val rawArtist = item.optString("primary_artists", item.optString("singers", "Unknown Artist"))
                val rawAlbum = item.optString("album", "")
                val rawImage = item.optString("image", "")
                val durationSec = item.optLong("duration", 180L)

                // High-resolution artwork upgrade
                val highResImage = rawImage
                    .replace("150x150.jpg", "500x500.jpg")
                    .replace("50x50.jpg", "500x500.jpg")

                val track = Track(
                    id = "full_" + rawId,
                    title = cleanHtml(rawTitle),
                    artist = cleanHtml(rawArtist),
                    album = cleanHtml(rawAlbum),
                    artworkUrl = highResImage,
                    streamUrl = streamUrl,
                    durationMs = durationSec * 1000L, // Full duration in milliseconds
                    genre = cleanHtml(item.optString("language", "Music")),
                    source = "Full Online Song",
                    isLiveStream = false
                )
                tracks.add(track)
            }
            tracks
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun searchItunesFallback(query: String, limit: Int): List<Track> {
        return try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://itunes.apple.com/search?term=$encoded&media=music&entity=song&limit=$limit"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "SoundWave-MusicApp/1.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()

            val responseBody = response.body?.string() ?: return emptyList()
            val json = JSONObject(responseBody)
            val results = json.optJSONArray("results") ?: return emptyList()

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
                val highResArtwork = if (rawArtwork.isNotEmpty()) {
                    rawArtwork.replace("100x100bb.jpg", "600x600bb.jpg")
                } else ""
                val durationMs = item.optLong("trackTimeMillis", 30000L)
                val genre = item.optString("primaryGenreName", "Music")

                tracks.add(
                    Track(
                        id = trackId,
                        title = cleanHtml(trackName),
                        artist = cleanHtml(artistName),
                        album = cleanHtml(collectionName),
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

    /**
     * Decrypts the high-fidelity media stream URL using DES decryption.
     */
    private fun decryptMediaUrl(encryptedUrl: String): String? {
        return try {
            val key = "38346591".toByteArray(Charsets.UTF_8)
            val keySpec = SecretKeySpec(key, "DES")
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val decodedBytes = Base64.decode(encryptedUrl, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            val url = String(decryptedBytes, Charsets.UTF_8)
            // Upgrade stream to 160kbps for crystal-clear full audio quality and enforce HTTPS
            url.replace("_96.mp4", "_160.mp4").replace("http://", "https://")
        } catch (e: Exception) {
            null
        }
    }

    private fun cleanHtml(text: String): String {
        return text.replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&#039;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&apos;", "'")
            .trim()
    }
}
