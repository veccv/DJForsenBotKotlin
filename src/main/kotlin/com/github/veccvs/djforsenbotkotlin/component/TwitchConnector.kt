package com.github.veccvs.djforsenbotkotlin.component

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.veccvs.djforsenbotkotlin.model.twitch.TwitchResponse
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class TwitchConnector {
  private val client = OkHttpClient()
  private val jsonMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

  // Cache the bot's user ID to avoid repeated API calls
  private var botUserId: String? = null

  fun getUserInfo(userName: String): TwitchResponse {
    val request = buildTwitchRequest(url = "https://api.twitch.tv/helix/users?login=$userName")
    client.newCall(request).execute().use { response ->
      if (!response.isSuccessful) throw IOException("Unexpected code $response")
      val responseBody = response.body?.string() ?: throw IOException("Empty response body")
      return try {
        jsonMapper.readValue<TwitchResponse>(responseBody)
      } catch (e: Exception) {
        throw IOException("Failed to parse response body", e)
      }
    }
  }

  fun sendWhisper(receiverUsername: String, message: String) {
    val senderUserId = getBotUserId()
    val receiverUserId = getUserInfo(receiverUsername).data[0].id
    val request =
      buildTwitchRequest(
          url =
            "https://api.twitch.tv/helix/whispers?from_user_id=$senderUserId&to_user_id=$receiverUserId",
          jsonBody = """{"message":"$message"}""",
        )
        .newBuilder()
        .addHeader("Content-Type", "application/json")
        .post("""{"message":"$message"}""".toRequestBody())
        .build()
    client.newCall(request).execute().use { response ->
      if (!response.isSuccessful) throw IOException("Unexpected code $response")
    }
  }

  private fun buildTwitchRequest(url: String, jsonBody: String? = null): Request {
    val builder =
      Request.Builder()
        .url(url)
        .addHeader("Authorization", "Bearer $OAUTH_TOKEN")
        .addHeader("Client-ID", CLIENT_ID)
    if (jsonBody != null) {
      builder.addHeader("Content-Type", "application/json").post(jsonBody.toRequestBody())
    }
    return builder.build()
  }

  private fun getBotUserId(): String =
    botUserId ?: getUserInfo(BOT_USERNAME).data[0].id.also { botUserId = it }

  companion object {
    private const val OAUTH_TOKEN = "3j4lbbaxfm86hweoiit9dpivfbjkb1"
    private const val CLIENT_ID = "gp762nuuoqcoxypju8c569th9wz7q5"
    private const val BOT_USERNAME = "djfors_"
  }
}
