package com.github.veccvs.djforsenbotkotlin.model

import com.fasterxml.jackson.annotation.JsonProperty
import kotlin.math.pow

data class Video(
  @JsonProperty("id") val id: String,
  @JsonProperty("thumbnails") val thumbnails: List<String>,
  @JsonProperty("title") val title: String,
  @JsonProperty("long_desc") val longDesc: String?,
  @JsonProperty("channel") val channel: String,
  @JsonProperty("views") val views: String,
  @JsonProperty("publish_time") val publishTime: String,
  @JsonProperty("url_suffix") val urlSuffix: String
) {
  @JsonProperty("duration") var duration: Int = 0

  constructor(
    id: String,
    thumbnails: List<String>,
    title: String,
    longDesc: String?,
    channel: String,
    duration: String,
    views: String,
    publishTime: String,
    urlSuffix: String
  ) : this(id, thumbnails, title, longDesc, channel, views, publishTime, urlSuffix) {
    if (duration == "") {
      this.duration = 0
      return
    }
    this.duration =
      duration
        .split(":")
        .reversed()
        .mapIndexed { index, part -> part.toInt() * (60.0.pow(index.toDouble())).toInt() }
        .sum()
  }
}
