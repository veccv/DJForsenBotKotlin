package com.github.veccvs.djforsenbotkotlin.service.command

import com.github.veccvs.djforsenbotkotlin.dao.CytubeDao
import com.github.veccvs.djforsenbotkotlin.model.UserSong
import com.github.veccvs.djforsenbotkotlin.service.UserSongService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Service for formatting user songs for display */
@Service
class UserSongFormatterService(
  @Autowired private val userSongService: UserSongService,
  @Autowired private val messageService: MessageService,
  @Autowired private val cytubeDao: CytubeDao,
) {
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
    val sortedSongs = songs.sortedByDescending { it.addedAt }
    return formatSongList(sortedSongs)
  }

  /**
   * Calculates the estimated time until a song is played
   *
   * @param songLink The link to the song
   * @return The estimated time in minutes, or null if the song is not in the queue
   */
  fun calculateEstimatedTime(songLink: String): Int? {
    val playlist = cytubeDao.getPlaylist() ?: return null
    val videoId = extractVideoId(songLink)

    var totalTime = 0
    for (item in playlist.queue) {
      if (item.link.id == videoId) {
        return totalTime / 60 // Convert seconds to minutes
      }
      totalTime += item.duration
    }

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

    // Sort by addedAt timestamp in descending order to get the most recently added unplayed songs
    val sortedSongs = unplayedSongs.sortedByDescending { it.addedAt }

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
          "$title (~${estimatedTime}min)"
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
