package com.github.veccvs.djforsenbotkotlin.service

import StreamInfo
import com.github.veccvs.djforsenbotkotlin.component.TwitchChatBot
import com.github.veccvs.djforsenbotkotlin.config.UserConfig
import com.github.veccvs.djforsenbotkotlin.dao.CytubeDao
import com.github.veccvs.djforsenbotkotlin.model.TwitchCommand
import com.github.veccvs.djforsenbotkotlin.model.User
import com.github.veccvs.djforsenbotkotlin.model.Video
import com.github.veccvs.djforsenbotkotlin.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Service
class CommandService(
  @Autowired private val cytubeDao: CytubeDao,
  @Autowired private val twitchChatBot: TwitchChatBot,
  @Autowired private val userRepository: UserRepository,
  @Autowired private val userConfig: UserConfig,
) {
  fun sendMessage(channel: String, message: String) {
    twitchChatBot.sendMessage(channel, message)
    //        else throw HttpClientErrorException(HttpStatus.FORBIDDEN, "Bot is disabled")
  }

  fun detectCommand(message: String): String? {
    val command = message.split(" ")[0]
    return if (command.startsWith(";")) command else null
  }

  fun parseCommand(message: String): TwitchCommand? {
    val twitchCommand = TwitchCommand("", emptyList())
    message.split(" ").forEachIndexed { index, s ->
      if (index == 0) {
        twitchCommand.command = s
      } else {
        twitchCommand.params += s
      }
    }
    return twitchCommand
  }

  fun getCorrectResult(searchResult: List<Video>): Video? {
    return searchResult.parallelStream().filter { it.duration in 1..399 }.findFirst().orElse(null)
  }

  fun commandHandler(username: String, message: String, channel: String) {
    if (StreamInfo.streamEnabled() && channel == "#forsen") return

    if (message.startsWith("!forsenJAMMER")) {
      sendMessage(channel, "pepeJAM @${username} bot made by veccvs")
      return
    }

    val command = detectCommand(message)
    if (command != null) {
      userRepository.findByUsername(username) ?: userRepository.save(User(username))
      if (!canResponseToCommand(username)) return
      setLastResponse(username)
      val twitchCommand = parseCommand(message)
      when (twitchCommand?.command) {
        ";link" -> {
          sendMessage(channel, "pepeJAM 👉 @${username} cytu.be/r/forsenboys")
        }
        ";search" -> {
          searchVideo(twitchCommand, channel, username)
        }
        ";add" -> {
          addVideoWithId(twitchCommand, channel, username)
        }
        ";help" -> {
          sendMessage(channel, "pepeJAM @${username} Commands: ;link, ;search, ;add, ;help")
        }
        else -> {
          sendMessage(channel, "pepeJAM @$username Unknown command, try ;link, ;search or ;add")
        }
      }
    }
  }

  private fun setLastResponse(username: String) {
    userRepository.save(
      userRepository.findByUsername(username)?.apply {
        lastResponse = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
      } ?: User(username)
    )
  }

  private fun addVideoWithId(twitchCommand: TwitchCommand, channel: String, username: String) {
    if (canUserAddVideoHandler(username, channel)) return

    if (cytubeDao.getBotStatus() != null) {
      val videoId = twitchCommand.params[0]
      if (videoId.length != 11) {
        sendMessage(
          channel,
          "pepeJAM @${username} Invalid video id. Use it like this: ;add 1a2b3c4d5e6",
        )
        return
      }

      if (cytubeDao.getBotStatus()?.botEnabled == true) {
        cytubeDao.addVideo("https://youtu.be/${videoId}")
        updateUserAddedVideo(username)
        sendMessage(channel, "pepeJAM @${username} added video with id: $videoId")
      } else {
        sendMessage(channel, "pepeJAM @${username} Bot is resetting, wait a few seconds :)")
      }
    } else {
      sendMessage(channel, "pepeJAM @${username} Bot is resetting, wait a few seconds :)")
    }
  }

  private fun updateUserAddedVideo(username: String) {
    userRepository.save(
      userRepository.findByUsername(username)?.apply {
        lastAddedVideo = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
      } ?: User(username)
    )
  }

  private fun searchVideo(twitchCommand: TwitchCommand, channel: String, username: String) {
    if (canUserAddVideoHandler(username, channel)) return

    if (cytubeDao.getBotStatus() != null) {
      val searchPhrase = twitchCommand.params.joinToString(" ")
      val searchResult = cytubeDao.searchVideos(searchPhrase)
      val correctResult = getCorrectResult(searchResult ?: emptyList())
      if (searchResult != null && correctResult != null) {
        if (cytubeDao.getBotStatus()?.botEnabled == true) {
          cytubeDao.addVideo("https://youtu.be/${correctResult.id}")
          updateUserAddedVideo(username)
          sendMessage(
            channel,
            "pepeJAM @${username} added video: ${correctResult.title.substring(0, 30)}[...]",
          )
        } else {
          sendMessage(channel, "pepeJAM @${username} Bot is resetting, wait a few seconds :)")
        }
      } else {
        sendMessage(channel, "pepeJAM @${username} No results found")
      }
    } else {
      sendMessage(channel, "pepeJAM @${username} Bot is resetting, wait a few seconds :)")
    }
  }

  private fun canUserAddVideoHandler(username: String, channel: String): Boolean {
    if (!canUserAddVideo(username)) {
      sendMessage(
        channel,
        "pepeJAM @${username} You can add a video every ${userConfig.minutesToAddVideo} minutes. Time to add next video: ${
                timeToNextVideo(
                        username
                )
              }",
      )
      return true
    }
    return false
  }

  fun canUserAddVideo(username: String): Boolean {
    val user = userRepository.findByUsername(username) ?: return false
    val nextVideoTime = user.lastAddedVideo.plusMinutes(userConfig.minutesToAddVideo?.toLong() ?: 0)
    return nextVideoTime == null ||
      nextVideoTime.isBefore(LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()))
  }

  fun timeToNextVideo(username: String): String {
    val user = userRepository.findByUsername(username) ?: return "0"
    val nextVideoTime = user.lastAddedVideo.plusMinutes(userConfig.minutesToAddVideo?.toLong() ?: 0)
    val now = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
    val result: String
    if (nextVideoTime.isBefore(now)) {
      result = "0"
    } else {
      val duration = Duration.between(now, nextVideoTime)
      val minutes = duration.toMinutes()
      val seconds = duration.seconds % 60
      result = "${minutes}min ${seconds}sec"
    }
    return result
  }

  fun canResponseToCommand(username: String): Boolean {
    val user = userRepository.findByUsername(username) ?: return false
    val nextCommandTime =
      user.lastResponse.plusSeconds(userConfig.secondsToResponseToCommand?.toLong() ?: 0)
    return nextCommandTime == null ||
      nextCommandTime.isBefore(LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()))
  }
}
