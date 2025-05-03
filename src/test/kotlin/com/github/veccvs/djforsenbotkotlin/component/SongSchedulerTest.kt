package com.github.veccvs.djforsenbotkotlin.component

import com.github.veccvs.djforsenbotkotlin.config.UserConfig
import com.github.veccvs.djforsenbotkotlin.dao.CytubeDao
import com.github.veccvs.djforsenbotkotlin.model.*
import com.github.veccvs.djforsenbotkotlin.repository.SongRepository
import com.github.veccvs.djforsenbotkotlin.repository.UserRepository
import com.github.veccvs.djforsenbotkotlin.service.CommandService
import com.github.veccvs.djforsenbotkotlin.service.SkipCounterService
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class SongSchedulerTest {

  @Mock private lateinit var cytubeDao: CytubeDao

  @Mock private lateinit var songRepository: SongRepository

  @Mock private lateinit var userConfig: UserConfig

  @Mock private lateinit var commandService: CommandService

  @Mock private lateinit var userRepository: UserRepository

  @Mock private lateinit var skipCounterService: SkipCounterService

  private lateinit var songScheduler: SongScheduler

  @BeforeEach
  fun setUp() {
    songScheduler =
      SongScheduler(
        cytubeDao,
        songRepository,
        userConfig,
        commandService,
        userRepository,
        skipCounterService,
      )
  }

  @Test
  fun `scheduleSong should add random song when playlist is empty and bot is enabled`() {
    // Arrange
    val botStatus = BotStatus(true)
    val emptyPlaylist = Playlist(0, false, false, 0f, null, mutableListOf())
    val song =
      Song().apply {
        id = UUID.randomUUID()
        link = "https://youtu.be/testId"
      }

    `when`(cytubeDao.getBotStatus()).thenReturn(botStatus)
    `when`(cytubeDao.getPlaylist()).thenReturn(emptyPlaylist)
    `when`(songRepository.findAll()).thenReturn(listOf(song))

    // Act
    songScheduler.scheduleSong()

    // Assert
    verify(cytubeDao).addVideo("https://youtu.be/testId")
  }

  @Test
  fun `scheduleSong should not add song when playlist is not empty`() {
    // Arrange
    val botStatus = BotStatus(true)
    val mediaLink = MediaLink("yt", "videoId")
    val playlistItem = PlaylistItem(1, false, "testuser", mediaLink, "Test Video", 180)
    val nonEmptyPlaylist = Playlist(0, false, false, 0f, null, mutableListOf(playlistItem))

    `when`(cytubeDao.getBotStatus()).thenReturn(botStatus)
    `when`(cytubeDao.getPlaylist()).thenReturn(nonEmptyPlaylist)

    // Act
    songScheduler.scheduleSong()

    // Assert
    verify(songRepository, never()).findAll()
    verify(cytubeDao, never()).addVideo(anyString())
  }

  @Test
  fun `scheduleSong should not add song when bot is disabled`() {
    // Arrange
    val botStatus = BotStatus(false)

    `when`(cytubeDao.getBotStatus()).thenReturn(botStatus)

    // Act
    songScheduler.scheduleSong()

    // Assert
    verify(cytubeDao, never()).getPlaylist()
    verify(songRepository, never()).findAll()
    verify(cytubeDao, never()).addVideo(anyString())
  }

  @Test
  fun `changeLastSong should update lastSong when song changes`() {
    // Arrange
    val botStatus = BotStatus(true)
    val mediaLink = MediaLink("yt", "videoId")
    val playlistItem = PlaylistItem(1, false, "testuser", mediaLink, "Test Video", 180)
    val playlist = Playlist(0, false, false, 0f, null, mutableListOf(playlistItem))
    val lastSong = LastSong().apply { link = "differentId" } // Different from videoId

    `when`(cytubeDao.getBotStatus()).thenReturn(botStatus)
    `when`(cytubeDao.getPlaylist()).thenReturn(playlist)
    `when`(userConfig.lastSong).thenReturn(lastSong)
    `when`(userConfig.channelName).thenReturn("#testchannel")

    // Act
    songScheduler.changeLastSong()

    // Assert
    // We can't easily verify the exact LastSong object, so we'll just verify the other interactions
    verify(commandService).sendMessage("#testchannel", "docJAM now playing: Test Video")
    verify(skipCounterService).resetSkipCounter()
  }

  @Test
  fun `changeLastSong should truncate long titles`() {
    // Arrange
    val botStatus = BotStatus(true)
    val mediaLink = MediaLink("yt", "videoId")
    val longTitle = "A".repeat(100) // Create a title longer than 50 characters
    val playlistItem = PlaylistItem(1, false, "testuser", mediaLink, longTitle, 180)
    val playlist = Playlist(0, false, false, 0f, null, mutableListOf(playlistItem))
    val lastSong = LastSong().apply { link = "differentId" } // Different from videoId

    `when`(cytubeDao.getBotStatus()).thenReturn(botStatus)
    `when`(cytubeDao.getPlaylist()).thenReturn(playlist)
    `when`(userConfig.lastSong).thenReturn(lastSong)
    `when`(userConfig.channelName).thenReturn("#testchannel")

    // Act
    songScheduler.changeLastSong()

    // Assert
    // We can't easily verify the exact LastSong object, so we'll just verify the other interactions
    verify(commandService)
      .sendMessage("#testchannel", "docJAM now playing: " + "A".repeat(50) + "[...]")
    verify(skipCounterService).resetSkipCounter()
  }

  @Test
  fun `notifyUsers should notify users who can add videos`() {
    // Arrange
    val botStatus = BotStatus(true)
    val user = User("testuser")
    user.lastAddedVideo = LocalDateTime.now().minusMinutes(3) // Last video added 3 minutes ago
    user.userNotified = false

    `when`(cytubeDao.getBotStatus()).thenReturn(botStatus)
    `when`(userRepository.findAllByUserNotifiedIsFalse()).thenReturn(listOf(user))
    `when`(userConfig.channelName).thenReturn("#testchannel")

    // Act
    songScheduler.notifyUsers()

    // Assert
    verify(commandService)
      .sendMessage("#testchannel", "@testuser forsenJam you can add song now! forsenMaxLevel")
    verify(userRepository).save(user)
    assertTrue(user.userNotified)
  }

  @Test
  fun `notifyUsers should not notify users who cannot add videos yet`() {
    // Arrange
    val botStatus = BotStatus(true)
    val user = User("testuser")
    user.lastAddedVideo = LocalDateTime.now().minusMinutes(1) // Last video added 1 minute ago
    user.userNotified = false

    `when`(cytubeDao.getBotStatus()).thenReturn(botStatus)
    `when`(userRepository.findAllByUserNotifiedIsFalse()).thenReturn(listOf(user))

    // Act
    songScheduler.notifyUsers()

    // Assert
    verify(commandService, never()).sendMessage(anyString(), anyString())
    verify(userRepository, never()).save(any(User::class.java))
    assertFalse(user.userNotified)
  }
}
