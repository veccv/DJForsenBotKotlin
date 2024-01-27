package com.github.veccvs.djforsenbotkotlin.model

data class Playlist(
  var time: Int,
  var locked: Boolean,
  var paused: Boolean,
  var currentTime: Int,
  private var _current: PlaylistItem?,
  val queue: MutableList<PlaylistItem>,
)
