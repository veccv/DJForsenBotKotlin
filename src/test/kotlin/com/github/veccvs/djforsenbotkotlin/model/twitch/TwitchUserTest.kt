package com.github.veccvs.djforsenbotkotlin.model.twitch

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TwitchUserTest {

    @Test
    fun `TwitchUser constructor should initialize with provided values`() {
        // Arrange & Act
        val twitchUser = TwitchUser(
            id = "12345",
            login = "testuser",
            displayName = "TestUser",
            type = "staff",
            broadcasterType = "partner",
            description = "Test description",
            profileImageUrl = "https://example.com/profile.jpg",
            offlineImageUrl = "https://example.com/offline.jpg",
            viewCount = 1000,
            createdAt = "2020-01-01T00:00:00Z",
            email = "test@example.com"
        )
        
        // Assert
        assertEquals("12345", twitchUser.id)
        assertEquals("testuser", twitchUser.login)
        assertEquals("TestUser", twitchUser.displayName)
        assertEquals("staff", twitchUser.type)
        assertEquals("partner", twitchUser.broadcasterType)
        assertEquals("Test description", twitchUser.description)
        assertEquals("https://example.com/profile.jpg", twitchUser.profileImageUrl)
        assertEquals("https://example.com/offline.jpg", twitchUser.offlineImageUrl)
        assertEquals(1000, twitchUser.viewCount)
        assertEquals("2020-01-01T00:00:00Z", twitchUser.createdAt)
        assertEquals("test@example.com", twitchUser.email)
    }
    
    @Test
    fun `TwitchUser constructor should handle null email`() {
        // Arrange & Act
        val twitchUser = TwitchUser(
            id = "12345",
            login = "testuser",
            displayName = "TestUser",
            type = "staff",
            broadcasterType = "partner",
            description = "Test description",
            profileImageUrl = "https://example.com/profile.jpg",
            offlineImageUrl = "https://example.com/offline.jpg",
            viewCount = 1000,
            createdAt = "2020-01-01T00:00:00Z",
            email = null
        )
        
        // Assert
        assertNull(twitchUser.email)
    }
    
    @Test
    fun `TwitchUser equals should work correctly`() {
        // Arrange
        val user1 = TwitchUser(
            id = "12345",
            login = "testuser",
            displayName = "TestUser",
            type = "staff",
            broadcasterType = "partner",
            description = "Test description",
            profileImageUrl = "https://example.com/profile.jpg",
            offlineImageUrl = "https://example.com/offline.jpg",
            viewCount = 1000,
            createdAt = "2020-01-01T00:00:00Z",
            email = "test@example.com"
        )
        
        val user2 = TwitchUser(
            id = "12345",
            login = "testuser",
            displayName = "TestUser",
            type = "staff",
            broadcasterType = "partner",
            description = "Test description",
            profileImageUrl = "https://example.com/profile.jpg",
            offlineImageUrl = "https://example.com/offline.jpg",
            viewCount = 1000,
            createdAt = "2020-01-01T00:00:00Z",
            email = "test@example.com"
        )
        
        val user3 = TwitchUser(
            id = "67890", // Different ID
            login = "testuser",
            displayName = "TestUser",
            type = "staff",
            broadcasterType = "partner",
            description = "Test description",
            profileImageUrl = "https://example.com/profile.jpg",
            offlineImageUrl = "https://example.com/offline.jpg",
            viewCount = 1000,
            createdAt = "2020-01-01T00:00:00Z",
            email = "test@example.com"
        )
        
        // Act & Assert
        assertEquals(user1, user2)
        assertNotEquals(user1, user3)
    }
    
    @Test
    fun `TwitchUser copy should create a new object with specified changes`() {
        // Arrange
        val user = TwitchUser(
            id = "12345",
            login = "testuser",
            displayName = "TestUser",
            type = "staff",
            broadcasterType = "partner",
            description = "Test description",
            profileImageUrl = "https://example.com/profile.jpg",
            offlineImageUrl = "https://example.com/offline.jpg",
            viewCount = 1000,
            createdAt = "2020-01-01T00:00:00Z",
            email = "test@example.com"
        )
        
        // Act
        val copied = user.copy(
            id = "67890",
            displayName = "NewName",
            viewCount = 2000
        )
        
        // Assert
        assertEquals("67890", copied.id)
        assertEquals("testuser", copied.login) // Unchanged
        assertEquals("NewName", copied.displayName)
        assertEquals("staff", copied.type) // Unchanged
        assertEquals("partner", copied.broadcasterType) // Unchanged
        assertEquals("Test description", copied.description) // Unchanged
        assertEquals("https://example.com/profile.jpg", copied.profileImageUrl) // Unchanged
        assertEquals("https://example.com/offline.jpg", copied.offlineImageUrl) // Unchanged
        assertEquals(2000, copied.viewCount)
        assertEquals("2020-01-01T00:00:00Z", copied.createdAt) // Unchanged
        assertEquals("test@example.com", copied.email) // Unchanged
    }
    
    @Test
    fun `TwitchResponse constructor should initialize with provided values`() {
        // Arrange
        val user1 = TwitchUser(
            id = "12345",
            login = "testuser1",
            displayName = "TestUser1",
            type = "staff",
            broadcasterType = "partner",
            description = "Test description 1",
            profileImageUrl = "https://example.com/profile1.jpg",
            offlineImageUrl = "https://example.com/offline1.jpg",
            viewCount = 1000,
            createdAt = "2020-01-01T00:00:00Z",
            email = "test1@example.com"
        )
        
        val user2 = TwitchUser(
            id = "67890",
            login = "testuser2",
            displayName = "TestUser2",
            type = "normal",
            broadcasterType = "affiliate",
            description = "Test description 2",
            profileImageUrl = "https://example.com/profile2.jpg",
            offlineImageUrl = "https://example.com/offline2.jpg",
            viewCount = 2000,
            createdAt = "2021-01-01T00:00:00Z",
            email = "test2@example.com"
        )
        
        val users = listOf(user1, user2)
        
        // Act
        val response = TwitchResponse(users)
        
        // Assert
        assertEquals(users, response.data)
        assertEquals(2, response.data.size)
        assertEquals(user1, response.data[0])
        assertEquals(user2, response.data[1])
    }
    
    @Test
    fun `TwitchResponse equals should work correctly`() {
        // Arrange
        val user1 = TwitchUser(
            id = "12345",
            login = "testuser1",
            displayName = "TestUser1",
            type = "staff",
            broadcasterType = "partner",
            description = "Test description 1",
            profileImageUrl = "https://example.com/profile1.jpg",
            offlineImageUrl = "https://example.com/offline1.jpg",
            viewCount = 1000,
            createdAt = "2020-01-01T00:00:00Z",
            email = "test1@example.com"
        )
        
        val users1 = listOf(user1)
        val users2 = listOf(user1)
        val users3 = listOf(user1.copy(id = "67890"))
        
        val response1 = TwitchResponse(users1)
        val response2 = TwitchResponse(users2)
        val response3 = TwitchResponse(users3)
        
        // Act & Assert
        assertEquals(response1, response2)
        assertNotEquals(response1, response3)
    }
    
    @Test
    fun `TwitchResponse copy should create a new object with specified changes`() {
        // Arrange
        val user1 = TwitchUser(
            id = "12345",
            login = "testuser1",
            displayName = "TestUser1",
            type = "staff",
            broadcasterType = "partner",
            description = "Test description 1",
            profileImageUrl = "https://example.com/profile1.jpg",
            offlineImageUrl = "https://example.com/offline1.jpg",
            viewCount = 1000,
            createdAt = "2020-01-01T00:00:00Z",
            email = "test1@example.com"
        )
        
        val user2 = TwitchUser(
            id = "67890",
            login = "testuser2",
            displayName = "TestUser2",
            type = "normal",
            broadcasterType = "affiliate",
            description = "Test description 2",
            profileImageUrl = "https://example.com/profile2.jpg",
            offlineImageUrl = "https://example.com/offline2.jpg",
            viewCount = 2000,
            createdAt = "2021-01-01T00:00:00Z",
            email = "test2@example.com"
        )
        
        val users1 = listOf(user1)
        val users2 = listOf(user2)
        
        val response = TwitchResponse(users1)
        
        // Act
        val copied = response.copy(data = users2)
        
        // Assert
        assertEquals(users2, copied.data)
        assertEquals(1, copied.data.size)
        assertEquals(user2, copied.data[0])
    }
}