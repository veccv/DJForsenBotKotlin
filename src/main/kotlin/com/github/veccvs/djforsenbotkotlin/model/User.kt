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
}
