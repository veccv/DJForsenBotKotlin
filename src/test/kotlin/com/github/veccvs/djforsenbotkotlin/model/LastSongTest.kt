package com.github.veccvs.djforsenbotkotlin.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LastSongTest {

    @Test
    fun `constructor should initialize with empty link`() {
        // Act
        val lastSong = LastSong()
        
        // Assert
        assertEquals("", lastSong.link)
    }
    
    @Test
    fun `setter should update link property`() {
        // Arrange
        val lastSong = LastSong()
        val newLink = "https://youtu.be/testId"
        
        // Act
        lastSong.link = newLink
        
        // Assert
        assertEquals(newLink, lastSong.link)
    }
    
    @Test
    fun `getter should return correct link value`() {
        // Arrange
        val lastSong = LastSong()
        val testLink = "https://youtu.be/anotherTestId"
        lastSong.link = testLink
        
        // Act & Assert
        assertEquals(testLink, lastSong.link)
    }
}