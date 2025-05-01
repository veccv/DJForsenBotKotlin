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
 * Main service for handling Twitch commands Delegates to specialized services for specific
 * functionality
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
) {
  private val twitchConnector = TwitchConnector()

  /** Delegates to MessageService.sendMessage */
  fun sendMessage(channel: String, message: String) {
    messageService.sendMessage(channel, message)
  }

  /** Main command handler that processes user commands */
  fun commandHandler(username: String, message: String, channel: String) {
    if (message.startsWith("!djfors_")) {
      messageService.sendMessage(channel, "docJAM @${username} bot made by veccvs")
      return
    }

    val command = commandParserService.detectCommand(message)
    if (command != null) {
      // Create user if not exists
      userRepository.findByUsername(username) ?: userRepository.save(User(username))

      // Check if user can respond to command
      if (!timeRestrictionService.canResponseToCommand(username)) return

      // Update last response time
      timeRestrictionService.setLastResponse(username)

      // Parse command and handle it
      val twitchCommand = commandParserService.parseCommand(message)
      when (twitchCommand?.command) {
        ";link",
        ";where" -> {
          messageService.sendMessage(
            channel,
            "docJAM @${username} I sent you a whisper with the link forsenCD ",
          )
          twitchConnector.sendWhisper(username, "https://cytu.be/r/forsenboys")
        }
        ";search",
        ";s" -> {
          handleSearchCommand(twitchCommand, channel, username)
        }
        ";rg" -> {
          val randomGachiSong = playlistService.randomGachiSong()
          val gachiCommand = TwitchCommand("", listOf(randomGachiSong))
          handleSearchCommand(gachiCommand, channel, username)
        }
        ";help" -> {
          messageService.sendMessage(
            channel,
            "docJAM @${username} Commands: ;link, ;where, ;search, ;s, ;help, ;playlist, ;skip, ;rg, ;mysongs",
          )
        }
        ";playlist" -> {
          playlistService.getPlaylist(username, channel)
        }
        ";skip" -> {
          skipCommand(username, channel)
        }
        ";mysongs" -> {
          userSongFormatterService.getUserSongs(username, channel)
        }
        else -> {
          messageService.sendMessage(
            channel,
            "docJAM @$username Unknown command, try ;link, ;search or ;help",
          )
        }
      }
    }
  }

  /** Handles the search command */
  private fun handleSearchCommand(twitchCommand: TwitchCommand, channel: String, username: String) {
    if (canUserAddVideoHandler(username, channel)) return
    videoSearchService.searchVideo(twitchCommand, channel, username)
  }

  /** Checks if a user can add a video and sends a message if they can't */
  private fun canUserAddVideoHandler(username: String, channel: String): Boolean {
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

  /** Handles the skip command */
  fun skipCommand(username: String, channel: String) {
    val canSkip = timeRestrictionService.canUserSkipVideo(username)
    val skipValue = userConfig.skipValue?.toLong() ?: 5
    val currentSkips = skipCounterService.getSkipCounter()
    val timeToNextSkip = timeRestrictionService.timeToNextSkip(username)

    if (canSkip) {
      timeRestrictionService.setLastSkip(username)
    }

    playlistService.handleSkipCommand(
      username,
      channel,
      canSkip,
      skipValue,
      currentSkips,
      timeToNextSkip,
    )
  }
}
