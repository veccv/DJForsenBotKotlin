package com.github.veccvs.djforsenbotkotlin.model.twitch

import com.fasterxml.jackson.annotation.JsonProperty

data class TwitchUser(
  @JsonProperty("id") val id: String,
  @JsonProperty("login") val login: String,
  @JsonProperty("display_name") val displayName: String,
  @JsonProperty("type") val type: String,
  @JsonProperty("broadcaster_type") val broadcasterType: String,
  @JsonProperty("description") val description: String,
  @JsonProperty("profile_image_url") val profileImageUrl: String,
  @JsonProperty("offline_image_url") val offlineImageUrl: String,
  @JsonProperty("view_count") val viewCount: Int,
  @JsonProperty("created_at") val createdAt: String,
  @JsonProperty("email") val email: String?
)

data class TwitchResponse(val data: List<TwitchUser>)
