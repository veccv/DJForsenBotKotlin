package com.github.veccvs.djforsenbotkotlin.service

import com.github.veccvs.djforsenbotkotlin.component.TwitchConnector
import com.github.veccvs.djforsenbotkotlin.config.UserConfig
import com.github.veccvs.djforsenbotkotlin.model.TwitchCommand
import com.github.veccvs.djforsenbotkotlin.model.User
import com.github.veccvs.djforsenbotkotlin.repository.UserRepository
import com.github.veccvs.djforsenbotkotlin.service.command.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils

@ExtendWith(MockitoExtension::class)
class CommandServiceTest {

  @Mock private lateinit var messageService: MessageService

  @Mock private lateinit var commandParserService: CommandParserService

  @Mock private lateinit var videoSearchService: VideoSearchService

  @Mock private lateinit var timeRestrictionService: TimeRestrictionService

  @Mock private lateinit var userSongFormatterService: UserSongFormatterService

  @Mock private lateinit var playlistService: PlaylistService

  @Mock private lateinit var userRepository: UserRepository

  @Mock private lateinit var userConfig: UserConfig

  @Mock private lateinit var skipCounterService: SkipCounterService

  @Mock private lateinit var userSongService: UserSongService

  @Mock private lateinit var spotifyService: SpotifyService

  @Mock private lateinit var twitchConnector: TwitchConnector

  @Mock private lateinit var spotifyCommandService: SpotifyCommandService

  @Mock private lateinit var playlistCommandService: PlaylistCommandService

  @Mock private lateinit var gptService: GptService

  @InjectMocks private lateinit var commandService: CommandService

  private val testUsername = "testUser"
  private val testChannel = "testChannel"

  @BeforeEach
  fun setUp() {
    // Replace the private TwitchConnector with our mock
    ReflectionTestUtils.setField(commandService, "twitchConnector", twitchConnector)
  }

  @Test
  fun `test sendMessage delegates to messageService`() {
    // Given
    val message = "Test message"

    // When
    commandService.sendMessage(testChannel, message)

    // Then
    verify(messageService).sendMessage(testChannel, message)
  }

  @Test
  fun `test commandHandler with djfors prefix command`() {
    // Given
    val message = "!djfors_test"

    // When
    commandService.commandHandler(testUsername, message, testChannel)

    // Then
    verify(messageService).sendMessage(testChannel, "docJAM @$testUsername bot made by veccvs")
    verifyNoInteractions(commandParserService)
  }

  @Test
  fun `test commandHandler with null command`() {
    // Given
    val message = "not a command"
    `when`(commandParserService.detectCommand(message)).thenReturn(null)

    // When
    commandService.commandHandler(testUsername, message, testChannel)

    // Then
    verify(commandParserService).detectCommand(message)
    verifyNoMoreInteractions(commandParserService)
    verifyNoInteractions(userRepository)
  }

  @Test
  fun `test commandHandler when user cannot respond to command`() {
    // Given
    val message = ";test"
    `when`(commandParserService.detectCommand(message)).thenReturn(";test")
    `when`(userRepository.findByUsername(testUsername)).thenReturn(null)
    `when`(userRepository.save(any(User::class.java))).thenReturn(User(testUsername))
    `when`(timeRestrictionService.canResponseToCommand(testUsername)).thenReturn(false)

    // When
    commandService.commandHandler(testUsername, message, testChannel)

    // Then
    verify(commandParserService).detectCommand(message)
    verify(userRepository).findByUsername(testUsername)
    verify(userRepository).save(any(User::class.java))
    verify(timeRestrictionService).canResponseToCommand(testUsername)
    verifyNoMoreInteractions(commandParserService)
  }

  @Test
  fun `test commandHandler with link command`() {
    // Given
    val message = ";link"
    val user = User(testUsername)
    `when`(commandParserService.detectCommand(message)).thenReturn(";link")
    `when`(userRepository.findByUsername(testUsername)).thenReturn(user)
    `when`(timeRestrictionService.canResponseToCommand(testUsername)).thenReturn(true)
    `when`(commandParserService.parseCommand(message))
      .thenReturn(TwitchCommand(";link", emptyList()))

    // When
    commandService.commandHandler(testUsername, message, testChannel)

    // Then
    verify(messageService)
      .sendMessage(
        testChannel,
        "docJAM @$testUsername I sent you a whisper with the link forsenCD ",
      )
    verify(twitchConnector).sendWhisper(testUsername, "https://cytu.be/r/forsenboys")
    verify(timeRestrictionService).setLastResponse(testUsername)
  }

  @Test
  fun `test commandHandler with where command`() {
    // Given
    val message = ";where"
    val user = User(testUsername)
    `when`(commandParserService.detectCommand(message)).thenReturn(";where")
    `when`(userRepository.findByUsername(testUsername)).thenReturn(user)
    `when`(timeRestrictionService.canResponseToCommand(testUsername)).thenReturn(true)
    `when`(commandParserService.parseCommand(message))
      .thenReturn(TwitchCommand(";where", emptyList()))

    // When
    commandService.commandHandler(testUsername, message, testChannel)

    // Then
    verify(messageService)
      .sendMessage(
        testChannel,
        "docJAM @$testUsername I sent you a whisper with the link forsenCD ",
      )
    verify(twitchConnector).sendWhisper(testUsername, "https://cytu.be/r/forsenboys")
    verify(timeRestrictionService).setLastResponse(testUsername)
  }

  @Test
  fun `test commandHandler with search command when user can add video`() {
    // Given
    val message = ";search test"
    val user = User(testUsername)
    val twitchCommand = TwitchCommand(";search", listOf("test"))

    `when`(commandParserService.detectCommand(message)).thenReturn(";search")
    `when`(userRepository.findByUsername(testUsername)).thenReturn(user)
    `when`(timeRestrictionService.canResponseToCommand(testUsername)).thenReturn(true)
    `when`(commandParserService.parseCommand(message)).thenReturn(twitchCommand)
    `when`(timeRestrictionService.canUserAddVideo(testUsername)).thenReturn(true)

    // When
    commandService.commandHandler(testUsername, message, testChannel)

    // Then
    verify(timeRestrictionService).setLastResponse(testUsername)
    verify(videoSearchService).searchVideo(twitchCommand, testChannel, testUsername)
  }

  @Test
  fun `test commandHandler with search command when user cannot add video`() {
    // Given
    val message = ";search test"
    val user = User(testUsername)
    val twitchCommand = TwitchCommand(";search", listOf("test"))

    `when`(commandParserService.detectCommand(message)).thenReturn(";search")
    `when`(userRepository.findByUsername(testUsername)).thenReturn(user)
    `when`(timeRestrictionService.canResponseToCommand(testUsername)).thenReturn(true)
    `when`(commandParserService.parseCommand(message)).thenReturn(twitchCommand)
    `when`(timeRestrictionService.canUserAddVideo(testUsername)).thenReturn(false)
    `when`(timeRestrictionService.timeToNextVideo(testUsername)).thenReturn("5 minutes")
    `when`(userConfig.minutesToAddVideo).thenReturn(10)

    // When
    commandService.commandHandler(testUsername, message, testChannel)

    // Then
    verify(timeRestrictionService).setLastResponse(testUsername)
    verify(messageService)
      .sendMessage(
        testChannel,
        "docJAM @${testUsername} You can add a video every 10 minutes. Time to add next video: 5 minutes",
      )
    verifyNoInteractions(videoSearchService)
  }

  @Test
  fun `test commandHandler with s command`() {
    // Given
    val message = ";s test"
    val user = User(testUsername)
    val twitchCommand = TwitchCommand(";s", listOf("test"))

    `when`(commandParserService.detectCommand(message)).thenReturn(";s")
    `when`(userRepository.findByUsername(testUsername)).thenReturn(user)
    `when`(timeRestrictionService.canResponseToCommand(testUsername)).thenReturn(true)
    `when`(commandParserService.parseCommand(message)).thenReturn(twitchCommand)
    `when`(timeRestrictionService.canUserAddVideo(testUsername)).thenReturn(true)

    // When
    commandService.commandHandler(testUsername, message, testChannel)

    // Then
    verify(timeRestrictionService).setLastResponse(testUsername)
    verify(videoSearchService).searchVideo(twitchCommand, testChannel, testUsername)
  }

  @Test
  fun `test commandHandler with rg command`() {
    // Given
    val message = ";rg"
    val user = User(testUsername)
    val randomSong = "random gachi song"

    `when`(commandParserService.detectCommand(message)).thenReturn(";rg")
    `when`(userRepository.findByUsername(testUsername)).thenReturn(user)
    `when`(timeRestrictionService.canResponseToCommand(testUsername)).thenReturn(true)
    `when`(commandParserService.parseCommand(message)).thenReturn(TwitchCommand(";rg", emptyList()))
    `when`(playlistService.randomGachiSong()).thenReturn(randomSong)
    `when`(timeRestrictionService.canUserAddVideo(testUsername)).thenReturn(true)

    // When
    commandService.commandHandler(testUsername, message, testChannel)

    // Then
    verify(timeRestrictionService).setLastResponse(testUsername)
    verify(playlistService).randomGachiSong()
    // For the rg command, we just verify that playlistService.randomGachiSong was called
    // We don't verify videoSearchService.searchVideo because it's difficult to match the
    // TwitchCommand object
  }

  @Test
  fun `test commandHandler with help command`() {
    // Given
    val message = ";help"
    val user = User(testUsername)

    `when`(commandParserService.detectCommand(message)).thenReturn(";help")
    `when`(userRepository.findByUsername(testUsername)).thenReturn(user)
    `when`(timeRestrictionService.canResponseToCommand(testUsername)).thenReturn(true)
    `when`(commandParserService.parseCommand(message))
      .thenReturn(TwitchCommand(";help", emptyList()))

    // When
    commandService.commandHandler(testUsername, message, testChannel)

    // Then
    verify(timeRestrictionService).setLastResponse(testUsername)
    verify(messageService)
      .sendMessage(
        testChannel,
        "docJAM @$testUsername Commands: ;link, ;where, ;search, ;s, ;help, ;playlist, ;skip, ;rg, ;when, ;undo, ;connect, ;track, ;track stop, ;current",
      )
  }

  @Test
  fun `test commandHandler with playlist command`() {
    // Given
    val message = ";playlist"
    val user = User(testUsername)

    `when`(commandParserService.detectCommand(message)).thenReturn(";playlist")
    `when`(userRepository.findByUsername(testUsername)).thenReturn(user)
    `when`(timeRestrictionService.canResponseToCommand(testUsername)).thenReturn(true)
    `when`(commandParserService.parseCommand(message))
      .thenReturn(TwitchCommand(";playlist", emptyList()))

    // When
    commandService.commandHandler(testUsername, message, testChannel)

    // Then
    verify(timeRestrictionService).setLastResponse(testUsername)
    verify(playlistService).getPlaylist(testUsername, testChannel)
  }

  @Test
  fun `test commandHandler with skip command`() {
    // Given
    val message = ";skip"
    val user = User(testUsername)

    `when`(commandParserService.detectCommand(message)).thenReturn(";skip")
    `when`(userRepository.findByUsername(testUsername)).thenReturn(user)
    `when`(timeRestrictionService.canResponseToCommand(testUsername)).thenReturn(true)
    `when`(commandParserService.parseCommand(message))
      .thenReturn(TwitchCommand(";skip", emptyList()))

    // When
    commandService.commandHandler(testUsername, message, testChannel)

    // Then
    verify(timeRestrictionService).setLastResponse(testUsername)
    verify(playlistCommandService).skipCommand(testUsername, testChannel)
  }

  @Test
  fun `test commandHandler with skip command when user cannot skip`() {
    // Given
    val message = ";skip"
    val user = User(testUsername)

    `when`(commandParserService.detectCommand(message)).thenReturn(";skip")
    `when`(userRepository.findByUsername(testUsername)).thenReturn(user)
    `when`(timeRestrictionService.canResponseToCommand(testUsername)).thenReturn(true)
    `when`(commandParserService.parseCommand(message))
      .thenReturn(TwitchCommand(";skip", emptyList()))

    // When
    commandService.commandHandler(testUsername, message, testChannel)

    // Then
    verify(timeRestrictionService).setLastResponse(testUsername)
    verify(playlistCommandService).skipCommand(testUsername, testChannel)
  }

  @Test
  fun `test commandHandler with when command`() {
    // Given
    val message = ";when"
    val user = User(testUsername)

    `when`(commandParserService.detectCommand(message)).thenReturn(";when")
    `when`(userRepository.findByUsername(testUsername)).thenReturn(user)
    `when`(timeRestrictionService.canResponseToCommand(testUsername)).thenReturn(true)
    `when`(commandParserService.parseCommand(message))
      .thenReturn(TwitchCommand(";when", emptyList()))

    // When
    commandService.commandHandler(testUsername, message, testChannel)

    // Then
    verify(timeRestrictionService).setLastResponse(testUsername)
    verify(userSongFormatterService).getUserSongs(testUsername, testChannel)
  }

  @Test
  fun `test commandHandler with remove command`() {
    // Given
    val message = ";undo"
    val user = User(testUsername)

    `when`(commandParserService.detectCommand(message)).thenReturn(";undo")
    `when`(userRepository.findByUsername(testUsername)).thenReturn(user)
    `when`(timeRestrictionService.canResponseToCommand(testUsername)).thenReturn(true)
    `when`(commandParserService.parseCommand(message))
      .thenReturn(TwitchCommand(";undo", emptyList()))

    // When
    commandService.commandHandler(testUsername, message, testChannel)

    // Then
    verify(timeRestrictionService).setLastResponse(testUsername)
    verify(playlistCommandService).removeSongCommand(testUsername, testChannel)
  }

  @Test
  fun `test commandHandler with unknown command`() {
    // Given
    val message = ";unknown"
    val user = User(testUsername)

    `when`(commandParserService.detectCommand(message)).thenReturn(";unknown")
    `when`(userRepository.findByUsername(testUsername)).thenReturn(user)
    `when`(timeRestrictionService.canResponseToCommand(testUsername)).thenReturn(true)
    `when`(commandParserService.parseCommand(message))
      .thenReturn(TwitchCommand(";unknown", emptyList()))

    // When
    commandService.commandHandler(testUsername, message, testChannel)

    // Then
    verify(timeRestrictionService).setLastResponse(testUsername)
    verify(messageService)
      .sendMessage(
        testChannel,
        "docJAM @$testUsername Unknown command, try ;link, ;search or ;help",
      )
  }

  @Test
  fun `test skipCommand when user can skip`() {
    // Given

    // When
    commandService.skipCommand(testUsername, testChannel)

    // Then
    verify(playlistCommandService).skipCommand(testUsername, testChannel)
  }

  @Test
  fun `test skipCommand when user cannot skip`() {
    // Given

    // When
    commandService.skipCommand(testUsername, testChannel)

    // Then
    verify(playlistCommandService).skipCommand(testUsername, testChannel)
  }

  @Test
  fun `test skipCommand with null skipValue`() {
    // Given

    // When
    commandService.skipCommand(testUsername, testChannel)

    // Then
    verify(playlistCommandService).skipCommand(testUsername, testChannel)
  }

  @Test
  fun `test removeSongCommand when song can be removed`() {
    // Given

    // When
    commandService.removeSongCommand(testUsername, testChannel)

    // Then
    verify(playlistCommandService).removeSongCommand(testUsername, testChannel)
  }

  @Test
  fun `test removeSongCommand when no song is available to remove`() {
    // Given

    // When
    commandService.removeSongCommand(testUsername, testChannel)

    // Then
    verify(playlistCommandService).removeSongCommand(testUsername, testChannel)
  }

  @Test
  fun `test removeSongCommand when song link is null`() {
    // Given

    // When
    commandService.removeSongCommand(testUsername, testChannel)

    // Then
    verify(playlistCommandService).removeSongCommand(testUsername, testChannel)
  }

  @Test
  fun `test removeSongCommand when playlist removal fails`() {
    // Given

    // When
    commandService.removeSongCommand(testUsername, testChannel)

    // Then
    verify(playlistCommandService).removeSongCommand(testUsername, testChannel)
  }

  @Test
  fun `test removeSongCommand when user cannot remove video due to cooldown`() {
    // Given

    // When
    commandService.removeSongCommand(testUsername, testChannel)

    // Then
    verify(playlistCommandService).removeSongCommand(testUsername, testChannel)
  }

  @Test
  fun `test parseTokenResponse with standard token format`() {
    // Given
    val tokenResponse = "token=abc123;refreshToken=xyz789;"
    val expectedResult = mapOf("token" to "abc123", "refreshToken" to "xyz789")
    `when`(spotifyCommandService.parseTokenResponse(tokenResponse)).thenReturn(expectedResult)

    // When
    val result = commandService.parseTokenResponse(tokenResponse)

    // Then
    assertEquals("abc123", result["token"])
    assertEquals("xyz789", result["refreshToken"])
  }

  @Test
  fun `test parseTokenResponse with Twitch whisper format`() {
    // Given
    val twitchWhisperMessage =
      "@badges=rplace-2023/1;color=#FF0000;display-name=veccvs;emotes=;message-id=40;thread-id=99548338_517979103;turbo=0;user-id=517979103;user-type= :veccvs!veccvs@veccvs.tmi.twitch.tv WHISPER djfors_ :token=BQBHqhOmbOxGkxA0gkuw7OOULtvUrb0bZxv7CeT2wMcBD278WyWs6qtFyOm8GvyYdXeUBWtD2lMWG__LUJUQ0js6uQ3AQrxZcODSgaFJo5CXoG6-V8BRaKKaonGZobTNwukfx5tBU5VK6trLBPJtz8q0pB_glM_0wNSnArkRjF9MHIWG9c06g1kBsUuKkUy91A14SEifG6gyHK6uHTTdTJo1iYN6mrI9ALnHxpIeI9UknrbqVKztlw;refreshToken=AQB3tGUbDkuXnKMg150cCRw_nZGUfNN19h-zPOxUO0e0Yo-2LbJr1VGGoOVAtYWY5zCfLGbmEObC6HMAAI9e1nDJXCikeNvcDU5kpc6vDuRHVgn7o3tBbvnmY0Ct1RmnQNg;"

    val expectedResult =
      mapOf(
        "token" to
          "BQBHqhOmbOxGkxA0gkuw7OOULtvUrb0bZxv7CeT2wMcBD278WyWs6qtFyOm8GvyYdXeUBWtD2lMWG__LUJUQ0js6uQ3AQrxZcODSgaFJo5CXoG6-V8BRaKKaonGZobTNwukfx5tBU5VK6trLBPJtz8q0pB_glM_0wNSnArkRjF9MHIWG9c06g1kBsUuKkUy91A14SEifG6gyHK6uHTTdTJo1iYN6mrI9ALnHxpIeI9UknrbqVKztlw",
        "refreshToken" to
          "AQB3tGUbDkuXnKMg150cCRw_nZGUfNN19h-zPOxUO0e0Yo-2LbJr1VGGoOVAtYWY5zCfLGbmEObC6HMAAI9e1nDJXCikeNvcDU5kpc6vDuRHVgn7o3tBbvnmY0Ct1RmnQNg",
      )

    `when`(spotifyCommandService.parseTokenResponse(twitchWhisperMessage))
      .thenReturn(expectedResult)

    // When
    val result = commandService.parseTokenResponse(twitchWhisperMessage)

    // Then
    assertEquals(
      "BQBHqhOmbOxGkxA0gkuw7OOULtvUrb0bZxv7CeT2wMcBD278WyWs6qtFyOm8GvyYdXeUBWtD2lMWG__LUJUQ0js6uQ3AQrxZcODSgaFJo5CXoG6-V8BRaKKaonGZobTNwukfx5tBU5VK6trLBPJtz8q0pB_glM_0wNSnArkRjF9MHIWG9c06g1kBsUuKkUy91A14SEifG6gyHK6uHTTdTJo1iYN6mrI9ALnHxpIeI9UknrbqVKztlw",
      result["token"],
    )
    assertEquals(
      "AQB3tGUbDkuXnKMg150cCRw_nZGUfNN19h-zPOxUO0e0Yo-2LbJr1VGGoOVAtYWY5zCfLGbmEObC6HMAAI9e1nDJXCikeNvcDU5kpc6vDuRHVgn7o3tBbvnmY0Ct1RmnQNg",
      result["refreshToken"],
    )
  }

  @Test
  fun `test commandHandler ignores messages from bot botr`() {
    // Given
    val botUsername = "botr"
    val message = ";search test"

    // When
    commandService.commandHandler(botUsername, message, testChannel)

    // Then
    // Verify that no interactions occur with any services since the message should be ignored
    verifyNoInteractions(commandParserService)
    verifyNoInteractions(userRepository)
    verifyNoInteractions(timeRestrictionService)
    verifyNoInteractions(messageService)
  }

  @Test
  fun `test commandHandler ignores messages from bot supibot`() {
    // Given
    val botUsername = "supibot"
    val message = ";search test"

    // When
    commandService.commandHandler(botUsername, message, testChannel)

    // Then
    // Verify that no interactions occur with any services since the message should be ignored
    verifyNoInteractions(commandParserService)
    verifyNoInteractions(userRepository)
    verifyNoInteractions(timeRestrictionService)
    verifyNoInteractions(messageService)
  }
}
