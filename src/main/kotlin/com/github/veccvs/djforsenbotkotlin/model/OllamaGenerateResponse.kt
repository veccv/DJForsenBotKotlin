package com.github.veccvs.djforsenbotkotlin.model

import com.fasterxml.jackson.annotation.JsonProperty

data class OllamaGenerateResponse(
  @JsonProperty("model") val model: String,
  @JsonProperty("created_at") val createdAt: String,
  @JsonProperty("response") val response: String,
  @JsonProperty("done") val done: Boolean,
  @JsonProperty("done_reason") val doneReason: String?,
  @JsonProperty("context") val context: List<Int>?,
  @JsonProperty("total_duration") val totalDuration: Long?,
  @JsonProperty("load_duration") val loadDuration: Long?,
  @JsonProperty("prompt_eval_count") val promptEvalCount: Int?,
  @JsonProperty("prompt_eval_duration") val promptEvalDuration: Long?,
  @JsonProperty("eval_count") val evalCount: Int?,
  @JsonProperty("eval_duration") val evalDuration: Long?,
)
