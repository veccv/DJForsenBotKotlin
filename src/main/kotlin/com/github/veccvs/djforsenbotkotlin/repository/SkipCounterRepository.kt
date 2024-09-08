package com.github.veccvs.djforsenbotkotlin.repository

import com.github.veccvs.djforsenbotkotlin.model.SkipCounter
import org.springframework.data.jpa.repository.JpaRepository

interface SkipCounterRepository : JpaRepository<SkipCounter, Int>
