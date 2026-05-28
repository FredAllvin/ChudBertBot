package personal.chudbertbot.service;

import com.github.twitch4j.TwitchClient;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;
import personal.chudbertbot.config.TwitchProperties;

import java.util.List;

@Service
public class TwitchClientService implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(TwitchClientService.class);

    private final TwitchClient twitchClient;
    private final TwitchProperties properties;

    public TwitchClientService(TwitchClient twitchClient, TwitchProperties properties) {
        this.twitchClient = twitchClient;
        this.properties = properties;
    }

    public void joinConfiguredChannels() {
        List<String> channels = properties.channels();
        if (channels == null || channels.isEmpty()) {
            log.warn("No channels configured. Set TWITCH_CHANNELS to join channels at startup.");
            return;
        }
        for (String channel : channels) {
            String sanitized = sanitizeChannelName(channel);
            if (!sanitized.isEmpty()) {
                log.info("Joining channel: {}", sanitized);
                twitchClient.getChat().joinChannel(sanitized);
            }
        }
    }

    public TwitchClient getTwitchClient() {
        return twitchClient;
    }

    /**
     * Strips any character outside [a-z0-9_] from a Twitch channel name.
     * Apply this to every channel name sourced from config or user input
     * before passing it to the Twitch library.
     */
    String sanitizeChannelName(String raw) {
        return raw.trim().toLowerCase().replaceAll("[^a-z0-9_]", "");
    }

    @PreDestroy
    @Override
    public void destroy() {
        log.info("Shutting down TwitchClientService: leaving channels and closing connection.");
        try {
            List<String> channels = properties.channels();
            if (channels != null) {
                for (String channel : channels) {
                    String sanitized = sanitizeChannelName(channel);
                    if (!sanitized.isEmpty()) {
                        twitchClient.getChat().leaveChannel(sanitized);
                    }
                }
            }
            twitchClient.close();
            log.info("TwitchClient closed cleanly.");
        } catch (Exception e) {
            log.error("Error during TwitchClient shutdown", e);
        }
    }
}
