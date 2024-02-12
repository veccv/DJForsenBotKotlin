package com.github.veccvs.djforsenbotkotlin.repository

import com.github.veccvs.djforsenbotkotlin.model.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UserRepository : JpaRepository<User, UUID> {
  fun findByUsername(username: String): User?

  fun findAllByUserNotifiedIsFalse(): List<User>
}
