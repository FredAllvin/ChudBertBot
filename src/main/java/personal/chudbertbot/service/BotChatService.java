package personal.chudbertbot.service;

import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import personal.chudbertbot.exception.MessageGuardException;

/**
 * Safe message sender for all bot commands.
 *
 * Every outgoing message passes through the full safety stack before being sent:
 *   1. Blank / null check
 *   2. 500-character truncation (Twitch hard limit)
 *   3. ChatGuard    — emoji count (≤30) and @mention count (≤1)
 *   4. BanphraseClient — pajbot banphrase check
 *
 * CommandHandler implementations MUST use this service — never call
 * twitchClient.getChat().sendMessage() directly.
 */
@Service
public class BotChatService {

    private static final Logger log = LoggerFactory.getLogger(BotChatService.class);
    private static final int MAX_LENGTH = 500;

    private final TwitchClient twitchClient;
    private final ChatGuard chatGuard;
    private final BanphraseClient banphraseClient;

    public BotChatService(TwitchClient twitchClient,
                          ChatGuard chatGuard,
                          BanphraseClient banphraseClient) {
        this.twitchClient = twitchClient;
        this.chatGuard = chatGuard;
        this.banphraseClient = banphraseClient;
    }

    /** Convenience overload — sends to the same channel as the triggering event. */
    public void sendMessage(ChannelMessageEvent triggerEvent, String message) {
        sendMessage(triggerEvent.getChannel().getName(), message);
    }

    /**
     * Validates and sends a message to the specified channel.
     * Silently drops the message (with a WARN log) if it fails any check.
     */
    public void sendMessage(String channel, String message) {
        if (message == null || message.isBlank()) return;

        String truncated = message.length() > MAX_LENGTH
                ? message.substring(0, MAX_LENGTH - 3) + "..."
                : message;

        try {
            chatGuard.validate(truncated);
        } catch (MessageGuardException e) {
            log.warn("Message to #{} dropped by guard: {}", channel, e.getMessage());
            return;
        }

        if (!banphraseClient.isSafe(truncated)) {
            log.warn("Message to #{} dropped by banphrase check", channel);
            return;
        }

        twitchClient.getChat().sendMessage(channel, truncated);
        log.debug("Sent message to #{}", channel);
    }
}
