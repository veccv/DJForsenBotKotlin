package com.github.veccvs.djforsenbotkotlin.service

import com.github.veccvs.djforsenbotkotlin.component.TwitchConnector
import com.github.veccvs.djforsenbotkotlin.config.UserConfig
import com.github.veccvs.djforsenbotkotlin.model.TwitchCommand
import com.github.veccvs.djforsenbotkotlin.model.User
import com.github.veccvs.djforsenbotkotlin.repository.UserRepository
import com.github.veccvs.djforsenbotkotlin.service.command.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

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
      println("[COMMAND HANDLER] Detected !djfors_ command")
      messageService.sendMessage(channel, "docJAM @${username} bot made by veccvs")
      return
    }

    val command = commandParserService.detectCommand(message)

    if (command != null) {
      println("[COMMAND HANDLER] Detected command: $command")
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
      "docJAM @${username} Commands: ;link, ;where, ;search, ;s, ;help, ;playlist, ;skip, ;rg, ;when, ;undo, ;connect, ;authorize, ;add spotify, ;track spotify, ;current"
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

      ";authorize" -> {
        authorizeSpotifyCommand(twitchCommand, username, channel)
      }

      ";track" -> {
        if (twitchCommand.params.isNotEmpty() && twitchCommand.params[0] == "spotify") {
          trackAndAddSpotifyCommand(username, channel)
        } else {
          messageService.sendMessage(channel, unknownCommandMessage)
        }
      }

      ";add" -> {
        if (twitchCommand.params.isNotEmpty()) {
          when (twitchCommand.params[0]) {
            "spotify" -> addSpotifyCommand(username, channel)
            else -> messageService.sendMessage(channel, unknownCommandMessage)
          }
        } else {
          messageService.sendMessage(channel, unknownCommandMessage)
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
    if (!timeRestrictionService.canUserAddVideo(username)) {
      messageService.sendMessage(
        channel,
        "docJAM @${username} You can add a video every ${userConfig.minutesToAddVideo} minutes. Time to add next video: ${
          timeRestrictionService.timeToNextVideo(username)
        }",
      )
      return true
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
   * Handles the connect command for Spotify integration Generates an authorization URL for the user
   * to grant permissions to the application
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
      // Generate the authorization URL
      val authUrl = spotifyService.getAuthorizationUrl()

      // Send the authorization URL to the user via whisper
      twitchConnector.sendWhisper(
        username,
        "To connect your Spotify account, please visit this URL and authorize the application: $authUrl",
      )

      messageService.sendMessage(
        channel,
        "docJAM @$username I've sent you a whisper with instructions to connect your Spotify account. After authorizing, you'll need to provide the code using ;authorize <code>",
      )
    } catch (e: Exception) {
      println("[COMMAND SERVICE] Error generating Spotify authorization URL: ${e.message}")
      messageService.sendMessage(
        channel,
        "docJAM @$username Error connecting to Spotify. Please try again later.",
      )
    }
  }

  /**
   * Handles the authorize command for Spotify integration Exchanges the authorization code for
   * access and refresh tokens
   *
   * @param twitchCommand The command containing the authorization code
   * @param username The username of the user
   * @param channel The channel to send the message to
   */
  private fun authorizeSpotifyCommand(
    twitchCommand: TwitchCommand?,
    username: String,
    channel: String,
  ) {
    if (twitchCommand?.params?.isEmpty() == true) {
      messageService.sendMessage(
        channel,
        "docJAM @$username Please provide the authorization code. Usage: ;authorize <code>",
      )
      return
    }

    val authCode = twitchCommand?.params?.get(0)
    val user = userRepository.findByUsername(username)

    if (user != null && !authCode.isNullOrBlank()) {
      try {
        // Exchange the authorization code for tokens
        val tokenResponse = spotifyService.exchangeCodeForToken(authCode)

        // Store the tokens in the user record
        user.spotifyAccessToken = tokenResponse["access_token"] as String
        user.spotifyRefreshToken = tokenResponse["refresh_token"] as String
        val expiresIn = tokenResponse["expires_in"] as Long
        user.spotifyTokenExpiration = java.time.LocalDateTime.now().plusSeconds(expiresIn - 60)

        userRepository.save(user)

        messageService.sendMessage(
          channel,
          "docJAM @$username Your Spotify account has been connected successfully. You can now use ;add spotify to add your currently playing song.",
        )
      } catch (e: Exception) {
        println("[COMMAND SERVICE] Error exchanging Spotify authorization code: ${e.message}")
        messageService.sendMessage(
          channel,
          "docJAM @$username Error connecting your Spotify account. The authorization code may be invalid or expired. Please try again with ;connect",
        )
      }
    } else {
      messageService.sendMessage(
        channel,
        "docJAM @$username Error connecting your Spotify account. Please try again.",
      )
    }
  }

  /**
   * Handles the add spotify command to add the currently playing Spotify song
   *
   * @param username The username of the user
   * @param channel The channel to send the message to
   */
  private fun addSpotifyCommand(username: String, channel: String) {
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

    if (currentSong == null) {
      messageService.sendMessage(
        channel,
        "docJAM @$username No song is currently playing on your Spotify account or there was an error getting the song information.",
      )
      return
    }

    // Check if we got an error response with a new token
    if (currentSong.containsKey("error") && currentSong["error"] == "token_expired") {
      // Update the user's access token
      user.spotifyAccessToken = currentSong["new_token"]
      userRepository.save(user)

      // Try again with the new token
      return addSpotifyCommand(username, channel)
    }

    // Search for the song on YouTube
    val searchCommand = TwitchCommand("", listOf(currentSong["title"] ?: ""))
    videoSearchService.searchVideo(searchCommand, channel, username)
  }

  /**
   * Tracks the user's currently playing Spotify songs and adds them to the playlist if they've been
   * playing for at least 2 minutes. Will track up to 5 songs.
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

    messageService.sendMessage(
      channel,
      "docJAM @$username Now tracking your Spotify. Songs that play for at least 2 minutes will be added (up to 5 songs). Tracking will stop after 10 minutes if no new songs are detected.",
    )

    // Start tracking in a background thread
    Thread {
        try {
          val addedSongs =
            mutableSetOf<String>() // Keep track of added song IDs to avoid duplicates
          var lastSongTitle: String? = null
          var lastSongUrl: String? = null
          var songsAdded = 0
          var lastNewSongTime =
            System.currentTimeMillis() // Track when the last new song was detected

          // Track until 5 songs have been added or no new song for 10 minutes
          while (songsAdded < 5) {
            // Check if it's been more than 10 minutes since the last new song
            if (
              System.currentTimeMillis() - lastNewSongTime > 600000
            ) { // 10 minutes in milliseconds
              messageService.sendMessage(
                channel,
                "docJAM @$username Stopped tracking after 10 minutes with no new songs. Added $songsAdded songs to the playlist.",
              )
              return@Thread
            }

            // Get the currently playing song
            val currentSong =
              spotifyService.getCurrentlyPlayingSong(
                user.spotifyAccessToken!!,
                user.spotifyRefreshToken!!,
              )

            // Check if we got an error response with a new token
            if (
              currentSong != null &&
                currentSong.containsKey("error") &&
                currentSong["error"] == "token_expired"
            ) {
              // Update the user's access token
              user.spotifyAccessToken = currentSong["new_token"]
              userRepository.save(user)
              continue // Try again with the new token
            }

            if (currentSong != null) {
              val currentTitle = currentSong["title"]
              val currentUrl = currentSong["url"]

              // If this is a new song, start tracking it
              if (currentTitle != lastSongTitle) {
                println("[SPOTIFY TRACKER] New song detected: $currentTitle")
                lastSongTitle = currentTitle
                lastSongUrl = currentUrl
                lastNewSongTime = System.currentTimeMillis() // Update the last new song time

                // Wait for 2 minutes
                Thread.sleep(120000)

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
                  // Update the user's access token
                  user.spotifyAccessToken = checkSong["new_token"]
                  userRepository.save(user)
                  continue // Try again with the new token
                }

                if (checkSong != null && checkSong["title"] == lastSongTitle) {
                  // The song has been playing for at least 2 minutes
                  // Check if we've already added this song
                  if (lastSongUrl != null && !addedSongs.contains(lastSongUrl)) {
                    // Add the song to the playlist
                    val searchCommand = TwitchCommand("", listOf(lastSongTitle ?: ""))
                    if (videoSearchService.searchVideo(searchCommand, channel, username)) {
                      addedSongs.add(lastSongUrl)
                      songsAdded++
                      messageService.sendMessage(
                        channel,
                        "docJAM @$username Added song $songsAdded/5: $lastSongTitle",
                      )
                    }
                  }
                }
              }
            }

            // Wait a bit before checking again
            Thread.sleep(10000) // Check every 10 seconds
          }

          messageService.sendMessage(
            channel,
            "docJAM @$username Finished tracking. Added 5 songs to the playlist.",
          )
        } catch (e: Exception) {
          println("[SPOTIFY TRACKER] Error tracking songs: ${e.message}")
          messageService.sendMessage(
            channel,
            "docJAM @$username Error tracking your Spotify songs. Please try again.",
          )
        }
      }
      .start()
  }

  /**
   * Adds the currently playing Spotify song to the playlist if it has been playing for at least 2
   * minutes. Only adds one song, unlike trackAndAddSpotifyCommand which adds up to 5 songs.
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
          // Wait for 2 minutes
          Thread.sleep(120000)

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
              // The song has been playing for at least 2 minutes
              // Add the song to the playlist
              val searchCommand = TwitchCommand("", listOf(songTitle ?: ""))
              if (videoSearchService.searchVideo(searchCommand, channel, username)) {
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
            // The song has been playing for at least 2 minutes
            // Add the song to the playlist
            val searchCommand = TwitchCommand("", listOf(songTitle ?: ""))
            if (videoSearchService.searchVideo(searchCommand, channel, username)) {
              messageService.sendMessage(channel, "docJAM @$username Added song: $songTitle")
            }
          } else {
            messageService.sendMessage(
              channel,
              "docJAM @$username The song \"$songTitle\" is no longer playing. You need to listen to the same song for at least 2 minutes.",
            )
          }
        } catch (e: Exception) {
          println("[SPOTIFY CURRENT] Error adding current song: ${e.message}")
          messageService.sendMessage(
            channel,
            "docJAM @$username Error adding your current Spotify song. Please try again.",
          )
        }
      }
      .start()
  }
}
