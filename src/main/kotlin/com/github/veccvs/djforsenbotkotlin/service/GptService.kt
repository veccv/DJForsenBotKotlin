package com.github.veccvs.djforsenbotkotlin.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

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

      // Prepare text – replace bot mentions for better context
      val processedText = text.lowercase().replace("djfors_", "you").replace("@djfors", "you")

      val uri =
        UriComponentsBuilder.fromUriString(daoAddress)
          .path("get-response")
          .build()
          .toUriString()

      // Build JSON body matching Flask endpoint expectations
      val requestBody = mapOf("text" to processedText, "nickname" to nickname)

      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON

      val entity = HttpEntity(requestBody, headers)

      val response = restTemplate.postForObject(uri, entity, String::class.java)

      logger.info("[GPT] Received response from GPT API: $response")
      return response
    } catch (e: Exception) {
      logger.error("[GPT] Error getting response from GPT API: ${e.message}", e)
      return null
    }
  }

  /**
   * Sends a message to the GPT music endpoint and returns the response.
   *
   * @param text The text message to send
   * @param nickname The nickname of the user who sent the message
   * @return The response from the GPT API, or null if there was an error
   */
  fun getMusicResponse(text: String, nickname: String): String? {
    try {
      logger.info("[GPT] Sending MUSIC message to GPT API: $text from user: $nickname")

      val processedText = text.lowercase().replace("djfors_", "you").replace("@djfors", "you")

      val uri =
        UriComponentsBuilder.fromUriString(daoAddress)
          .path("get-music-response")
          .build()
          .toUriString()

      val requestBody = mapOf("text" to processedText, "nickname" to nickname)

      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON

      val entity = HttpEntity(requestBody, headers)

      val response = restTemplate.postForObject(uri, entity, String::class.java)

      logger.info("[GPT] Received MUSIC response from GPT API: $response")
      return response
    } catch (e: Exception) {
      logger.error("[GPT] Error getting MUSIC response from GPT API: ${e.message}", e)
      return null
    }
  }
}
