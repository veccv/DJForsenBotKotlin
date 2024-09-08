package com.github.veccvs.djforsenbotkotlin.model

import jakarta.persistence.*
import lombok.*

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "skip_counter", schema = "public")
class SkipCounter {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "skip_counter_seq")
  @SequenceGenerator(
    name = "skip_counter_seq",
    sequenceName = "skip_counter_seq",
    allocationSize = 1,
  )
  @Column(name = "id", nullable = false)
  var id: Int? = null

  @Column(name = "count", nullable = false) var count: Int = 0
}
