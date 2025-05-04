package com.github.veccvs.djforsenbotkotlin.utils

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI

object BanPhraseChecker {
  private const val URL_ENDPOINT = "https://forsen.tv/api/v1/banphrases/test"

  // For testing purposes
  internal var httpConnectionFactory: ((URI) -> HttpURLConnection)? = null

  fun check(text: String): Boolean {
    val response = postRequest(text.ifEmpty { "dd" })
    return response.getBoolean("banned")
  }

  fun chatCheck(text: String): Int {
    val response = postRequest(text)
    return if (response.getBoolean("banned"))
      response.getJSONObject("ban-phrase_data").getInt("length")
    else 0
  }

  internal fun postRequest(text: String): JSONObject {
    val uri = URI(URL_ENDPOINT)
    val connection =
      httpConnectionFactory?.invoke(uri) ?: uri.toURL().openConnection() as HttpURLConnection
    connection.requestMethod = "POST"
    connection.setRequestProperty("Content-Type", "application/json; utf-8")
    connection.doOutput = true

    val jsonInputString = "{\"message\": \"$text\"}"
    connection.outputStream.use { os -> os.write(jsonInputString.toByteArray()) }

    return connection.inputStream.bufferedReader().use { it.readText() }.let { JSONObject(it) }
  }
}
