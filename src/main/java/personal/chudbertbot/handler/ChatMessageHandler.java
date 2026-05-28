package personal.chudbertbot.handler;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ChatMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatMessageHandler.class);

    public void handle(ChannelMessageEvent event) {
        // No commands yet — stub for future command dispatch.
        // Do not log message content; only metadata to protect user privacy.
        log.debug("Handler received message from user={} in channel={}",
                event.getUser().getName(), event.getChannel().getName());
    }
}
