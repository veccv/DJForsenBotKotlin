package com.github.veccvs.djforsenbotkotlin.component

import com.github.veccvs.djforsenbotkotlin.config.UserConfig
import com.github.veccvs.djforsenbotkotlin.service.CommandService
import com.github.veccvs.djforsenbotkotlin.utils.StreamInfo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.pircbotx.Channel
import org.pircbotx.PircBotX
import org.pircbotx.User
import org.pircbotx.hooks.events.MessageEvent
import org.pircbotx.output.OutputIRC
import java.lang.reflect.Field
import java.net.HttpURLConnection
import java.net.URI

@ExtendWith(MockitoExtension::class)
class TwitchChatBotTest {

  @Mock private lateinit var commandService: CommandService

  @Mock private lateinit var userConfig: UserConfig

  @Mock private lateinit var bot: PircBotX

  @Mock private lateinit var outputIRC: OutputIRC

  @Mock private lateinit var chatMonitor: TwitchChatMonitor

  private lateinit var twitchChatBot: TwitchChatBot

  // Store the original StreamInfo.streamEnabled method
  private var originalStreamInfoMethod: ((URI) -> HttpURLConnection)? = null

  @BeforeEach
  fun setUp() {
    // Create TwitchChatBot with mocked dependencies
    twitchChatBot = TwitchChatBot(commandService, userConfig, chatMonitor)

    // Use reflection to set the bot field
    val botField: Field = TwitchChatBot::class.java.getDeclaredField("bot")
    botField.isAccessible = true
    botField.set(twitchChatBot, bot)

    // Store the original StreamInfo.httpConnectionFactory
    originalStreamInfoMethod = StreamInfo.httpConnectionFactory
  }

  @AfterEach
  fun tearDown() {
    // Restore the original StreamInfo.httpConnectionFactory
    StreamInfo.httpConnectionFactory = originalStreamInfoMethod
  }

  @Test
  fun `sendMessage should send message to channel when not streaming to Forsen`() {
    // Arrange
    val channel = "#testchannel"
    val message = "Test message"

    // Mock StreamInfo.streamEnabled to return false
    StreamInfo.httpConnectionFactory = { _ ->
      mock(HttpURLConnection::class.java).apply {
        `when`(inputStream).thenReturn("""{"data":[]}""".byteInputStream())
      }
    }

    // Mock the bot.sendIRC() method
    `when`(bot.sendIRC()).thenReturn(outputIRC)

    // Mock bot.isConnected to return true so we don't retry
    `when`(bot.isConnected).thenReturn(true)

    // Mock chatMonitor.verifyMessageSent to return true
    `when`(chatMonitor.verifyMessageSent(message, 5)).thenReturn(true)

    // Act
    twitchChatBot.sendMessage(channel, message)

    // Assert
    verify(outputIRC).message(channel, message)
  }

  @Test
  fun `sendMessage should not send message to Forsen channel when streaming`() {
    // Arrange
    val channel = "#forsen"
    val message = "Test message"

    // Mock StreamInfo.streamEnabled to return true
    StreamInfo.httpConnectionFactory = { _ ->
      mock(HttpURLConnection::class.java).apply {
        `when`(inputStream).thenReturn("""{"data":[{"type":"live"}]}""".byteInputStream())
      }
    }

    // Act
    twitchChatBot.sendMessage(channel, message)

    // Assert
    verify(outputIRC, never()).message(channel, message)
  }

  @Test
  fun `onMessage should call commandHandler with correct parameters`() {
    // Arrange
    val username = "testuser"
    val message = ";help"
    val channelName = "#testchannel"

    // Mock Channel
    val mockChannel = mock(Channel::class.java)
    `when`(mockChannel.name).thenReturn(channelName)

    // Mock User
    val mockUser = mock(User::class.java)
    `when`(mockUser.nick).thenReturn(username)

    // Mock MessageEvent
    val mockEvent = mock(MessageEvent::class.java)
    `when`(mockEvent.channel).thenReturn(mockChannel)
    `when`(mockEvent.user).thenReturn(mockUser)
    `when`(mockEvent.message).thenReturn(message)

    // Act
    twitchChatBot.onMessage(mockEvent)

    // Assert
    verify(commandService).commandHandler(username, message, channelName)
  }
}
