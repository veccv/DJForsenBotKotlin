package com.github.veccvs.djforsenbotkotlin.repository

import com.github.veccvs.djforsenbotkotlin.model.Song
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface SongRepository : JpaRepository<Song, UUID> {
  fun existsByLink(song: String): Boolean
  fun findByLink(link: String): Song?
}
