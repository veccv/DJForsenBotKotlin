package com.github.veccvs.djforsenbotkotlin.service.command

import com.github.veccvs.djforsenbotkotlin.component.TwitchChatBot
import com.github.veccvs.djforsenbotkotlin.utils.BanPhraseChecker
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Service for handling message sending to Twitch chat
 */
@Service
class MessageService(
  @Autowired private val twitchChatBot: TwitchChatBot
) {
  /**
   * Sends a message to the specified channel, checking for banned phrases
   *
   * @param channel The channel to send the message to
   * @param message The message to send
   */
  fun sendMessage(channel: String, message: String) {
    if (BanPhraseChecker.check(message)) {
      twitchChatBot.sendMessage(channel, "docJAM banned phrase detected")
      return
    }
    twitchChatBot.sendMessage(channel, message)
  }
}