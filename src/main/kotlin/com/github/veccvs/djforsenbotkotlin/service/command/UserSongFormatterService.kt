package com.github.veccvs.djforsenbotkotlin.service.command

import com.github.veccvs.djforsenbotkotlin.dao.CytubeDao
import com.github.veccvs.djforsenbotkotlin.model.Playlist
import com.github.veccvs.djforsenbotkotlin.service.UserSongService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class UserSongFormatterService(
  @Autowired private val userSongService: UserSongService,
  @Autowired private val messageService: MessageService,
  @Autowired private val cytubeDao: CytubeDao,
) {
  private var lastPlaylist: Playlist? = null
  private var lastUpdateTimeMillis: Long = 0

  fun getUserSongs(username: String, channel: String) {
    val unplayed = userSongService.getUnplayedUserSongs(username)
    if (unplayed.isEmpty()) {
      messageService.sendMessage(channel, "@$username docJAM You don't have any unplayed songs")
      return
    }
    val songs = unplayed.sortedBy { it.addedAt }
    val formatted =
      songs.take(3).mapNotNull { userSong ->
        val songLink = userSong.song?.link ?: return@mapNotNull null
        val title = userSong.title ?: "Unknown"
        val eta = estimateSongTime(songLink)
        formatSongTitleWithEta(title, eta)
      }
    val message =
      buildString {
          append("@$username docJAM ")
          append(formatted.joinToString(", "))
          if (songs.size > 3) append(" and ${songs.size - 3} more")
        }
        .truncateTo(150)
    messageService.sendMessage(channel, message)
  }

  fun estimateSongTime(songLink: String): String? {
    val playlist = cytubeDao.getPlaylist() ?: return null
    val videoId = extractVideoId(songLink)
    val now = Instant.now().toEpochMilli()
    val adjustedCurrentTime = computeAdjustedCurrentTime(playlist, now)
    var accumulatedTime = 0f

    playlist.queue.forEachIndexed { idx, item ->
      val timeForThisItem =
        if (idx == 0 && !playlist.paused) {
          (item.duration - adjustedCurrentTime).coerceAtLeast(0f)
        } else {
          item.duration.toFloat()
        }
      if (item.link.id == videoId) {
        updateLastPlaylistAndTime(playlist, now)
        return formatSecondsToTime(
          if (idx == 0 && !playlist.paused) item.duration - adjustedCurrentTime else accumulatedTime
        )
      }
      accumulatedTime += timeForThisItem
    }
    updateLastPlaylistAndTime(playlist, now)
    return null
  }

  private fun computeAdjustedCurrentTime(playlist: Playlist, now: Long): Float {
    if (
      lastPlaylist != null &&
        lastUpdateTimeMillis > 0 &&
        !playlist.paused &&
        lastPlaylist!!.queue.isNotEmpty() &&
        playlist.queue.isNotEmpty() &&
        lastPlaylist!!.queue[0].link.id == playlist.queue[0].link.id
    ) {
      val elapsedSec = ((now - lastUpdateTimeMillis) / 1000).toInt()
      return (playlist.currentTime + elapsedSec).coerceAtMost(playlist.queue[0].duration.toFloat())
    }
    return playlist.currentTime
  }

  private fun formatSongTitleWithEta(title: String, eta: String?) =
    if (eta != null) "$title (in ~$eta)" else "$title (not in queue)"

  private fun formatSecondsToTime(seconds: Float): String {
    val mins = (seconds / 60).toInt()
    val secs = (seconds % 60).toInt()
    return "%02d:%02d".format(mins, secs)
  }

  private fun updateLastPlaylistAndTime(playlist: Playlist, now: Long) {
    lastPlaylist = playlist
    lastUpdateTimeMillis = now
  }

  private fun extractVideoId(link: String) =
    when {
      "youtu.be" in link -> link.substringAfterLast("/")
      "youtube.com" in link -> link.substringAfter("v=").substringBefore("&")
      else -> link
    }

  private fun String.truncateTo(maxLength: Int) =
    if (length > maxLength) substring(0, maxLength - 3) + "..." else this
}
