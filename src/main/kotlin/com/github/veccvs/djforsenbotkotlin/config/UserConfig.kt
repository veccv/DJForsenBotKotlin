package com.github.veccvs.djforsenbotkotlin.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

@Configuration
@PropertySource("classpath:application.properties")
class UserConfig {
  @Value("\${user.minutes-to-add-video}") val minutesToAddVideo: Int? = null
}
