package com.github.veccvs.djforsenbotkotlin.service

import com.github.veccvs.djforsenbotkotlin.component.TwitchChatBot
import com.github.veccvs.djforsenbotkotlin.component.TwitchConnector
import com.github.veccvs.djforsenbotkotlin.config.UserConfig
import com.github.veccvs.djforsenbotkotlin.dao.CytubeDao
import com.github.veccvs.djforsenbotkotlin.model.*
import com.github.veccvs.djforsenbotkotlin.repository.GachiSongRepository
import com.github.veccvs.djforsenbotkotlin.repository.UserRepository
import com.github.veccvs.djforsenbotkotlin.service.command.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SimpleCommandServiceTest {
  @Mock private lateinit var cytubeDao: CytubeDao
  @Mock private lateinit var twitchChatBot: TwitchChatBot
  @Mock private lateinit var userRepository: UserRepository
  @Mock private lateinit var userConfig: UserConfig
  @Mock private lateinit var songService: SongService
  @Mock private lateinit var skipCounterService: SkipCounterService
  @Mock private lateinit var gachiSongRepository: GachiSongRepository
  @Mock private lateinit var userSongService: UserSongService
  @Mock private lateinit var messageService: MessageService
  @Mock private lateinit var commandParserService: CommandParserService
  @Mock private lateinit var videoSearchService: VideoSearchService
  @Mock private lateinit var timeRestrictionService: TimeRestrictionService
  @Mock private lateinit var userSongFormatterService: UserSongFormatterService
  @Mock private lateinit var playlistService: PlaylistService
  
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
    
    // Set up messageService to delegate to twitchChatBot
    doAnswer { invocation ->
      val channel = invocation.getArgument<String>(0)
      val message = invocation.getArgument<String>(1)
      twitchChatBot.sendMessage(channel, message)
      null
    }.`when`(messageService).sendMessage("#testchannel", "docJAM @testuser bot made by veccvs")
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
}