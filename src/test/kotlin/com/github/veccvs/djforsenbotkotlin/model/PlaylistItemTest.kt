package com.github.veccvs.djforsenbotkotlin.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PlaylistItemTest {

    @Test
    fun `constructor should initialize with provided values`() {
        // Arrange
        val mediaLink = MediaLink("yt", "videoId")
        
        // Act
        val playlistItem = PlaylistItem(
            uid = 1,
            temp = false,
            username = "testuser",
            link = mediaLink,
            title = "Test Video",
            duration = 180
        )
        
        // Assert
        assertEquals(1, playlistItem.uid)
        assertFalse(playlistItem.temp)
        assertEquals("testuser", playlistItem.username)
        assertEquals(mediaLink, playlistItem.link)
        assertEquals("Test Video", playlistItem.title)
        assertEquals(180, playlistItem.duration)
    }
    
    @Test
    fun `equals should return true for identical objects`() {
        // Arrange
        val mediaLink = MediaLink("yt", "videoId")
        val item1 = PlaylistItem(1, false, "testuser", mediaLink, "Test Video", 180)
        val item2 = PlaylistItem(1, false, "testuser", mediaLink, "Test Video", 180)
        
        // Act & Assert
        assertEquals(item1, item2)
        assertEquals(item1.hashCode(), item2.hashCode())
    }
    
    @Test
    fun `equals should return false for different objects`() {
        // Arrange
        val mediaLink1 = MediaLink("yt", "videoId1")
        val mediaLink2 = MediaLink("yt", "videoId2")
        
        val item1 = PlaylistItem(1, false, "testuser", mediaLink1, "Test Video", 180)
        val item2 = PlaylistItem(2, false, "testuser", mediaLink1, "Test Video", 180)
        val item3 = PlaylistItem(1, true, "testuser", mediaLink1, "Test Video", 180)
        val item4 = PlaylistItem(1, false, "otheruser", mediaLink1, "Test Video", 180)
        val item5 = PlaylistItem(1, false, "testuser", mediaLink2, "Test Video", 180)
        val item6 = PlaylistItem(1, false, "testuser", mediaLink1, "Other Video", 180)
        val item7 = PlaylistItem(1, false, "testuser", mediaLink1, "Test Video", 240)
        
        // Act & Assert
        assertNotEquals(item1, item2) // Different uid
        assertNotEquals(item1, item3) // Different temp
        assertNotEquals(item1, item4) // Different username
        assertNotEquals(item1, item5) // Different link
        assertNotEquals(item1, item6) // Different title
        assertNotEquals(item1, item7) // Different duration
    }
    
    @Test
    fun `copy should create a new object with specified changes`() {
        // Arrange
        val mediaLink = MediaLink("yt", "videoId")
        val item = PlaylistItem(1, false, "testuser", mediaLink, "Test Video", 180)
        
        // Act
        val copiedWithNewUid = item.copy(uid = 2)
        val copiedWithNewTemp = item.copy(temp = true)
        val copiedWithNewUsername = item.copy(username = "newuser")
        val copiedWithNewLink = item.copy(link = MediaLink("sc", "audioId"))
        val copiedWithNewTitle = item.copy(title = "New Title")
        val copiedWithNewDuration = item.copy(duration = 240)
        
        // Assert
        assertEquals(2, copiedWithNewUid.uid)
        assertTrue(copiedWithNewTemp.temp)
        assertEquals("newuser", copiedWithNewUsername.username)
        assertEquals(MediaLink("sc", "audioId"), copiedWithNewLink.link)
        assertEquals("New Title", copiedWithNewTitle.title)
        assertEquals(240, copiedWithNewDuration.duration)
    }
    
    @Test
    fun `toString should return a string representation`() {
        // Arrange
        val mediaLink = MediaLink("yt", "videoId")
        val item = PlaylistItem(1, false, "testuser", mediaLink, "Test Video", 180)
        
        // Act
        val result = item.toString()
        
        // Assert
        assertTrue(result.contains("1"))
        assertTrue(result.contains("false"))
        assertTrue(result.contains("testuser"))
        assertTrue(result.contains("Test Video"))
        assertTrue(result.contains("180"))
    }
    
    @Test
    fun `component functions should work for destructuring`() {
        // Arrange
        val mediaLink = MediaLink("yt", "videoId")
        val item = PlaylistItem(1, false, "testuser", mediaLink, "Test Video", 180)
        
        // Act
        val (uid, temp, username, link, title, duration) = item
        
        // Assert
        assertEquals(1, uid)
        assertFalse(temp)
        assertEquals("testuser", username)
        assertEquals(mediaLink, link)
        assertEquals("Test Video", title)
        assertEquals(180, duration)
    }
}