package com.github.veccvs.djforsenbotkotlin.repository

import com.github.veccvs.djforsenbotkotlin.model.Song
import com.github.veccvs.djforsenbotkotlin.model.User
import com.github.veccvs.djforsenbotkotlin.model.UserSong
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UserSongRepository : JpaRepository<UserSong, UUID> {
    fun findByUserAndSong(user: User, song: Song): UserSong?
    fun findByUserAndSongAndPlayed(user: User, song: Song, played: Boolean): List<UserSong>
    fun findByUser(user: User): List<UserSong>
    fun findBySong(song: Song): List<UserSong>
    fun findByPlayed(played: Boolean): List<UserSong>
}