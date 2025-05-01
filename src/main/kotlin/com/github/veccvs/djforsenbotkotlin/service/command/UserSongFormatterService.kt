package com.github.veccvs.djforsenbotkotlin.service.command

import com.github.veccvs.djforsenbotkotlin.model.UserSong
import com.github.veccvs.djforsenbotkotlin.service.UserSongService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Service for formatting user songs for display
 */
@Service
class UserSongFormatterService(
  @Autowired private val userSongService: UserSongService,
  @Autowired private val messageService: MessageService
) {
  /**
   * Formats a list of songs for display
   *
   * @param songs The list of songs to format
   * @return The formatted string
   */
  fun formatSongList(songs: List<UserSong>): String {
    val formattedSongs = songs
      .take(3)
      .joinToString(", ") { it.title ?: "Unknown" }

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
   * Displays the songs added by a user and their played status
   *
   * @param username The username of the user
   * @param channel The channel to send the message to
   */
  fun getUserSongs(username: String, channel: String) {
    val userSongs = userSongService.getUserSongs(username)

    if (userSongs.isEmpty()) {
      messageService.sendMessage(channel, "@$username docJAM You haven't added any songs yet")
      return
    }

    val playedSongs = userSongs.filter { it.played }
    val unplayedSongs = userSongs.filter { !it.played }

    val message = StringBuilder("@$username docJAM Your songs: ")

    if (playedSongs.isNotEmpty()) {
      message.append("Played (${playedSongs.size}): ")
      // Sort by playedAt timestamp in descending order to get the most recently played songs
      message.append(formatPlayedSongs(playedSongs))
    }

    if (unplayedSongs.isNotEmpty()) {
      if (playedSongs.isNotEmpty()) {
        message.append(" | ")
      }
      message.append("Unplayed (${unplayedSongs.size}): ")
      // Sort by addedAt timestamp in descending order to get the most recently added unplayed songs
      message.append(formatUnplayedSongs(unplayedSongs))
    }

    messageService.sendMessage(channel, message.toString())
  }
}