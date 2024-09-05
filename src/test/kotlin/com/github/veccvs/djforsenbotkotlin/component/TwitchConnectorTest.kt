package com.github.veccvs.djforsenbotkotlin.component

import org.junit.jupiter.api.Test

class TwitchConnectorTest {

  @Test
  fun getUserInfo() {
    val twitchConnector = TwitchConnector()
    println(twitchConnector.getUserInfo("djfors_"))
    println(twitchConnector.getUserInfo("veccvs"))
    twitchConnector.sendWhisperWithCustomHeaders("veccvs", "test")
  }
}
