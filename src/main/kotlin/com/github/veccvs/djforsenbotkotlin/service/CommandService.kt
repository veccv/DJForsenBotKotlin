package com.github.veccvs.djforsenbotkotlin.service

import com.github.veccvs.djforsenbotkotlin.component.TwitchConnector
import com.github.veccvs.djforsenbotkotlin.config.UserConfig
import com.github.veccvs.djforsenbotkotlin.model.TwitchCommand
import com.github.veccvs.djforsenbotkotlin.model.User
import com.github.veccvs.djforsenbotkotlin.repository.UserRepository
import com.github.veccvs.djforsenbotkotlin.service.command.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * Service responsible for managing and executing commands in a Twitch chat context. It processes
 * user input, interprets commands, interacts with dependent services, and sends appropriate
 * responses or performs targeted actions.
 *
 * This class primarily focuses on handling custom commands, performing actions such as searching
 * for videos, managing playlists, working with user-requested commands, and enforcing time-based
 * restrictions.
 *
 * Dependencies for this service are injected via constructor-based autowiring.
 */
@Service
class CommandService(
  @Autowired private val messageService: MessageService,
  @Autowired private val commandParserService: CommandParserService,
  @Autowired private val videoSearchService: VideoSearchService,
  @Autowired private val timeRestrictionService: TimeRestrictionService,
  @Autowired private val userSongFormatterService: UserSongFormatterService,
  @Autowired private val playlistService: PlaylistService,
  @Autowired private val userRepository: UserRepository,
  @Autowired private val userConfig: UserConfig,
  @Autowired private val skipCounterService: SkipCounterService,
  @Autowired private val userSongService: UserSongService,
  @Autowired private val spotifyService: SpotifyService,
  @Autowired private val spotifyCommandService: SpotifyCommandService,
  @Autowired private val playlistCommandService: PlaylistCommandService,
  @Autowired private val gptService: GptService,
) {
  /**
   * Companion object for the CommandService class. Acts as a container for shared constants and
   * static-like properties/methods.
   */
  companion object {
    /**
     * The constant link to the Cytube room used by the application. This link is primarily used
     * within the CommandService for sending users to the shared Cytube room. It is used in response
     * to specific commands such as "; link" or "; where".
     */
    private const val CYTUBE_LINK = "https://cytu.be/r/forsenboys"

    /** Logger instance for this class. */
    private val logger = LoggerFactory.getLogger(CommandService::class.java)
  }

  /**
   * An instance of TwitchConnector used for interacting with Twitch services such as sending
   * whispers. Acts as a utility to handle Twitch-specific actions required by CommandService.
   */
  private val twitchConnector = TwitchConnector()

  private val failedResponseTimestamps: MutableMap<String, LocalDateTime> = ConcurrentHashMap()

  private val pendingUsers = ConcurrentHashMap.newKeySet<String>()

  /**
   * Sends a message to the specified channel.
   *
   * @param channel The name of the channel where the message will be sent.
   * @param message The content of the message to be sent.
   */
  fun sendMessage(channel: String, message: String) {
    messageService.sendMessage(channel, message)
  }

  /**
   * Handles commands sent by a user in a Twitch chat channel. It processes the input message,
   * identifies commands, and takes the appropriate actions based on the detected command.
   *
   * @param username The username of the Twitch user who sent the message.
   * @param message The content of the chat message sent by the user.
   * @param channel The Twitch channel where the message was sent.
   */
  fun commandHandler(username: String, message: String, channel: String) {
    // Ignore messages from specific bots
    if (username == "botr" || username == "supibot") {
      logger.info("[COMMAND HANDLER] Ignoring message from bot: $username")
      return
    }

    // Only log messages that are bot commands or mentions
    if (
      message.startsWith("!djfors_") ||
        message.startsWith(";") ||
        commandParserService.detectBotMention(message)
    ) {
      println("[COMMAND HANDLER] Processing message from $username in $channel: $message")
    }

    if (message.startsWith("!djfors_")) {
      logger.info("[COMMAND HANDLER] Detected !djfors_ command")
      messageService.sendMessage(channel, "docJAM @${username} bot made by veccvs")
      return
    }

    // Check if the bot is mentioned
    if (commandParserService.detectBotMention(message)) {
      logger.info("[COMMAND HANDLER] Detected bot mention from $username")
      handleBotMention(username, message, channel)
      return
    }

    val command = commandParserService.detectCommand(message)

    if (command != null) {
      logger.info("[COMMAND HANDLER] Detected command: $command")
      userRepository.findByUsername(username) ?: userRepository.save(User(username))
      if (!timeRestrictionService.canResponseToCommand(username)) return
      timeRestrictionService.setLastResponse(username)
      val twitchCommand = commandParserService.parseCommand(message)
      handleDetectedCommand(twitchCommand, username, channel)
    }
  }

  /**
   * Handles the execution of detected Twitch commands by parsing the command and delegating the
   * appropriate action based on the given command type.
   *
   * @param twitchCommand The detected Twitch command to handle. May be null if no command is
   *   detected.
   * @param username The username of the user who issued the command.
   * @param channel The channel in which the command was issued.
   */
  private fun handleDetectedCommand(
    twitchCommand: TwitchCommand?,
    username: String,
    channel: String,
  ) {
    val helpMessage =
      "docJAM @${username} Commands: ;link, ;where, ;search, ;s, ;m, ;match, ;help, ;playlist, ;skip, ;rg, ;when, ;undo, ;connect, ;track, ;track stop, ;current"
    val unknownCommandMessage = "docJAM @$username Unknown command, try ;link, ;search or ;help"
    when (twitchCommand?.command) {
      ";link",
      ";where" -> {
        twitchConnector.sendWhisper(username, CYTUBE_LINK)
        messageService.sendMessage(
          channel,
          "docJAM @${username} I sent you a whisper with the link forsenCD ",
        )
      }

      ";search",
      ";s" -> {
        handleSearchCommand(twitchCommand, channel, username)
      }

      ";rg" -> {
        handleSearchCommand(
          TwitchCommand("", listOf(playlistService.randomGachiSong())),
          channel,
          username,
        )
      }

      ";help" -> {
        messageService.sendMessage(channel, helpMessage)
      }

      ";playlist" -> {
        playlistService.getPlaylist(username, channel)
      }

      ";skip" -> {
        playlistCommandService.skipCommand(username, channel)
      }

      ";when" -> {
        userSongFormatterService.getUserSongs(username, channel)
      }

      ";undo" -> {
        playlistCommandService.removeSongCommand(username, channel)
      }

      ";connect" -> {
        spotifyCommandService.connectSpotifyCommand(twitchCommand, username, channel)
      }

      ";track" -> {
        if (twitchCommand.params.isNotEmpty() && twitchCommand.params[0] == "stop") {
          spotifyCommandService.stopTrackingCommand(username, channel)
        } else {
          spotifyCommandService.trackAndAddSpotifyCommand(username, channel)
        }
      }

      ";current" -> {
        spotifyCommandService.addCurrentSpotifyCommand(username, channel)
      }

      ";m",
      ";match" -> {
        handleMusicMatchCommand(twitchCommand, channel, username)
      }

      else -> {
        messageService.sendMessage(channel, unknownCommandMessage)
      }
    }
  }

  /**
   * Handles the "; search" or "; s" command by performing a video search based on the provided
   * Twitch command. Verifies if the user can add a video within the channel before initiating the
   * search.
   *
   * @param twitchCommand The parsed Twitch command containing the search query and parameters.
   * @param channel The Twitch channel where the command was issued.
   * @param username The username of the user who issued the command.
   */
  private fun handleSearchCommand(twitchCommand: TwitchCommand, channel: String, username: String) {
    if (checkAndNotifyUserCanAddVideo(username, channel)) return
    videoSearchService.searchVideo(twitchCommand, channel, username)
  }

  /**
   * Checks whether the specified user can add a video and notifies the user via the specified
   * channel if they are restricted from doing so due to cooldown limits.
   *
   * @param username The username of the user attempting to add a video.
   * @param channel The communication channel to send the restriction notification if required.
   * @return True, if the user is restricted from adding a video and a notification was sent, false
   *   otherwise.
   */
  private fun checkAndNotifyUserCanAddVideo(username: String, channel: String): Boolean {
    // Check if user is on cooldown
    if (!timeRestrictionService.canUserAddVideo(username)) {
      messageService.sendMessage(
        channel,
        "docJAM @${username} You can add a video every ${userConfig.minutesToAddVideo} minutes. Time to add next video: ${
          timeRestrictionService.timeToNextVideo(username)
        }",
      )
      return true
    }

    // Check if user is currently tracking Spotify or has been notified already
    val user = userRepository.findByUsername(username)
    if (user != null && user.userNotified) {
      // User is already tracking or has been notified, don't send another notification
      return false
    }

    return false
  }

  /**
   * Processes a Spotify token message from a whisper and updates the user's account
   *
   * @param username The username of the user
   * @param tokenMessage The token message from the whisper
   * @return True if the tokens were successfully processed and saved, false otherwise
   */
  fun processSpotifyTokenMessage(username: String, tokenMessage: String): Boolean {
    return spotifyCommandService.processSpotifyTokenMessage(username, tokenMessage)
  }

  /**
   * Parses the token response from the user in the format "token=value;refreshToken=value;" The
   * response might be embedded in a full Twitch whisper message.
   *
   * @param tokenResponse The token response string
   * @return A map containing the parsed token values
   */
  fun parseTokenResponse(tokenResponse: String): Map<String, String> {
    return spotifyCommandService.parseTokenResponse(tokenResponse)
  }

  /**
   * Handles the skip command for a given user in a specific channel. Determines if the user is
   * allowed to skip, calculates the skip-related parameters, and invokes the playlist service to
   * process the skip.
   *
   * @param username The username of the user attempting to issue the skip command.
   * @param channel The channel where the skip command is being executed.
   */
  fun skipCommand(username: String, channel: String) {
    playlistCommandService.skipCommand(username, channel)
  }

  /**
   * Processes the removal of a recently added song for a given user in a specified channel.
   * Verifies user limitations such as time restrictions before attempting to remove the song. Sends
   * appropriate response messages to the channel based on the outcome.
   *
   * @param username The username of the person requesting the song removal.
   * @param channel The name of the channel where the command was issued.
   */
  fun removeSongCommand(username: String, channel: String) {
    playlistCommandService.removeSongCommand(username, channel)
  }

  /**
   * Handles when the bot is mentioned in a message. Extracts the message content after the mention
   * and sends it to the GPT API.
   *
   * @param username The username of the user who mentioned the bot
   * @param message The full message that contains the mention
   * @param channel The channel where the message was sent
   */
  private fun handleBotMention(username: String, message: String, channel: String) {
    if (!pendingUsers.add(username)) {
      logger.info("[GPT] User $username already has a pending GPT request – skipping")
      return
    }

    timeRestrictionService.setLastResponse(username)

    if (message.isBlank()) {
      messageService.sendMessage(channel, "docJAM @$username How can I help you?")
      return
    }

    logger.info("[GPT] Processing mention from $username with content: $message")

    val now = LocalDateTime.now()
    failedResponseTimestamps[username]?.let { lastFail ->
      val cooldownSeconds = userConfig.secondsToResponseToCommand?.toLong() ?: 6L
      if (Duration.between(lastFail, now).seconds < cooldownSeconds) {
        logger.info("[GPT] User $username still in failure cooldown, ignoring mention")
        return
      } else {
        failedResponseTimestamps.remove(username)
      }
    }

    val gptResponse: String?
    try {
      gptResponse = gptService.getGptResponse(message, username)
    } finally {
      pendingUsers.remove(username)
    }

    if (gptResponse != null) {
      // Send the response back to the channel
      messageService.sendMessage(channel, gptResponse)
    } else {
      // Send an error message if the GPT API call failed
      failedResponseTimestamps[username] = now
      messageService.sendMessage(
        channel,
        "docJAM @$username Sorry, I couldn't process your request at this time.",
      )
    }
  }

  /**
   * Handles the ";m" or ";match" command. Uses GPT music endpoint to transform the user query into
   * a song search phrase and then delegates the search & add operation to [VideoSearchService].
   *
   * @param twitchCommand The parsed Twitch command containing the raw user query.
   * @param channel The Twitch channel where the command was issued.
   * @param username The username of the user who issued the command.
   */
  private fun handleMusicMatchCommand(
    twitchCommand: TwitchCommand?,
    channel: String,
    username: String,
  ) {
    // Verify whether the user is allowed to add a new video
    if (checkAndNotifyUserCanAddVideo(username, channel)) return

    val userQuery = twitchCommand?.params?.joinToString(" ") ?: ""

    if (userQuery.isBlank()) {
      messageService.sendMessage(
        channel,
        "@$username docJAM Please provide a search query, e.g. ;m never gonna give you up",
      )
      return
    }

    val gptResult = gptService.getMusicResponse(userQuery, username)

    if (gptResult.isNullOrBlank()) {
      messageService.sendMessage(
        channel,
        "@$username docJAM Sorry, I couldn't find a matching song right now.",
      )
      return
    }

    // GPT may already return a full YouTube URL. If so, try to add directly. Otherwise use it as
    // search phrase.
    val gptTrimmed = gptResult.trim()

    val videoAdded =
      if (gptTrimmed.startsWith("http")) {
        // Wrap the URL in a TwitchCommand so existing logic can handle it uniformly
        videoSearchService.searchVideo(
          TwitchCommand("", listOf(gptTrimmed)),
          channel,
          username,
          sendMessage = true,
        )
      } else {
        videoSearchService.searchVideo(TwitchCommand("", listOf(gptTrimmed)), channel, username)
      }

    if (!videoAdded) {
      messageService.sendMessage(channel, "@$username docJAM Sorry, couldn't add the song.")
    }
  }
}
