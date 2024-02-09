package com.github.veccvs.djforsenbotkotlin.utils

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object BanPhraseChecker {
  private const val url = "https://forsen.tv/api/v1/banphrases/test"

  fun check(text: String): Boolean {
    val response = postRequest(if (text.isEmpty()) "dd" else text)
    return response.getBoolean("banned")
  }

  fun chatCheck(text: String): Int {
    val response = postRequest(text)
    return if (response.getBoolean("banned"))
      response.getJSONObject("banphrase_data").getInt("length")
    else 0
  }

  private fun postRequest(text: String): JSONObject {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.requestMethod = "POST"
    connection.setRequestProperty("Content-Type", "application/json; utf-8")
    connection.doOutput = true

    val jsonInputString = "{\"message\": \"$text\"}"
    connection.outputStream.use { os -> os.write(jsonInputString.toByteArray()) }

    return connection.inputStream.bufferedReader().use { it.readText() }.let { JSONObject(it) }
  }
}
