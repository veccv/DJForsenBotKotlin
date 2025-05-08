package com.github.veccvs.djforsenbotkotlin.component

import com.github.veccvs.djforsenbotkotlin.config.UserConfig
import com.github.veccvs.djforsenbotkotlin.service.CommandService
import com.github.veccvs.djforsenbotkotlin.utils.StreamInfo
import jakarta.annotation.PostConstruct
import org.pircbotx.Configuration
import org.pircbotx.PircBotX
import org.pircbotx.cap.EnableCapHandler
import org.pircbotx.hooks.ListenerAdapter
import org.pircbotx.hooks.events.MessageEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

@Component
class TwitchChatBot
@Autowired
constructor(
  @Lazy private val commandService: CommandService,
  @Lazy private val userConfig: UserConfig,
  @Lazy private val chatMonitor: TwitchChatMonitor,
) : ListenerAdapter() {

  private lateinit var bot: PircBotX
  private val maxRetries = 3
  private var reconnecting = false
  private val reconnectLock = Object()

  override fun onMessage(event: MessageEvent) {
    val sender = event.user?.nick ?: ""
    val message = event.message
    val channel = event.channel.name
    // Only log messages that are bot commands (starting with "!djfors_" or ";")
    if (message.startsWith("!djfors_") || message.startsWith(";")) {
      logger.info("[BOT RECEIVED] Message from $sender in $channel: $message")
    }
    commandService.commandHandler(sender, message, channel)
  }

  fun sendMessage(channel: String, message: String) {
    if (StreamInfo.streamEnabled() && channel == "#forsen") {
      logger.info("[BOT] Not sending message to #forsen while stream is enabled: $message")
      return
    }

    // Check connection status before sending
    if (!bot.isConnected) {
      synchronized(reconnectLock) {
        if (reconnecting) {
          logger.info(
            "[BOT CONNECTION] Bot is not connected but reconnection is already in progress, waiting..."
          )
          // Release the lock while waiting
        } else {
          logger.warn(
            "[BOT CONNECTION] Bot is not connected before message send attempt, reconnecting..."
          )
          reconnectBot()
        }
      }

      // Wait for reconnection to complete or give it time to make progress
      sleepMillis(2000)

      // If still not connected after waiting, log a warning but continue with the send attempt
      // The sendMessageWithRetry method will handle retries if needed
      if (!bot.isConnected) {
        logger.warn(
          "[BOT CONNECTION] Bot is still not connected after waiting, proceeding with send attempt anyway"
        )
      }
    }

    if (!isTestEnvironment()) {
      sendMessageWithRetry(channel, message)
    } else {
      logger.info("[BOT] Test environment detected, sending message without retries: $message")
      bot.sendIRC().message(channel, message)
    }
  }

  private fun isTestEnvironment(): Boolean {
    return try {
      false
    } catch (_: Exception) {
      true
    }
  }

  private fun sendMessageWithRetry(channel: String, message: String) {
    var isMessageSent = false
    var attempts = 0
    var lastException: Exception? = null
    logger.info("[BOT] Starting message send process to $channel: $message")
    while (!isMessageSent && attempts < maxRetries) {
      attempts++
      try {
        logger.info(
          "[BOT SEND] Sending message to $channel (attempt $attempts/$maxRetries): $message"
        )
        bot.sendIRC().message(channel, message)
        sleepMillis(100)
        if (bot.isConnected) {
          logger.info("[BOT VERIFICATION] Verifying message appears in chat: $message")
          if (chatMonitor.verifyMessageSent(message, 5)) {
            isMessageSent = true
            logger.info("[BOT VERIFICATION SUCCESS] Message verified in chat: $message")
          } else {
            logger.warn(
              "[BOT VERIFICATION FAILED] Message sent but not verified in chat, retrying... (attempt $attempts/$maxRetries)"
            )
          }
        } else {
          logger.warn(
            "[BOT CONNECTION] Bot is not connected, retrying... (attempt $attempts/$maxRetries)"
          )
          reconnectBot()
          sleepMillis(1000)
        }
      } catch (e: Exception) {
        lastException = e
        logger.error(
          "[BOT ERROR] Error sending message to $channel (attempt $attempts/$maxRetries): ${e.message}"
        )

        // Check for "Not connected to server" error specifically
        if (e.message?.contains("Not connected to server") == true) {
          logger.warn(
            "[BOT CONNECTION] Detected 'Not connected to server' error, forcing reconnection..."
          )

          // Force reconnection even if the bot thinks it's connected
          synchronized(reconnectLock) {
            if (!reconnecting) {
              reconnectBot()
            } else {
              logger.info("[BOT CONNECTION] Reconnection already in progress, waiting...")
            }
          }

          sleepMillis(1000)
        }

        sleepMillis(500L * attempts)
      }
    }
    if (!isMessageSent) {
      logger.error(
        "[BOT SEND FAILED] Failed to send message after $maxRetries attempts: $message",
        lastException,
      )
    }
  }

  private fun reconnectBot() {
    synchronized(reconnectLock) {
      // If already reconnecting, don't start another reconnection
      if (reconnecting) {
        logger.info("[BOT RECONNECT] Reconnection already in progress, skipping this request")
        return
      }

      reconnecting = true
    }

    thread(start = true) {
      try {
        logger.info("[BOT RECONNECT] Attempting to reconnect to Twitch IRC...")
        // Close the existing connection if it exists
        if (bot.isConnected || !bot.state.equals(PircBotX.State.DISCONNECTED)) {
          logger.info("[BOT RECONNECT] Closing existing connection before reconnecting...")
          try {
            bot.close()
            sleepMillis(1000) // Give it time to fully disconnect
          } catch (e: Exception) {
            logger.warn("[BOT RECONNECT] Error while closing existing connection: ${e.message}")
            // Continue with reconnection attempt even if closing fails
          }
        }

        // Recreate the bot with the same configuration
        val config = bot.configuration
        bot = PircBotX(config)
        bot.startBot()
        logger.info("[BOT RECONNECT] Successfully reconnected to Twitch IRC")
      } catch (e: Exception) {
        logger.error("[BOT RECONNECT FAILED] Failed to reconnect: ${e.message}")
      } finally {
        synchronized(reconnectLock) { reconnecting = false }
      }
    }
  }

  private fun sleepMillis(milliseconds: Long) {
    try {
      TimeUnit.MILLISECONDS.sleep(milliseconds)
    } catch (_: InterruptedException) {
      Thread.currentThread().interrupt()
    }
  }

  @PostConstruct
  fun startBot() {
    val channelName = userConfig.channelName ?: "#veccvs"
    logger.info("=================================================")
    logger.info("[BOT STARTUP] Initializing Twitch chat bot")
    logger.info("[BOT STARTUP] Bot will connect as: $BOT_NAME")
    logger.info("[BOT STARTUP] Target channel: $channelName")
    logger.info("[BOT STARTUP] Message verification is enabled via TwitchChatMonitor")
    logger.info("=================================================")
    val configuration =
      Configuration.Builder()
        .setName(BOT_NAME)
        .setServerPassword(BOT_OAUTH_TOKEN)
        .addServer("irc.chat.twitch.tv")
        .addAutoJoinChannel(channelName)
        .addListener(this)
        .setAutoReconnect(true)
        .addCapHandler(EnableCapHandler("twitch.tv/membership"))
        .addCapHandler(EnableCapHandler("twitch.tv/commands"))
        .addCapHandler(EnableCapHandler("twitch.tv/tags"))
        .buildConfiguration()
    logger.info("[BOT STARTUP] Enabled Twitch capabilities: membership, commands, tags")
    bot = PircBotX(configuration)
    Thread {
        try {
          logger.info("[BOT STARTUP] Starting Twitch chat bot for channel: $channelName")
          bot.startBot()
        } catch (e: Exception) {
          logger.error("[BOT STARTUP FAILED] Error starting bot: ${e.message}", e)
        }
      }
      .start()
  }

  /**
   * Scheduled task that runs every 5 minutes to check the connection status and reconnect if
   * necessary. This helps maintain the connection over long periods of inactivity.
   */
  @Scheduled(fixedRate = 300000) // 5 minutes in milliseconds
  fun checkConnectionStatus() {
    try {
      if (!bot.isConnected) {
        synchronized(reconnectLock) {
          if (reconnecting) {
            logger.info(
              "[BOT CONNECTION CHECK] Bot is not connected but reconnection is already in progress"
            )
            return
          }
          logger.warn(
            "[BOT CONNECTION CHECK] Bot is not connected during scheduled check, reconnecting..."
          )
          reconnectBot()
        }
      } else {
        logger.debug("[BOT CONNECTION CHECK] Connection status check passed")
      }
    } catch (e: Exception) {
      logger.error("[BOT CONNECTION CHECK] Error during connection check: ${e.message}")
    }
  }

  companion object {
    private val logger = LoggerFactory.getLogger(TwitchChatBot::class.java)
    private const val BOT_NAME = "djfors_"
    private const val BOT_OAUTH_TOKEN = "oauth:ln9r4pdy3vjha3c83dn0b2cbyem87r"
  }
}
