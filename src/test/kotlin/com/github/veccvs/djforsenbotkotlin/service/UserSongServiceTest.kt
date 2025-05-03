package com.github.veccvs.djforsenbotkotlin.service

import com.github.veccvs.djforsenbotkotlin.dao.CytubeDao
import com.github.veccvs.djforsenbotkotlin.model.*
import com.github.veccvs.djforsenbotkotlin.repository.SongRepository
import com.github.veccvs.djforsenbotkotlin.repository.UserRepository
import com.github.veccvs.djforsenbotkotlin.repository.UserSongRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class UserSongServiceTest {

  @Mock private lateinit var userSongRepository: UserSongRepository

  @Mock private lateinit var userRepository: UserRepository

  @Mock private lateinit var songRepository: SongRepository

  @Mock private lateinit var cytubeDao: CytubeDao

  private lateinit var userSongService: UserSongService

  @BeforeEach
  fun setUp() {
    userSongService = UserSongService(userSongRepository, userRepository, songRepository, cytubeDao)
  }

  @Test
  fun `test checkSongsInPlaylist marks songs as played if not in playlist`() {
    // Given
    val user = User("testUser")
    val song1 =
      Song().apply {
        id = UUID.randomUUID()
        link = "https://www.youtube.com/watch?v=123456"
      }
    val song2 =
      Song().apply {
        id = UUID.randomUUID()
        link = "https://www.youtube.com/watch?v=789012"
      }

    val userSong1 =
      UserSong().apply {
        id = UUID.randomUUID()
        this.user = user
        this.song = song1
        title = "Test Song 1"
        played = false
        addedAt = LocalDateTime.now().minusHours(1)
      }

    val userSong2 =
      UserSong().apply {
        id = UUID.randomUUID()
        this.user = user
        this.song = song2
        title = "Test Song 2"
        played = false
        addedAt = LocalDateTime.now()
      }

    val unplayedSongs = listOf(userSong1, userSong2)

    // Create a playlist with only song2 in it
    val playlistItem =
      PlaylistItem(
        uid = 1,
        temp = false,
        username = "testUser",
        link = MediaLink("yt", "789012"),
        title = "Test Song 2",
        duration = 180,
      )

    val playlist =
      Playlist(
        time = 0,
        locked = false,
        paused = false,
        currentTime = 0f,
        _current = null,
        queue = mutableListOf(playlistItem),
      )

    // Mock repository and DAO responses
    `when`(userSongRepository.findByPlayed(false)).thenReturn(unplayedSongs)
    `when`(cytubeDao.getPlaylist()).thenReturn(playlist)

    // When
    userSongService.checkSongsInPlaylist()

    // Then
    // Verify that userSong1 was marked as played (since it's not in the playlist)
    verify(userSongRepository)
      .save(argThat { song -> song === userSong1 && song.played && song.playedAt != null })

    // Verify that userSong2 was not marked as played (since it's in the playlist)
    verify(userSongRepository, never()).save(argThat { song -> song === userSong2 })
  }
}
