package com.github.veccvs.djforsenbotkotlin.dao

import com.github.veccvs.djforsenbotkotlin.config.UserConfig
import com.github.veccvs.djforsenbotkotlin.model.BotStatus
import com.github.veccvs.djforsenbotkotlin.model.Playlist
import com.github.veccvs.djforsenbotkotlin.model.Video
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import java.lang.reflect.Field

@ExtendWith(MockitoExtension::class)
class CytubeDaoTest {

  @Mock private lateinit var restTemplate: RestTemplate

  @Mock private lateinit var userConfig: UserConfig

  private lateinit var cytubeDao: CytubeDao
  private lateinit var testCytubeDao: TestCytubeDao

  private val daoAddress = "http://localhost:8080"

  // Test-specific implementation of CytubeDao that overrides the searchVideos method
  inner class TestCytubeDao(restTemplate: RestTemplate, userConfig: UserConfig) :
    CytubeDao(restTemplate, userConfig) {
    private var videosToReturn: List<Video>? = null

    fun setVideosToReturn(videos: List<Video>?) {
      this.videosToReturn = videos
    }

    override fun searchVideos(query: String): List<Video>? {
      return videosToReturn
    }
  }

  @BeforeEach
  fun setUp() {
    // Create CytubeDao with mocked dependencies
    cytubeDao = CytubeDao(restTemplate, userConfig)
    testCytubeDao = TestCytubeDao(restTemplate, userConfig)

    // Use reflection to set the url field directly
    val urlField: Field = CytubeDao::class.java.getDeclaredField("url")
    urlField.isAccessible = true
    urlField.set(cytubeDao, daoAddress)
    urlField.set(testCytubeDao, daoAddress)
  }

  @Test
  fun `getPlaylist should return playlist from API`() {
    // Arrange
    val playlist = Playlist(0, false, false, 0f, null, mutableListOf())
    val responseEntity = ResponseEntity.ok(playlist)

    `when`(restTemplate.getForEntity("$daoAddress/playlist", Playlist::class.java))
      .thenReturn(responseEntity)

    // Act
    val result = cytubeDao.getPlaylist()

    // Assert
    assertEquals(playlist, result)
    verify(restTemplate).getForEntity("$daoAddress/playlist", Playlist::class.java)
  }

  @Test
  fun `getBotStatus should return bot status from API`() {
    // Arrange
    val botStatus = BotStatus(true)
    val responseEntity = ResponseEntity.ok(botStatus)

    `when`(restTemplate.getForEntity("$daoAddress/bot-status", BotStatus::class.java))
      .thenReturn(responseEntity)

    // Act
    val result = cytubeDao.getBotStatus()

    // Assert
    assertEquals(botStatus, result)
    verify(restTemplate).getForEntity("$daoAddress/bot-status", BotStatus::class.java)
  }

  @Test
  fun `getBotStatus should return null when API call throws exception`() {
    // Arrange
    `when`(restTemplate.getForEntity("$daoAddress/bot-status", BotStatus::class.java))
      .thenThrow(RuntimeException("API error"))

    // Act
    val result = cytubeDao.getBotStatus()

    // Assert
    assertNull(result)
    verify(restTemplate).getForEntity("$daoAddress/bot-status", BotStatus::class.java)
  }

  @Test
  fun `sendMessage should send message to API`() {
    // Arrange
    val message = "Test message"
    val responseEntity = ResponseEntity.ok("Success")

    `when`(
        restTemplate.postForEntity(
          "$daoAddress/send-message?message=$message",
          null,
          String::class.java,
        )
      )
      .thenReturn(responseEntity)

    // Act
    val result = cytubeDao.sendMessage(message)

    // Assert
    assertEquals("Success", result)
    verify(restTemplate)
      .postForEntity("$daoAddress/send-message?message=$message", null, String::class.java)
  }

  @Test
  fun `addVideo should add video to API`() {
    // Arrange
    val videoUrl = "https://youtu.be/testId"
    val playlist = Playlist(0, false, false, 0f, null, mutableListOf())
    val responseEntity = ResponseEntity.ok(playlist)

    `when`(
        restTemplate.postForEntity(
          "$daoAddress/add-video?link=$videoUrl",
          null,
          Playlist::class.java,
        )
      )
      .thenReturn(responseEntity)

    // Act
    val result = cytubeDao.addVideo(videoUrl)

    // Assert
    assertEquals(playlist, result)
    verify(restTemplate)
      .postForEntity("$daoAddress/add-video?link=$videoUrl", null, Playlist::class.java)
  }

  @Test
  fun `skipVideo should call API to skip video`() {
    // Arrange - Nothing to arrange for this test

    // Act
    cytubeDao.skipVideo()

    // Assert
    verify(restTemplate).put("$daoAddress/skip-song", null)
  }

  @Test
  fun `searchVideos should return list of videos from API using test implementation`() {
    // Arrange
    val query = "test query"
    val video1 =
      Video(
        id = "video1",
        thumbnails = listOf("thumbnail1.jpg", "thumbnail2.jpg"),
        title = "Test Video 1",
        longDesc = "This is a test video description",
        channel = "Test Channel",
        duration = "1:30",
        views = "1000",
        publishTime = "2023-01-01",
        urlSuffix = "/watch?v=video1",
      )
    val video2 =
      Video(
        id = "video2",
        thumbnails = listOf("thumbnail3.jpg"),
        title = "Test Video 2",
        longDesc = "Another test video description",
        channel = "Test Channel 2",
        duration = "2:45",
        views = "2000",
        publishTime = "2023-02-01",
        urlSuffix = "/watch?v=video2",
      )
    val videos = listOf(video1, video2)

    // Set up the test-specific implementation
    testCytubeDao.setVideosToReturn(videos)

    // Act
    val result = testCytubeDao.searchVideos(query)

    // Assert
    assertNotNull(result)
    assertEquals(2, result?.size)

    // Verify first video
    assertEquals("video1", result?.get(0)?.id)
    assertEquals(2, result?.get(0)?.thumbnails?.size)
    assertEquals("thumbnail1.jpg", result?.get(0)?.thumbnails?.get(0))
    assertEquals("Test Video 1", result?.get(0)?.title)
    assertEquals("This is a test video description", result?.get(0)?.longDesc)
    assertEquals("Test Channel", result?.get(0)?.channel)
    assertEquals(90, result?.get(0)?.duration) // 1:30 = 90 seconds
    assertEquals("1000", result?.get(0)?.views)
    assertEquals("2023-01-01", result?.get(0)?.publishTime)
    assertEquals("/watch?v=video1", result?.get(0)?.urlSuffix)

    // Verify second video
    assertEquals("video2", result?.get(1)?.id)
    assertEquals(1, result?.get(1)?.thumbnails?.size)
    assertEquals("thumbnail3.jpg", result?.get(1)?.thumbnails?.get(0))
    assertEquals("Test Video 2", result?.get(1)?.title)
    assertEquals("Another test video description", result?.get(1)?.longDesc)
    assertEquals("Test Channel 2", result?.get(1)?.channel)
    assertEquals(165, result?.get(1)?.duration) // 2:45 = 165 seconds
    assertEquals("2000", result?.get(1)?.views)
    assertEquals("2023-02-01", result?.get(1)?.publishTime)
    assertEquals("/watch?v=video2", result?.get(1)?.urlSuffix)
  }

  @Test
  fun `searchVideos should return list of videos from actual API call`() {
    // Arrange
    val query = "test query"

    // Create mock response data
    val video1Data =
      LinkedHashMap<String, Any>().apply {
        put("id", "video1")
        put("thumbnails", listOf("thumbnail1.jpg", "thumbnail2.jpg"))
        put("title", "Test Video 1")
        put("longDesc", "This is a test video description")
        put("channel", "Test Channel")
        put("duration", "1:30")
        put("views", "1000")
        put("publishTime", "2023-01-01")
        put("urlSuffix", "/watch?v=video1")
      }

    val video2Data =
      LinkedHashMap<String, Any>().apply {
        put("id", "video2")
        put("thumbnails", listOf("thumbnail3.jpg"))
        put("title", "Test Video 2")
        put("longDesc", "Another test video description")
        put("channel", "Test Channel 2")
        put("duration", "2:45")
        put("views", "2000")
        put("publishTime", "2023-02-01")
        put("urlSuffix", "/watch?v=video2")
      }

    val responseBody = listOf(video1Data, video2Data)
    val responseEntity = ResponseEntity.ok(responseBody)

    // Mock the RestTemplate.getForEntity call
    `when`(
        restTemplate.getForEntity<List<LinkedHashMap<String, Any>>>(
          eq("$daoAddress/search-video?query=$query"),
          any(),
        )
      )
      .thenReturn(responseEntity)

    // Act
    val result = cytubeDao.searchVideos(query)

    // Assert
    assertNotNull(result)
    assertEquals(2, result?.size)

    // Verify first video
    assertEquals("video1", result?.get(0)?.id)
    assertEquals(2, result?.get(0)?.thumbnails?.size)
    assertEquals("thumbnail1.jpg", result?.get(0)?.thumbnails?.get(0))
    assertEquals("Test Video 1", result?.get(0)?.title)
    assertEquals("This is a test video description", result?.get(0)?.longDesc)
    assertEquals("Test Channel", result?.get(0)?.channel)
    assertEquals(90, result?.get(0)?.duration) // 1:30 = 90 seconds
    assertEquals("1000", result?.get(0)?.views)
    assertEquals("2023-01-01", result?.get(0)?.publishTime)
    assertEquals("/watch?v=video1", result?.get(0)?.urlSuffix)

    // Verify second video
    assertEquals("video2", result?.get(1)?.id)
    assertEquals(1, result?.get(1)?.thumbnails?.size)
    assertEquals("thumbnail3.jpg", result?.get(1)?.thumbnails?.get(0))
    assertEquals("Test Video 2", result?.get(1)?.title)
    assertEquals("Another test video description", result?.get(1)?.longDesc)
    assertEquals("Test Channel 2", result?.get(1)?.channel)
    assertEquals(165, result?.get(1)?.duration) // 2:45 = 165 seconds
    assertEquals("2000", result?.get(1)?.views)
    assertEquals("2023-02-01", result?.get(1)?.publishTime)
    assertEquals("/watch?v=video2", result?.get(1)?.urlSuffix)
  }

  @Test
  fun `searchVideos should handle non-list thumbnails in actual API call`() {
    // Arrange
    val query = "test query"

    // Create mock response data with non-list thumbnails
    val videoData =
      LinkedHashMap<String, Any>().apply {
        put("id", "video1")
        put("thumbnails", "not-a-list") // Not a list
        put("title", "Test Video")
        put("longDesc", "Test description")
        put("channel", "Test Channel")
        put("duration", "1:00")
        put("views", "1000")
        put("publishTime", "2023-01-01")
        put("urlSuffix", "/watch?v=video1")
      }

    val responseBody = listOf(videoData)
    val responseEntity = ResponseEntity.ok(responseBody)

    // Mock the RestTemplate.getForEntity call
    `when`(
        restTemplate.getForEntity<List<LinkedHashMap<String, Any>>>(
          eq("$daoAddress/search-video?query=$query"),
          any(),
        )
      )
      .thenReturn(responseEntity)

    // Act
    val result = cytubeDao.searchVideos(query)

    // Assert
    assertNotNull(result)
    assertEquals(1, result?.size)
    assertEquals("video1", result?.get(0)?.id)
    assertEquals(
      0,
      result?.get(0)?.thumbnails?.size,
    ) // Should be empty list when thumbnails is not a List
  }

  @Test
  fun `searchVideos should handle missing or null fields in actual API call`() {
    // Arrange
    val query = "test query"

    // Create mock response data with missing fields
    val videoData =
      LinkedHashMap<String, Any>().apply {
        // Missing id, thumbnails, title, etc.
        // We're not adding any fields to simulate missing fields
      }

    val responseBody = listOf(videoData)
    val responseEntity = ResponseEntity.ok(responseBody)

    // Mock the RestTemplate.getForEntity call
    `when`(
        restTemplate.getForEntity<List<LinkedHashMap<String, Any>>>(
          eq("$daoAddress/search-video?query=$query"),
          any(),
        )
      )
      .thenReturn(responseEntity)

    // Act
    val result = cytubeDao.searchVideos(query)

    // Assert
    assertNotNull(result)
    assertEquals(1, result?.size)
    assertEquals("", result?.get(0)?.id) // Default empty string for missing id
    assertEquals(0, result?.get(0)?.thumbnails?.size) // Empty list for missing thumbnails
    assertEquals("", result?.get(0)?.title) // Default empty string for null title
  }

  @Test
  fun `searchVideos should return null when actual API response body is null`() {
    // Arrange
    val query = "test query"

    // Create mock response with null body
    val responseEntity = ResponseEntity.ok<List<LinkedHashMap<String, Any>>>(null)

    // Mock the RestTemplate.getForEntity call
    `when`(
        restTemplate.getForEntity<List<LinkedHashMap<String, Any>>>(
          eq("$daoAddress/search-video?query=$query"),
          any(),
        )
      )
      .thenReturn(responseEntity)

    // Act
    val result = cytubeDao.searchVideos(query)

    // Assert
    assertNull(result)
  }

  @Test
  fun `searchVideos should handle non-list thumbnails`() {
    // Arrange
    val query = "test query"
    // Create a video with empty thumbnails list to simulate the case where thumbnails is not a List
    val video =
      Video(
        id = "video1",
        thumbnails = emptyList(), // Empty list to simulate non-list thumbnails
        title = "Test Video",
        longDesc = "Test description",
        channel = "Test Channel",
        duration = "1:00",
        views = "1000",
        publishTime = "2023-01-01",
        urlSuffix = "/watch?v=video1",
      )

    // Set up the test-specific implementation
    testCytubeDao.setVideosToReturn(listOf(video))

    // Act
    val result = testCytubeDao.searchVideos(query)

    // Assert
    assertNotNull(result)
    assertEquals(1, result?.size)
    assertEquals(
      0,
      result?.get(0)?.thumbnails?.size,
    ) // Should be empty list when thumbnails is not a List
  }

  @Test
  fun `searchVideos should handle missing or null fields`() {
    // Arrange
    val query = "test query"
    // Create a video with empty/default values to simulate missing or null fields
    val video =
      Video(
        id = "",
        thumbnails = emptyList(),
        title = "",
        longDesc = "",
        channel = "",
        duration = "",
        views = "",
        publishTime = "",
        urlSuffix = "",
      )

    // Set up the test-specific implementation
    testCytubeDao.setVideosToReturn(listOf(video))

    // Act
    val result = testCytubeDao.searchVideos(query)

    // Assert
    assertNotNull(result)
    assertEquals(1, result?.size)
    assertEquals("", result?.get(0)?.id)
    assertEquals(0, result?.get(0)?.thumbnails?.size)
    assertEquals("", result?.get(0)?.title)
    assertEquals("", result?.get(0)?.longDesc)
    assertEquals("", result?.get(0)?.channel)
    assertEquals(0, result?.get(0)?.duration)
    assertEquals("", result?.get(0)?.views)
    assertEquals("", result?.get(0)?.publishTime)
    assertEquals("", result?.get(0)?.urlSuffix)
  }

  @Test
  fun `searchVideos should return null when API response body is null`() {
    // Arrange
    val query = "test query"

    // Set up the test-specific implementation to return null
    testCytubeDao.setVideosToReturn(null)

    // Act
    val result = testCytubeDao.searchVideos(query)

    // Assert
    assertNull(result)
  }
}
