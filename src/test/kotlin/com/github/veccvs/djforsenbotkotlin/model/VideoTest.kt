package com.github.veccvs.djforsenbotkotlin.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class VideoTest {

    @Test
    fun `primary constructor should initialize with provided values`() {
        // Arrange & Act
        val video = Video(
            id = "testId",
            thumbnails = listOf("thumbnail1", "thumbnail2"),
            title = "Test Video",
            longDesc = "This is a test video",
            channel = "Test Channel",
            views = "1000",
            publishTime = "2023-01-01",
            urlSuffix = "test-suffix"
        )
        
        // Assert
        assertEquals("testId", video.id)
        assertEquals(listOf("thumbnail1", "thumbnail2"), video.thumbnails)
        assertEquals("Test Video", video.title)
        assertEquals("This is a test video", video.longDesc)
        assertEquals("Test Channel", video.channel)
        assertEquals("1000", video.views)
        assertEquals("2023-01-01", video.publishTime)
        assertEquals("test-suffix", video.urlSuffix)
        assertEquals(0, video.duration) // Default value
    }
    
    @Test
    fun `secondary constructor should parse duration correctly`() {
        // Arrange & Act
        val video = Video(
            id = "testId",
            thumbnails = listOf("thumbnail1"),
            title = "Test Video",
            longDesc = "This is a test video",
            channel = "Test Channel",
            duration = "1:30:45", // 1 hour, 30 minutes, 45 seconds
            views = "1000",
            publishTime = "2023-01-01",
            urlSuffix = "test-suffix"
        )
        
        // Assert
        assertEquals(5445, video.duration) // 1*3600 + 30*60 + 45 = 5445 seconds
    }
    
    @Test
    fun `secondary constructor should handle minutes and seconds format`() {
        // Arrange & Act
        val video = Video(
            id = "testId",
            thumbnails = listOf("thumbnail1"),
            title = "Test Video",
            longDesc = "This is a test video",
            channel = "Test Channel",
            duration = "5:30", // 5 minutes, 30 seconds
            views = "1000",
            publishTime = "2023-01-01",
            urlSuffix = "test-suffix"
        )
        
        // Assert
        assertEquals(330, video.duration) // 5*60 + 30 = 330 seconds
    }
    
    @Test
    fun `secondary constructor should handle seconds only format`() {
        // Arrange & Act
        val video = Video(
            id = "testId",
            thumbnails = listOf("thumbnail1"),
            title = "Test Video",
            longDesc = "This is a test video",
            channel = "Test Channel",
            duration = "45", // 45 seconds
            views = "1000",
            publishTime = "2023-01-01",
            urlSuffix = "test-suffix"
        )
        
        // Assert
        assertEquals(45, video.duration) // 45 seconds
    }
    
    @Test
    fun `secondary constructor should handle empty duration`() {
        // Arrange & Act
        val video = Video(
            id = "testId",
            thumbnails = listOf("thumbnail1"),
            title = "Test Video",
            longDesc = "This is a test video",
            channel = "Test Channel",
            duration = "", // Empty duration
            views = "1000",
            publishTime = "2023-01-01",
            urlSuffix = "test-suffix"
        )
        
        // Assert
        assertEquals(0, video.duration) // Should default to 0
    }
    
    @Test
    fun `equals should work correctly`() {
        // Arrange
        val video1 = Video(
            id = "testId",
            thumbnails = listOf("thumbnail1"),
            title = "Test Video",
            longDesc = "This is a test video",
            channel = "Test Channel",
            views = "1000",
            publishTime = "2023-01-01",
            urlSuffix = "test-suffix"
        )
        video1.duration = 300
        
        val video2 = Video(
            id = "testId",
            thumbnails = listOf("thumbnail1"),
            title = "Test Video",
            longDesc = "This is a test video",
            channel = "Test Channel",
            views = "1000",
            publishTime = "2023-01-01",
            urlSuffix = "test-suffix"
        )
        video2.duration = 300
        
        val video3 = Video(
            id = "differentId",
            thumbnails = listOf("thumbnail1"),
            title = "Test Video",
            longDesc = "This is a test video",
            channel = "Test Channel",
            views = "1000",
            publishTime = "2023-01-01",
            urlSuffix = "test-suffix"
        )
        video3.duration = 300
        
        // Act & Assert
        assertEquals(video1, video2)
        assertNotEquals(video1, video3)
    }
    
    @Test
    fun `copy should create a new object with specified changes`() {
        // Arrange
        val video = Video(
            id = "testId",
            thumbnails = listOf("thumbnail1"),
            title = "Test Video",
            longDesc = "This is a test video",
            channel = "Test Channel",
            views = "1000",
            publishTime = "2023-01-01",
            urlSuffix = "test-suffix"
        )
        video.duration = 300
        
        // Act
        val copied = video.copy(id = "newId", title = "New Title")
        copied.duration = 600
        
        // Assert
        assertEquals("newId", copied.id)
        assertEquals("New Title", copied.title)
        assertEquals(600, copied.duration)
        
        // Original should be unchanged
        assertEquals("testId", video.id)
        assertEquals("Test Video", video.title)
        assertEquals(300, video.duration)
    }
    
    @Test
    fun `toString should return a string representation`() {
        // Arrange
        val video = Video(
            id = "testId",
            thumbnails = listOf("thumbnail1"),
            title = "Test Video",
            longDesc = "This is a test video",
            channel = "Test Channel",
            views = "1000",
            publishTime = "2023-01-01",
            urlSuffix = "test-suffix"
        )
        
        // Act
        val result = video.toString()
        
        // Assert
        assertTrue(result.contains("testId"))
        assertTrue(result.contains("Test Video"))
        assertTrue(result.contains("Test Channel"))
    }
}