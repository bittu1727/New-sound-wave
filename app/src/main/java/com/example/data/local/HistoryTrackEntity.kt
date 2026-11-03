package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.example.model.Track
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "playback_history")
data class HistoryTrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String,
    val streamUrl: String,
    val durationMs: Long,
    val genre: String,
    val source: String,
    val isLiveStream: Boolean,
    val playedAt: Long = System.currentTimeMillis()
) {
    fun toTrack(): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        artworkUrl = artworkUrl,
        streamUrl = streamUrl,
        durationMs = durationMs,
        genre = genre,
        source = source,
        isLiveStream = isLiveStream
    )

    companion object {
        fun fromTrack(track: Track): HistoryTrackEntity = HistoryTrackEntity(
            id = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            artworkUrl = track.artworkUrl,
            streamUrl = track.streamUrl,
            durationMs = track.durationMs,
            genre = track.genre,
            source = track.source,
            isLiveStream = track.isLiveStream,
            playedAt = System.currentTimeMillis()
        )
    }
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM playback_history ORDER BY playedAt DESC LIMIT 50")
    fun getRecentlyPlayed(): Flow<List<HistoryTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(track: HistoryTrackEntity)

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()
}
