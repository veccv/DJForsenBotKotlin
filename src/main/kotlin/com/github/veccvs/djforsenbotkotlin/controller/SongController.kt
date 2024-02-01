package com.github.veccvs.djforsenbotkotlin.controller

import com.github.veccvs.djforsenbotkotlin.model.Song
import com.github.veccvs.djforsenbotkotlin.repository.SongRepository
import lombok.RequiredArgsConstructor
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/song")
@RestController
@RequiredArgsConstructor
class SongController(private val songRepository: SongRepository) {
  @PostMapping("/add")
  fun addSong(@RequestParam songLink: String): Song {
    return songRepository.save(Song().apply { this.link = songLink })
  }
}
