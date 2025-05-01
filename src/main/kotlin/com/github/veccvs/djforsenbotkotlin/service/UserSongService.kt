package com.github.veccvs.djforsenbotkotlin.service

import com.github.veccvs.djforsenbotkotlin.dao.CytubeDao
import com.github.veccvs.djforsenbotkotlin.model.Song
import com.github.veccvs.djforsenbotkotlin.model.UserSong
import com.github.veccvs.djforsenbotkotlin.repository.SongRepository
import com.github.veccvs.djforsenbotkotlin.repository.UserRepository
import com.github.veccvs.djforsenbotkotlin.repository.UserSongRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class UserSongService
@Autowired
constructor(
  private val userSongRepository: UserSongRepository,
  private val userRepository: UserRepository,
  private val songRepository: SongRepository,
  private val cytubeDao: CytubeDao,
) {
  /**
   * Adds a song to the user's song list
   *
   * @param username The username of the user who added the song
   * @param songLink The link to the song
   * @param title The title of the song
   * @return The created UserSong
   */
  fun addUserSong(username: String, songLink: String, title: String): UserSong? {
    val user = userRepository.findByUsername(username) ?: return null

    // Find or create the song
    var song = songRepository.findByLink(songLink)
    if (song == null) {
      song = Song().apply { this.link = songLink }
      songRepository.save(song)
    }

    // Create the user song
    val userSong =
      UserSong().apply {
        this.user = user
        this.song = song
        this.title = title
        this.played = false
        this.addedAt = LocalDateTime.now()
      }

    return userSongRepository.save(userSong)
  }

  /**
   * Checks if a song has been played and updates its status This method is scheduled to run every
   * minute
   */
  @Scheduled(fixedRate = 60000) // Run every minute
  fun checkPlayedSongs() {
    val playlist = cytubeDao.getPlaylist() ?: return
    val currentItem = playlist.queue.firstOrNull() ?: return

    // Find all unplayed user songs
    val unplayedSongs = userSongRepository.findByPlayed(false)

    // Check if any of the unplayed songs match the current item
    for (userSong in unplayedSongs) {
      val songLink = userSong.song?.link ?: continue

      // Extract the video ID from the song link
      val videoId = extractVideoId(songLink)

      // Check if the current item's link matches the song link
      if (currentItem.link.id == videoId) {
        // Update the user song as played
        userSong.played = true
        userSong.playedAt = LocalDateTime.now()
        userSongRepository.save(userSong)
      }
    }
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
   * Gets all songs added by a user
   *
   * @param username The username of the user
   * @return A list of UserSongs
   */
  fun getUserSongs(username: String): List<UserSong> {
    val user = userRepository.findByUsername(username) ?: return emptyList()
    return userSongRepository.findByUser(user)
  }

  /**
   * Gets all played songs added by a user
   *
   * @param username The username of the user
   * @return A list of UserSongs
   */
  fun getPlayedUserSongs(username: String): List<UserSong> {
    val user = userRepository.findByUsername(username) ?: return emptyList()
    return userSongRepository.findByUser(user).filter { it.played }
  }

  /**
   * Gets all unplayed songs added by a user
   *
   * @param username The username of the user
   * @return A list of UserSongs
   */
  fun getUnplayedUserSongs(username: String): List<UserSong> {
    val user = userRepository.findByUsername(username) ?: return emptyList()
    return userSongRepository.findByUser(user).filter { !it.played }
  }
}
