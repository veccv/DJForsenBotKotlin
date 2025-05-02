package com.github.veccvs.djforsenbotkotlin.service.command

import com.github.veccvs.djforsenbotkotlin.model.TwitchCommand
import org.springframework.stereotype.Service

/**
 * Service for parsing Twitch chat commands
 */
@Service
class CommandParserService {
  /**
   * Detects if a message contains a command
   *
   * @param message The message to check
   * @return The command if found, null otherwise
   */
  fun detectCommand(message: String): String? {
    println("[COMMAND PARSER] Detecting command in message: $message")
    val command = message.split(" ")[0]
    val result = if (command.startsWith(";")) command else null
    println("[COMMAND PARSER] Command detected: $result")
    return result
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
