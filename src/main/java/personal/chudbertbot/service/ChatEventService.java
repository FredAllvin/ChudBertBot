package personal.chudbertbot.service;

import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import personal.chudbertbot.handler.ChatMessageHandler;

@Service
public class ChatEventService {

    private static final Logger log = LoggerFactory.getLogger(ChatEventService.class);

    private final TwitchClientService twitchClientService;
    private final ChatMessageHandler chatMessageHandler;

    public ChatEventService(TwitchClientService twitchClientService,
                            ChatMessageHandler chatMessageHandler) {
        this.twitchClientService = twitchClientService;
        this.chatMessageHandler = chatMessageHandler;
    }

    @PostConstruct
    public void init() {
        TwitchClient client = twitchClientService.getTwitchClient();

        // Register listeners BEFORE joining channels to avoid a race condition
        // where messages arrive in the window before handlers are attached.
        client.getEventManager().onEvent(ChannelMessageEvent.class, event -> {
            log.debug("Message received in #{} from {}",
                    event.getChannel().getName(), event.getUser().getName());
            chatMessageHandler.handle(event);
        });

        twitchClientService.joinConfiguredChannels();
        log.info("ChatEventService initialized. Bot is connected and listening.");
    }
}
