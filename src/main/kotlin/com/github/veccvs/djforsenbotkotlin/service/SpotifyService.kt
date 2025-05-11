package com.github.veccvs.djforsenbotkotlin.service

import com.github.veccvs.djforsenbotkotlin.config.SpotifyConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.time.Instant
import java.util.*

/** Service for interacting with the Spotify API */
@Service
class SpotifyService(
  @Autowired private val restTemplate: RestTemplate,
  @Autowired private val spotifyConfig: SpotifyConfig,
) {
  private var userAccessToken: String? = spotifyConfig.userToken
  private var userRefreshToken: String? = spotifyConfig.refreshToken
  private var tokenExpiration: Instant? = null

  /**
   * Generates the authorization URL for the user to grant permissions
   *
   * @return The authorization URL
   */
  fun getAuthorizationUrl(): String {
    if (spotifyConfig.clientId.isNullOrBlank() || spotifyConfig.redirectUri.isNullOrBlank()) {
      throw IllegalStateException("Spotify client ID and redirect URI must be configured")
    }

    return UriComponentsBuilder.fromHttpUrl(spotifyConfig.authUrl)
      .queryParam("client_id", spotifyConfig.clientId)
      .queryParam("response_type", "code")
      .queryParam("redirect_uri", spotifyConfig.redirectUri)
      .queryParam("scope", spotifyConfig.scopes)
      .build()
      .toUriString()
  }

  /**
   * Exchanges an authorization code for access and refresh tokens
   *
   * @param code The authorization code received from Spotify
   * @return A map containing the access token, refresh token, and expiration
   */
  fun exchangeCodeForToken(code: String): Map<String, Any> {
    if (
      spotifyConfig.clientId.isNullOrBlank() ||
        spotifyConfig.clientSecret.isNullOrBlank() ||
        spotifyConfig.redirectUri.isNullOrBlank()
    ) {
      throw IllegalStateException(
        "Spotify client ID, client secret, and redirect URI must be configured"
      )
    }

    try {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

      // Add Authorization header with Base64 encoded client_id:client_secret
      val auth = "${spotifyConfig.clientId}:${spotifyConfig.clientSecret}"
      val encodedAuth = Base64.getEncoder().encodeToString(auth.toByteArray())
      headers.set("Authorization", "Basic $encodedAuth")

      val body = LinkedMultiValueMap<String, String>()
      body.add("grant_type", "authorization_code")
      body.add("code", code)
      body.add("redirect_uri", spotifyConfig.redirectUri)

      val entity = HttpEntity(body, headers)

      val response = restTemplate.postForObject(spotifyConfig.tokenUrl, entity, Map::class.java)

      if (response != null) {
        userAccessToken = response["access_token"] as String
        userRefreshToken = response["refresh_token"] as String
        val expiresIn = (response["expires_in"] as Int).toLong()
        tokenExpiration = Instant.now().plusSeconds(expiresIn - 60)

        return mapOf(
          "access_token" to userAccessToken!!,
          "refresh_token" to userRefreshToken!!,
          "expires_in" to expiresIn,
        )
      } else {
        throw Exception("Failed to exchange code for token: Response was null")
      }
    } catch (e: Exception) {
      println("[SPOTIFY SERVICE] Error exchanging code for token: ${e.message}")
      throw e
    }
  }

  /**
   * Refreshes the access token using the refresh token
   *
   * @return The new access token
   */
  private fun refreshAccessToken(): String {
    if (spotifyConfig.clientId.isNullOrBlank() || spotifyConfig.clientSecret.isNullOrBlank()) {
      throw IllegalStateException("Spotify client ID and client secret must be configured")
    }

    if (userRefreshToken.isNullOrBlank()) {
      throw IllegalStateException(
        "No refresh token available. User must authorize the application first."
      )
    }

    try {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

      // Add Authorization header with Base64 encoded client_id:client_secret
      val auth = "${spotifyConfig.clientId}:${spotifyConfig.clientSecret}"
      val encodedAuth = Base64.getEncoder().encodeToString(auth.toByteArray())
      headers.set("Authorization", "Basic $encodedAuth")

      val body = LinkedMultiValueMap<String, String>()
      body.add("grant_type", "refresh_token")
      body.add("refresh_token", userRefreshToken)

      val entity = HttpEntity(body, headers)

      val response = restTemplate.postForObject(spotifyConfig.tokenUrl, entity, Map::class.java)

      if (response != null) {
        userAccessToken = response["access_token"] as String
        val expiresIn = (response["expires_in"] as Int).toLong()
        tokenExpiration = Instant.now().plusSeconds(expiresIn - 60)

        // Some implementations also return a new refresh token
        if (response.containsKey("refresh_token")) {
          userRefreshToken = response["refresh_token"] as String
        }

        return userAccessToken!!
      } else {
        throw Exception("Failed to refresh access token: Response was null")
      }
    } catch (e: Exception) {
      println("[SPOTIFY SERVICE] Error refreshing access token: ${e.message}")
      throw e
    }
  }

  /**
   * Gets a valid user access token, refreshing if necessary
   *
   * @return The user access token
   */
  private fun getUserAccessToken(): String {
    // Check if we have a valid token
    if (
      userAccessToken != null && tokenExpiration != null && Instant.now().isBefore(tokenExpiration)
    ) {
      return userAccessToken!!
    }

    // If we have a refresh token, use it to get a new access token
    if (!userRefreshToken.isNullOrBlank()) {
      return refreshAccessToken()
    }

    throw IllegalStateException(
      "No access token or refresh token available. User must authorize the application first."
    )
  }

  /**
   * Gets the currently playing song for the authenticated user
   *
   * @param accessToken The user's access token
   * @param refreshToken The user's refresh token
   * @return A map containing the song title and URL, or null if no song is playing
   */
  fun getCurrentlyPlayingSong(accessToken: String, refreshToken: String): Map<String, String>? {
    try {
      // Temporarily set the user tokens for this request
      val tempUserAccessToken = userAccessToken
      val tempUserRefreshToken = userRefreshToken

      userAccessToken = accessToken
      userRefreshToken = refreshToken

      try {
        // Get user access token and add it to the request headers
        val token = getUserAccessToken()
        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer $token")

        val entity = HttpEntity<String>(headers)

        try {
          val response: ResponseEntity<Map<*, *>> =
            restTemplate.exchange(
              "https://api.spotify.com/v1/me/player/currently-playing",
              HttpMethod.GET,
              entity,
              Map::class.java,
            )

          // If nothing is playing, Spotify returns 204 No Content
          if (response.statusCode == HttpStatus.NO_CONTENT) {
            return null
          }

          val responseBody = response.body ?: return null

          @Suppress("UNCHECKED_CAST")
          val item = responseBody["item"] as? Map<String, Any> ?: return null
          val songName = item["name"] as? String ?: return null

          @Suppress("UNCHECKED_CAST")
          val artists = item["artists"] as? List<Map<String, Any>> ?: return null
          val artistNames =
            artists
              .mapNotNull { it["name"] as? String }
              .takeIf { it.isNotEmpty() }
              ?.joinToString(", ") ?: "Unknown Artist"

          val externalUrls = item["external_urls"] as? Map<String, String> ?: return null
          val songUrl = externalUrls["spotify"] ?: return null

          return mapOf("title" to "$artistNames - $songName", "url" to songUrl)
        } catch (e: HttpClientErrorException) {
          if (e.statusCode == HttpStatus.UNAUTHORIZED) {
            // Token might be expired, try refreshing
            if (!userRefreshToken.isNullOrBlank()) {
              val newToken = refreshAccessToken()
              // Return the new token to the caller so they can update the user record
              return mapOf("error" to "token_expired", "new_token" to newToken)
            }
          }
          throw e
        }
      } finally {
        // Restore the original tokens
        userAccessToken = tempUserAccessToken
        userRefreshToken = tempUserRefreshToken
      }
    } catch (e: Exception) {
      println("[SPOTIFY SERVICE] Error getting currently playing song: ${e.message}")
      return null
    }
  }

  /**
   * Gets the currently playing song using the tokens from SpotifyConfig This is mainly for testing
   * purposes
   *
   * @return A map containing the song title and URL, or null if no song is playing
   */
  fun getCurrentlyPlayingSong(): Map<String, String>? {
    if (spotifyConfig.userToken.isNullOrBlank() || spotifyConfig.refreshToken.isNullOrBlank()) {
      throw IllegalStateException("No user tokens configured in SpotifyConfig")
    }

    return getCurrentlyPlayingSong(spotifyConfig.userToken!!, spotifyConfig.refreshToken!!)
  }

  /**
   * Sets the user tokens manually (useful for testing or when tokens are obtained externally)
   *
   * @param accessToken The user access token
   * @param refreshToken The user refresh token
   * @param expiresIn The token expiration in seconds
   */
  fun setUserTokens(accessToken: String, refreshToken: String, expiresIn: Long) {
    userAccessToken = accessToken
    userRefreshToken = refreshToken
    tokenExpiration = Instant.now().plusSeconds(expiresIn - 60)
  }
}
