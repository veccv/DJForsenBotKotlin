package com.github.veccvs.djforsenbotkotlin.service

import com.github.veccvs.djforsenbotkotlin.model.Song
import com.github.veccvs.djforsenbotkotlin.repository.SongRepository
import org.springframework.stereotype.Service

@Service
class SongService(private val songRepository: SongRepository) {
  fun addUniqueSong(song: String) {
    if (songRepository.existsByLink(song)) return
    songRepository.save(Song().apply { this.link = song })
  }
}
