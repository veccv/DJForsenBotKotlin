package com.github.veccvs.djforsenbotkotlin.service.command

import com.github.veccvs.djforsenbotkotlin.dao.CytubeDao
import com.github.veccvs.djforsenbotkotlin.repository.GachiSongRepository
import com.github.veccvs.djforsenbotkotlin.service.SkipCounterService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Service for handling playlist-related functionality
 */
@Service
class PlaylistService(
  @Autowired private val cytubeDao: CytubeDao,
  @Autowired private val messageService: MessageService,
  @Autowired private val skipCounterService: SkipCounterService,
  @Autowired private val gachiSongRepository: GachiSongRepository
) {
  /**
   * Gets the current playlist and sends it to the user
   *
   * @param username The username of the user
   * @param channel The channel to send the message to
   */
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

    messageService.sendMessage(channel, "@$username docJAM Playlist: $videoList")
  }

  /**
   * Handles the skip command
   *
   * @param username The username of the user
   * @param channel The channel to send the message to
   * @param canSkip Whether the user can skip
   * @param skipValue The number of skips needed to skip a video
   * @param currentSkips The current number of skips
   * @param timeToNextSkip The time until the user can skip again
   */
  fun handleSkipCommand(
    username: String,
    channel: String,
    canSkip: Boolean,
    skipValue: Long,
    currentSkips: Int,
    timeToNextSkip: String
  ) {
    if (canSkip) {
      skipCounterService.incrementSkipCounter()

      if (skipCounterService.getSkipCounter() >= skipValue) {
        cytubeDao.skipVideo()
        messageService.sendMessage(channel, "docJAM skipped the video")
      } else {
        messageService.sendMessage(
          channel,
          "docJAM @${username} ${skipValue.minus(skipCounterService.getSkipCounter())} more skips needed to skip the video",
        )
      }
    } else {
      messageService.sendMessage(
        channel,
        "docJAM @${username} You can skip a video every ${skipValue} minutes, time to skip video: $timeToNextSkip",
      )
    }
  }

  /**
   * Gets a random gachi song title
   *
   * @return A random gachi song title
   */
  fun randomGachiSong(): String {
    return gachiSongRepository.findAll().random().title.orEmpty()
  }

  /**
   * Removes a video from the playlist
   *
   * @param videoUrl The URL of the video to remove
   * @return True if the video was removed successfully, false otherwise
   */
  fun removeVideo(videoUrl: String): Boolean {
    val playlist = cytubeDao.removeVideo(videoUrl)
    return playlist != null
  }
}
