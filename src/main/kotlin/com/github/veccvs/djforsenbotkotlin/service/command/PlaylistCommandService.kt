package com.github.veccvs.djforsenbotkotlin.service.command

import com.github.veccvs.djforsenbotkotlin.config.UserConfig
import com.github.veccvs.djforsenbotkotlin.repository.UserRepository
import com.github.veccvs.djforsenbotkotlin.service.SkipCounterService
import com.github.veccvs.djforsenbotkotlin.service.UserSongService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Service responsible for handling playlist-related commands and functionality. This includes
 * skipping songs, removing songs, and managing the playlist.
 */
@Service
class PlaylistCommandService(
  @Autowired private val messageService: MessageService,
  @Autowired private val timeRestrictionService: TimeRestrictionService,
  @Autowired private val userSongService: UserSongService,
  @Autowired private val playlistService: PlaylistService,
  @Autowired private val userRepository: UserRepository,
  @Autowired private val userConfig: UserConfig,
  @Autowired private val skipCounterService: SkipCounterService,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(PlaylistCommandService::class.java)
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
    val userSong = userSongService.removeRecentUserSong(username)
    if (userSong == null) {
      messageService.sendMessage(
        channel,
        "docJAM @$username You don't have any recently added songs to remove (must be within 5 minutes of adding)",
      )
      return
    }
    val songLink = userSong.song?.link
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
        "docJAM @$username Removed your song '${userSong.title}'. You can add another song now.",
      )
    } else {
      messageService.sendMessage(
        channel,
        "docJAM @$username Error removing song from playlist. Please try again.",
      )
    }
  }
}
