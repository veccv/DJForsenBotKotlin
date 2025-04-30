package com.github.veccvs.djforsenbotkotlin.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SkipCounterTest {

    @Test
    fun `constructor should initialize with default values`() {
        // Act
        val skipCounter = SkipCounter()
        
        // Assert
        assertNull(skipCounter.id)
        assertEquals(0, skipCounter.count)
    }
    
    @Test
    fun `setters should update properties`() {
        // Arrange
        val skipCounter = SkipCounter()
        
        // Act
        skipCounter.id = 1
        skipCounter.count = 5
        
        // Assert
        assertEquals(1, skipCounter.id)
        assertEquals(5, skipCounter.count)
    }
    
    @Test
    fun `getters should return correct values`() {
        // Arrange
        val skipCounter = SkipCounter()
        skipCounter.id = 2
        skipCounter.count = 10
        
        // Act & Assert
        assertEquals(2, skipCounter.id)
        assertEquals(10, skipCounter.count)
    }
}