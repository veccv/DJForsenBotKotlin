package com.github.veccvs.djforsenbotkotlin

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication @EnableScheduling class DjForsenBotKotlinApplication

fun main(args: Array<String>) {
  runApplication<DjForsenBotKotlinApplication>(*args)
}
