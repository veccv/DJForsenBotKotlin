package com.github.veccvs.djforsenbotkotlin.component

import com.github.veccvs.djforsenbotkotlin.dao.CytubeDao
import com.github.veccvs.djforsenbotkotlin.repository.SongRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class SongScheduler(
  @Autowired private val cytubeDao: CytubeDao,
  @Autowired private val songRepository: SongRepository,
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
}
