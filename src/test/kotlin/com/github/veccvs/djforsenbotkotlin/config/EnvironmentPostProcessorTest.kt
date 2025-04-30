package com.github.veccvs.djforsenbotkotlin.config

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.boot.SpringApplication
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MutablePropertySources

@ExtendWith(MockitoExtension::class)
class EnvironmentPostProcessorTest {

    @Mock
    private lateinit var environment: ConfigurableEnvironment

    @Mock
    private lateinit var application: SpringApplication

    @Mock
    private lateinit var propertySources: MutablePropertySources

    @Test
    fun `postProcessEnvironment should not throw exception`() {
        // Arrange
        val processor = EnvironmentPostProcessor()
        `when`(environment.propertySources).thenReturn(propertySources)
        
        // Act & Assert
        assertDoesNotThrow {
            processor.postProcessEnvironment(environment, application)
        }
    }
}