package com.github.veccvs.djforsenbotkotlin.service.command

import com.github.veccvs.djforsenbotkotlin.dao.CytubeDao
import com.github.veccvs.djforsenbotkotlin.model.Playlist
import com.github.veccvs.djforsenbotkotlin.service.UserSongService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/** Service for formatting user songs for display */
@Service
class UserSongFormatterService(
  @Autowired private val userSongService: UserSongService,
  @Autowired private val messageService: MessageService,
  @Autowired private val cytubeDao: CytubeDao,
) {
  // Store the last playlist state and timestamp to calculate elapsed time
  private var lastPlaylist: Playlist? = null
  private var lastUpdateTime: Long = 0

  /**
   * Calculates the estimated time until a song is played
   *
   * @param songLink The link to the song
   * @return The estimated time in MM:ss format, or null if the song is not in the queue
   */
  fun calculateEstimatedTime(songLink: String): String? {
    val currentPlaylist = cytubeDao.getPlaylist() ?: return null
    val videoId = extractVideoId(songLink)
    val currentTime = Instant.now().toEpochMilli()

    println("[DEBUG] Calculating estimated time for video ID: $videoId")
    println(
      "[DEBUG] Current playlist paused: ${currentPlaylist.paused}, currentTime: ${currentPlaylist.currentTime}"
    )

    // Initialize total time to 0
    var totalTime = 0F

    // Calculate elapsed time since the last update if we have previous data
    var adjustedCurrentTime = currentPlaylist.currentTime
    if (lastPlaylist != null && lastUpdateTime > 0 && !currentPlaylist.paused) {
      // Calculate elapsed seconds since the last update
      val elapsedMillis = currentTime - lastUpdateTime
      val elapsedSeconds = (elapsedMillis / 1000).toInt()

      // Only adjust if the playlist item is the same as before
      if (
        lastPlaylist?.queue?.isNotEmpty() == true &&
          currentPlaylist.queue.isNotEmpty() &&
          lastPlaylist?.queue?.get(0)?.link?.id == currentPlaylist.queue[0].link.id
      ) {

        // Adjust the current time by adding elapsed seconds
        adjustedCurrentTime =
          (currentPlaylist.currentTime + elapsedSeconds).coerceAtMost(
            currentPlaylist.queue[0].duration.toFloat()
          )

        println(
          "[DEBUG] Adjusted currentTime: $adjustedCurrentTime (original: ${currentPlaylist.currentTime}, elapsed: $elapsedSeconds)"
        )
      }
    }

    // Add the duration of all videos in the queue until we find the target video
    for ((index, item) in currentPlaylist.queue.withIndex()) {
      println("[DEBUG] Queue item: ${item.title}, duration: ${item.duration}, id: ${item.link.id}")

      // Calculate the time to add for this item
      var timeToAdd: Float
      // For the first video (currently playing), only add the remaining time
      if (index == 0 && !currentPlaylist.paused) {
        timeToAdd = 0F.coerceAtLeast(item.duration - adjustedCurrentTime)
        println(
          "[DEBUG] Added remaining duration for current video: $timeToAdd, total: ${totalTime + timeToAdd}"
        )
      } else {
        timeToAdd = item.duration.toFloat()
        println("[DEBUG] Added full duration: ${item.duration}, total: ${totalTime + timeToAdd}")
      }

      // Check if this is the target video
      if (item.link.id == videoId) {
        // If this is the currently playing video, return the remaining time for this video
        if (index == 0 && !currentPlaylist.paused) {
          val remainingSeconds = 0F.coerceAtLeast(item.duration - adjustedCurrentTime)
          val minutes = (remainingSeconds / 60).toInt()
          val seconds = (remainingSeconds % 60).toInt()
          val formattedTime = String.format("%02d:%02d", minutes, seconds)
          println(
            "[DEBUG] Found target video (currently playing). Remaining time: $remainingSeconds, formatted: $formattedTime"
          )

          // Store the current playlist and timestamp for the next call
          lastPlaylist = currentPlaylist
          lastUpdateTime = currentTime

          return formattedTime
        }

        // Otherwise, return the total time until this video starts playing
        val seconds = 0F.coerceAtLeast(totalTime)
        val minutes = (seconds / 60).toInt()
        val remainingSeconds = (seconds % 60).toInt()
        val formattedTime = String.format("%02d:%02d", minutes, remainingSeconds)
        println("[DEBUG] Found target video. Total time: $totalTime, formatted: $formattedTime")

        // Store the current playlist and timestamp for the next call
        lastPlaylist = currentPlaylist
        lastUpdateTime = currentTime

        return formattedTime
      }

      // Add the time for this item to the total
      totalTime += timeToAdd
    }

    println("[DEBUG] Video not found in queue")

    // Store the current playlist and timestamp for the next call
    lastPlaylist = currentPlaylist
    lastUpdateTime = currentTime

    return null // Song not found in the queue
  }

  /**
   * Extracts the video ID from a YouTube link
   *
   * @param link The YouTube link
   * @return The video ID
   */
  private fun extractVideoId(link: String): String {
    // Handle YouTube links
    if (link.contains("youtu.be")) {
      return link.substringAfterLast("/")
    }

    // Handle YouTube.com links
    if (link.contains("youtube.com")) {
      return link.substringAfter("v=").substringBefore("&")
    }

    // If it's already just an ID, return it
    return link
  }

  /**
   * Displays the unplayed songs added by a user with estimated time until they are played
   *
   * @param username The username of the user
   * @param channel The channel to send the message to
   */
  @Transactional
  fun getUserSongs(username: String, channel: String) {
    val unplayedSongs = userSongService.getUnplayedUserSongs(username)

    if (unplayedSongs.isEmpty()) {
      messageService.sendMessage(channel, "@$username docJAM You don't have any unplayed songs")
      return
    }

    val message = StringBuilder("@$username docJAM ")

    // Sort by addedAt timestamp in ascending order
    val sortedSongs = unplayedSongs.sortedBy { it.addedAt }

    // Format songs with estimated time
    val formattedSongs =
      sortedSongs.take(3).mapNotNull { userSong ->
        // Safely handle null song or link
        val song = userSong.song
        if (song == null) return@mapNotNull null

        val songLink = song.link
        if (songLink == null) return@mapNotNull null

        val title = userSong.title ?: "Unknown"
        val estimatedTime = calculateEstimatedTime(songLink)

        if (estimatedTime != null) {
          // Don't adjust the estimated time - use the calculated time directly
          // The calculation in calculateEstimatedTime is already accurate
          "$title (in ~${estimatedTime})"
        } else {
          "$title (not in queue)"
        }
      }

    // Join the formatted songs
    message.append(formattedSongs.joinToString(", "))

    // Add suffix if there are more songs
    if (sortedSongs.size > 3) {
      message.append(" and ${sortedSongs.size - 3} more")
    }

    // Ensure the message length is limited to 150 characters
    var finalMessage = message.toString()
    if (finalMessage.length > 150) {
      finalMessage = finalMessage.substring(0, 147) + "..."
    }

    messageService.sendMessage(channel, finalMessage)
  }
}
