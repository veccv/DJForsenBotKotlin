package com.github.veccvs.djforsenbotkotlin.service.command

import com.github.veccvs.djforsenbotkotlin.component.TwitchConnector
import com.github.veccvs.djforsenbotkotlin.config.UserConfig
import com.github.veccvs.djforsenbotkotlin.model.TwitchCommand
import com.github.veccvs.djforsenbotkotlin.model.User
import com.github.veccvs.djforsenbotkotlin.repository.UserRepository
import com.github.veccvs.djforsenbotkotlin.service.SpotifyService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Service responsible for handling Spotify-related commands and functionality. This includes
 * connecting to Spotify, tracking songs, and adding current songs to the playlist.
 */
@Service
class SpotifyCommandService(
  @Autowired private val messageService: MessageService,
  @Autowired private val videoSearchService: VideoSearchService,
  @Autowired private val timeRestrictionService: TimeRestrictionService,
  @Autowired private val userRepository: UserRepository,
  @Autowired private val userConfig: UserConfig,
  @Autowired private val spotifyService: SpotifyService,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(SpotifyCommandService::class.java)
  }

  private val twitchConnector = TwitchConnector()

  /**
   * Checks whether the specified user can add a video and notifies the user via the specified
   * channel if they are restricted from doing so due to cooldown limits.
   *
   * @param username The username of the user attempting to add a video.
   * @param channel The communication channel to send the restriction notification if required.
   * @return True, if the user is restricted from adding a video and a notification was sent, false
   *   otherwise.
   */
  fun checkAndNotifyUserCanAddVideo(username: String, channel: String): Boolean {
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
   * Handles the connect command for Spotify integration. Sends a link to the Spotify authentication
   * page to the user via private message.
   *
   * @param twitchCommand The command (not used)
   * @param username The username of the user
   * @param channel The channel to send the message to
   */
  fun connectSpotifyCommand(twitchCommand: TwitchCommand?, username: String, channel: String) {
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
      logger.error("[SPOTIFY COMMAND] Error sending Spotify authentication link: ${e.message}")
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
      logger.error("[SPOTIFY COMMAND] Error processing Spotify token message: ${e.message}")
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

    logger.info("[SPOTIFY COMMAND] Extracted token part: $tokenPart")

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

    logger.info("[SPOTIFY COMMAND] Parsed token map: $result")
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
  fun trackAndAddSpotifyCommand(username: String, channel: String) {
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
        "docJAM @$username You are already tracking your Spotify songs. Use ;track stop to stop tracking.",
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
  fun stopTrackingCommand(username: String, channel: String) {
    val user = userRepository.findByUsername(username)

    if (user == null) {
      messageService.sendMessage(
        channel,
        "docJAM @$username You need to connect your Spotify account first. Use ;connect to get started.",
      )
      return
    }

    // Check if the user is actually tracking
    if (!user.isTracking) {
      messageService.sendMessage(
        channel,
        "docJAM @$username You are not currently tracking your Spotify songs.",
      )
      return
    }

    // Check if the user can stop tracking (5-minute cooldown)
    if (!timeRestrictionService.canUserStopTracking(username)) {
      messageService.sendMessage(
        channel,
        "docJAM @$username You can stop tracking every 5 minutes. Time to next stop: ${
                    timeRestrictionService.timeToNextTrackStop(username)
                }",
      )
      return
    }

    // Stop tracking
    timeRestrictionService.setLastTrackStop(username)

    messageService.sendMessage(channel, "docJAM @$username Stopped tracking your Spotify songs.")
  }

  /**
   * Adds the currently playing Spotify song to the playlist if it has been playing for at least 2
   * minutes. Only adds one song, unlike trackAndAddSpotifyCommand which adds up to the configured
   * maximum number of songs.
   *
   * @param username The username of the user
   * @param channel The channel to send the message to
   */
  fun addCurrentSpotifyCommand(username: String, channel: String) {
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
