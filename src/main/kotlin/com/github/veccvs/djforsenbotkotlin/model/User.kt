package com.github.veccvs.djforsenbotkotlin.model

import jakarta.persistence.*
import lombok.AllArgsConstructor
import lombok.Getter
import lombok.NoArgsConstructor
import lombok.Setter
import java.time.LocalDateTime
import java.util.*

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "user", schema = "public")
class User(@Column(name = "username", nullable = false) var username: String) {
  @Id
  @GeneratedValue
  @Column(name = "id", nullable = false, columnDefinition = "uuid")
  var id: UUID? = null

  @Column(name = "points", nullable = false) var points: Int = 0
  @Column(name = "last_added_video", nullable = false)
  var lastAddedVideo: LocalDateTime = LocalDateTime.now().minusDays(1)
  @Column(name = "last_response", nullable = false)
  var lastResponse: LocalDateTime = LocalDateTime.now().minusDays(1)
  @Column(name = "user_notified", nullable = true) var userNotified: Boolean = true
  @Column(name = "last_skip", nullable = false)
  var lastSkip: LocalDateTime = LocalDateTime.now().minusDays(1)
  @Column(name = "last_removed_video", nullable = false)
  var lastRemovedVideo: LocalDateTime = LocalDateTime.now().minusDays(1)

  // Spotify integration fields
  @Column(name = "spotify_user_id", nullable = true) var spotifyUserId: String? = null
  @Column(name = "spotify_access_token", nullable = true, length = 500)
  var spotifyAccessToken: String? = null
  @Column(name = "spotify_refresh_token", nullable = true, length = 500)
  var spotifyRefreshToken: String? = null
  @Column(name = "spotify_token_expiration", nullable = true)
  var spotifyTokenExpiration: LocalDateTime? = null

  // Track stop command tracking
  @Column(name = "last_track_stop", nullable = false)
  var lastTrackStop: LocalDateTime = LocalDateTime.now().minusDays(1)

  // Flag to indicate if tracking is active
  @Column(name = "is_tracking", nullable = false)
  var isTracking: Boolean = false
}
