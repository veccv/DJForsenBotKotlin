package com.github.veccvs.djforsenbotkotlin.repository

import com.github.veccvs.djforsenbotkotlin.model.GachiSong
import org.springframework.data.jpa.repository.JpaRepository

interface GachiSongRepository : JpaRepository<GachiSong, Long>
