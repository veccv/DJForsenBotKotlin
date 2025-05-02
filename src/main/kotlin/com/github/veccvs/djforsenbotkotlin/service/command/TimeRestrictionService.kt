package com.github.veccvs.djforsenbotkotlin.service.command

import com.github.veccvs.djforsenbotkotlin.config.UserConfig
import com.github.veccvs.djforsenbotkotlin.model.User
import com.github.veccvs.djforsenbotkotlin.repository.UserRepository
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Service for handling user time restrictions
 */
@Service
class TimeRestrictionService(
  @Autowired private val userRepository: UserRepository,
  @Autowired private val userConfig: UserConfig
) {
  enum class TimeUnit {
    MINUTES, SECONDS
  }

  /**
   * Generic method to check if a user can perform a time-restricted action
   *
   * @param username The username of the user
   * @param lastActionTime Function to get the last action time from the user
   * @param intervalValue The interval value to add to the last action time
   * @param timeUnit The time unit to use (minutes or seconds)
   * @return True if the user can perform the action, false otherwise
   */
  fun canPerformTimeRestrictedAction(
    username: String,
    lastActionTime: (User) -> LocalDateTime,
    intervalValue: Long,
    timeUnit: TimeUnit
  ): Boolean {
    val user = userRepository.findByUsername(username) ?: return false
    val nextActionTime = when (timeUnit) {
      TimeUnit.MINUTES -> lastActionTime(user).plusMinutes(intervalValue)
      TimeUnit.SECONDS -> lastActionTime(user).plusSeconds(intervalValue)
    }
    return nextActionTime == null ||
      nextActionTime.isBefore(LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()))
  }

  /**
   * Checks if a user can add a video
   *
   * @param username The username of the user
   * @return True if the user can add a video, false otherwise
   */
  fun canUserAddVideo(username: String): Boolean {
    return canPerformTimeRestrictedAction(
      username,
      { it.lastAddedVideo },
      userConfig.minutesToAddVideo?.toLong() ?: 0,
      TimeUnit.MINUTES
    )
  }

  /**
   * Gets the time until a user can add a video
   *
   * @param username The username of the user
   * @return The time until the user can add a video
   */
  fun timeToNextVideo(username: String): String {
    val user = userRepository.findByUsername(username) ?: return "0"
    return timeToNextAction(user.lastAddedVideo, userConfig.minutesToAddVideo?.toLong() ?: 0)
  }

  /**
   * Checks if a user can respond to a command
   *
   * @param username The username of the user
   * @return True if the user can respond to a command, false otherwise
   */
  fun canResponseToCommand(username: String): Boolean {
    return canPerformTimeRestrictedAction(
      username,
      { it.lastResponse },
      userConfig.secondsToResponseToCommand?.toLong() ?: 0,
      TimeUnit.SECONDS
    )
  }

  /**
   * Updates the user's last response timestamp
   *
   * @param username The username of the user
   */
  fun setLastResponse(username: String) {
    userRepository.save(
      userRepository.findByUsername(username)?.apply {
        lastResponse = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
      } ?: User(username)
    )
  }

  /**
   * Checks if a user can skip a video
   *
   * @param username The username of the user
   * @return True if the user can skip a video, false otherwise
   */
  fun canUserSkipVideo(username: String): Boolean {
    return canPerformTimeRestrictedAction(
      username,
      { it.lastSkip },
      userConfig.minutesToSkipVideo?.toLong() ?: 0,
      TimeUnit.MINUTES
    )
  }

  /**
   * Gets the time until a user can skip a video
   *
   * @param username The username of the user
   * @return The time until the user can skip a video
   */
  fun timeToNextSkip(username: String): String {
    val user = userRepository.findByUsername(username) ?: return "0"
    return timeToNextAction(user.lastSkip, userConfig.minutesToSkipVideo?.toLong() ?: 0)
  }

  /**
   * Updates the user's last skip timestamp
   *
   * @param username The username of the user
   */
  fun setLastSkip(username: String) {
    userRepository.save(
      userRepository.findByUsername(username)?.apply {
        lastSkip = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
      } ?: User(username)
    )
  }

  /**
   * Resets the user's video cooldown by setting lastAddedVideo to a day ago
   * This allows the user to add a new video immediately
   *
   * @param username The username of the user
   */
  fun resetVideoCooldown(username: String) {
    userRepository.save(
      userRepository.findByUsername(username)?.apply {
        lastAddedVideo = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).minusDays(1)
      } ?: User(username)
    )
  }

  /**
   * Checks if a user can remove a video
   *
   * @param username The username of the user
   * @return True if the user can remove a video, false otherwise
   */
  fun canUserRemoveVideo(username: String): Boolean {
    return canPerformTimeRestrictedAction(
      username,
      { it.lastRemovedVideo },
      5, // 5 minutes cooldown for removing videos
      TimeUnit.MINUTES
    )
  }

  /**
   * Gets the time until a user can remove a video
   *
   * @param username The username of the user
   * @return The time until the user can remove a video
   */
  fun timeToNextRemoval(username: String): String {
    val user = userRepository.findByUsername(username) ?: return "0"
    return timeToNextAction(user.lastRemovedVideo, 5) // 5 minutes cooldown
  }

  /**
   * Updates the user's last removal timestamp
   *
   * @param username The username of the user
   */
  fun setLastRemoval(username: String) {
    userRepository.save(
      userRepository.findByUsername(username)?.apply {
        lastRemovedVideo = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
      } ?: User(username)
    )
  }

  /**
   * Calculates the time until the next action can be performed
   *
   * @param lastActionTime The time of the last action
   * @param intervalMinutes The interval in minutes
   * @return The formatted time until the next action
   */
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
