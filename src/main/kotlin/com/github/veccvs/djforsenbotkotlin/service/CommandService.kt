package com.github.veccvs.djforsenbotkotlin.service

import com.github.veccvs.djforsenbotkotlin.component.TwitchChatBot
import com.github.veccvs.djforsenbotkotlin.component.TwitchConnector
import com.github.veccvs.djforsenbotkotlin.config.UserConfig
import com.github.veccvs.djforsenbotkotlin.dao.CytubeDao
import com.github.veccvs.djforsenbotkotlin.model.TwitchCommand
import com.github.veccvs.djforsenbotkotlin.model.User
import com.github.veccvs.djforsenbotkotlin.model.Video
import com.github.veccvs.djforsenbotkotlin.repository.UserRepository
import com.github.veccvs.djforsenbotkotlin.utils.BanPhraseChecker
import com.github.veccvs.djforsenbotkotlin.utils.StreamInfo
import java.text.Normalizer
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.regex.Pattern
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CommandService(
  @Autowired private val cytubeDao: CytubeDao,
  @Autowired private val twitchChatBot: TwitchChatBot,
  @Autowired private val userRepository: UserRepository,
  @Autowired private val userConfig: UserConfig,
  @Autowired private val songService: SongService,
  @Autowired private val skipCounterService: SkipCounterService,
) {
  private val twitchConnector = TwitchConnector()

  fun sendMessage(channel: String, message: String) {
    if (BanPhraseChecker.check(message)) {
      twitchChatBot.sendMessage(channel, "docJAM banned phrase detected")
      return
    }
    twitchChatBot.sendMessage(channel, message)
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

    if (message.startsWith("!djfors_")) {
      sendMessage(channel, "docJAM @${username} bot made by veccvs")
      return
    }

    val command = detectCommand(message)
    if (command != null) {
      userRepository.findByUsername(username) ?: userRepository.save(User(username))
      if (!canResponseToCommand(username)) return
      setLastResponse(username)
      val twitchCommand = parseCommand(message)
      when (twitchCommand?.command) {
        ";link",
        ";where" -> {
          twitchChatBot.sendMessage(
            channel,
            "docJAM @${username} I sent you a whisper with the link forsenCD ",
          )
          twitchConnector.sendWhisper(username, "https://cytu.be/r/forsenboys")
        }
        ";search",
        ";s" -> {
          searchVideo(twitchCommand, channel, username)
        }
        ";help" -> {
          sendMessage(
            channel,
            "docJAM @${username} Commands: ;link, ;where, ;search, ;s, ;help, ;playlist",
          )
        }
        ";playlist" -> {
          getPlaylist(username, channel)
        }
        ";skip" -> {
          skipCommand(username, channel)
        }
        else -> {
          sendMessage(channel, "docJAM @$username Unknown command, try ;link, ;search or ;help")
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

  private fun updateUserAddedVideo(username: String) {
    userRepository.save(
      userRepository.findByUsername(username)?.apply {
        lastAddedVideo = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
        userNotified = false
      } ?: User(username)
    )
  }

  fun normalizeText(input: String): String {
    val temp = Normalizer.normalize(input, Normalizer.Form.NFD)
    val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
    return pattern.matcher(temp).replaceAll("").replace("[^\\p{Alnum}]".toRegex(), " ")
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
          songService.addUniqueSong("https://youtu.be/${correctResult.id}")
          updateUserAddedVideo(username)
          val videoTitle = normalizeText(correctResult.title).drop(0).take(50)
          sendMessage(channel, "$username docJAM added $videoTitle [...]")
        } else {
          sendMessage(channel, "@${username} docJAM Bot is resetting, wait a few seconds :)")
        }
      } else {
        sendMessage(channel, "@${username} docJAM No results found")
      }
    } else {
      sendMessage(channel, "@${username} docJAM Bot is resetting, wait a few seconds :)")
    }
  }

  private fun canUserAddVideoHandler(username: String, channel: String): Boolean {
    if (!canUserAddVideo(username)) {
      sendMessage(
        channel,
        "docJAM @${username} You can add a video every ${userConfig.minutesToAddVideo} minutes. Time to add next video: ${
                timeToNextVideo(
                        username
                )
              }",
      )
      return true
    }
    return false
  }

  fun getPlaylist(username: String, channel: String) {
    val playlist = cytubeDao.getPlaylist()
    val videoList =
      playlist
        ?.queue
        ?.parallelStream()
        ?.limit(3)
        ?.map { it.title.drop(0).take(20) }
        ?.toList()
        ?.joinToString(", ")

    sendMessage(channel, "@$username docJAM Playlist: $videoList")
  }

  fun canUserAddVideo(username: String): Boolean {
    val user = userRepository.findByUsername(username) ?: return false
    val nextVideoTime = user.lastAddedVideo.plusMinutes(userConfig.minutesToAddVideo?.toLong() ?: 0)
    return nextVideoTime == null ||
      nextVideoTime.isBefore(LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()))
  }

  fun timeToNextVideo(username: String): String {
    val user = userRepository.findByUsername(username) ?: return "0"
    return timeToNextAction(user.lastAddedVideo, userConfig.minutesToAddVideo?.toLong() ?: 0)
  }

  fun canResponseToCommand(username: String): Boolean {
    val user = userRepository.findByUsername(username) ?: return false
    val nextCommandTime =
      user.lastResponse.plusSeconds(userConfig.secondsToResponseToCommand?.toLong() ?: 0)
    return nextCommandTime == null ||
      nextCommandTime.isBefore(LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()))
  }

  fun canUserSkipVideo(username: String): Boolean {
    val user = userRepository.findByUsername(username) ?: return false
    val skipVideoTime = user.lastSkip.plusMinutes(userConfig.minutesToSkipVideo?.toLong() ?: 0)
    return skipVideoTime == null ||
      skipVideoTime.isBefore(LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()))
  }

  fun skipCommand(username: String, channel: String) {
    if (canUserSkipVideo(username)) {
      skipCounterService.incrementSkipCounter()
      userRepository.save(
        userRepository.findByUsername(username)?.apply {
          lastSkip = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
        } ?: User(username)
      )

      if (skipCounterService.getSkipCounter() >= (userConfig.skipValue?.toLong() ?: 5)) {
        cytubeDao.skipVideo()
        sendMessage(channel, "docJAM skipped the video")
      } else {
        sendMessage(
          channel,
          "docJAM @${username} ${userConfig.skipValue?.toLong()?.minus(skipCounterService.getSkipCounter())} more skips needed to skip the video",
        )
      }
    } else {
      sendMessage(
        channel,
        "docJAM @${username} You can skip a video every ${userConfig.minutesToSkipVideo} minutes, time to skip video: ${
                timeToNextSkip(
                        username
                )
              }",
      )
    }
  }

  fun timeToNextSkip(username: String): String {
    val user = userRepository.findByUsername(username) ?: return "0"
    return timeToNextAction(user.lastSkip, userConfig.minutesToSkipVideo?.toLong() ?: 0)
  }

  fun timeToNextAction(lastActionTime: LocalDateTime, intervalMinutes: Long): String {
    val now = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
    val nextActionTime = lastActionTime.plusMinutes(intervalMinutes)
    return if (nextActionTime.isBefore(now)) {
      "0"
    } else {
      val duration = Duration.between(now, nextActionTime)
      val minutes = duration.toMinutes()
      val seconds = duration.seconds % 60
      "${minutes}min ${seconds}sec"
    }
  }
}
