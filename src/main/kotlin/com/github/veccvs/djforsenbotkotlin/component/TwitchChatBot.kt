package com.github.veccvs.djforsenbotkotlin.component

import com.github.veccvs.djforsenbotkotlin.config.UserConfig
import com.github.veccvs.djforsenbotkotlin.dao.CytubeDao
import com.github.veccvs.djforsenbotkotlin.service.CommandService
import jakarta.annotation.PostConstruct
import org.pircbotx.Configuration
import org.pircbotx.PircBotX
import org.pircbotx.cap.EnableCapHandler
import org.pircbotx.hooks.ListenerAdapter
import org.pircbotx.hooks.events.MessageEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Component
class TwitchChatBot
@Autowired
constructor(
  private val cytubeDao: CytubeDao,
  @Lazy private val commandService: CommandService,
  @Lazy private val userConfig: UserConfig,
) : ListenerAdapter() {
  private lateinit var bot: PircBotX

  override fun onMessage(event: MessageEvent) {
    if (cytubeDao.getBotStatus() != null)
      cytubeDao.sendMessage("${event.user?.nick}: ${event.message}")
    commandService.commandHandler(event.user?.nick ?: "", event.message, event.channel.name)
  }

  fun sendMessage(channel: String, message: String) {
    bot.sendIRC().message(channel, message)
  }

  @PostConstruct
  fun startBot() {
    val configuration =
      Configuration.Builder()
        .setName("forsenJAMMER")
        .setServerPassword("oauth:8ch29qb40ryykrvb33xzkawix07yi8")
        .addServer("irc.chat.twitch.tv")
        .addAutoJoinChannel(userConfig.channelName ?: "#veccv")
        .addListener(this)
        .setAutoReconnect(true)
        .addCapHandler(EnableCapHandler("twitch.tv/membership"))
        .buildConfiguration()
    bot = PircBotX(configuration)
    Thread { bot.startBot() }.start()
  }
}
