package com.github.veccvs.djforsenbotkotlin.service.command

import com.github.veccvs.djforsenbotkotlin.dao.CytubeDao
import com.github.veccvs.djforsenbotkotlin.model.TwitchCommand
import com.github.veccvs.djforsenbotkotlin.model.User
import com.github.veccvs.djforsenbotkotlin.model.Video
import com.github.veccvs.djforsenbotkotlin.repository.UserRepository
import com.github.veccvs.djforsenbotkotlin.service.SongService
import com.github.veccvs.djforsenbotkotlin.service.UserSongService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.regex.Pattern

/** Service for handling video search functionality */
@Service
class VideoSearchService(
  @Autowired private val cytubeDao: CytubeDao,
  @Autowired private val songService: SongService,
  @Autowired private val userRepository: UserRepository,
  @Autowired private val userSongService: UserSongService,
  @Autowired private val messageService: MessageService,
) {
  /**
   * Gets the correct result from a list of videos based on duration
   *
   * @param searchResult The list of videos to search through
   * @return The first video with a duration between 1 and 399 seconds, or null if none found
   */
  fun getCorrectResult(searchResult: List<Video>): Video? {
    return searchResult.parallelStream().filter { it.duration in 1..399 }.findFirst().orElse(null)
  }

  /**
   * Normalizes text by removing diacritical marks and replacing non-alphanumeric characters with
   * spaces
   *
   * @param input The text to normalize
   * @return The normalized text
   */
  fun normalizeText(input: String): String {
    val temp = Normalizer.normalize(input, Normalizer.Form.NFD)
    val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
    return pattern.matcher(temp).replaceAll("").replace("[^\\p{Alnum}]".toRegex(), " ")
  }

  /**
   * Searches for a video and adds it to the playlist if found
   *
   * @param twitchCommand The command containing the search parameters
   * @param channel The channel to send the message to
   * @param username The username of the user who sent the command
   * @param sendMessage Whether to send a message about the added video (default: true)
   * @param updateNotificationFlag Whether to update the userNotified flag (default: true)
   * @return True if the search was successful, false otherwise
   */
  fun searchVideo(
    twitchCommand: TwitchCommand,
    channel: String,
    username: String,
    sendMessage: Boolean = true,
    updateNotificationFlag: Boolean = true,
  ): Boolean {
    if (cytubeDao.getBotStatus() != null) {
      val searchPhrase = twitchCommand.params.joinToString(" ")
      val searchResult = cytubeDao.searchVideos(searchPhrase)
      val correctResult = getCorrectResult(searchResult ?: emptyList())
      if (searchResult != null && correctResult != null) {
        if (cytubeDao.getBotStatus()?.botEnabled == true) {
          val videoUrl = "https://youtu.be/${correctResult.id}"
          cytubeDao.addVideo(videoUrl)
          songService.addUniqueSong(videoUrl)
          updateUserAddedVideo(username, updateNotificationFlag)

          // Store the song in the UserSong entity
          val videoTitle = normalizeText(correctResult.title).drop(0).take(50)
          userSongService.addUserSong(username, videoUrl, videoTitle)

          if (sendMessage) {
            messageService.sendMessage(channel, "$username docJAM added $videoTitle [...]")
          }
          return true
        } else {
          messageService.sendMessage(
            channel,
            "@${username} docJAM Bot is resetting, wait a few seconds :)",
          )
        }
      } else {
        updateUserAddedVideo(username, updateNotificationFlag)
        messageService.sendMessage(channel, "@${username} docJAM No results found")
      }
    } else {
      messageService.sendMessage(
        channel,
        "@${username} docJAM Bot is resetting, wait a few seconds :)",
      )
    }
    return false
  }

  /**
   * Updates the user's last added video timestamp
   *
   * @param username The username of the user
   * @param updateNotificationFlag Whether to update the userNotified flag (default: true)
   */
  private fun updateUserAddedVideo(username: String, updateNotificationFlag: Boolean = true) {
    userRepository.save(
      userRepository.findByUsername(username)?.apply {
        lastAddedVideo = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
        // Only update the userNotified flag if updateNotificationFlag is true
        if (updateNotificationFlag) {
          userNotified = false
        }
      } ?: User(username)
    )
  }
}
