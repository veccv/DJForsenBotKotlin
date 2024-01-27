import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI

class StreamInfo {
  companion object {
    @JvmStatic
    fun streamEnabled(): Boolean {
      var cantResponse = false
      val url = URI("https://api.twitch.tv/helix/streams?user_login=forsen").toURL()
      with(url.openConnection() as HttpURLConnection) {
        requestMethod = "GET"
        setRequestProperty("Client-ID", "gp762nuuoqcoxypju8c569th9wz7q5")
        setRequestProperty("Authorization", "Bearer 2onenuu5ja8eycvov16x705b9ahjsa")
        setRequestProperty("Accept", "application/vnd.twitchtv.v5+json")

        inputStream.bufferedReader().use {
          val response = it.readText()
          val r = JSONObject(response)
          if (r.getJSONArray("data").length() > 0) {
            cantResponse = true
          }
        }
      }
      return cantResponse
    }
  }
}
