package com.github.veccvs.djforsenbotkotlin.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*
import lombok.AllArgsConstructor
import lombok.Getter
import lombok.NoArgsConstructor
import lombok.Setter

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
}
