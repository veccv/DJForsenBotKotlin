package com.github.veccvs.djforsenbotkotlin.dao

import com.github.veccvs.djforsenbotkotlin.config.UserConfig
import com.github.veccvs.djforsenbotkotlin.model.BotStatus
import com.github.veccvs.djforsenbotkotlin.model.Playlist
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import java.lang.reflect.Field

@ExtendWith(MockitoExtension::class)
class CytubeDaoTest {

    @Mock
    private lateinit var restTemplate: RestTemplate

    @Mock
    private lateinit var userConfig: UserConfig

    private lateinit var cytubeDao: CytubeDao

    private val daoAddress = "http://localhost:8080"

    @BeforeEach
    fun setUp() {
        // Create CytubeDao with mocked dependencies
        cytubeDao = CytubeDao(restTemplate, userConfig)

        // Use reflection to set the url field directly
        val urlField: Field = CytubeDao::class.java.getDeclaredField("url")
        urlField.isAccessible = true
        urlField.set(cytubeDao, daoAddress)
    }

    @Test
    fun `getPlaylist should return playlist from API`() {
        // Arrange
        val playlist = Playlist(0, false, false, 0, null, mutableListOf())
        val responseEntity = ResponseEntity.ok(playlist)

        `when`(restTemplate.getForEntity("$daoAddress/playlist", Playlist::class.java))
            .thenReturn(responseEntity)

        // Act
        val result = cytubeDao.getPlaylist()

        // Assert
        assertEquals(playlist, result)
        verify(restTemplate).getForEntity("$daoAddress/playlist", Playlist::class.java)
    }

    @Test
    fun `getBotStatus should return bot status from API`() {
        // Arrange
        val botStatus = BotStatus(true)
        val responseEntity = ResponseEntity.ok(botStatus)

        `when`(restTemplate.getForEntity("$daoAddress/bot-status", BotStatus::class.java))
            .thenReturn(responseEntity)

        // Act
        val result = cytubeDao.getBotStatus()

        // Assert
        assertEquals(botStatus, result)
        verify(restTemplate).getForEntity("$daoAddress/bot-status", BotStatus::class.java)
    }

    @Test
    fun `getBotStatus should return null when API call throws exception`() {
        // Arrange
        `when`(restTemplate.getForEntity("$daoAddress/bot-status", BotStatus::class.java))
            .thenThrow(RuntimeException("API error"))

        // Act
        val result = cytubeDao.getBotStatus()

        // Assert
        assertNull(result)
        verify(restTemplate).getForEntity("$daoAddress/bot-status", BotStatus::class.java)
    }

    @Test
    fun `sendMessage should send message to API`() {
        // Arrange
        val message = "Test message"
        val responseEntity = ResponseEntity.ok("Success")

        `when`(restTemplate.postForEntity("$daoAddress/send-message?message=$message", null, String::class.java))
            .thenReturn(responseEntity)

        // Act
        val result = cytubeDao.sendMessage(message)

        // Assert
        assertEquals("Success", result)
        verify(restTemplate).postForEntity("$daoAddress/send-message?message=$message", null, String::class.java)
    }

    @Test
    fun `addVideo should add video to API`() {
        // Arrange
        val videoUrl = "https://youtu.be/testId"
        val playlist = Playlist(0, false, false, 0, null, mutableListOf())
        val responseEntity = ResponseEntity.ok(playlist)

        `when`(restTemplate.postForEntity("$daoAddress/add-video?link=$videoUrl", null, Playlist::class.java))
            .thenReturn(responseEntity)

        // Act
        val result = cytubeDao.addVideo(videoUrl)

        // Assert
        assertEquals(playlist, result)
        verify(restTemplate).postForEntity("$daoAddress/add-video?link=$videoUrl", null, Playlist::class.java)
    }

    @Test
    fun `skipVideo should call API to skip video`() {
        // Arrange - Nothing to arrange for this test

        // Act
        cytubeDao.skipVideo()

        // Assert
        verify(restTemplate).put("$daoAddress/skip-song", null)
    }
}
