package com.github.veccvs.djforsenbotkotlin.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.veccvs.djforsenbotkotlin.model.OllamaGenerateRequest
import com.github.veccvs.djforsenbotkotlin.model.OllamaGenerateResponse
import com.github.veccvs.djforsenbotkotlin.model.OllamaOptions
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

/**
 * Service for interacting with the Ollama API endpoint. This service handles sending user messages
 * to the Ollama API and processing the responses.
 */
@Service
class GptService {

  private val ollamaApiUrl = "http://192.168.0.121:11434/api/generate"
  private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
  private val restTemplate =
    RestTemplate().apply {
      messageConverters.add(MappingJackson2HttpMessageConverter(objectMapper))
    }
  private val logger = LoggerFactory.getLogger(GptService::class.java)

  /**
   * Sends a message to the Ollama API and returns the response.
   *
   * @param text The text message to send to the Ollama API
   * @param nickname The nickname of the user who sent the message
   * @return The response from the Ollama API, or null if there was an error
   */
  fun getGptResponse(text: String, nickname: String): String? {
    try {
      logger.info("[GPT] Sending message to Ollama API: $text from user: $nickname")

      // Prepare text – replace bot mentions for better context
      val processedText = text.lowercase().replace("djfors_", "you").replace("@djfors", "you")

      // Build Ollama API request
      val requestBody =
        OllamaGenerateRequest(
          model = "llama3.2:1b",
          prompt = processedText,
          stream = false,
          options = OllamaOptions(numCtx = 2048, numPredict = 120),
        )

      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON

      val entity = HttpEntity(requestBody, headers)

      val responseString = restTemplate.postForObject(ollamaApiUrl, entity, String::class.java)

      if (responseString == null) {
        logger.error("[GPT] Received null response from Ollama API")
        return null
      }

      val response = objectMapper.readValue<OllamaGenerateResponse>(responseString)
      logger.info("[GPT] Received response from Ollama API: ${response.response}")

      return response.response
    } catch (e: Exception) {
      logger.error("[GPT] Error getting response from Ollama API: ${e.message}", e)
      return null
    }
  }

  /**
   * Sends a message to the Ollama API and returns the response for music searches.
   *
   * @param text The text message to send
   * @param nickname The nickname of the user who sent the message
   * @return The response from the Ollama API, or null if there was an error
   */
  fun getMusicResponse(text: String, nickname: String): String? {
    try {
      logger.info("[GPT] Sending MUSIC message to Ollama API: $text from user: $nickname")

      val promptText =
        "Answer only with author - song name. Don't add any other words. Find song which user described as: " +
          text

      // Build Ollama API request
      val requestBody =
        OllamaGenerateRequest(
          model = "llama3.2:1b",
          prompt = promptText,
          stream = false,
          options = OllamaOptions(numCtx = 2048, numPredict = 120),
        )

      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON

      val entity = HttpEntity(requestBody, headers)

      val responseString = restTemplate.postForObject(ollamaApiUrl, entity, String::class.java)

      if (responseString == null) {
        logger.error("[GPT] Received null response from Ollama API")
        return null
      }

      val response = objectMapper.readValue<OllamaGenerateResponse>(responseString)
      logger.info("[GPT] Received MUSIC response from Ollama API: ${response.response}")

      return response.response
    } catch (e: Exception) {
      logger.error("[GPT] Error getting MUSIC response from Ollama API: ${e.message}", e)
      return null
    }
  }
}
