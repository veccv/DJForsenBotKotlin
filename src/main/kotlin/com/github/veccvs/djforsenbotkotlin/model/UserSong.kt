package com.github.veccvs.djforsenbotkotlin.model

import jakarta.persistence.*
import lombok.*
import java.time.LocalDateTime
import java.util.*

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "user_song", schema = "public")
class UserSong {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    var id: UUID? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "song_id", nullable = false)
    var song: Song? = null

    @Column(name = "title", nullable = true)
    var title: String? = null

    @Column(name = "played", nullable = false)
    var played: Boolean = false

    @Column(name = "added_at", nullable = false)
    var addedAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "played_at", nullable = true)
    var playedAt: LocalDateTime? = null
}
