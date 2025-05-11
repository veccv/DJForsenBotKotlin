package com.github.veccvs.djforsenbotkotlin.component

import com.github.veccvs.djforsenbotkotlin.config.UserConfig
import com.github.veccvs.djforsenbotkotlin.dao.CytubeDao
import com.github.veccvs.djforsenbotkotlin.model.LastSong
import com.github.veccvs.djforsenbotkotlin.repository.SongRepository
import com.github.veccvs.djforsenbotkotlin.repository.UserRepository
import com.github.veccvs.djforsenbotkotlin.service.CommandService
import com.github.veccvs.djforsenbotkotlin.service.SkipCounterService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class SongScheduler(
  @Autowired private val cytubeDao: CytubeDao,
  @Autowired private val songRepository: SongRepository,
  @Autowired private val userConfig: UserConfig,
  @Autowired private val commandService: CommandService,
  @Autowired private val userRepository: UserRepository,
  @Autowired private val skipCounterService: SkipCounterService,
) {
  @Scheduled(fixedDelay = 2000)
  fun scheduleSong() {
    if (
      cytubeDao.getBotStatus() != null &&
        cytubeDao.getBotStatus()!!.botEnabled &&
        cytubeDao.getPlaylist()?.queue?.isEmpty() == true
    ) {
      songRepository.findAll().random().link?.let { cytubeDao.addVideo(it) }
    }
  }

  @Scheduled(fixedDelay = 2000)
  fun changeLastSong() {
    if (
      cytubeDao.getBotStatus() != null &&
        cytubeDao.getBotStatus()!!.botEnabled &&
        cytubeDao.getPlaylist()?.queue?.isEmpty() == false
    ) {
      if (userConfig.lastSong.link == "") {
        userConfig.lastSong = LastSong().apply { link = cytubeDao.getPlaylist()!!.queue[0].link.id }
      } else if (cytubeDao.getPlaylist()!!.queue[0].link.id != userConfig.lastSong.link) {
        userConfig.lastSong = LastSong().apply { link = cytubeDao.getPlaylist()!!.queue[0].link.id }
        if (cytubeDao.getPlaylist()!!.queue[0].title.length < 50) {
          commandService.sendMessage(
            userConfig.channelName!!,
            "docJAM now playing: ${cytubeDao.getPlaylist()!!.queue[0].title.substring(0,
                            cytubeDao.getPlaylist()!!.queue[0].title.length,
                            )}",
          )
          skipCounterService.resetSkipCounter()
        } else {
          commandService.sendMessage(
            userConfig.channelName!!,
            "docJAM now playing: ${cytubeDao.getPlaylist()!!.queue[0].title.substring(0, 50)}[...]",
          )
          skipCounterService.resetSkipCounter()
        }
      }
    }
  }

  @Scheduled(fixedDelay = 2000)
  fun notifyUsers() {
    if (cytubeDao.getBotStatus() != null && cytubeDao.getBotStatus()!!.botEnabled) {
      // Only notify users who haven't been notified yet and are not currently tracking Spotify
      userRepository.findAllByUserNotifiedIsFalse().forEach {
        // Check if enough time has passed since they last added a video
        if (LocalDateTime.now().isAfter(it.lastAddedVideo.plusMinutes(2))) {
          // Only send notification if the user is not currently tracking Spotify
          // (userNotified is already false, so we know they're not tracking)
          commandService.sendMessage(
            userConfig.channelName!!,
            "@${it.username} forsenJam you can add song now! forsenMaxLevel",
          )
          it.userNotified = true
          userRepository.save(it)
        }
      }
    }
  }
}
