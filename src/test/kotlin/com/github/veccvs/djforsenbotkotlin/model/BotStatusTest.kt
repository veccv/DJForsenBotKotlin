package com.github.veccvs.djforsenbotkotlin.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BotStatusTest {

    @Test
    fun `constructor should initialize with provided botEnabled value`() {
        // Arrange & Act
        val botStatusEnabled = BotStatus(true)
        val botStatusDisabled = BotStatus(false)
        
        // Assert
        assertTrue(botStatusEnabled.botEnabled)
        assertFalse(botStatusDisabled.botEnabled)
    }
    
    @Test
    fun `getter should return correct value`() {
        // Arrange
        val botStatusEnabled = BotStatus(true)
        val botStatusDisabled = BotStatus(false)
        
        // Act & Assert
        assertTrue(botStatusEnabled.botEnabled)
        assertFalse(botStatusDisabled.botEnabled)
    }
}