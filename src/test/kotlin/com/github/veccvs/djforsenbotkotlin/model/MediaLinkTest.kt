package com.github.veccvs.djforsenbotkotlin.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MediaLinkTest {

    @Test
    fun `constructor should initialize with provided values`() {
        // Arrange & Act
        val mediaLink = MediaLink("yt", "videoId")
        
        // Assert
        assertEquals("yt", mediaLink.type)
        assertEquals("videoId", mediaLink.id)
    }
    
    @Test
    fun `equals should return true for identical objects`() {
        // Arrange
        val mediaLink1 = MediaLink("yt", "videoId")
        val mediaLink2 = MediaLink("yt", "videoId")
        
        // Act & Assert
        assertEquals(mediaLink1, mediaLink2)
        assertEquals(mediaLink1.hashCode(), mediaLink2.hashCode())
    }
    
    @Test
    fun `equals should return false for different objects`() {
        // Arrange
        val mediaLink1 = MediaLink("yt", "videoId1")
        val mediaLink2 = MediaLink("yt", "videoId2")
        val mediaLink3 = MediaLink("sc", "videoId1")
        
        // Act & Assert
        assertNotEquals(mediaLink1, mediaLink2)
        assertNotEquals(mediaLink1, mediaLink3)
        assertNotEquals(mediaLink2, mediaLink3)
    }
    
    @Test
    fun `copy should create a new object with specified changes`() {
        // Arrange
        val mediaLink = MediaLink("yt", "videoId")
        
        // Act
        val copiedWithNewType = mediaLink.copy(type = "sc")
        val copiedWithNewId = mediaLink.copy(id = "newId")
        val copiedWithBothNew = mediaLink.copy(type = "sc", id = "newId")
        
        // Assert
        assertEquals("sc", copiedWithNewType.type)
        assertEquals("videoId", copiedWithNewType.id)
        
        assertEquals("yt", copiedWithNewId.type)
        assertEquals("newId", copiedWithNewId.id)
        
        assertEquals("sc", copiedWithBothNew.type)
        assertEquals("newId", copiedWithBothNew.id)
    }
    
    @Test
    fun `toString should return a string representation`() {
        // Arrange
        val mediaLink = MediaLink("yt", "videoId")
        
        // Act
        val result = mediaLink.toString()
        
        // Assert
        assertTrue(result.contains("yt"))
        assertTrue(result.contains("videoId"))
    }
    
    @Test
    fun `component functions should work for destructuring`() {
        // Arrange
        val mediaLink = MediaLink("yt", "videoId")
        
        // Act
        val (type, id) = mediaLink
        
        // Assert
        assertEquals("yt", type)
        assertEquals("videoId", id)
    }
}