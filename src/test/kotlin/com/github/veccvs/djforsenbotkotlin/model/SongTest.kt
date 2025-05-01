package com.github.veccvs.djforsenbotkotlin.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

class SongTest {

    @Test
    fun `constructor should initialize with default values`() {
        // Act
        val song = Song()
        
        // Assert
        assertNull(song.id)
        assertNull(song.link)
    }
    
    @Test
    fun `setters should update properties`() {
        // Arrange
        val song = Song()
        val uuid = UUID.randomUUID()
        val link = "https://youtu.be/testId"
        
        // Act
        song.id = uuid
        song.link = link
        
        // Assert
        assertEquals(uuid, song.id)
        assertEquals(link, song.link)
    }
    
    @Test
    fun `getters should return correct values`() {
        // Arrange
        val song = Song()
        val uuid = UUID.randomUUID()
        val link = "https://youtu.be/testId"
        song.id = uuid
        song.link = link
        
        // Act & Assert
        assertEquals(uuid, song.id)
        assertEquals(link, song.link)
    }
}