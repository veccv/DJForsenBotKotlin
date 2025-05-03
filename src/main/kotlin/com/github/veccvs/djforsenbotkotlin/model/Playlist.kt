package com.github.veccvs.djforsenbotkotlin.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Playlist(
  @JsonProperty("time") var time: Int,
  @JsonProperty("locked") var locked: Boolean,
  @JsonProperty("paused") var paused: Boolean,
  @JsonProperty("current_time") var currentTime: Float,
  @JsonProperty("current") private var _current: PlaylistItem?,
  @JsonProperty("queue") val queue: MutableList<PlaylistItem>,
)
