package com.github.veccvs.djforsenbotkotlin.config

import io.github.cdimascio.dotenv.Dotenv
import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource

class EnvironmentPostProcessor : EnvironmentPostProcessor {
  override fun postProcessEnvironment(
    environment: ConfigurableEnvironment,
    application: SpringApplication,
  ) {
    val dotenv = Dotenv.load()
    val dotenvProperties = dotenv.entries().associate { it.key to it.value }
    val propertySource = MapPropertySource("dotenv", dotenvProperties)
    environment.propertySources.addLast(propertySource)
  }
}
