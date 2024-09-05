package com.github.veccvs.djforsenbotkotlin.component

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.veccvs.djforsenbotkotlin.model.twitch.TwitchResponse
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
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

  fun sendWhisperWithCustomHeaders(receiverUserName: String, whisperMessage: String) {
    val receiverUserId = getUserInfo(receiverUserName).data[0].id

    val client = OkHttpClient.Builder().build()
    val mediaType = "text/plain;charset=UTF-8".toMediaType()
    val body =
      """
        [{"operationName":"SendWhisper","variables":{"input":{"message":"$whisperMessage","nonce":"b84811d5db5dcbf7756c11d35738ff87","recipientUserID":"$receiverUserId"}},"extensions":{"persistedQuery":{"version":1,"sha256Hash":"3bbd599e7891aaf3ab6a4f5788fd008f21ad0d64f6c47ea6081979f87e406c08"}}}]
    """
        .trimIndent()
        .toRequestBody(mediaType)

    val request =
      Request.Builder()
        .url("https://gql.twitch.tv/gql")
        .post(body)
        .addHeader(
          "sec-ch-ua",
          "\"Chromium\";v=\"128\", \"Not;A=Brand\";v=\"24\", \"Google Chrome\";v=\"128\"",
        )
        .addHeader("Accept-Language", "de-DE")
        .addHeader("sec-ch-ua-mobile", "?1")
        .addHeader("Client-Version", "60822c7e-8bca-4fe8-9529-be0154c7e129")
        .addHeader("Authorization", "OAuth pvqjc23juzz7uhoxvdykw34jq3p8f9")
        .addHeader(
          "User-Agent",
          "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36",
        )
        .addHeader("Content-Type", "text/plain;charset=UTF-8")
        .addHeader("Referer", "https://www.twitch.tv/")
        .addHeader(
          "Client-Integrity",
          "v4.local.3fbuDaG9qpq-8AjoZthelNbJ-Ws64tKPoXkyHUTa-n-LhUo6FjoVUDEvivJOroC-LVZqYCIXK28txC9k15m-AMzKnVUX70pxpATFMx5du_yFE0GspV9xlJ5WZwO6I1WGaC4-o3jPDTQuxzRopHlw5a0vaNyQAPSm6CXb-9XqTnvR9hmpMEWp9ZzLeLHlhJ3Abz78Cs92KqHQY13HtwLtKcwqw9LeYzEnJ-AvFlGgk5Y2IWP-M79l5jDyWzDHUDg7kG-4PUpIpxgtA7WeH8_FdkXgMrMvMCZuD2k7QM8lQjxh7Qv-NHSj4fZ_k4DbQXiR5uDsWVNHTPHbjGmiED9kaepjKcVDGggdJ-TS6YPvSI-BRWQEMqtqOVym1O28QMP3ObRPgGu6NGTqZ2q-PopAfSg74sB50s7wmjNs-yJ9uuflHk-qHHEFbZm2CddbT3gPma3WsD5vNBvCSHI",
        )
        .addHeader("Client-Session-Id", "28bf38e80e4abb8c")
        .addHeader("Client-Id", "kimne78kx3ncx6brgo4mv6wki5h1ko")
        .addHeader("X-Device-Id", "PKODYl0nU8NJDFKPSnQH9AncwgH8BaDv")
        .addHeader("sec-ch-ua-platform", "\"Android\"")
        .build()

    client.newCall(request).execute().use { response ->
      if (!response.isSuccessful) throw IOException("Unexpected code $response")
      println(response.body?.string())
    }
  }
}
