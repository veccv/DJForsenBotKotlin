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
import java.net.HttpURLConnection
import java.net.URI

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
}
