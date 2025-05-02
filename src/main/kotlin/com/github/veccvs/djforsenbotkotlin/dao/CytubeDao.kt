package com.github.veccvs.djforsenbotkotlin.dao

import com.github.veccvs.djforsenbotkotlin.config.UserConfig
import com.github.veccvs.djforsenbotkotlin.model.BotStatus
import com.github.veccvs.djforsenbotkotlin.model.Playlist
import com.github.veccvs.djforsenbotkotlin.model.Video
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForEntity

@Component
class CytubeDao
@Autowired
constructor(private val restTemplate: RestTemplate, private val userConfig: UserConfig) {

  private val url = userConfig.daoAddress

  fun getPlaylist(): Playlist? {
    val response: ResponseEntity<Playlist> = restTemplate.getForEntity("$url/playlist")
    return response.body
  }

  fun getBotStatus(): BotStatus? {
    try {
      val response: ResponseEntity<BotStatus> = restTemplate.getForEntity("$url/bot-status")
      return response.body
    } catch (e: Exception) {
      return null
    }
  }

  fun sendMessage(message: String): String? {
    val response: ResponseEntity<String> =
      restTemplate.postForEntity("$url/send-message?message=$message", null, String::class.java)
    return response.body
  }

  fun addVideo(videoUrl: String): Playlist? {
    val response: ResponseEntity<Playlist> =
      restTemplate.postForEntity("$url/add-video?link=$videoUrl", null, Playlist::class.java)
    return response.body
  }

  fun searchVideos(query: String): List<Video>? {
    val response: ResponseEntity<List<LinkedHashMap<String, Any>>> =
      restTemplate.getForEntity("$url/search-video?query=$query")
    return response.body?.map {
      val thumbnails =
        if (it["thumbnails"] is List<*>) {
          (it["thumbnails"] as List<*>).filterIsInstance<String>()
        } else {
          emptyList()
        }
      Video(
        it["id"] as? String ?: "",
        thumbnails,
        it["title"] as? String ?: "",
        it["longDesc"] as? String ?: "",
        it["channel"] as? String ?: "",
        it["duration"] as? String ?: "",
        it["views"] as? String ?: "",
        it["publishTime"] as? String ?: "",
        it["urlSuffix"] as? String ?: "",
      )
    }
  }

  fun skipVideo() {
    restTemplate.put("$url/skip-song", null)
  }

  /**
   * Removes a video from the playlist by its URL
   *
   * @param videoUrl The URL of the video to remove
   * @return The updated playlist, or null if the operation failed
   */
  fun removeVideo(videoUrl: String): Playlist? {
    val response: ResponseEntity<Playlist> =
      restTemplate.postForEntity("$url/remove-video?link=$videoUrl", null, Playlist::class.java)
    return response.body
  }
}
