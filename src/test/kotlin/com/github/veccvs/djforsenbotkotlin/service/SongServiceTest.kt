package com.github.veccvs.djforsenbotkotlin.service

import com.github.veccvs.djforsenbotkotlin.model.Song
import com.github.veccvs.djforsenbotkotlin.repository.SongRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class SongServiceTest {
    @Mock
    private lateinit var songRepository: SongRepository

    @InjectMocks
    private lateinit var songService: SongService

    @Test
    fun `addUniqueSong should save song when it does not exist`() {
        // Arrange
        val songLink = "https://youtu.be/testId"
        `when`(songRepository.existsByLink(songLink)).thenReturn(false)

        // Act
        songService.addUniqueSong(songLink)

        // Assert
        verify(songRepository).existsByLink(songLink)
        verify(songRepository).save(any(Song::class.java))
    }

    @Test
    fun `addUniqueSong should not save song when it already exists`() {
        // Arrange
        val songLink = "https://youtu.be/testId"
        `when`(songRepository.existsByLink(songLink)).thenReturn(true)

        // Act
        songService.addUniqueSong(songLink)

        // Assert
        verify(songRepository).existsByLink(songLink)
        verify(songRepository, never()).save(any(Song::class.java))
    }
}