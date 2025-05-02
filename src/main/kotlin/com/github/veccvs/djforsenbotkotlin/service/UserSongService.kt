package com.github.veccvs.djforsenbotkotlin.service

import com.github.veccvs.djforsenbotkotlin.dao.CytubeDao
import com.github.veccvs.djforsenbotkotlin.model.Song
import com.github.veccvs.djforsenbotkotlin.model.UserSong
import com.github.veccvs.djforsenbotkotlin.repository.SongRepository
import com.github.veccvs.djforsenbotkotlin.repository.UserRepository
import com.github.veccvs.djforsenbotkotlin.repository.UserSongRepository
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
  @Scheduled(fixedRate = 2000) // Run every 5 seconds
  @Transactional
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
  @Transactional
  fun getUnplayedUserSongs(username: String): List<UserSong> {
    val user = userRepository.findByUsername(username) ?: return emptyList()
    return userSongRepository.findByUser(user).filter { !it.played }
  }

  /**
   * Gets the most recently added unplayed song for a user
   *
   * @param username The username of the user
   * @return The most recently added unplayed song, or null if none exists
   */
  @Transactional
  fun getMostRecentUnplayedUserSong(username: String): UserSong? {
    val unplayedSongs = getUnplayedUserSongs(username)
    return unplayedSongs.maxByOrNull { it.addedAt }
  }

  /**
   * Removes the most recently added unplayed song for a user
   * 
   * @param username The username of the user
   * @return The removed song, or null if no song was removed
   */
  @Transactional
  fun removeRecentUserSong(username: String): UserSong? {
    val song = getMostRecentUnplayedUserSong(username) ?: return null

    // Check if the song was added within the last 5 minutes
    val fiveMinutesAgo = LocalDateTime.now().minusMinutes(5)
    if (song.addedAt.isBefore(fiveMinutesAgo)) {
      return null
    }

    // Mark the song as played (effectively removing it from the unplayed list)
    song.played = true
    song.playedAt = LocalDateTime.now()
    userSongRepository.save(song)

    // Force initialization of the song entity to prevent LazyInitializationException
    song.song?.link?.let { _ -> }

    return song
  }

  /**
   * Checks if songs exist in the playlist when the application starts
   * If a song is not in the playlist, mark it as played
   */
  @PostConstruct
  @Transactional
  fun checkSongsInPlaylist() {
    val playlist = cytubeDao.getPlaylist() ?: return

    // Find all unplayed user songs
    val unplayedSongs = userSongRepository.findByPlayed(false)

    // Check each unplayed song against the playlist
    for (userSong in unplayedSongs) {
      val songLink = userSong.song?.link ?: continue
      val videoId = extractVideoId(songLink)

      // Check if the song exists in the playlist
      val songInPlaylist = playlist.queue.any { it.link.id == videoId }

      // If the song is not in the playlist, mark it as played
      if (!songInPlaylist) {
        userSong.played = true
        userSong.playedAt = LocalDateTime.now()
        userSongRepository.save(userSong)
      }
    }
  }
}
