package com.github.veccvs.djforsenbotkotlin.service

import com.github.veccvs.djforsenbotkotlin.model.SkipCounter
import com.github.veccvs.djforsenbotkotlin.repository.SkipCounterRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class SkipCounterServiceTest {
    @Mock
    private lateinit var skipCounterRepository: SkipCounterRepository

    @InjectMocks
    private lateinit var skipCounterService: SkipCounterService

    private lateinit var existingSkipCounter: SkipCounter

    @BeforeEach
    fun setUp() {
        existingSkipCounter = SkipCounter()
        existingSkipCounter.id = 1
        existingSkipCounter.count = 5
    }

    @Test
    fun `resetSkipCounter should reset counter to 0 when counter exists`() {
        // Arrange
        `when`(skipCounterRepository.findById(1)).thenReturn(Optional.of(existingSkipCounter))

        // Act
        skipCounterService.resetSkipCounter()

        // Assert
        assertEquals(0, existingSkipCounter.count)
        verify(skipCounterRepository, times(2)).findById(1) // Called in initializeSkipCounterIfAbsent and resetSkipCounter
        verify(skipCounterRepository).save(existingSkipCounter)
    }

    @Test
    fun `resetSkipCounter should create new counter when counter does not exist`() {
        // Arrange
        val newSkipCounter = SkipCounter()
        newSkipCounter.id = 1
        newSkipCounter.count = 0

        // First call returns empty, second call (after save) returns the new counter
        `when`(skipCounterRepository.findById(1))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(newSkipCounter))

        `when`(skipCounterRepository.save(any(SkipCounter::class.java))).thenReturn(newSkipCounter)

        // Act
        skipCounterService.resetSkipCounter()

        // Assert
        verify(skipCounterRepository, times(2)).findById(1)
        verify(skipCounterRepository, times(2)).save(any(SkipCounter::class.java))
    }

    @Test
    fun `incrementSkipCounter should increment counter by 1 when counter exists`() {
        // Arrange
        `when`(skipCounterRepository.findById(1)).thenReturn(Optional.of(existingSkipCounter))
        val initialCount = existingSkipCounter.count

        // Act
        skipCounterService.incrementSkipCounter()

        // Assert
        assertEquals(initialCount + 1, existingSkipCounter.count)
        verify(skipCounterRepository, times(2)).findById(1) // Called in initializeSkipCounterIfAbsent and incrementSkipCounter
        verify(skipCounterRepository).save(existingSkipCounter)
    }

    @Test
    fun `incrementSkipCounter should create new counter when counter does not exist`() {
        // Arrange
        val newSkipCounter = SkipCounter()
        newSkipCounter.id = 1
        newSkipCounter.count = 0

        // First call returns empty, second call (after save) returns the new counter
        `when`(skipCounterRepository.findById(1))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(newSkipCounter))

        `when`(skipCounterRepository.save(any(SkipCounter::class.java))).thenReturn(newSkipCounter)

        // Act
        skipCounterService.incrementSkipCounter()

        // Assert
        assertEquals(1, newSkipCounter.count) // Should be incremented to 1
        verify(skipCounterRepository, times(2)).findById(1)
        verify(skipCounterRepository, times(2)).save(any(SkipCounter::class.java))
    }

    @Test
    fun `getSkipCounter should return counter value when counter exists`() {
        // Arrange
        `when`(skipCounterRepository.findById(1)).thenReturn(Optional.of(existingSkipCounter))

        // Act
        val result = skipCounterService.getSkipCounter()

        // Assert
        assertEquals(existingSkipCounter.count, result)
        verify(skipCounterRepository, times(2)).findById(1) // Called in initializeSkipCounterIfAbsent and getSkipCounter
        verify(skipCounterRepository, never()).save(any(SkipCounter::class.java))
    }

    @Test
    fun `getSkipCounter should create new counter and return 0 when counter does not exist`() {
        // Arrange
        val newSkipCounter = SkipCounter()
        newSkipCounter.id = 1
        newSkipCounter.count = 0

        // First call returns empty, second call (after save) returns the new counter
        `when`(skipCounterRepository.findById(1))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(newSkipCounter))

        `when`(skipCounterRepository.save(any(SkipCounter::class.java))).thenReturn(newSkipCounter)

        // Act
        val result = skipCounterService.getSkipCounter()

        // Assert
        assertEquals(0, result)
        verify(skipCounterRepository, times(2)).findById(1)
        verify(skipCounterRepository).save(any(SkipCounter::class.java))
    }

    @Test
    fun `initializeSkipCounterIfAbsent should create new counter when counter does not exist`() {
        // Arrange
        val newSkipCounter = SkipCounter()
        newSkipCounter.id = 1
        newSkipCounter.count = 0

        // First call returns empty, second call (after save) returns the new counter
        `when`(skipCounterRepository.findById(1))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(newSkipCounter))

        `when`(skipCounterRepository.save(any(SkipCounter::class.java))).thenReturn(newSkipCounter)

        // Act - call a method that uses initializeSkipCounterIfAbsent
        skipCounterService.getSkipCounter()

        // Assert
        verify(skipCounterRepository, times(2)).findById(1)
        verify(skipCounterRepository).save(any(SkipCounter::class.java))
    }

    @Test
    fun `initializeSkipCounterIfAbsent should not create new counter when counter exists`() {
        // Arrange
        `when`(skipCounterRepository.findById(1)).thenReturn(Optional.of(existingSkipCounter))

        // Act - call a method that uses initializeSkipCounterIfAbsent
        skipCounterService.getSkipCounter()

        // Assert
        verify(skipCounterRepository, times(2)).findById(1) // Called in initializeSkipCounterIfAbsent and getSkipCounter
        verify(skipCounterRepository, never()).save(any(SkipCounter::class.java))
    }
}
