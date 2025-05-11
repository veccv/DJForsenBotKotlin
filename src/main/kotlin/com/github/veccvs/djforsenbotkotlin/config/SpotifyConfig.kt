package com.github.veccvs.djforsenbotkotlin.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

/**
 * Configuration class for Spotify API credentials and settings.
 * Includes OAuth authorization settings for accessing user-specific data.
 */
@Configuration
@PropertySource("classpath:application.properties")
class SpotifyConfig {
    @Value("\${spotify.client-id:#{environment.SPOTIFY_CLIENT_ID}}")
    val clientId: String? = null

    @Value("\${spotify.client-secret:#{environment.SPOTIFY_CLIENT_SECRET}}")
    val clientSecret: String? = null

    @Value("\${spotify.token-url:https://accounts.spotify.com/api/token}")
    val tokenUrl: String = "https://accounts.spotify.com/api/token"

    @Value("\${spotify.auth-url:https://accounts.spotify.com/authorize}")
    val authUrl: String = "https://accounts.spotify.com/authorize"

    @Value("\${spotify.redirect-uri:#{environment.SPOTIFY_REDIRECT_URI}}")
    val redirectUri: String? = null

    @Value("\${spotify.scopes:user-read-currently-playing user-read-playback-state}")
    val scopes: String = "user-read-currently-playing user-read-playback-state"

    @Value("\${spotify.user-token:#{environment.SPOTIFY_USER_TOKEN}}")
    val userToken: String? = null

    @Value("\${spotify.refresh-token:#{environment.SPOTIFY_REFRESH_TOKEN}}")
    val refreshToken: String? = null
}
