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
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

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
    // Only log messages that are bot commands
    if (message.startsWith("!djfors_") || message.startsWith(";")) {
      println("[COMMAND HANDLER] Processing message from $username in $channel: $message")
    }

    if (message.startsWith("!djfors_")) {
      logger.info("[COMMAND HANDLER] Detected !djfors_ command")
      messageService.sendMessage(channel, "docJAM @${username} bot made by veccvs")
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
      "docJAM @${username} Commands: ;link, ;where, ;search, ;s, ;help, ;playlist, ;skip, ;rg, ;when, ;undo, ;connect, ;track, ;track stop, ;current"
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
        skipCommand(username, channel)
      }

      ";when" -> {
        userSongFormatterService.getUserSongs(username, channel)
      }

      ";undo" -> {
        removeSongCommand(username, channel)
      }

      ";connect" -> {
        connectSpotifyCommand(twitchCommand, username, channel)
      }

      ";track" -> {
        if (twitchCommand.params.isNotEmpty() && twitchCommand.params[0] == "stop") {
          stopTrackingCommand(username, channel)
        } else {
          trackAndAddSpotifyCommand(username, channel)
        }
      }

      ";current" -> {
        addCurrentSpotifyCommand(username, channel)
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
   * Handles the skip command for a given user in a specific channel. Determines if the user is
   * allowed to skip, calculates the skip-related parameters, and invokes the playlist service to
   * process the skip.
   *
   * @param username The username of the user attempting to issue the skip command.
   * @param channel The channel where the skip command is being executed.
   */
  fun skipCommand(username: String, channel: String) {
    val canSkip = timeRestrictionService.canUserSkipVideo(username)
    val skipValue = userConfig.skipValue?.toLong() ?: 5
    skipCounterService.getSkipCounter()
    val timeToNextSkip = timeRestrictionService.timeToNextSkip(username)
    if (canSkip) {
      timeRestrictionService.setLastSkip(username)
    }
    playlistService.handleSkipCommand(username, channel, canSkip, skipValue, timeToNextSkip)
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
    if (!timeRestrictionService.canUserRemoveVideo(username)) {
      messageService.sendMessage(
        channel,
        "docJAM @$username You can remove a video every 5 minutes. Time to next removal: ${
          timeRestrictionService.timeToNextRemoval(username)
        }",
      )
      return
    }
    val song = userSongService.removeRecentUserSong(username)
    if (song == null) {
      messageService.sendMessage(
        channel,
        "docJAM @$username You don't have any recently added songs to remove (must be within 5 minutes of adding)",
      )
      return
    }
    val songLink = song.song?.link
    if (songLink == null) {
      messageService.sendMessage(
        channel,
        "docJAM @$username Error removing song. Please try again.",
      )
      return
    }
    val success = playlistService.removeVideo(songLink)
    if (success) {
      timeRestrictionService.resetVideoCooldown(username)
      timeRestrictionService.setLastRemoval(username)
      // Set userNotified to true to prevent notification
      val user = userRepository.findByUsername(username)
      if (user != null) {
        user.userNotified = true
        userRepository.save(user)
      }
      messageService.sendMessage(
        channel,
        "docJAM @$username Removed your song '${song.title}'. You can add another song now.",
      )
    } else {
      messageService.sendMessage(
        channel,
        "docJAM @$username Error removing song from playlist. Please try again.",
      )
    }
  }

  /**
   * Handles the connect command for Spotify integration. Sends a link to the Spotify authentication
   * page to the user via private message.
   *
   * @param twitchCommand The command (not used)
   * @param username The username of the user
   * @param channel The channel to send the message to
   */
  private fun connectSpotifyCommand(
    twitchCommand: TwitchCommand?,
    username: String,
    channel: String,
  ) {
    try {
      // Send the Spotify auth link to the user via whisper
      val authUrl = "https://spotify-auth-lilac.vercel.app/"
      twitchConnector.sendWhisper(
        username,
        "To connect your Spotify account, please visit this URL: $authUrl and authorize the application. After authorizing, send me the copied token",
      )

      messageService.sendMessage(
        channel,
        "docJAM @$username I've sent you a whisper with instructions to connect your Spotify account.",
      )
    } catch (e: Exception) {
      logger.error("[COMMAND SERVICE] Error sending Spotify authentication link: ${e.message}")
      messageService.sendMessage(
        channel,
        "docJAM @$username Error connecting to Spotify. Please try again later.",
      )
    }
  }

  /**
   * Processes a Spotify token message from a whisper and updates the user's account
   *
   * @param username The username of the user
   * @param tokenMessage The token message from the whisper
   * @return True if the tokens were successfully processed and saved, false otherwise
   */
  fun processSpotifyTokenMessage(username: String, tokenMessage: String): Boolean {
    try {
      val tokenMap = parseTokenResponse(tokenMessage)

      if (tokenMap.containsKey("token") && tokenMap.containsKey("refreshToken")) {
        val user = userRepository.findByUsername(username) ?: userRepository.save(User(username))

        // Update the user's Spotify tokens
        user.spotifyAccessToken = tokenMap["token"]
        user.spotifyRefreshToken = tokenMap["refreshToken"]
        // Set a default expiration time (1 hour)
        user.spotifyTokenExpiration = LocalDateTime.now().plusMinutes(5)

        userRepository.save(user)
        return true
      }
    } catch (e: Exception) {
      logger.error("[COMMAND SERVICE] Error processing Spotify token message: ${e.message}")
    }
    return false
  }

  /**
   * Parses the token response from the user in the format "token=value;refreshToken=value;" The
   * response might be embedded in a full Twitch whisper message.
   *
   * @param tokenResponse The token response string
   * @return A map containing the parsed token values
   */
  fun parseTokenResponse(tokenResponse: String): Map<String, String> {
    val result = mutableMapOf<String, String>()

    // Extract the actual token part from the message
    // The token part comes after "WHISPER djfors_ :" in a Twitch whisper
    val tokenPart =
      if (tokenResponse.contains("WHISPER djfors_")) {
        val parts = tokenResponse.split("WHISPER djfors_ :")
        if (parts.size > 1) parts[1] else tokenResponse
      } else {
        tokenResponse
      }

    logger.info("[COMMAND SERVICE] Extracted token part: $tokenPart")

    // Split by semicolon to get individual key-value pairs
    val pairs = tokenPart.split(";")

    for (pair in pairs) {
      // Split each pair by equals sign to get key and value
      val keyValue = pair.trim().split("=", limit = 2)
      if (keyValue.size == 2) {
        val key = keyValue[0].trim()
        val value = keyValue[1].trim()
        if (key.isNotBlank() && value.isNotBlank()) {
          result[key] = value
        }
      }
    }

    logger.info("[COMMAND SERVICE] Parsed token map: $result")
    return result
  }

  /**
   * Tracks the user's currently playing Spotify songs. The first song is added immediately, while
   * subsequent songs are added when the user is eligible to add them based on time restrictions.
   * Will track up to the configured maximum number of songs total.
   *
   * @param username The username of the user
   * @param channel The channel to send the message to
   */
  private fun trackAndAddSpotifyCommand(username: String, channel: String) {
    if (checkAndNotifyUserCanAddVideo(username, channel)) return

    val user = userRepository.findByUsername(username)

    if (user?.spotifyAccessToken == null || user.spotifyRefreshToken == null) {
      messageService.sendMessage(
        channel,
        "docJAM @$username You need to connect your Spotify account first. Use ;connect to get started.",
      )
      return
    }

    // Check if the user is already tracking
    if (user != null && user.isTracking) {
      messageService.sendMessage(
        channel,
        "docJAM @$username You are already tracking your Spotify songs. Use ;track stop to stop tracking."
      )
      return
    }

    // Set userNotified to true to prevent notification and set isTracking to true
    if (user != null) {
      user.userNotified = true
      user.isTracking = true
      userRepository.save(user)
    }

    messageService.sendMessage(
      channel,
      "docJAM @$username Now tracking your Spotify. Songs will be added when you're eligible to add them (up to ${userConfig.maxTrackedSongs} songs total).",
    )

    // Start tracking in a background thread
    Thread {
        try {
          val addedSongs =
            mutableSetOf<String>() // Keep track of added song IDs to avoid duplicates
          var lastSongTitle: String? = null
          var lastSongUrl: String? = null
          var songsAdded = 0
          var isFirstSong = true // Flag to track if it's the first song
          var lastNewSongTime =
            System.currentTimeMillis() // Track when the last new song was detected
          mutableListOf<Pair<String, String?>>() // Pairs of (title, url)

          // Track until the configured maximum number of songs have been added or no new song for
          // 10 minutes
          while (songsAdded < userConfig.maxTrackedSongs) {
            // Check if it's been more than 10 minutes since the last new song
            if (
              System.currentTimeMillis() - lastNewSongTime > 600000
            ) { // 10 minutes in milliseconds
              // Update user to set isTracking to false
              val updatedUser = userRepository.findByUsername(username)
              if (updatedUser != null) {
                updatedUser.isTracking = false
                userRepository.save(updatedUser)
              }

              messageService.sendMessage(
                channel,
                "docJAM @$username Stopped tracking after 10 minutes with no new songs. Added $songsAdded songs to the playlist.",
              )
              return@Thread
            }

            // We don't use pending songs anymore - we try to add songs immediately when detected

            // Get the latest user data to ensure we have the most up-to-date tokens
            val updatedUser = userRepository.findByUsername(username)
            if (
              updatedUser == null ||
                updatedUser.spotifyAccessToken == null ||
                updatedUser.spotifyRefreshToken == null
            ) {
              logger.warn("[SPOTIFY TRACKER] User data or tokens missing for $username")
              messageService.sendMessage(
                channel,
                "docJAM @$username Error tracking your Spotify songs: account data missing. Please try again.",
              )
              return@Thread
            }

            // Get the currently playing song
            val currentSong =
              try {
                spotifyService.getCurrentlyPlayingSong(
                  updatedUser.spotifyAccessToken!!,
                  updatedUser.spotifyRefreshToken!!,
                )
              } catch (e: Exception) {
                logger.error("[SPOTIFY TRACKER] Error getting current song: ${e.message}")
                null
              }

            // Check if we got an error response with a new token
            if (
              currentSong != null &&
                currentSong.containsKey("error") &&
                currentSong["error"] == "token_expired"
            ) {
              // Update the user's access token
              logger.info("[SPOTIFY TRACKER] Refreshing token for $username")
              updatedUser.spotifyAccessToken = currentSong["new_token"]
              userRepository.save(updatedUser)

              // Short sleep to avoid hammering the API
              try {
                Thread.sleep(20000)
              } catch (ie: InterruptedException) {
                logger.warn("[SPOTIFY TRACKER] Thread interrupted during token refresh sleep")
                Thread.currentThread().interrupt() // Preserve interrupt status
              }

              continue // Try again with the new token
            }

            if (currentSong != null) {
              val currentTitle = currentSong["title"]
              val currentUrl = currentSong["url"]

              // If this is a new song, start tracking it internally (don't announce to user yet)
              if (currentTitle != null && currentTitle != lastSongTitle) {
                if (!timeRestrictionService.canUserAddVideo(username)) {
                  continue
                }

                lastSongTitle = currentTitle
                lastSongUrl = currentUrl
                lastNewSongTime = System.currentTimeMillis() // Update the last new song time

                // This way we can try to add the song while the user is still listening to it
                if (isFirstSong) {
                  isFirstSong = false // Mark that we've processed the first song
                  logger.info("[SPOTIFY TRACKER] First song detected: $currentTitle")
                }

                // Get the latest user data again before checking the song
                val latestUser = userRepository.findByUsername(username)
                if (
                  latestUser == null ||
                    latestUser.spotifyAccessToken == null ||
                    latestUser.spotifyRefreshToken == null
                ) {
                  logger.warn(
                    "[SPOTIFY TRACKER] User data or tokens missing for $username during song check"
                  )
                  continue // Skip this iteration and try again
                }

                // Check if the same song is still playing
                val checkSong =
                  try {
                    spotifyService.getCurrentlyPlayingSong(
                      latestUser.spotifyAccessToken!!,
                      latestUser.spotifyRefreshToken!!,
                    )
                  } catch (e: Exception) {
                    logger.error("[SPOTIFY TRACKER] Error checking current song: ${e.message}")
                    null
                  }

                // Check if we got an error response with a new token
                if (
                  checkSong != null &&
                    checkSong.containsKey("error") &&
                    checkSong["error"] == "token_expired"
                ) {
                  // Update the user's access token
                  logger.info("[SPOTIFY TRACKER] Refreshing token during song check for $username")
                  latestUser.spotifyAccessToken = checkSong["new_token"]
                  userRepository.save(latestUser)

                  // Short sleep to avoid hammering the API
                  try {
                    Thread.sleep(1000)
                  } catch (ie: InterruptedException) {
                    logger.warn("[SPOTIFY TRACKER] Thread interrupted during token refresh sleep")
                    Thread.currentThread().interrupt() // Preserve interrupt status
                  }

                  continue // Try again with the new token
                }

                if (checkSong != null && checkSong["title"] == lastSongTitle) {
                  // The song is still playing and user can add it - now we can officially detect it
                  logger.info("[SPOTIFY TRACKER] New song detected: $lastSongTitle")

                  // Check if we've already added this song
                  if (lastSongUrl != null && !addedSongs.contains(lastSongUrl)) {
                    // Add the song to the playlist
                    logger.info("[SPOTIFY TRACKER] Attempting to add song: $lastSongTitle")
                    val searchCommand = TwitchCommand("", listOf(lastSongTitle ?: ""))
                    try {
                      val added =
                        videoSearchService.searchVideo(
                          searchCommand,
                          channel,
                          username,
                          false,
                          false,
                        )
                      if (added) {
                        addedSongs.add(lastSongUrl)
                        songsAdded++
                        logger.info(
                          "[SPOTIFY TRACKER] Successfully added song $songsAdded/${userConfig.maxTrackedSongs}: $lastSongTitle"
                        )
                        // Update the lastAddedVideo timestamp to reset the cooldown
                        val user = userRepository.findByUsername(username)
                        if (user != null) {
                          user.lastAddedVideo =
                            LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
                          userRepository.save(user)
                        }
                        messageService.sendMessage(
                          channel,
                          "docJAM @$username Spotify tracker added song $songsAdded/${userConfig.maxTrackedSongs}: $lastSongTitle",
                        )
                      } else {
                        logger.warn("[SPOTIFY TRACKER] Failed to add song: $lastSongTitle")
                      }
                    } catch (e: Exception) {
                      logger.error("[SPOTIFY TRACKER] Error adding song: ${e.message}")
                    }
                  } else {
                    logger.info(
                      "[SPOTIFY TRACKER] Song already added or URL is null: $lastSongTitle"
                    )
                  }
                } else {
                  // Even if the user skipped the song, we'll still try to add it
                  // This way we can add songs that the user only listened to for a short time
                  logger.info(
                    "[SPOTIFY TRACKER] User skipped the song or not playing anymore, but we'll still try to add it: $lastSongTitle"
                  )

                  // Check if we've already added this song
                  if (lastSongUrl != null && !addedSongs.contains(lastSongUrl)) {
                    // Add the song to the playlist
                    logger.info("[SPOTIFY TRACKER] Attempting to add skipped song: $lastSongTitle")
                    val searchCommand = TwitchCommand("", listOf(lastSongTitle ?: ""))
                    try {
                      val added =
                        videoSearchService.searchVideo(
                          searchCommand,
                          channel,
                          username,
                          false,
                          false,
                        )
                      if (added) {
                        addedSongs.add(lastSongUrl)
                        songsAdded++
                        logger.info(
                          "[SPOTIFY TRACKER] Successfully added skipped song $songsAdded/${userConfig.maxTrackedSongs}: $lastSongTitle"
                        )
                        // Update the lastAddedVideo timestamp to reset the cooldown
                        val user = userRepository.findByUsername(username)
                        if (user != null) {
                          user.lastAddedVideo =
                            LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
                          userRepository.save(user)
                        }
                        messageService.sendMessage(
                          channel,
                          "docJAM @$username Spotify tracker added song $songsAdded/${userConfig.maxTrackedSongs}: $lastSongTitle",
                        )
                      } else {
                        logger.warn("[SPOTIFY TRACKER] Failed to add skipped song: $lastSongTitle")
                      }
                    } catch (e: Exception) {
                      logger.error("[SPOTIFY TRACKER] Error adding skipped song: ${e.message}")
                    }
                  } else {
                    logger.info(
                      "[SPOTIFY TRACKER] Skipped song already added or URL is null: $lastSongTitle"
                    )
                  }
                }
              }
            } else {
              logger.info("[SPOTIFY TRACKER] No song currently playing or error getting song info")
            }

            // Wait a bit before checking again
            try {
              Thread.sleep(10000) // Check every 10 seconds
            } catch (ie: InterruptedException) {
              logger.warn("[SPOTIFY TRACKER] Thread interrupted during 10-second wait")
              Thread.currentThread().interrupt() // Preserve interrupt status
            }
          }

          // Update user to set isTracking to false
          val updatedUser = userRepository.findByUsername(username)
          if (updatedUser != null) {
            updatedUser.isTracking = false
            userRepository.save(updatedUser)
          }

          messageService.sendMessage(
            channel,
            "docJAM @$username Finished tracking. Added $songsAdded songs to the playlist.",
          )
        } catch (e: Exception) {
          logger.error("[SPOTIFY TRACKER] Error tracking songs: ${e.message}", e)

          // Update user to set isTracking to false
          val updatedUser = userRepository.findByUsername(username)
          if (updatedUser != null) {
            updatedUser.isTracking = false
            userRepository.save(updatedUser)
          }

          messageService.sendMessage(
            channel,
            "docJAM @$username Error tracking your Spotify songs. Please try again.",
          )
        }
      }
      .start()
  }

  /**
   * Stops tracking the user's Spotify songs. Checks if the user can stop tracking based on the
   * 5-minute cooldown period to prevent abuse.
   *
   * @param username The username of the user
   * @param channel The channel to send the message to
   */
  private fun stopTrackingCommand(username: String, channel: String) {
    val user = userRepository.findByUsername(username)

    if (user == null) {
      messageService.sendMessage(
        channel,
        "docJAM @$username You need to connect your Spotify account first. Use ;connect to get started."
      )
      return
    }

    // Check if the user is actually tracking
    if (!user.isTracking) {
      messageService.sendMessage(
        channel,
        "docJAM @$username You are not currently tracking your Spotify songs."
      )
      return
    }

    // Check if the user can stop tracking (5-minute cooldown)
    if (!timeRestrictionService.canUserStopTracking(username)) {
      messageService.sendMessage(
        channel,
        "docJAM @$username You can stop tracking every 5 minutes. Time to next stop: ${
          timeRestrictionService.timeToNextTrackStop(username)
        }"
      )
      return
    }

    // Stop tracking
    timeRestrictionService.setLastTrackStop(username)

    messageService.sendMessage(
      channel,
      "docJAM @$username Stopped tracking your Spotify songs."
    )
  }

  /**
   * Adds the currently playing Spotify song to the playlist if it has been playing for at least 2
   * minutes. Only adds one song, unlike trackAndAddSpotifyCommand which adds up to the configured
   * maximum number of songs.
   *
   * @param username The username of the user
   * @param channel The channel to send the message to
   */
  private fun addCurrentSpotifyCommand(username: String, channel: String) {
    if (checkAndNotifyUserCanAddVideo(username, channel)) return

    val user = userRepository.findByUsername(username)

    if (user?.spotifyAccessToken == null || user.spotifyRefreshToken == null) {
      messageService.sendMessage(
        channel,
        "docJAM @$username You need to connect your Spotify account first. Use ;connect to get started.",
      )
      return
    }

    val currentSong =
      spotifyService.getCurrentlyPlayingSong(user.spotifyAccessToken!!, user.spotifyRefreshToken!!)

    // Check if we got an error response with a new token
    if (
      currentSong != null &&
        currentSong.containsKey("error") &&
        currentSong["error"] == "token_expired"
    ) {
      // Update the user's access token
      user.spotifyAccessToken = currentSong["new_token"]
      userRepository.save(user)

      // Try again with the new token
      return addCurrentSpotifyCommand(username, channel)
    }

    if (currentSong == null) {
      messageService.sendMessage(
        channel,
        "docJAM @$username No song is currently playing on your Spotify account or there was an error getting the song information.",
      )
      return
    }

    val songTitle = currentSong["title"]

    // Start tracking in a background thread
    Thread {
        try {
          // Check if the same song is still playing
          val checkSong =
            spotifyService.getCurrentlyPlayingSong(
              user.spotifyAccessToken!!,
              user.spotifyRefreshToken!!,
            )

          // Check if we got an error response with a new token
          if (
            checkSong != null &&
              checkSong.containsKey("error") &&
              checkSong["error"] == "token_expired"
          ) {
            // Update the user's access token in the main thread
            synchronized(user) {
              user.spotifyAccessToken = checkSong["new_token"]
              userRepository.save(user)
            }

            // Try again with the new token
            val retryCheckSong =
              spotifyService.getCurrentlyPlayingSong(
                user.spotifyAccessToken!!,
                user.spotifyRefreshToken!!,
              )

            if (retryCheckSong != null && retryCheckSong["title"] == songTitle) {
              val searchCommand = TwitchCommand("", listOf(songTitle ?: ""))
              if (videoSearchService.searchVideo(searchCommand, channel, username, false, false)) {
                messageService.sendMessage(channel, "docJAM @$username Added song: $songTitle")
              }
            } else {
              messageService.sendMessage(
                channel,
                "docJAM @$username The song \"$songTitle\" is no longer playing. You need to listen to the same song for at least 2 minutes.",
              )
            }
            return@Thread
          }

          if (checkSong != null && checkSong["title"] == songTitle) {
            val searchCommand = TwitchCommand("", listOf(songTitle ?: ""))
            if (videoSearchService.searchVideo(searchCommand, channel, username, false, false)) {
              messageService.sendMessage(channel, "docJAM @$username Added song: $songTitle")
            }
          } else {
            messageService.sendMessage(
              channel,
              "docJAM @$username The song \"$songTitle\" is no longer playing. You need to listen to the same song for at least 2 minutes.",
            )
          }
        } catch (e: Exception) {
          logger.error("[SPOTIFY CURRENT] Error adding current song: ${e.message}", e)
          messageService.sendMessage(
            channel,
            "docJAM @$username Error adding your current Spotify song. Please try again.",
          )
        }
      }
      .start()
  }
}
