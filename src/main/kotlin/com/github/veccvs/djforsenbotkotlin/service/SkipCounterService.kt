package com.github.veccvs.djforsenbotkotlin.service

import com.github.veccvs.djforsenbotkotlin.model.SkipCounter
import com.github.veccvs.djforsenbotkotlin.repository.SkipCounterRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SkipCounterService(@Autowired private val skipCounterRepository: SkipCounterRepository) {
  fun resetSkipCounter() {
    initializeSkipCounterIfAbsent()

    val foundSkipCounter = skipCounterRepository.findById(1).get()
    foundSkipCounter.count = 0
    skipCounterRepository.save(foundSkipCounter)
  }

  private fun initializeSkipCounterIfAbsent() {
    val skipCounter = skipCounterRepository.findById(1)

    if (!skipCounter.isPresent) {
      skipCounterRepository.save(SkipCounter())
    }
  }

  fun incrementSkipCounter() {
    initializeSkipCounterIfAbsent()

    val foundSkipCounter = skipCounterRepository.findById(1).get()
    foundSkipCounter.count++
    skipCounterRepository.save(foundSkipCounter)
  }

  fun getSkipCounter(): Int {
    initializeSkipCounterIfAbsent()

    return skipCounterRepository.findById(1).get().count
  }
}
