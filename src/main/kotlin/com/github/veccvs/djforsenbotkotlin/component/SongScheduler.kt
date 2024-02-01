package com.github.veccvs.djforsenbotkotlin.component

import com.github.veccvs.djforsenbotkotlin.config.UserConfig
import com.github.veccvs.djforsenbotkotlin.dao.CytubeDao
import com.github.veccvs.djforsenbotkotlin.model.LastSong
import com.github.veccvs.djforsenbotkotlin.repository.SongRepository
import com.github.veccvs.djforsenbotkotlin.service.CommandService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class SongScheduler(
  @Autowired private val cytubeDao: CytubeDao,
  @Autowired private val songRepository: SongRepository,
  @Autowired private val userConfig: UserConfig,
  @Autowired private val commandService: CommandService,
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
            "pepeJAM now playing: ${cytubeDao.getPlaylist()!!.queue[0].title.substring(0,
                            cytubeDao.getPlaylist()!!.queue[0].title.length,
                            )}",
          )
        } else {
          commandService.sendMessage(
            userConfig.channelName!!,
            "pepeJAM now playing: ${cytubeDao.getPlaylist()!!.queue[0].title.substring(0, 50)}[...]",
          )
        }
      }
    }
  }
}
