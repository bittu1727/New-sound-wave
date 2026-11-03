package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.example.model.Track
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "favorite_tracks")
data class FavoriteTrackEntity(
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
    val addedAt: Long = System.currentTimeMillis()
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
        fun fromTrack(track: Track): FavoriteTrackEntity = FavoriteTrackEntity(
            id = track.id,
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
    }
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite_tracks ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteTrackEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_tracks WHERE id = :trackId)")
    fun isFavorite(trackId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(track: FavoriteTrackEntity)

    @Query("DELETE FROM favorite_tracks WHERE id = :trackId")
    suspend fun deleteFavorite(trackId: String)
}
