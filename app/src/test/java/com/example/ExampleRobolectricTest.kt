package com.example

import com.example.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleRobolectricTest {

  @Test
  fun `track formatted duration calculation`() {
    val track = Track(
      id = "test_1",
      title = "Test Song",
      artist = "Test Artist",
      streamUrl = "https://example.com/audio.mp3",
      durationMs = 185000L
    )
    assertEquals("3:05", track.formattedDuration)

    val liveTrack = track.copy(isLiveStream = true)
    assertEquals("LIVE", liveTrack.formattedDuration)
  }
}
