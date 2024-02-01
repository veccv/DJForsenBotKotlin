package com.github.veccvs.djforsenbotkotlin.model

import jakarta.persistence.*
import lombok.*
import java.util.*

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
open class Song {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false)
  open var id: UUID? = null

  @Column(name = "link") open var link: String? = null
}
