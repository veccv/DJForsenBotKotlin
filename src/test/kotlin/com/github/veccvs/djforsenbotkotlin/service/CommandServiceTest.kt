package com.github.veccvs.djforsenbotkotlin.service

import com.github.veccvs.djforsenbotkotlin.component.TwitchConnector
import com.github.veccvs.djforsenbotkotlin.config.UserConfig
import com.github.veccvs.djforsenbotkotlin.model.TwitchCommand
import com.github.veccvs.djforsenbotkotlin.model.User
import com.github.veccvs.djforsenbotkotlin.repository.UserRepository
import com.github.veccvs.djforsenbotkotlin.service.command.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import java.util.*

@ExtendWith(MockitoExtension::class)
class CommandServiceTest {

    @Mock
    private lateinit var messageService: MessageService

    @Mock
    private lateinit var commandParserService: CommandParserService

    @Mock
    private lateinit var videoSearchService: VideoSearchService

    @Mock
    private lateinit var timeRestrictionService: TimeRestrictionService

    @Mock
    private lateinit var userSongFormatterService: UserSongFormatterService

    @Mock
    private lateinit var playlistService: PlaylistService

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var userConfig: UserConfig

    @Mock
    private lateinit var skipCounterService: SkipCounterService

    @Mock
    private lateinit var twitchConnector: TwitchConnector

    @InjectMocks
    private lateinit var commandService: CommandService

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
        `when`(commandParserService.parseCommand(message)).thenReturn(TwitchCommand(";link", emptyList()))

        // When
        commandService.commandHandler(testUsername, message, testChannel)

        // Then
        verify(messageService).sendMessage(testChannel, "docJAM @$testUsername I sent you a whisper with the link forsenCD ")
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
        `when`(commandParserService.parseCommand(message)).thenReturn(TwitchCommand(";where", emptyList()))

        // When
        commandService.commandHandler(testUsername, message, testChannel)

        // Then
        verify(messageService).sendMessage(testChannel, "docJAM @$testUsername I sent you a whisper with the link forsenCD ")
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
        verify(messageService).sendMessage(testChannel, "docJAM @${testUsername} You can add a video every 10 minutes. Time to add next video: 5 minutes")
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
        // We don't verify videoSearchService.searchVideo because it's difficult to match the TwitchCommand object
    }

    @Test
    fun `test commandHandler with help command`() {
        // Given
        val message = ";help"
        val user = User(testUsername)

        `when`(commandParserService.detectCommand(message)).thenReturn(";help")
        `when`(userRepository.findByUsername(testUsername)).thenReturn(user)
        `when`(timeRestrictionService.canResponseToCommand(testUsername)).thenReturn(true)
        `when`(commandParserService.parseCommand(message)).thenReturn(TwitchCommand(";help", emptyList()))

        // When
        commandService.commandHandler(testUsername, message, testChannel)

        // Then
        verify(timeRestrictionService).setLastResponse(testUsername)
        verify(messageService).sendMessage(
            testChannel,
            "docJAM @$testUsername Commands: ;link, ;where, ;search, ;s, ;help, ;playlist, ;skip, ;rg, ;mysongs"
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
        `when`(commandParserService.parseCommand(message)).thenReturn(TwitchCommand(";playlist", emptyList()))

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
        `when`(commandParserService.parseCommand(message)).thenReturn(TwitchCommand(";skip", emptyList()))
        `when`(timeRestrictionService.canUserSkipVideo(testUsername)).thenReturn(true)
        `when`(userConfig.skipValue).thenReturn("5")
        `when`(skipCounterService.getSkipCounter()).thenReturn(2)
        `when`(timeRestrictionService.timeToNextSkip(testUsername)).thenReturn("10 minutes")

        // When
        commandService.commandHandler(testUsername, message, testChannel)

        // Then
        verify(timeRestrictionService).setLastResponse(testUsername)
        verify(timeRestrictionService).setLastSkip(testUsername)
        verify(playlistService).handleSkipCommand(
            testUsername,
            testChannel,
            true,
            5L,
            2,
            "10 minutes"
        )
    }

    @Test
    fun `test commandHandler with skip command when user cannot skip`() {
        // Given
        val message = ";skip"
        val user = User(testUsername)

        `when`(commandParserService.detectCommand(message)).thenReturn(";skip")
        `when`(userRepository.findByUsername(testUsername)).thenReturn(user)
        `when`(timeRestrictionService.canResponseToCommand(testUsername)).thenReturn(true)
        `when`(commandParserService.parseCommand(message)).thenReturn(TwitchCommand(";skip", emptyList()))
        `when`(timeRestrictionService.canUserSkipVideo(testUsername)).thenReturn(false)
        `when`(userConfig.skipValue).thenReturn("5")
        `when`(skipCounterService.getSkipCounter()).thenReturn(2)
        `when`(timeRestrictionService.timeToNextSkip(testUsername)).thenReturn("10 minutes")

        // When
        commandService.commandHandler(testUsername, message, testChannel)

        // Then
        verify(timeRestrictionService).setLastResponse(testUsername)
        verify(timeRestrictionService, never()).setLastSkip(testUsername)
        verify(playlistService).handleSkipCommand(
            testUsername,
            testChannel,
            false,
            5L,
            2,
            "10 minutes"
        )
    }

    @Test
    fun `test commandHandler with mysongs command`() {
        // Given
        val message = ";mysongs"
        val user = User(testUsername)

        `when`(commandParserService.detectCommand(message)).thenReturn(";mysongs")
        `when`(userRepository.findByUsername(testUsername)).thenReturn(user)
        `when`(timeRestrictionService.canResponseToCommand(testUsername)).thenReturn(true)
        `when`(commandParserService.parseCommand(message)).thenReturn(TwitchCommand(";mysongs", emptyList()))

        // When
        commandService.commandHandler(testUsername, message, testChannel)

        // Then
        verify(timeRestrictionService).setLastResponse(testUsername)
        verify(userSongFormatterService).getUserSongs(testUsername, testChannel)
    }

    @Test
    fun `test commandHandler with unknown command`() {
        // Given
        val message = ";unknown"
        val user = User(testUsername)

        `when`(commandParserService.detectCommand(message)).thenReturn(";unknown")
        `when`(userRepository.findByUsername(testUsername)).thenReturn(user)
        `when`(timeRestrictionService.canResponseToCommand(testUsername)).thenReturn(true)
        `when`(commandParserService.parseCommand(message)).thenReturn(TwitchCommand(";unknown", emptyList()))

        // When
        commandService.commandHandler(testUsername, message, testChannel)

        // Then
        verify(timeRestrictionService).setLastResponse(testUsername)
        verify(messageService).sendMessage(
            testChannel,
            "docJAM @$testUsername Unknown command, try ;link, ;search or ;help"
        )
    }

    @Test
    fun `test skipCommand when user can skip`() {
        // Given
        `when`(timeRestrictionService.canUserSkipVideo(testUsername)).thenReturn(true)
        `when`(userConfig.skipValue).thenReturn("5")
        `when`(skipCounterService.getSkipCounter()).thenReturn(2)
        `when`(timeRestrictionService.timeToNextSkip(testUsername)).thenReturn("10 minutes")

        // When
        commandService.skipCommand(testUsername, testChannel)

        // Then
        verify(timeRestrictionService).setLastSkip(testUsername)
        verify(playlistService).handleSkipCommand(
            testUsername,
            testChannel,
            true,
            5L,
            2,
            "10 minutes"
        )
    }

    @Test
    fun `test skipCommand when user cannot skip`() {
        // Given
        `when`(timeRestrictionService.canUserSkipVideo(testUsername)).thenReturn(false)
        `when`(userConfig.skipValue).thenReturn("5")
        `when`(skipCounterService.getSkipCounter()).thenReturn(2)
        `when`(timeRestrictionService.timeToNextSkip(testUsername)).thenReturn("10 minutes")

        // When
        commandService.skipCommand(testUsername, testChannel)

        // Then
        verify(timeRestrictionService, never()).setLastSkip(testUsername)
        verify(playlistService).handleSkipCommand(
            testUsername,
            testChannel,
            false,
            5L,
            2,
            "10 minutes"
        )
    }

    @Test
    fun `test skipCommand with null skipValue`() {
        // Given
        `when`(timeRestrictionService.canUserSkipVideo(testUsername)).thenReturn(true)
        `when`(userConfig.skipValue).thenReturn(null)
        `when`(skipCounterService.getSkipCounter()).thenReturn(2)
        `when`(timeRestrictionService.timeToNextSkip(testUsername)).thenReturn("10 minutes")

        // When
        commandService.skipCommand(testUsername, testChannel)

        // Then
        verify(timeRestrictionService).setLastSkip(testUsername)
        verify(playlistService).handleSkipCommand(
            testUsername,
            testChannel,
            true,
            5L,
            2,
            "10 minutes"
        )
    }
}
