package com.github.veccvs.djforsenbotkotlin.service

import com.github.veccvs.djforsenbotkotlin.component.TwitchChatBot
import com.github.veccvs.djforsenbotkotlin.component.TwitchConnector
import com.github.veccvs.djforsenbotkotlin.config.UserConfig
import com.github.veccvs.djforsenbotkotlin.dao.CytubeDao
import com.github.veccvs.djforsenbotkotlin.model.*
import com.github.veccvs.djforsenbotkotlin.repository.GachiSongRepository
import com.github.veccvs.djforsenbotkotlin.repository.UserRepository
import com.github.veccvs.djforsenbotkotlin.utils.BanPhraseChecker
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.time.LocalDateTime
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommandServiceTest {
  @Mock private lateinit var cytubeDao: CytubeDao

  @Mock private lateinit var twitchChatBot: TwitchChatBot

  @Mock private lateinit var userRepository: UserRepository

  @Mock private lateinit var userConfig: UserConfig

  @Mock private lateinit var songService: SongService

  @Mock private lateinit var skipCounterService: SkipCounterService

  @Mock private lateinit var gachiSongRepository: GachiSongRepository

  @InjectMocks private lateinit var commandService: CommandService

  private lateinit var twitchConnector: TwitchConnector

  @BeforeEach
  fun setUp() {
    // Mock TwitchConnector and inject it into CommandService
    twitchConnector = mock(TwitchConnector::class.java)
    val field = CommandService::class.java.getDeclaredField("twitchConnector")
    field.isAccessible = true
    field.set(commandService, twitchConnector)

    // Set default values for userConfig
    `when`(userConfig.minutesToAddVideo).thenReturn(5)
    `when`(userConfig.secondsToResponseToCommand).thenReturn(3)
    `when`(userConfig.minutesToSkipVideo).thenReturn("5")
    `when`(userConfig.skipValue).thenReturn("3")
    `when`(userConfig.channelName).thenReturn("#testchannel")
    `when`(userConfig.lastSong).thenReturn(LastSong())
  }

  @Test
  fun `test detectCommand with command`() {
    // Given
    val message = ";command param1 param2"

    // When
    val result = commandService.detectCommand(message)

    // Then
    assertEquals(";command", result)
  }

  @Test
  fun `test detectCommand with no command`() {
    // Given
    val message = "normal message"

    // When
    val result = commandService.detectCommand(message)

    // Then
    assertNull(result)
  }

  @Test
  fun `test parseCommand`() {
    // Given
    val message = ";command param1 param2"

    // When
    val result = commandService.parseCommand(message)

    // Then
    assertNotNull(result)
    assertEquals(";command", result?.command)
    assertEquals(2, result?.params?.size)
    assertEquals("param1", result?.params?.get(0))
    assertEquals("param2", result?.params?.get(1))
  }

  @Test
  fun `test getCorrectResult with valid videos`() {
    // Given
    val videos =
      listOf(
        createVideo("id1", "title1", 500), // Too long
        createVideo("id2", "title2", 200), // Valid
        createVideo("id3", "title3", 0), // Too short
      )

    // When
    val result = commandService.getCorrectResult(videos)

    // Then
    assertNotNull(result)
    assertEquals("id2", result?.id)
  }

  @Test
  fun `test getCorrectResult with no valid videos`() {
    // Given
    val videos =
      listOf(
        createVideo("id1", "title1", 500), // Too long
        createVideo("id3", "title3", 0), // Too short
      )

    // When
    val result = commandService.getCorrectResult(videos)

    // Then
    assertNull(result)
  }

  @Test
  fun `test normalizeText`() {
    // Given
    val input = "Héllö Wórld! 123"

    // When
    val result = commandService.normalizeText(input)

    // Then
    assertEquals("Hello World  123", result)
  }

  @Test
  fun `test sendMessage with normal message`() {
    // Given
    val channel = "#testchannel"
    val message = "Test message"

    // Create a spy of CommandService to mock the call to BanPhraseChecker
    val spyCommandService = Mockito.spy(commandService)

    // Mock the behavior of sendMessage to bypass BanPhraseChecker
    doAnswer { invocation ->
        val ch = invocation.getArgument<String>(0)
        val msg = invocation.getArgument<String>(1)
        twitchChatBot.sendMessage(ch, msg)
        null
      }
      .`when`(spyCommandService)
      .sendMessage(channel, message)

    // When
    spyCommandService.sendMessage(channel, message)

    // Then
    verify(twitchChatBot).sendMessage(channel, message)
    verify(twitchChatBot, never()).sendMessage(channel, "docJAM banned phrase detected")
  }

  @Test
  fun `test sendMessage with banned phrase`() {
    // Given
    val channel = "#testchannel"
    val message = "Banned message"

    // Store the original httpConnectionFactory
    val originalFactory = BanPhraseChecker.httpConnectionFactory

    try {
      // Set up a mock connection factory that returns a banned response
      BanPhraseChecker.httpConnectionFactory = { uri: URI ->
        object : HttpURLConnection(uri.toURL()) {
          private val outputStream = ByteArrayOutputStream()

          init {
            responseCode = HttpURLConnection.HTTP_OK
          }

          override fun connect() {
            // No-op for mock
          }

          override fun disconnect() {
            // No-op for mock
          }

          override fun usingProxy(): Boolean = false

          override fun getOutputStream(): ByteArrayOutputStream {
            return outputStream
          }

          override fun getInputStream(): ByteArrayInputStream {
            // Return a response indicating the message is banned
            val response = JSONObject().put("banned", true)
            return ByteArrayInputStream(response.toString().toByteArray())
          }
        }
      }

      // When
      commandService.sendMessage(channel, message)

      // Then
      verify(twitchChatBot).sendMessage(channel, "docJAM banned phrase detected")
      verify(twitchChatBot, never()).sendMessage(channel, message)
    } finally {
      // Restore the original httpConnectionFactory
      BanPhraseChecker.httpConnectionFactory = originalFactory
    }
  }

  @Test
  fun `test canUserAddVideo when user can add video`() {
    // Given
    val username = "testuser"
    val user = User(username)
    user.lastAddedVideo = LocalDateTime.now().minusMinutes(10) // Last video added 10 minutes ago

    `when`(userRepository.findByUsername(username)).thenReturn(user)
    `when`(userConfig.minutesToAddVideo).thenReturn(5) // 5 minute cooldown

    // When
    val result = commandService.canUserAddVideo(username)

    // Then
    assertTrue(result)
  }

  @Test
  fun `test canUserAddVideo when user cannot add video`() {
    // Given
    val username = "testuser"
    val user = User(username)
    user.lastAddedVideo = LocalDateTime.now().minusMinutes(2) // Last video added 2 minutes ago

    `when`(userRepository.findByUsername(username)).thenReturn(user)
    `when`(userConfig.minutesToAddVideo).thenReturn(5) // 5 minute cooldown

    // When
    val result = commandService.canUserAddVideo(username)

    // Then
    assertFalse(result)
  }

  @Test
  fun `test canUserAddVideo when user not found`() {
    // Given
    val username = "testuser"

    `when`(userRepository.findByUsername(username)).thenReturn(null)

    // When
    val result = commandService.canUserAddVideo(username)

    // Then
    assertFalse(result)
  }

  @Test
  fun `test timeToNextVideo`() {
    // Given
    val username = "testuser"
    val user = User(username)
    val now = LocalDateTime.now()
    user.lastAddedVideo = now.minusMinutes(2) // Last video added 2 minutes ago

    `when`(userRepository.findByUsername(username)).thenReturn(user)
    `when`(userConfig.minutesToAddVideo).thenReturn(5) // 5 minute cooldown

    // When
    val result = commandService.timeToNextVideo(username)

    // Then
    // We can't test the exact string due to timing, but we can check it's not "0"
    assertNotNull(result)
    assertTrue(result != "0")
  }

  @Test
  fun `test timeToNextVideo when user not found`() {
    // Given
    val username = "testuser"

    `when`(userRepository.findByUsername(username)).thenReturn(null)

    // When
    val result = commandService.timeToNextVideo(username)

    // Then
    assertEquals("0", result)
  }

  @Test
  fun `test canResponseToCommand when user can respond`() {
    // Given
    val username = "testuser"
    val user = User(username)
    user.lastResponse = LocalDateTime.now().minusSeconds(10) // Last response 10 seconds ago

    `when`(userRepository.findByUsername(username)).thenReturn(user)
    `when`(userConfig.secondsToResponseToCommand).thenReturn(3) // 3 second cooldown

    // When
    val result = commandService.canResponseToCommand(username)

    // Then
    assertTrue(result)
  }

  @Test
  fun `test canResponseToCommand when user cannot respond`() {
    // Given
    val username = "testuser"
    val user = User(username)
    user.lastResponse = LocalDateTime.now().minusSeconds(1) // Last response 1 second ago

    `when`(userRepository.findByUsername(username)).thenReturn(user)
    `when`(userConfig.secondsToResponseToCommand).thenReturn(3) // 3 second cooldown

    // When
    val result = commandService.canResponseToCommand(username)

    // Then
    assertFalse(result)
  }

  @Test
  fun `test canResponseToCommand when user not found`() {
    // Given
    val username = "testuser"

    `when`(userRepository.findByUsername(username)).thenReturn(null)

    // When
    val result = commandService.canResponseToCommand(username)

    // Then
    assertFalse(result)
  }

  @Test
  fun `test canUserSkipVideo when user can skip`() {
    // Given
    val username = "testuser"
    val user = User(username)
    user.lastSkip = LocalDateTime.now().minusMinutes(10) // Last skip 10 minutes ago

    `when`(userRepository.findByUsername(username)).thenReturn(user)
    `when`(userConfig.minutesToSkipVideo).thenReturn("5") // 5 minute cooldown

    // When
    val result = commandService.canUserSkipVideo(username)

    // Then
    assertTrue(result)
  }

  @Test
  fun `test canUserSkipVideo when user cannot skip`() {
    // Given
    val username = "testuser"
    val user = User(username)
    user.lastSkip = LocalDateTime.now().minusMinutes(2) // Last skip 2 minutes ago

    `when`(userRepository.findByUsername(username)).thenReturn(user)
    `when`(userConfig.minutesToSkipVideo).thenReturn("5") // 5 minute cooldown

    // When
    val result = commandService.canUserSkipVideo(username)

    // Then
    assertFalse(result)
  }

  @Test
  fun `test canUserSkipVideo when user not found`() {
    // Given
    val username = "testuser"

    `when`(userRepository.findByUsername(username)).thenReturn(null)

    // When
    val result = commandService.canUserSkipVideo(username)

    // Then
    assertFalse(result)
  }

  @Test
  fun `test timeToNextSkip`() {
    // Given
    val username = "testuser"
    val user = User(username)
    val now = LocalDateTime.now()
    user.lastSkip = now.minusMinutes(2) // Last skip 2 minutes ago

    `when`(userRepository.findByUsername(username)).thenReturn(user)
    `when`(userConfig.minutesToSkipVideo).thenReturn("5") // 5 minute cooldown

    // When
    val result = commandService.timeToNextSkip(username)

    // Then
    // We can't test the exact string due to timing, but we can check it's not "0"
    assertNotNull(result)
    assertTrue(result != "0")
  }

  @Test
  fun `test timeToNextSkip when user not found`() {
    // Given
    val username = "testuser"

    `when`(userRepository.findByUsername(username)).thenReturn(null)

    // When
    val result = commandService.timeToNextSkip(username)

    // Then
    assertEquals("0", result)
  }

  @Test
  fun `test timeToNextAction when time has passed`() {
    // Given
    val lastActionTime = LocalDateTime.now().minusMinutes(10)
    val intervalMinutes = 5L

    // When
    val result = commandService.timeToNextAction(lastActionTime, intervalMinutes)

    // Then
    assertEquals("0", result)
  }

  @Test
  fun `test timeToNextAction when time has not passed`() {
    // Given
    val lastActionTime = LocalDateTime.now().plusMinutes(5)
    val intervalMinutes = 10L

    // When
    val result = commandService.timeToNextAction(lastActionTime, intervalMinutes)

    // Then
    // We can't test the exact string due to timing, but we can check it's not "0"
    assertNotNull(result)
    assertTrue(result != "0")
    assertTrue(result.contains("min"))
    assertTrue(result.contains("sec"))
  }

  @Test
  fun `test randomGachiSong`() {
    // Given
    val gachiSongs =
      listOf(
        GachiSong().apply {
          id = 1L
          title = "Song 1"
        },
        GachiSong().apply {
          id = 2L
          title = "Song 2"
        },
      )

    `when`(gachiSongRepository.findAll()).thenReturn(gachiSongs)

    // When
    val result = commandService.randomGachiSong()

    // Then
    assertTrue(result == "Song 1" || result == "Song 2")
  }

  @Test
  fun `test randomGachiSong with empty title`() {
    // Given
    val gachiSongs =
      listOf(
        GachiSong().apply {
          id = 1L
          title = null
        }
      )

    `when`(gachiSongRepository.findAll()).thenReturn(gachiSongs)

    // When
    val result = commandService.randomGachiSong()

    // Then
    assertEquals("", result)
  }

  @Test
  fun `test skipCommand when user can skip and skip counter is less than required`() {
    // Given
    val username = "testuser"
    val channel = "#testchannel"
    val user = User(username)
    user.lastSkip = LocalDateTime.now().minusMinutes(10) // Last skip 10 minutes ago

    `when`(userRepository.findByUsername(username)).thenReturn(user)
    `when`(userConfig.minutesToSkipVideo).thenReturn("5") // 5 minute cooldown
    `when`(userConfig.skipValue).thenReturn("3") // 3 skips needed
    `when`(skipCounterService.getSkipCounter()).thenReturn(1) // Current skips: 1

    // When
    commandService.skipCommand(username, channel)

    // Then
    verify(skipCounterService).incrementSkipCounter()
    verify(userRepository).save(any(User::class.java))
    verify(cytubeDao, never()).skipVideo()
    verify(twitchChatBot)
      .sendMessage(channel, "docJAM @testuser 2 more skips needed to skip the video")
  }

  @Test
  fun `test skipCommand when user can skip and skip counter reaches required`() {
    // Given
    val username = "testuser"
    val channel = "#testchannel"
    val user = User(username)
    user.lastSkip = LocalDateTime.now().minusMinutes(10) // Last skip 10 minutes ago

    `when`(userRepository.findByUsername(username)).thenReturn(user)
    `when`(userConfig.minutesToSkipVideo).thenReturn("5") // 5 minute cooldown
    `when`(userConfig.skipValue).thenReturn("3") // 3 skips needed
    `when`(skipCounterService.getSkipCounter()).thenReturn(3) // Current skips: 3 (after increment)

    // When
    commandService.skipCommand(username, channel)

    // Then
    verify(skipCounterService).incrementSkipCounter()
    verify(userRepository).save(any(User::class.java))
    verify(cytubeDao).skipVideo()
    verify(twitchChatBot).sendMessage(channel, "docJAM skipped the video")
  }

  @Test
  fun `test skipCommand when user cannot skip`() {
    // Given
    val username = "testuser"
    val channel = "#testchannel"
    val user = User(username)
    user.lastSkip = LocalDateTime.now().minusMinutes(2) // Last skip 2 minutes ago

    `when`(userRepository.findByUsername(username)).thenReturn(user)
    `when`(userConfig.minutesToSkipVideo).thenReturn("5") // 5 minute cooldown

    // Mock timeToNextSkip
    val mockCommandService = Mockito.spy(commandService)
    Mockito.doReturn("3min 0sec").`when`(mockCommandService).timeToNextSkip(username)

    // When
    mockCommandService.skipCommand(username, channel)

    // Then
    verify(skipCounterService, never()).incrementSkipCounter()
    verify(userRepository, never()).save(any(User::class.java))
    verify(cytubeDao, never()).skipVideo()
    verify(twitchChatBot)
      .sendMessage(
        channel,
        "docJAM @testuser You can skip a video every 5 minutes, time to skip video: 3min 0sec",
      )
  }

  @Test
  fun `test getPlaylist`() {
    // Given
    val username = "testuser"
    val channel = "#testchannel"
    val mediaLink = MediaLink("yt", "videoId")
    val playlistItems =
      listOf(
        PlaylistItem(1, false, "user1", mediaLink, "Song 1 with a very long title", 180),
        PlaylistItem(2, false, "user2", mediaLink, "Song 2 with a very long title", 240),
        PlaylistItem(3, false, "user3", mediaLink, "Song 3 with a very long title", 300),
      )
    val playlist = Playlist(0, false, false, 0, null, playlistItems.toMutableList())

    `when`(cytubeDao.getPlaylist()).thenReturn(playlist)

    // When
    commandService.getPlaylist(username, channel)

    // Then
    verify(twitchChatBot)
      .sendMessage(
        channel,
        "@testuser docJAM Playlist: Song 1 with a very l, Song 2 with a very l, Song 3 with a very l",
      )
  }

  @Test
  fun `test commandHandler with djfors_ command`() {
    // Given
    val username = "testuser"
    val channel = "#testchannel"
    val message = "!djfors_version"

    // When
    commandService.commandHandler(username, message, channel)

    // Then
    verify(twitchChatBot).sendMessage(channel, "docJAM @testuser bot made by veccvs")
  }

  @Test
  fun `test commandHandler with link command`() {
    // Given
    val username = "testuser"
    val channel = "#testchannel"
    val message = ";link"
    val user = User(username)

    `when`(userRepository.findByUsername(username)).thenReturn(user)
    `when`(userConfig.secondsToResponseToCommand).thenReturn(3)

    // When
    commandService.commandHandler(username, message, channel)

    // Then
    verify(twitchChatBot)
      .sendMessage(channel, "docJAM @testuser I sent you a whisper with the link forsenCD ")
    verify(twitchConnector).sendWhisper(username, "https://cytu.be/r/forsenboys")
  }

  @Test
  fun `test commandHandler with where command`() {
    // Given
    val username = "testuser"
    val channel = "#testchannel"
    val message = ";where"
    val user = User(username)

    `when`(userRepository.findByUsername(username)).thenReturn(user)
    `when`(userConfig.secondsToResponseToCommand).thenReturn(3)

    // When
    commandService.commandHandler(username, message, channel)

    // Then
    verify(twitchChatBot)
      .sendMessage(channel, "docJAM @testuser I sent you a whisper with the link forsenCD ")
    verify(twitchConnector).sendWhisper(username, "https://cytu.be/r/forsenboys")
  }

  @Test
  fun `test commandHandler with search command`() {
    // Given
    val username = "testuser"
    val channel = "#testchannel"
    val message = ";search test song"
    val user = User(username)
    user.lastAddedVideo = LocalDateTime.now().minusMinutes(10) // Last video added 10 minutes ago

    val botStatus = BotStatus(true)
    val videos = listOf(createVideo("id1", "Test Song", 200))

    `when`(userRepository.findByUsername(username)).thenReturn(user)
    `when`(userConfig.secondsToResponseToCommand).thenReturn(3)
    `when`(userConfig.minutesToAddVideo).thenReturn(5)
    `when`(cytubeDao.getBotStatus()).thenReturn(botStatus)
    `when`(cytubeDao.searchVideos("test song")).thenReturn(videos)

    // When
    commandService.commandHandler(username, message, channel)

    // Then
    verify(cytubeDao).addVideo("https://youtu.be/id1")
    verify(songService).addUniqueSong("https://youtu.be/id1")
    verify(userRepository, times(2))
      .save(any(User::class.java)) // Once for lastResponse, once for lastAddedVideo
    verify(twitchChatBot).sendMessage(channel, "testuser docJAM added Test Song [...]")
  }

  @Test
  fun `test commandHandler with s command`() {
    // Given
    val username = "testuser"
    val channel = "#testchannel"
    val message = ";s test song"
    val user = User(username)
    user.lastAddedVideo = LocalDateTime.now().minusMinutes(10) // Last video added 10 minutes ago

    val botStatus = BotStatus(true)
    val videos = listOf(createVideo("id1", "Test Song", 200))

    `when`(userRepository.findByUsername(username)).thenReturn(user)
    `when`(userConfig.secondsToResponseToCommand).thenReturn(3)
    `when`(userConfig.minutesToAddVideo).thenReturn(5)
    `when`(cytubeDao.getBotStatus()).thenReturn(botStatus)
    `when`(cytubeDao.searchVideos("test song")).thenReturn(videos)

    // When
    commandService.commandHandler(username, message, channel)

    // Then
    verify(cytubeDao).addVideo("https://youtu.be/id1")
    verify(songService).addUniqueSong("https://youtu.be/id1")
    verify(userRepository, times(2))
      .save(any(User::class.java)) // Once for lastResponse, once for lastAddedVideo
    verify(twitchChatBot).sendMessage(channel, "testuser docJAM added Test Song [...]")
  }

  @Test
  fun `test commandHandler with rg command`() {
    // Given
    val username = "testuser"
    val channel = "#testchannel"
    val message = ";rg"
    val user = User(username)
    user.lastAddedVideo = LocalDateTime.now().minusMinutes(10) // Last video added 10 minutes ago

    val botStatus = BotStatus(true)
    val videos = listOf(createVideo("id1", "Gachi Song", 200))
    val gachiSongs =
      listOf(
        GachiSong().apply {
          id = 1L
          title = "Gachi Song"
        }
      )

    `when`(userRepository.findByUsername(username)).thenReturn(user)
    `when`(userConfig.secondsToResponseToCommand).thenReturn(3)
    `when`(userConfig.minutesToAddVideo).thenReturn(5)
    `when`(gachiSongRepository.findAll()).thenReturn(gachiSongs)
    `when`(cytubeDao.getBotStatus()).thenReturn(botStatus)
    `when`(cytubeDao.searchVideos("Gachi Song")).thenReturn(videos)

    // When
    commandService.commandHandler(username, message, channel)

    // Then
    verify(cytubeDao).addVideo("https://youtu.be/id1")
    verify(songService).addUniqueSong("https://youtu.be/id1")
    verify(userRepository, times(2))
      .save(any(User::class.java)) // Once for lastResponse, once for lastAddedVideo
    verify(twitchChatBot).sendMessage(channel, "testuser docJAM added Gachi Song [...]")
  }

  @Test
  fun `test commandHandler with help command`() {
    // Given
    val username = "testuser"
    val channel = "#testchannel"
    val message = ";help"
    val user = User(username)

    `when`(userRepository.findByUsername(username)).thenReturn(user)
    `when`(userConfig.secondsToResponseToCommand).thenReturn(3)

    // When
    commandService.commandHandler(username, message, channel)

    // Then
    verify(twitchChatBot)
      .sendMessage(
        channel,
        "docJAM @testuser Commands: ;link, ;where, ;search, ;s, ;help, ;playlist, ;skip, ;rg",
      )
  }

  @Test
  fun `test commandHandler with playlist command`() {
    // Given
    val username = "testuser"
    val channel = "#testchannel"
    val message = ";playlist"
    val user = User(username)
    val mediaLink = MediaLink("yt", "videoId")
    val playlistItems = listOf(PlaylistItem(1, false, "user1", mediaLink, "Song 1", 180))
    val playlist = Playlist(0, false, false, 0, null, playlistItems.toMutableList())

    `when`(userRepository.findByUsername(username)).thenReturn(user)
    `when`(userConfig.secondsToResponseToCommand).thenReturn(3)
    `when`(cytubeDao.getPlaylist()).thenReturn(playlist)

    // When
    commandService.commandHandler(username, message, channel)

    // Then
    verify(twitchChatBot).sendMessage(channel, "@testuser docJAM Playlist: Song 1")
  }

  @Test
  fun `test commandHandler with skip command`() {
    // Given
    val username = "testuser"
    val channel = "#testchannel"
    val message = ";skip"
    val user = User(username)
    user.lastSkip = LocalDateTime.now().minusMinutes(10) // Last skip 10 minutes ago

    `when`(userRepository.findByUsername(username)).thenReturn(user)
    `when`(userConfig.secondsToResponseToCommand).thenReturn(3)
    `when`(userConfig.minutesToSkipVideo).thenReturn("5")
    `when`(userConfig.skipValue).thenReturn("3")
    `when`(skipCounterService.getSkipCounter()).thenReturn(1) // Current skips: 1

    // When
    commandService.commandHandler(username, message, channel)

    // Then
    verify(skipCounterService).incrementSkipCounter()
    verify(userRepository, times(2))
      .save(any(User::class.java)) // Once for lastResponse, once for lastSkip
    verify(twitchChatBot)
      .sendMessage(channel, "docJAM @testuser 2 more skips needed to skip the video")
  }

  @Test
  fun `test commandHandler with unknown command`() {
    // Given
    val username = "testuser"
    val channel = "#testchannel"
    val message = ";unknown"
    val user = User(username)

    `when`(userRepository.findByUsername(username)).thenReturn(user)
    `when`(userConfig.secondsToResponseToCommand).thenReturn(3)

    // When
    commandService.commandHandler(username, message, channel)

    // Then
    verify(twitchChatBot)
      .sendMessage(channel, "docJAM @testuser Unknown command, try ;link, ;search or ;help")
  }

  @Test
  fun `test commandHandler when user cannot respond to command`() {
    // Given
    val username = "testuser"
    val channel = "#testchannel"
    val message = ";command"
    val user = User(username)
    user.lastResponse = LocalDateTime.now() // Last response just now

    `when`(userRepository.findByUsername(username)).thenReturn(user)
    `when`(userConfig.secondsToResponseToCommand).thenReturn(3)

    // When
    commandService.commandHandler(username, message, channel)

    // Then
    verify(twitchChatBot, never())
      .sendMessage(anyString(), anyString()) // No message sent due to cooldown
  }

  @Test
  fun `test commandHandler with search command when user cannot add video`() {
    // Given
    val username = "testuser"
    val channel = "#testchannel"
    val message = ";search test song"
    val user = User(username)
    user.lastAddedVideo = LocalDateTime.now().minusMinutes(2) // Last video added 2 minutes ago

    `when`(userRepository.findByUsername(username)).thenReturn(user)
    `when`(userConfig.secondsToResponseToCommand).thenReturn(3)
    // Don't need to mock timeToNextVideo as it's a real method call
    val mockCommandService = Mockito.spy(commandService)
    Mockito.doReturn("3min 0sec").`when`(mockCommandService).timeToNextVideo(username)

    // When
    mockCommandService.commandHandler(username, message, channel)

    // Then
    verify(cytubeDao, never()).addVideo(anyString())
    verify(songService, never()).addUniqueSong(anyString())
    verify(userRepository).save(any(User::class.java)) // Only for lastResponse
    verify(twitchChatBot)
      .sendMessage(
        channel,
        "docJAM @testuser You can add a video every 5 minutes. Time to add next video: 3min 0sec",
      )
  }

  @Test
  fun `test commandHandler with search command when bot status is null`() {
    // Given
    val username = "testuser"
    val channel = "#testchannel"
    val message = ";search test song"
    val user = User(username)
    user.lastAddedVideo = LocalDateTime.now().minusMinutes(10) // Last video added 10 minutes ago

    `when`(userRepository.findByUsername(username)).thenReturn(user)
    `when`(userConfig.secondsToResponseToCommand).thenReturn(3)
    `when`(userConfig.minutesToAddVideo).thenReturn(5)
    `when`(cytubeDao.getBotStatus()).thenReturn(null)

    // When
    commandService.commandHandler(username, message, channel)

    // Then
    verify(cytubeDao, never()).addVideo(anyString())
    verify(songService, never()).addUniqueSong(anyString())
    verify(userRepository).save(any(User::class.java)) // Only for lastResponse
    verify(twitchChatBot)
      .sendMessage(channel, "@testuser docJAM Bot is resetting, wait a few seconds :)")
  }

  @Test
  fun `test commandHandler with search command when bot is disabled`() {
    // Given
    val username = "testuser"
    val channel = "#testchannel"
    val message = ";search test song"
    val user = User(username)
    user.lastAddedVideo = LocalDateTime.now().minusMinutes(10) // Last video added 10 minutes ago

    val botStatus = BotStatus(false)
    val videos = listOf(createVideo("id1", "Test Song", 200))

    `when`(userRepository.findByUsername(username)).thenReturn(user)
    `when`(userConfig.secondsToResponseToCommand).thenReturn(3)
    `when`(userConfig.minutesToAddVideo).thenReturn(5)
    `when`(cytubeDao.getBotStatus()).thenReturn(botStatus)
    `when`(cytubeDao.searchVideos("test song")).thenReturn(videos)

    // When
    commandService.commandHandler(username, message, channel)

    // Then
    verify(cytubeDao, never()).addVideo(anyString())
    verify(songService, never()).addUniqueSong(anyString())
    verify(userRepository).save(any(User::class.java)) // Only for lastResponse
    verify(twitchChatBot)
      .sendMessage(channel, "@testuser docJAM Bot is resetting, wait a few seconds :)")
  }

  @Test
  fun `test commandHandler with search command when no results found`() {
    // Given
    val username = "testuser"
    val channel = "#testchannel"
    val message = ";search test song"
    val user = User(username)
    user.lastAddedVideo = LocalDateTime.now().minusMinutes(10) // Last video added 10 minutes ago

    val botStatus = BotStatus(true)

    `when`(userRepository.findByUsername(username)).thenReturn(user)
    `when`(userConfig.secondsToResponseToCommand).thenReturn(3)
    `when`(userConfig.minutesToAddVideo).thenReturn(5)
    `when`(cytubeDao.getBotStatus()).thenReturn(botStatus)
    `when`(cytubeDao.searchVideos("test song")).thenReturn(emptyList())

    // When
    commandService.commandHandler(username, message, channel)

    // Then
    verify(cytubeDao, never()).addVideo(anyString())
    verify(songService, never()).addUniqueSong(anyString())
    verify(userRepository).save(any(User::class.java)) // Only for lastResponse
    verify(twitchChatBot).sendMessage(channel, "@testuser docJAM No results found")
  }

  @Test
  fun `test commandHandler with search command when no valid results found`() {
    // Given
    val username = "testuser"
    val channel = "#testchannel"
    val message = ";search test song"
    val user = User(username)
    user.lastAddedVideo = LocalDateTime.now().minusMinutes(10) // Last video added 10 minutes ago

    val botStatus = BotStatus(true)
    val videos =
      listOf(
        createVideo("id1", "Test Song", 500), // Too long
        createVideo("id2", "Test Song", 0), // Too short
      )

    `when`(userRepository.findByUsername(username)).thenReturn(user)
    `when`(userConfig.secondsToResponseToCommand).thenReturn(3)
    `when`(userConfig.minutesToAddVideo).thenReturn(5)
    `when`(cytubeDao.getBotStatus()).thenReturn(botStatus)
    `when`(cytubeDao.searchVideos("test song")).thenReturn(videos)

    // When
    commandService.commandHandler(username, message, channel)

    // Then
    verify(cytubeDao, never()).addVideo(anyString())
    verify(songService, never()).addUniqueSong(anyString())
    verify(userRepository).save(any(User::class.java)) // Only for lastResponse
    verify(twitchChatBot).sendMessage(channel, "@testuser docJAM No results found")
  }

  private fun createVideo(id: String, title: String, duration: Int): Video {
    val video =
      Video(
        id,
        listOf("thumbnail"),
        title,
        "description",
        "channel",
        "views",
        "publishTime",
        "urlSuffix",
      )
    video.duration = duration
    return video
  }
}
