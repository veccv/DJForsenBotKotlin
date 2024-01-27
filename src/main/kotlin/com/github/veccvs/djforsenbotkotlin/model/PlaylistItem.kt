package com.github.veccvs.djforsenbotkotlin.model

data class PlaylistItem(
  val uid: Int,
  val temp: Boolean,
  val username: String,
  val link: MediaLink,
  val title: String,
  val duration: Int,
)
