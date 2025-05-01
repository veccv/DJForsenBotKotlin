package com.github.veccvs.djforsenbotkotlin.config

import com.github.veccvs.djforsenbotkotlin.model.LastSong
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UserConfigTest {

    @Test
    fun `lastSong should be initialized with empty link`() {
        // Arrange
        val userConfig = UserConfig()

        // Act - Nothing to do, the constructor initializes lastSong

        // Assert
        assertNotNull(userConfig.lastSong)
        assertEquals("", userConfig.lastSong.link)
    }

    @Test
    fun `lastSong setter should update the property`() {
        // Arrange
        val userConfig = UserConfig()
        val newLastSong = LastSong().apply { link = "https://youtu.be/testId" }

        // Act
        userConfig.lastSong = newLastSong

        // Assert
        assertEquals(newLastSong, userConfig.lastSong)
        assertEquals("https://youtu.be/testId", userConfig.lastSong.link)
    }
}
