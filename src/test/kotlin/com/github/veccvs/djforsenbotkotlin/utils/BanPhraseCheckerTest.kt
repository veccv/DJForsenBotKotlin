package com.github.veccvs.djforsenbotkotlin.utils

import org.json.JSONObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI

@ExtendWith(MockitoExtension::class)
class BanPhraseCheckerTest {

    // Store the last text sent to the mock connection
    private var lastTextSent: String? = null

    // Store the mock response to return
    private var mockResponseToReturn: JSONObject = JSONObject()

    @BeforeEach
    fun setUp() {
        // Set up a mock connection factory
        BanPhraseChecker.httpConnectionFactory = { uri ->
            object : HttpURLConnection(uri.toURL()) {
                private val outputStream = ByteArrayOutputStream()

                init {
                    responseCode = HTTP_OK
                }

                override fun connect() {
                    connected = true
                }

                override fun disconnect() {
                    connected = false
                }

                override fun usingProxy(): Boolean = false

                override fun getOutputStream(): ByteArrayOutputStream {
                    return outputStream
                }

                override fun getInputStream(): ByteArrayInputStream {
                    // Capture the request body for verification
                    val requestBody = outputStream.toString(Charsets.UTF_8.name())
                    // Extract the text from the JSON request
                    val textMatch = Regex("\"message\":\\s*\"([^\"]+)\"").find(requestBody)
                    lastTextSent = textMatch?.groupValues?.get(1)

                    // Return the mock response
                    return ByteArrayInputStream(mockResponseToReturn.toString().toByteArray())
                }
            }
        }
    }

    @AfterEach
    fun tearDown() {
        // Reset the connection factory
        BanPhraseChecker.httpConnectionFactory = null
        lastTextSent = null
        mockResponseToReturn = JSONObject()
    }

    @Test
    fun `check returns true when text is banned`() {
        // Arrange
        mockResponseToReturn = JSONObject().put("banned", true)

        // Act
        val result = BanPhraseChecker.check("banned text")

        // Assert
        assertTrue(result)
        assertEquals("banned text", lastTextSent)
    }

    @Test
    fun `check returns false when text is not banned`() {
        // Arrange
        mockResponseToReturn = JSONObject().put("banned", false)

        // Act
        val result = BanPhraseChecker.check("safe text")

        // Assert
        assertFalse(result)
        assertEquals("safe text", lastTextSent)
    }

    @Test
    fun `check handles empty string by using default value`() {
        // Arrange
        mockResponseToReturn = JSONObject().put("banned", false)

        // Act
        val result = BanPhraseChecker.check("")

        // Assert
        assertFalse(result)
        // Verify that "dd" was sent instead of empty string
        assertEquals("dd", lastTextSent)
    }

    @Test
    fun `chatCheck returns ban length when text is banned`() {
        // Arrange
        val banphraseData = JSONObject().put("length", 600)
        mockResponseToReturn = JSONObject()
            .put("banned", true)
            .put("banphrase_data", banphraseData)

        // Act
        val result = BanPhraseChecker.chatCheck("banned text")

        // Assert
        assertEquals(600, result)
        assertEquals("banned text", lastTextSent)
    }

    @Test
    fun `chatCheck returns 0 when text is not banned`() {
        // Arrange
        mockResponseToReturn = JSONObject().put("banned", false)

        // Act
        val result = BanPhraseChecker.chatCheck("safe text")

        // Assert
        assertEquals(0, result)
        assertEquals("safe text", lastTextSent)
    }
}
