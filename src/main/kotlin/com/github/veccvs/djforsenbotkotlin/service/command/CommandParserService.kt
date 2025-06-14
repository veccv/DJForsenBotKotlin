package com.github.veccvs.djforsenbotkotlin.service.command

import com.github.veccvs.djforsenbotkotlin.model.TwitchCommand
import org.springframework.stereotype.Service

/** Service for parsing Twitch chat commands */
@Service
class CommandParserService {
  companion object {
    // Bot username for mention detection
    private const val BOT_USERNAME = "djfors_"
  }

  /**
   * Detects if a message contains a command
   *
   * @param message The message to check
   * @return The command if found, null otherwise
   */
  fun detectCommand(message: String): String? {
    // Only log if the message might be a command
    if (message.startsWith(";")) {
      println("[COMMAND PARSER] Detecting command in message: $message")
    }

    val command = message.split(" ")[0]
    val result = if (command.startsWith(";")) command else null

    // Only log if a command was actually detected
    if (result != null) {
      println("[COMMAND PARSER] Command detected: $result")
    }

    return result
  }

  /**
   * Detects if a message mentions the bot
   *
   * @param message The message to check
   * @return True if the bot is mentioned, false otherwise
   */
  fun detectBotMention(message: String): Boolean {
    // Check for @botname or just botname at the beginning of the message
    val normalizedMessage = message.trim().lowercase()
    val mentionWithAt = "@$BOT_USERNAME".lowercase()

    return normalizedMessage.contains(mentionWithAt) ||
      normalizedMessage.contains(BOT_USERNAME.lowercase())
  }

  /**
   * Extracts the actual message content after the bot mention
   *
   * @param message The full message that mentions the bot
   * @return The message content after the bot mention, or the original message if no mention is
   *   found
   */
  fun extractMessageAfterMention(message: String): String {
    val normalizedMessage = message.trim()
    val mentionWithAt = "@$BOT_USERNAME"

    return when {
      normalizedMessage.contains(mentionWithAt, ignoreCase = true) ->
        normalizedMessage.substring(mentionWithAt.length).trim()

      normalizedMessage.contains(BOT_USERNAME, ignoreCase = true) ->
        normalizedMessage.substring(BOT_USERNAME.length).trim()

      else -> normalizedMessage
    }
  }

  /**
   * Parses a message into a TwitchCommand object
   *
   * @param message The message to parse
   * @return The parsed TwitchCommand
   */
  fun parseCommand(message: String): TwitchCommand? {
    val twitchCommand = TwitchCommand("", emptyList())
    message.split(" ").forEachIndexed { index, s ->
      if (index == 0) {
        twitchCommand.command = s
      } else {
        twitchCommand.params += s
      }
    }
    return twitchCommand
  }
}
