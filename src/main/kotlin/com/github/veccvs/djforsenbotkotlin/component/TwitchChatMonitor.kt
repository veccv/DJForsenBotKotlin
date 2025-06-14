package com.github.veccvs.djforsenbotkotlin.component

import com.github.veccvs.djforsenbotkotlin.config.UserConfig
import jakarta.annotation.PostConstruct
import org.pircbotx.Configuration
import org.pircbotx.PircBotX
import org.pircbotx.cap.EnableCapHandler
import org.pircbotx.hooks.ListenerAdapter
import org.pircbotx.hooks.events.MessageEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * A component that connects to Twitch chats anonymously to monitor messages. This is used to verify
 * if messages sent by the bot are actually appearing in the chat.
 */
@Component
class TwitchChatMonitor @Autowired constructor(private val userConfig: UserConfig) :
  ListenerAdapter() {

  companion object {
    private const val BOT_USERNAME = "djfors_"
    private const val ANONYMOUS_USERNAME = "justinfan12345"
    private const val MESSAGE_STALE_THRESHOLD_MS = 10_000
  }

  private lateinit var bot: PircBotX
  private val logger = LoggerFactory.getLogger(TwitchChatMonitor::class.java)

  // Store received messages with their timestamps
  private val receivedMessagesTimestamps: ConcurrentHashMap<String, Long> = ConcurrentHashMap()

  // Latch to signal when a specific message is received
  private var messageLatch: CountDownLatch? = null
  private var messageToVerify: String? = null

  override fun onMessage(event: MessageEvent) {
    val sender = event.user?.nick ?: ""
    val message = event.message
    val channel = event.channel.name
    logger.info("[CHAT MONITOR] Received message in $channel from $sender: $message")
    // Store the message with the current timestamp
    receivedMessagesTimestamps[message] = System.currentTimeMillis()

    // Check if this is the message we're waiting for
    if (messageToVerify != null) {
      val normalizedReceived = normalizeMessage(message)
      val normalizedExpected = normalizeMessage(messageToVerify!!)

      if (normalizedReceived == normalizedExpected && sender == BOT_USERNAME) {
        logger.info("[VERIFICATION SUCCESS] Bot message verified in chat: $message")

        // Log detailed comparison if the raw messages don't match but normalized versions do
        if (message != messageToVerify) {
          logger.info("[VERIFICATION DETAIL] Raw messages differ but normalized versions match:")
          logger.info("[VERIFICATION DETAIL] Expected: '$messageToVerify'")
          logger.info("[VERIFICATION DETAIL] Received: '$message'")
          logger.info("[VERIFICATION DETAIL] Normalized expected: '$normalizedExpected'")
          logger.info("[VERIFICATION DETAIL] Normalized received: '$normalizedReceived'")
        }

        messageLatch?.countDown()
      }
    }
  }

  fun verifyMessageSent(message: String, timeoutSeconds: Int = 5): Boolean {
    clearStaleMessages()

    // Check if we've already seen this message, using normalized comparison
    val normalizedMessage = normalizeMessage(message)

    // Find the matching message if any
    val matchingMessage =
      receivedMessagesTimestamps.keys.find { normalizeMessage(it) == normalizedMessage }
    val alreadySeen = matchingMessage != null

    if (alreadySeen) {
      logger.info("[VERIFICATION SUCCESS] Message was already seen in chat: $message")

      // Log detailed comparison if the raw messages don't match but normalized versions do
      if (matchingMessage != message) {
        logger.info("[VERIFICATION DETAIL] Raw messages differ but normalized versions match:")
        logger.info("[VERIFICATION DETAIL] Expected: '$message'")
        logger.info("[VERIFICATION DETAIL] Matched with: '$matchingMessage'")
        logger.info("[VERIFICATION DETAIL] Normalized expected: '$normalizedMessage'")
        logger.info(
          "[VERIFICATION DETAIL] Normalized matched: '${normalizeMessage(matchingMessage!!)}'"
        )
      }
    }
    return if (alreadySeen) {
      true
    } else {
      messageToVerify = message
      messageLatch = CountDownLatch(1)
      logger.info(
        "[VERIFICATION STARTED] Waiting for message to appear in chat: $message (timeout: ${timeoutSeconds}s)"
      )
      try {
        val received = messageLatch!!.await(timeoutSeconds.toLong(), TimeUnit.SECONDS)
        if (received) {
          logger.info("[VERIFICATION SUCCESS] Successfully verified message in chat: $message")
          true
        } else {
          logger.warn(
            "[VERIFICATION FAILED] Could not verify message in chat within timeout: $message"
          )
          false
        }
      } finally {
        messageToVerify = null
        messageLatch = null
      }
    }
  }

  /** Remove messages older than MESSAGE_STALE_THRESHOLD_MS. */
  private fun clearStaleMessages() {
    val now = System.currentTimeMillis()
    receivedMessagesTimestamps.entries.removeIf { now - it.value > MESSAGE_STALE_THRESHOLD_MS }
  }

  /**
   * Normalizes a message for comparison by handling special characters. This helps with
   * verification when messages contain characters that might be encoded differently.
   */
  private fun normalizeMessage(message: String): String {
    return message
      .replace("'", "'") // Normalize apostrophes
      .replace("'", "'") // Handle different apostrophe types
      .replace("'", "'") // Handle more apostrophe variants
      .replace(
        """, "\"") // Normalize quotes
      .replace(""",
        "\"",
      ) // Handle different quote types
      .replace("–", "-") // Normalize en dash
      .replace("—", "-") // Normalize em dash
      .replace("\u00A0", " ") // Replace non-breaking space with regular space
      .replace(Regex("\\s+"), " ") // Normalize multiple spaces to single space
      .trim()
  }

  @PostConstruct
  fun startMonitor() {
    val channelName = userConfig.channelName ?: "#veccvs"
    logger.info("=================================================")
    logger.info("[CHAT MONITOR] Initializing anonymous Twitch chat monitor")
    logger.info("[CHAT MONITOR] This monitor will verify that bot messages appear in chat")
    logger.info("[CHAT MONITOR] Target channel: $channelName")
    logger.info("=================================================")
    val configuration = createBotConfiguration(channelName)
    logger.info("[CHAT MONITOR] Enabled Twitch capabilities: membership, commands, tags")
    bot = PircBotX(configuration)
    thread(start = true, name = "twitch-chat-monitor") {
      try {
        logger.info(
          "[CHAT MONITOR] Starting anonymous Twitch chat monitor for channel: $channelName"
        )
        bot.startBot()
      } catch (e: Exception) {
        logger.error("[CHAT MONITOR] Error in Twitch chat monitor: ${e.message}", e)
      }
    }
  }

  /** Configures the PircBotX for anonymous Twitch chat monitoring. */
  private fun createBotConfiguration(channelName: String): Configuration {
    return Configuration.Builder()
      .setName(ANONYMOUS_USERNAME)
      .addServer("irc.chat.twitch.tv")
      .addAutoJoinChannel(channelName)
      .addListener(this)
      .setAutoReconnect(true)
      .addCapHandler(EnableCapHandler("twitch.tv/membership"))
      .addCapHandler(EnableCapHandler("twitch.tv/commands"))
      .addCapHandler(EnableCapHandler("twitch.tv/tags"))
      .buildConfiguration()
  }
}
