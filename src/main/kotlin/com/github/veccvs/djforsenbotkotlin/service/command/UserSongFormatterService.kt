package com.github.veccvs.djforsenbotkotlin.service.command

import com.github.veccvs.djforsenbotkotlin.dao.CytubeDao
import com.github.veccvs.djforsenbotkotlin.model.Playlist
import com.github.veccvs.djforsenbotkotlin.model.UserSong
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
   * Formats a list of songs for display
   *
   * @param songs The list of songs to format
   * @return The formatted string
   */
  fun formatSongList(songs: List<UserSong>): String {
    val formattedSongs = songs.take(3).joinToString(", ") { it.title ?: "Unknown" }

    val suffix = if (songs.size > 3) " and ${songs.size - 3} more" else ""

    return formattedSongs + suffix
  }

  /**
   * Formats a list of played songs for display
   *
   * @param songs The list of played songs to format
   * @return The formatted string
   */
  fun formatPlayedSongs(songs: List<UserSong>): String {
    val sortedSongs = songs.sortedByDescending { it.playedAt }
    return formatSongList(sortedSongs)
  }

  /**
   * Formats a list of unplayed songs for display
   *
   * @param songs The list of unplayed songs to format
   * @return The formatted string
   */
  fun formatUnplayedSongs(songs: List<UserSong>): String {
    val sortedSongs = songs.sortedBy { it.addedAt }
    return formatSongList(sortedSongs)
  }

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

    // Start with the time already elapsed in the current video
    var totalTime = 0

    // If we have a previous playlist state, calculate the elapsed time
    if (lastPlaylist != null && lastUpdateTime > 0 && !currentPlaylist.paused) {
      val elapsedSeconds = ((currentTime - lastUpdateTime) / 1000).toInt()
      println("[DEBUG] Time since last update: $elapsedSeconds seconds")

      // If the playlist hasn't changed (same video is playing), adjust the current time
      if (
        lastPlaylist?.queue?.isNotEmpty() == true &&
          currentPlaylist.queue.isNotEmpty() &&
          lastPlaylist?.queue?.get(0)?.link?.id == currentPlaylist.queue[0].link.id
      ) {

        // Calculate the adjusted current time
        val adjustedCurrentTime = currentPlaylist.currentTime + elapsedSeconds
        println(
          "[DEBUG] Adjusted currentTime: $adjustedCurrentTime (original: ${currentPlaylist.currentTime}, elapsed: $elapsedSeconds)"
        )

        // Subtract the adjusted current time from the total
        totalTime -= adjustedCurrentTime
        println("[DEBUG] Subtracted adjusted currentTime: $totalTime")
      } else {
        // If the playlist has changed, just use the current time from the playlist
        totalTime -= currentPlaylist.currentTime
        println(
          "[DEBUG] Playlist changed, using original currentTime: ${currentPlaylist.currentTime}"
        )
      }
    } else if (!currentPlaylist.paused) {
      // If we don't have a previous state, just use the current time from the playlist
      totalTime -= currentPlaylist.currentTime
      println(
        "[DEBUG] No previous state, using original currentTime: ${currentPlaylist.currentTime}"
      )
    }

    // Store the current playlist and timestamp for the next call
    lastPlaylist = currentPlaylist
    lastUpdateTime = currentTime

    // Add the duration of all videos in the queue until we find the target video
    for (item in currentPlaylist.queue) {
      println("[DEBUG] Queue item: ${item.title}, duration: ${item.duration}, id: ${item.link.id}")

      if (item.link.id == videoId) {
        // If the total time is negative, it means the video will play very soon
        val seconds = Math.max(0, totalTime)
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        val formattedTime = String.format("%02d:%02d", minutes, remainingSeconds)
        println("[DEBUG] Found target video. Total time: $totalTime, formatted: $formattedTime")
        return formattedTime
      }
      totalTime += item.duration
      println("[DEBUG] Added duration: $totalTime")
    }

    println("[DEBUG] Video not found in queue")
    return null // Song not found in queue
  }

  /**
   * Extracts the video ID from a YouTube link
   *
   * @param link The YouTube link
   * @return The video ID
   */
  private fun extractVideoId(link: String): String {
    // Handle youtu.be links
    if (link.contains("youtu.be")) {
      return link.substringAfterLast("/")
    }

    // Handle youtube.com links
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

    val message = StringBuilder("@$username docJAM Your unplayed songs: ")

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
          "$title (${estimatedTime})"
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

    // Ensure message length is limited to 150 characters
    var finalMessage = message.toString()
    if (finalMessage.length > 150) {
      finalMessage = finalMessage.substring(0, 147) + "..."
    }

    messageService.sendMessage(channel, finalMessage)
  }
}
