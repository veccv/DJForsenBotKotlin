package com.github.veccvs.djforsenbotkotlin.config

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestTemplate

class RestTemplateConfigTest {

    @Test
    fun `restTemplate should return a RestTemplate instance`() {
        // Arrange
        val config = RestTemplateConfig()
        
        // Act
        val restTemplate = config.restTemplate()
        
        // Assert
        assertNotNull(restTemplate)
        assertTrue(restTemplate is RestTemplate)
    }
}