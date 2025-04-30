package com.github.veccvs.djforsenbotkotlin.utils

import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

@ExtendWith(MockitoExtension::class)
class StreamInfoTest {

    private var originalFactory: ((URI) -> HttpURLConnection)? = null
    private var mockResponseToReturn: JSONObject = JSONObject()

    @BeforeEach
    fun setUp() {
        // Store the original factory
        originalFactory = StreamInfo.httpConnectionFactory

        // Set up a mock connection factory
        StreamInfo.httpConnectionFactory = { uri ->
            object : HttpURLConnection(uri.toURL()) {
                init {
                    responseCode = HttpURLConnection.HTTP_OK
                }

                override fun connect() {
                    connected = true
                }

                override fun disconnect() {
                    connected = false
                }

                override fun usingProxy(): Boolean = false

                override fun getInputStream(): ByteArrayInputStream {
                    // Return the mock response
                    return ByteArrayInputStream(mockResponseToReturn.toString().toByteArray())
                }
            }
        }
    }

    @AfterEach
    fun tearDown() {
        // Reset the factory
        StreamInfo.httpConnectionFactory = originalFactory
        mockResponseToReturn = JSONObject()
    }

    @Test
    fun `streamEnabled returns true when stream is enabled`() {
        // Arrange
        val dataArray = JSONArray()
        dataArray.put(JSONObject().put("type", "live"))
        mockResponseToReturn = JSONObject().put("data", dataArray)

        // Act
        val result = StreamInfo.streamEnabled()

        // Assert
        assertTrue(result)
    }

    @Test
    fun `streamEnabled returns false when stream is not enabled`() {
        // Arrange
        mockResponseToReturn = JSONObject().put("data", JSONArray())

        // Act
        val result = StreamInfo.streamEnabled()

        // Assert
        assertFalse(result)
    }

    @Test
    fun `streamEnabled handles IOException and returns false`() {
        // Arrange
        StreamInfo.httpConnectionFactory = { uri ->
            object : HttpURLConnection(uri.toURL()) {
                init {
                    responseCode = HttpURLConnection.HTTP_OK
                }

                override fun connect() {
                    connected = true
                }

                override fun disconnect() {
                    connected = false
                }

                override fun usingProxy(): Boolean = false

                override fun getInputStream(): ByteArrayInputStream {
                    throw IOException("Test exception")
                }
            }
        }

        // Act
        val result = StreamInfo.streamEnabled()

        // Assert
        assertFalse(result)
    }

    @Test
    fun `streamEnabled handles malformed JSON and returns false`() {
        // Arrange
        StreamInfo.httpConnectionFactory = { uri ->
            object : HttpURLConnection(uri.toURL()) {
                init {
                    responseCode = HttpURLConnection.HTTP_OK
                }

                override fun connect() {
                    connected = true
                }

                override fun disconnect() {
                    connected = false
                }

                override fun usingProxy(): Boolean = false

                override fun getInputStream(): ByteArrayInputStream {
                    return ByteArrayInputStream("Not a valid JSON".toByteArray())
                }
            }
        }

        // Act
        val result = StreamInfo.streamEnabled()

        // Assert
        assertFalse(result)
    }

    @Test
    fun `streamEnabled handles connection errors and returns false`() {
        // Arrange
        StreamInfo.httpConnectionFactory = { uri ->
            throw IOException("Test connection error")
        }

        // Act
        val result = StreamInfo.streamEnabled()

        // Assert
        assertFalse(result)
    }

    @Test
    fun `streamEnabled handles JSONArray access errors and returns false`() {
        // Arrange
        StreamInfo.httpConnectionFactory = { uri ->
            object : HttpURLConnection(uri.toURL()) {
                init {
                    responseCode = HttpURLConnection.HTTP_OK
                }

                override fun connect() {
                    connected = true
                }

                override fun disconnect() {
                    connected = false
                }

                override fun usingProxy(): Boolean = false

                override fun getInputStream(): ByteArrayInputStream {
                    // JSON without 'data' field
                    return ByteArrayInputStream("""{"something": "else"}""".toByteArray())
                }
            }
        }

        // Act
        val result = StreamInfo.streamEnabled()

        // Assert
        assertFalse(result)
    }

    @Test
    fun `streamEnabled handles null httpConnectionFactory and returns false on error`() {
        // Arrange - Save original factory
        val originalFactory = StreamInfo.httpConnectionFactory

        try {
            // Set factory to null to force using default implementation
            StreamInfo.httpConnectionFactory = null

            // This test will simulate a connection error when httpConnectionFactory is null
            // We can't easily mock the default implementation, so we'll just verify that
            // the method doesn't throw an exception and returns false when there's an error

            // Act & Assert
            // The actual connection will fail because we're using invalid credentials
            // but our error handling should catch this and return false
            val result = StreamInfo.streamEnabled()
            assertFalse(result)
        } finally {
            // Restore original factory
            StreamInfo.httpConnectionFactory = originalFactory
        }
    }

    @Test
    fun `StreamInfo class can be instantiated`() {
        // Act
        val streamInfo = StreamInfo()

        // Assert
        assertNotNull(streamInfo)
    }
}
