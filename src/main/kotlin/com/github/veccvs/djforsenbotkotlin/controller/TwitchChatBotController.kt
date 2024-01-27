package com.github.veccvs.djforsenbotkotlin.controller

import com.github.veccvs.djforsenbotkotlin.service.CommandService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/twitch")
class TwitchChatBotController(@Autowired private val commandService: CommandService) {
  @RequestMapping("/send-message")
  fun sendMessage(
    @RequestParam message: String,
    @RequestParam channel: String,
  ): ResponseEntity<String> {
    commandService.sendMessage("#${channel}", message)
    return ResponseEntity.ok("Message sent")
  }
}
