package com.github.veccvs.djforsenbotkotlin.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

class UserTest {

    @Test
    fun `primary constructor should initialize with provided username and default values`() {
        // Arrange
        val username = "testuser"
        
        // Act
        val user = User(username)
        
        // Assert
        assertEquals(username, user.username)
        assertNull(user.id)
        assertEquals(0, user.points)
        assertTrue(user.lastAddedVideo.isBefore(LocalDateTime.now()))
        assertTrue(user.lastResponse.isBefore(LocalDateTime.now()))
        assertTrue(user.userNotified)
        assertTrue(user.lastSkip.isBefore(LocalDateTime.now()))
    }
    
    @Test
    fun `setters should update properties`() {
        // Arrange
        val user = User("testuser")
        val uuid = UUID.randomUUID()
        val now = LocalDateTime.now()
        
        // Act
        user.id = uuid
        user.points = 100
        user.lastAddedVideo = now
        user.lastResponse = now
        user.userNotified = false
        user.lastSkip = now
        
        // Assert
        assertEquals(uuid, user.id)
        assertEquals(100, user.points)
        assertEquals(now, user.lastAddedVideo)
        assertEquals(now, user.lastResponse)
        assertFalse(user.userNotified)
        assertEquals(now, user.lastSkip)
    }
    
    @Test
    fun `getters should return correct values`() {
        // Arrange
        val username = "testuser"
        val user = User(username)
        val uuid = UUID.randomUUID()
        val now = LocalDateTime.now()
        
        user.id = uuid
        user.points = 100
        user.lastAddedVideo = now
        user.lastResponse = now
        user.userNotified = false
        user.lastSkip = now
        
        // Act & Assert
        assertEquals(username, user.username)
        assertEquals(uuid, user.id)
        assertEquals(100, user.points)
        assertEquals(now, user.lastAddedVideo)
        assertEquals(now, user.lastResponse)
        assertFalse(user.userNotified)
        assertEquals(now, user.lastSkip)
    }
}