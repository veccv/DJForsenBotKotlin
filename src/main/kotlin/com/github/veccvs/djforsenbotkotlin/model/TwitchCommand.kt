package com.github.veccvs.djforsenbotkotlin.model

import lombok.AllArgsConstructor
import lombok.Data
import lombok.NoArgsConstructor

@AllArgsConstructor
@NoArgsConstructor
@Data
class TwitchCommand(var command: String, var params: List<String>)
