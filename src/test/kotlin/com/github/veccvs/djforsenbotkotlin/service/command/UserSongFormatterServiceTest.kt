package com.github.veccvs.djforsenbotkotlin.service.command

import com.github.veccvs.djforsenbotkotlin.dao.CytubeDao
import com.github.veccvs.djforsenbotkotlin.model.*
import com.github.veccvs.djforsenbotkotlin.service.UserSongService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class UserSongFormatterServiceTest {

  @Mock private lateinit var userSongService: UserSongService

  @Mock private lateinit var messageService: MessageService

  @Mock private lateinit var cytubeDao: CytubeDao

  private lateinit var userSongFormatterService: UserSongFormatterService

  private val testUsername = "testUser"
  private val testChannel = "testChannel"

  @BeforeEach
  fun setUp() {
    userSongFormatterService = UserSongFormatterService(userSongService, messageService, cytubeDao)
  }

  @Test
  fun `test getUserSongs with empty unplayed songs`() {
    // Given
    `when`(userSongService.getUnplayedUserSongs(testUsername)).thenReturn(emptyList())

    // When
    userSongFormatterService.getUserSongs(testUsername, testChannel)

    // Then
    verify(messageService)
      .sendMessage(testChannel, "@$testUsername docJAM You don't have any unplayed songs")
  }

  @Test
  fun `test getUserSongs with unplayed songs`() {
    // Given
    val user = User(testUsername)
    val song1 =
      Song().apply {
        id = UUID.randomUUID()
        link = "https://www.youtube.com/watch?v=123456"
      }
    val song2 =
      Song().apply {
        id = UUID.randomUUID()
        link = "https://www.youtube.com/watch?v=789012"
      }

    val userSong1 =
      UserSong().apply {
        id = UUID.randomUUID()
        this.user = user
        this.song = song1
        title = "Test Song 1"
        played = false
        addedAt = LocalDateTime.now().minusHours(1)
      }

    val userSong2 =
      UserSong().apply {
        id = UUID.randomUUID()
        this.user = user
        this.song = song2
        title = "Test Song 2"
        played = false
        addedAt = LocalDateTime.now()
      }

    val unplayedSongs = listOf(userSong1, userSong2)
    `when`(userSongService.getUnplayedUserSongs(testUsername)).thenReturn(unplayedSongs)
    `when`(cytubeDao.getPlaylist()).thenReturn(null)

    // When
    userSongFormatterService.getUserSongs(testUsername, testChannel)

    // Then
    verify(messageService)
      .sendMessage(
        testChannel,
        "@$testUsername docJAM Your unplayed songs: Test Song 1 (not in queue), Test Song 2 (not in queue)",
      )
  }

  @Test
  fun `test getUserSongs with null song`() {
    // Given
    val user = User(testUsername)
    val userSong =
      UserSong().apply {
        id = UUID.randomUUID()
        this.user = user
        this.song = null
        title = "Test Song"
        played = false
        addedAt = LocalDateTime.now()
      }

    val unplayedSongs = listOf(userSong)
    `when`(userSongService.getUnplayedUserSongs(testUsername)).thenReturn(unplayedSongs)

    // When
    userSongFormatterService.getUserSongs(testUsername, testChannel)

    // Then
    verify(messageService).sendMessage(testChannel, "@$testUsername docJAM Your unplayed songs: ")
  }

  @Test
  fun `test getUserSongs with null link`() {
    // Given
    val user = User(testUsername)
    val song =
      Song().apply {
        id = UUID.randomUUID()
        link = null
      }
    val userSong =
      UserSong().apply {
        id = UUID.randomUUID()
        this.user = user
        this.song = song
        title = "Test Song"
        played = false
        addedAt = LocalDateTime.now()
      }

    val unplayedSongs = listOf(userSong)
    `when`(userSongService.getUnplayedUserSongs(testUsername)).thenReturn(unplayedSongs)

    // When
    userSongFormatterService.getUserSongs(testUsername, testChannel)

    // Then
    verify(messageService).sendMessage(testChannel, "@$testUsername docJAM Your unplayed songs: ")
  }

  @Test
  fun `test calculateEstimatedTime uses currentTime from Playlist`() {
    // Given
    val videoId = "testVideoId"
    val songLink = "https://www.youtube.com/watch?v=$videoId"

    // Create playlist items
    val currentVideo =
      PlaylistItem(
        1,
        false,
        "testuser",
        MediaLink("yt", "currentVideoId"),
        "Current Video",
        180, // 3 minutes duration
      )

    val targetVideo =
      PlaylistItem(
        2,
        false,
        "testuser",
        MediaLink("yt", videoId),
        "Target Video",
        240, // 4 minutes duration
      )

    val thirdVideo =
      PlaylistItem(
        3,
        false,
        "testuser",
        MediaLink("yt", "thirdVideoId"),
        "Third Video",
        300, // 5 minutes duration
      )

    // Create playlist with currentTime = 60 (1 minute into the current video)
    val playlist =
      Playlist(
        0,
        false,
        false,
        60f, // currentTime: 1 minute into the current video
        null,
        mutableListOf(currentVideo, targetVideo, thirdVideo),
      )

    `when`(cytubeDao.getPlaylist()).thenReturn(playlist)

    // When
    val result = userSongFormatterService.calculateEstimatedTime(songLink)

    // Then
    // Expected:
    // - Current video has 180 seconds duration, we're 60 seconds in, so 120 seconds remaining
    // - Target video is next, so estimated time is 120 seconds = 02:00
    assertEquals("02:00", result)
  }

  @Test
  fun `test calculateEstimatedTime adjusts time based on elapsed time`() {
    // Given
    val videoId = "testVideoId"
    val songLink = "https://www.youtube.com/watch?v=$videoId"

    // Create playlist items
    val currentVideo =
      PlaylistItem(
        1,
        false,
        "testuser",
        MediaLink("yt", "currentVideoId"),
        "Current Video",
        180, // 3 minutes duration
      )

    val targetVideo =
      PlaylistItem(
        2,
        false,
        "testuser",
        MediaLink("yt", videoId),
        "Target Video",
        240, // 4 minutes duration
      )

    // Create playlist with currentTime = 60 (1 minute into the current video)
    val playlist1 =
      Playlist(
        0,
        false,
        false,
        60f, // currentTime: 1 minute into the current video
        null,
        mutableListOf(currentVideo, targetVideo),
      )

    // First call to set up lastPlaylist and lastUpdateTime
    `when`(cytubeDao.getPlaylist()).thenReturn(playlist1)
    val result1 = userSongFormatterService.calculateEstimatedTime(songLink)
    assertEquals("02:00", result1) // 120 seconds = 02:00

    // Create a second playlist with the same currentTime (simulating that the external system
    // didn't update it)
    val playlist2 =
      Playlist(
        0,
        false,
        false,
        60f, // Same currentTime as before
        null,
        mutableListOf(currentVideo, targetVideo),
      )

    // Mock the current time to be 30 seconds later
    val field = UserSongFormatterService::class.java.getDeclaredField("lastUpdateTime")
    field.isAccessible = true
    field.set(
      userSongFormatterService,
      field.get(userSongFormatterService) as Long - 30000,
    ) // 30 seconds earlier

    // Second call should adjust the time based on elapsed time
    `when`(cytubeDao.getPlaylist()).thenReturn(playlist2)
    val result2 = userSongFormatterService.calculateEstimatedTime(songLink)

    // Expected:
    // - 30 seconds have passed, so remaining time should be 90 seconds = 01:30
    assertEquals("01:30", result2)
  }
}
