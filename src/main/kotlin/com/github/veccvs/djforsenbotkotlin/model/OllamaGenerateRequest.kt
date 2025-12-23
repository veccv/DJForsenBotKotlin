package com.github.veccvs.djforsenbotkotlin.model

import com.fasterxml.jackson.annotation.JsonProperty

data class OllamaGenerateRequest(
  @JsonProperty("model") val model: String,
  @JsonProperty("prompt") val prompt: String,
  @JsonProperty("stream") val stream: Boolean,
  @JsonProperty("options") val options: OllamaOptions,
)

data class OllamaOptions(
  @JsonProperty("num_ctx") val numCtx: Int,
  @JsonProperty("num_predict") val numPredict: Int,
)
