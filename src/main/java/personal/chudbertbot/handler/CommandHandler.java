package personal.chudbertbot.handler;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;

public interface CommandHandler {
    String getTrigger();
    String getDescription();
    void handle(ChannelMessageEvent event, String args);
}
