package com.github.veccvs.djforsenbotkotlin.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PlaylistTest {

    @Test
    fun `constructor should initialize with provided values`() {
        // Arrange
        val mediaLink = MediaLink("yt", "videoId")
        val playlistItem = PlaylistItem(1, false, "testuser", mediaLink, "Test Video", 180)
        val queue = mutableListOf(playlistItem)

        // Act
        val playlist = Playlist(
            time = 100,
            locked = true,
            paused = false,
            currentTime = 50,
            _current = playlistItem,
            queue = queue
        )

        // Assert
        assertEquals(100, playlist.time)
        assertTrue(playlist.locked)
        assertFalse(playlist.paused)
        assertEquals(50, playlist.currentTime)
        assertEquals(queue, playlist.queue)
    }

    @Test
    fun `equals should return true for identical objects`() {
        // Arrange
        val mediaLink = MediaLink("yt", "videoId")
        val playlistItem = PlaylistItem(1, false, "testuser", mediaLink, "Test Video", 180)
        val queue1 = mutableListOf(playlistItem)
        val queue2 = mutableListOf(playlistItem)

        val playlist1 = Playlist(100, true, false, 50, playlistItem, queue1)
        val playlist2 = Playlist(100, true, false, 50, playlistItem, queue2)

        // Act & Assert
        assertEquals(playlist1, playlist2)
        assertEquals(playlist1.hashCode(), playlist2.hashCode())
    }

    @Test
    fun `equals should return false for different objects`() {
        // Arrange
        val mediaLink = MediaLink("yt", "videoId")
        val playlistItem1 = PlaylistItem(1, false, "testuser", mediaLink, "Test Video", 180)
        val playlistItem2 = PlaylistItem(2, false, "testuser", mediaLink, "Test Video", 180)
        val queue1 = mutableListOf(playlistItem1)
        val queue2 = mutableListOf(playlistItem2)

        val playlist1 = Playlist(100, true, false, 50, playlistItem1, queue1)
        val playlist2 = Playlist(200, true, false, 50, playlistItem1, queue1) // Different time
        val playlist3 = Playlist(100, false, false, 50, playlistItem1, queue1) // Different locked
        val playlist4 = Playlist(100, true, true, 50, playlistItem1, queue1) // Different paused
        val playlist5 = Playlist(100, true, false, 60, playlistItem1, queue1) // Different currentTime
        val playlist6 = Playlist(100, true, false, 50, playlistItem2, queue1) // Different _current
        val playlist7 = Playlist(100, true, false, 50, playlistItem1, queue2) // Different queue

        // Act & Assert
        assertNotEquals(playlist1, playlist2)
        assertNotEquals(playlist1, playlist3)
        assertNotEquals(playlist1, playlist4)
        assertNotEquals(playlist1, playlist5)
        assertNotEquals(playlist1, playlist6)
        assertNotEquals(playlist1, playlist7)
    }

    @Test
    fun `copy should create a new object with specified changes`() {
        // Arrange
        val mediaLink = MediaLink("yt", "videoId")
        val playlistItem1 = PlaylistItem(1, false, "testuser", mediaLink, "Test Video", 180)
        val playlistItem2 = PlaylistItem(2, false, "testuser", mediaLink, "Test Video", 180)
        val queue1 = mutableListOf(playlistItem1)
        val queue2 = mutableListOf(playlistItem2)

        val playlist = Playlist(100, true, false, 50, playlistItem1, queue1)

        // Act
        val copiedWithNewTime = playlist.copy(time = 200)
        val copiedWithNewLocked = playlist.copy(locked = false)
        val copiedWithNewPaused = playlist.copy(paused = true)
        val copiedWithNewCurrentTime = playlist.copy(currentTime = 60)
        val copiedWithNewQueue = playlist.copy(queue = queue2)

        // Assert
        assertEquals(200, copiedWithNewTime.time)
        assertFalse(copiedWithNewLocked.locked)
        assertTrue(copiedWithNewPaused.paused)
        assertEquals(60, copiedWithNewCurrentTime.currentTime)
        assertEquals(queue2, copiedWithNewQueue.queue)
    }

    @Test
    fun `toString should return a string representation`() {
        // Arrange
        val mediaLink = MediaLink("yt", "videoId")
        val playlistItem = PlaylistItem(1, false, "testuser", mediaLink, "Test Video", 180)
        val queue = mutableListOf(playlistItem)

        val playlist = Playlist(100, true, false, 50, playlistItem, queue)

        // Act
        val result = playlist.toString()

        // Assert
        assertTrue(result.contains("100"))
        assertTrue(result.contains("true"))
        assertTrue(result.contains("false"))
        assertTrue(result.contains("50"))
    }

    @Test
    fun `component functions should work for destructuring`() {
        // Arrange
        val mediaLink = MediaLink("yt", "videoId")
        val playlistItem = PlaylistItem(1, false, "testuser", mediaLink, "Test Video", 180)
        val queue = mutableListOf(playlistItem)

        val playlist = Playlist(100, true, false, 50, playlistItem, queue)

        // Act - We can only access public components
        val time = playlist.component1()
        val locked = playlist.component2()
        val paused = playlist.component3()
        val currentTime = playlist.component4()
        // component5 is private (_current)
        val queueResult = playlist.component6()

        // Assert
        assertEquals(100, time)
        assertTrue(locked)
        assertFalse(paused)
        assertEquals(50, currentTime)
        assertEquals(queue, queueResult)
    }

    @Test
    fun `queue should be mutable`() {
        // Arrange
        val mediaLink = MediaLink("yt", "videoId")
        val playlistItem1 = PlaylistItem(1, false, "testuser", mediaLink, "Test Video", 180)
        val playlistItem2 = PlaylistItem(2, false, "testuser", mediaLink, "Another Video", 240)
        val queue = mutableListOf(playlistItem1)

        val playlist = Playlist(100, true, false, 50, playlistItem1, queue)

        // Act
        playlist.queue.add(playlistItem2)

        // Assert
        assertEquals(2, playlist.queue.size)
        assertEquals(playlistItem1, playlist.queue[0])
        assertEquals(playlistItem2, playlist.queue[1])
    }
}
