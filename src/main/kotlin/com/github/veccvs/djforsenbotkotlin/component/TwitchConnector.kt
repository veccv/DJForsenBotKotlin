package com.github.veccvs.djforsenbotkotlin.component

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.veccvs.djforsenbotkotlin.model.twitch.TwitchResponse
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

class TwitchConnector {
  private val client = OkHttpClient()
  private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

  private val oauthToken = "3j4lbbaxfm86hweoiit9dpivfbjkb1"

  fun getUserInfo(userName: String): TwitchResponse {
    val request =
      Request.Builder()
        .url("https://api.twitch.tv/helix/users?login=$userName")
        .addHeader("Authorization", "Bearer $oauthToken")
        .addHeader("Client-ID", "gp762nuuoqcoxypju8c569th9wz7q5")
        .build()

    val response: Response = client.newCall(request).execute()
    response.use {
      if (!response.isSuccessful) throw IOException("Unexpected code $response")

      val responseBody = response.body?.string() ?: throw IOException("Empty response body")

      return try {
        val userData: TwitchResponse = objectMapper.readValue(responseBody)
        userData
      } catch (e: Exception) {
        throw IOException("Failed to parse response body", e)
      }
    }
  }

  fun sendWhisper(receiverUsername: String, message: String) {
    val senderUserId = getUserInfo("djfors_").data[0].id
    val receiverUserId = getUserInfo(receiverUsername).data[0].id

    val request =
      Request.Builder()
        .url(
          "https://api.twitch.tv/helix/whispers?from_user_id=$senderUserId&to_user_id=$receiverUserId"
        )
        .addHeader("Authorization", "Bearer $oauthToken")
        .addHeader("Client-ID", "gp762nuuoqcoxypju8c569th9wz7q5")
        .addHeader("Content-Type", "application/json")
        .post("{\"message\":\"$message\"}".toRequestBody())
        .build()

    val response: Response = client.newCall(request).execute()
    response.use { if (!response.isSuccessful) throw IOException("Unexpected code $response") }
  }
}
