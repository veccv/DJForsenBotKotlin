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
 * A component that connects to Twitch chat anonymously to monitor messages.
 * This is used to verify if messages sent by the bot are actually appearing in the chat.
 */
@Component
class TwitchChatMonitor @Autowired constructor(
    private val userConfig: UserConfig
) : ListenerAdapter() {
    private lateinit var bot: PircBotX
    private val logger = LoggerFactory.getLogger(TwitchChatMonitor::class.java)

    // Store received messages with their timestamps
    private val receivedMessages = ConcurrentHashMap<String, Long>()

    // Latch to signal when a specific message is received
    private var messageLatch: CountDownLatch? = null
    private var messageToVerify: String? = null

    override fun onMessage(event: MessageEvent) {
        val sender = event.user?.nick ?: ""
        val message = event.message
        val channel = event.channel.name

        // Log all messages at info level for better visibility
        logger.info("[CHAT MONITOR] Received message in $channel from $sender: $message")

        // Store the message with current timestamp
        receivedMessages[message] = System.currentTimeMillis()

        // Check if this is the message we're waiting for
        if (message == messageToVerify && sender == "djfors_") {
            logger.info("[VERIFICATION SUCCESS] Bot message verified in chat: $message")
            messageLatch?.countDown()
        }
    }

    /**
     * Verifies if a specific message from the bot appears in the chat within the given timeout.
     * 
     * @param message The message to verify
     * @param timeoutSeconds The maximum time to wait for the message in seconds
     * @return true if the message was seen, false otherwise
     */
    fun verifyMessageSent(message: String, timeoutSeconds: Int = 5): Boolean {
        // Clear old messages that are more than 10 seconds old
        val now = System.currentTimeMillis()
        receivedMessages.entries.removeIf { now - it.value > 10000 }

        // Check if we've already seen this message in the last few seconds
        if (receivedMessages.containsKey(message)) {
            logger.info("[VERIFICATION SUCCESS] Message was already seen in chat: $message")
            return true
        }

        // Set up the latch and message to verify
        messageToVerify = message
        messageLatch = CountDownLatch(1)
        logger.info("[VERIFICATION STARTED] Waiting for message to appear in chat: $message (timeout: ${timeoutSeconds}s)")

        try {
            // Wait for the message to appear
            val received = messageLatch!!.await(timeoutSeconds.toLong(), TimeUnit.SECONDS)
            if (received) {
                logger.info("[VERIFICATION SUCCESS] Successfully verified message in chat: $message")
                return true
            } else {
                logger.warn("[VERIFICATION FAILED] Could not verify message in chat within timeout: $message")
                return false
            }
        } finally {
            // Clean up
            messageToVerify = null
            messageLatch = null
        }
    }

    @PostConstruct
    fun startMonitor() {
        val channelName = userConfig.channelName ?: "#veccvs"

        logger.info("=================================================")
        logger.info("[CHAT MONITOR] Initializing anonymous Twitch chat monitor")
        logger.info("[CHAT MONITOR] This monitor will verify that bot messages appear in chat")
        logger.info("[CHAT MONITOR] Target channel: $channelName")
        logger.info("=================================================")

        // Configure anonymous connection (no oauth token needed)
        val configuration = Configuration.Builder()
            .setName("justinfan12345") // Anonymous username prefix
            .addServer("irc.chat.twitch.tv")
            .addAutoJoinChannel(channelName)
            .addListener(this)
            .setAutoReconnect(true)
            .addCapHandler(EnableCapHandler("twitch.tv/membership"))
            .addCapHandler(EnableCapHandler("twitch.tv/commands"))
            .addCapHandler(EnableCapHandler("twitch.tv/tags"))
            .buildConfiguration()

        logger.info("[CHAT MONITOR] Enabled Twitch capabilities: membership, commands, tags")

        bot = PircBotX(configuration)

        // Start the bot in a separate thread
        thread(start = true, name = "twitch-chat-monitor") {
            try {
                logger.info("[CHAT MONITOR] Starting anonymous Twitch chat monitor for channel: $channelName")
                bot.startBot()
            } catch (e: Exception) {
                logger.error("[CHAT MONITOR] Error in Twitch chat monitor: ${e.message}", e)
            }
        }
    }
}
