package com.github.veccvs.djforsenbotkotlin.model

import com.fasterxml.jackson.annotation.JsonProperty

class BotStatus(@JsonProperty("bot_enabled") val botEnabled: Boolean)
