package com.github.veccvs.djforsenbotkotlin.config

import com.github.veccvs.djforsenbotkotlin.model.LastSong
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

@Configuration
@PropertySource("classpath:application.properties")
class UserConfig {
  @Value("\${user.minutes-to-add-video}") val minutesToAddVideo: Int? = null
  @Value("\${user.seconds-to-response}") val secondsToResponseToCommand: Int? = null
  @Value("\${chat.channel-name}") val channelName: String? = null
  @Value("\${user.minutes-to-skip-video}") val minutesToSkipVideo: String? = null
  @Value("\${user.skip-value}") val skipValue: String? = null
  @Value("\${user.max-tracked-songs}") val maxTrackedSongs: Int = 5
  @Value("\${bot.dao-address}") val daoAddress: String? = null
  var lastSong: LastSong = LastSong()
}
