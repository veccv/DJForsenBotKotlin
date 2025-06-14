package com.github.veccvs.djforsenbotkotlin.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Service for interacting with the GPT API endpoint. This service handles sending user messages to
 * the GPT API and processing the responses.
 */
@Service
class GptService {

  @Value("\${bot.dao-address:http://cytubebot-base:7777/}") private lateinit var daoAddress: String

  private val restTemplate = RestTemplate()
  private val logger = LoggerFactory.getLogger(GptService::class.java)

  /**
   * Sends a message to the GPT API and returns the response.
   *
   * @param text The text message to send to the GPT API
   * @param nickname The nickname of the user who sent the message
   * @return The response from the GPT API, or null if there was an error
   */
  fun getGptResponse(text: String, nickname: String): String? {
    try {
      logger.info("[GPT] Sending message to GPT API: $text from user: $nickname")

      val text = text.lowercase().replace("djfors_", "you").replace("@djfors", "you")
      val encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString())
      val encodedNickname = URLEncoder.encode(nickname, StandardCharsets.UTF_8.toString())

      val uri =
        UriComponentsBuilder.fromUriString(daoAddress)
          .path("get-response")
          .queryParam("text", encodedText)
          .queryParam("nickname", encodedNickname)
          .build()
          .toUriString()

      val response = restTemplate.getForObject(uri, String::class.java)

      logger.info("[GPT] Received response from GPT API: $response")
      return response
    } catch (e: Exception) {
      logger.error("[GPT] Error getting response from GPT API: ${e.message}", e)
      return null
    }
  }
}
