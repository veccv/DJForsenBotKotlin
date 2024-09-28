package com.github.veccvs.djforsenbotkotlin.model

import jakarta.persistence.*
import lombok.*

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "gachi_songs")
open class GachiSong {
  @Id @Column(name = "id", nullable = false) open var id: Long? = null

  @Column(name = "title") open var title: String? = null
}
