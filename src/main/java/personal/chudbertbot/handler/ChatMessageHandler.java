package personal.chudbertbot.handler;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import personal.chudbertbot.service.CommandRegistry;

@Component
public class ChatMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatMessageHandler.class);
    private static final String COMMAND_PREFIX = "!";

    private final CommandRegistry commandRegistry;

    public ChatMessageHandler(CommandRegistry commandRegistry) {
        this.commandRegistry = commandRegistry;
    }

    public void handle(ChannelMessageEvent event) {
        String message = event.getMessage();
        if (message == null || !message.startsWith(COMMAND_PREFIX)) return;

        log.debug("Processing command from user={} in channel={}",
                event.getUser().getName(), event.getChannel().getName());

        String[] parts = message.substring(COMMAND_PREFIX.length()).split(" ", 2);
        String trigger = COMMAND_PREFIX + parts[0].toLowerCase();
        String args = parts.length > 1 ? sanitizeArgs(parts[1]) : "";

        commandRegistry.findByTrigger(trigger)
                .ifPresentOrElse(
                        handler -> handler.handle(event, args),
                        () -> log.debug("No handler registered for command: {}", trigger)
                );
    }

    private String sanitizeArgs(String args) {
        return args.trim().replaceAll("\\p{Cntrl}", "");
    }
}
