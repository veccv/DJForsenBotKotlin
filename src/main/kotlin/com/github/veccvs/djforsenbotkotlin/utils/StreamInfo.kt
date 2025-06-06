package com.github.veccvs.djforsenbotkotlin.utils

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI

class StreamInfo {
  companion object {
    // Factory for creating HttpURLConnection can be replaced in tests
    @JvmStatic var httpConnectionFactory: ((URI) -> HttpURLConnection)? = null

    @JvmStatic
    fun streamEnabled(): Boolean {
      var cantResponse = false
      val uri = URI("https://api.twitch.tv/helix/streams?user_login=forsen")

      try {
        // Use the factory if provided, otherwise use the default implementation
        val connection =
          httpConnectionFactory?.invoke(uri) ?: uri.toURL().openConnection() as HttpURLConnection

        with(connection) {
          requestMethod = "GET"
          setRequestProperty("Client-ID", "gp762nuuoqcoxypju8c569th9wz7q5")
          setRequestProperty("Authorization", "Bearer 2onenuu5ja8eycvov16x705b9ahjsa")
          setRequestProperty("Accept", "application/vnd.twitchtv.v5+json")

          try {
            inputStream.bufferedReader().use {
              val response = it.readText()
              try {
                val r = JSONObject(response)
                if (r.getJSONArray("data").length() > 0) {
                  cantResponse = true
                }
              } catch (_: Exception) {
                // Handle JSON parsing errors
                cantResponse = false
              }
            }
          } catch (_: Exception) {
            // Handle IO errors
            cantResponse = false
          }
        }
      } catch (_: Exception) {
        // Handle connection errors
        cantResponse = false
      }
      return cantResponse
    }
  }
}
