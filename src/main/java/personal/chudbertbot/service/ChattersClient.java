package personal.chudbertbot.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fetches the live chatters list from Twitch's TMI API and caches it for 60 seconds.
 *
 * Use isChatter(channel, username) before sending any @mention to verify the user
 * is actually in chat. Fails safe (returns false) if the API is unavailable.
 *
 * API: GET https://tmi.twitch.tv/group/user/{channel}/chatters
 */
@Service
public class ChattersClient {

    private static final Logger log = LoggerFactory.getLogger(ChattersClient.class);
    private static final Duration CACHE_TTL = Duration.ofSeconds(60);
    private static final String CHATTERS_URL =
            "https://tmi.twitch.tv/group/user/{channel}/chatters";

    private final RestClient restClient;
    private final Map<String, CachedChatters> cache = new ConcurrentHashMap<>();

    public ChattersClient() {
        this.restClient = RestClient.create();
    }

    /**
     * Returns true if the given username is currently in the channel's chat.
     * Case-insensitive. Returns false on API failure to prevent accidental pings.
     */
    public boolean isChatter(String channel, String username) {
        return fetchChatters(channel.toLowerCase()).contains(username.toLowerCase());
    }

    /**
     * Returns the full set of usernames in a channel's chat (lowercase).
     * Results are cached for 60 seconds.
     */
    public Set<String> getChatters(String channel) {
        return fetchChatters(channel.toLowerCase());
    }

    private Set<String> fetchChatters(String channel) {
        CachedChatters cached = cache.get(channel);
        if (cached != null && !cached.isExpired()) {
            return cached.usernames();
        }

        try {
            ChattersResponse response = restClient.get()
                    .uri(CHATTERS_URL, channel)
                    .retrieve()
                    .body(ChattersResponse.class);

            if (response == null || response.chatters() == null) {
                log.warn("Empty chatters response for channel '{}'", channel);
                return Set.of();
            }

            Set<String> all = collectUsernames(response.chatters());
            cache.put(channel, new CachedChatters(all, Instant.now()));
            log.debug("Cached {} chatters for channel '{}'", all.size(), channel);
            return all;

        } catch (Exception e) {
            log.warn("Failed to fetch chatters for '{}': {} — failing safe (returning empty set)",
                    channel, e.getMessage());
            return Set.of();
        }
    }

    private Set<String> collectUsernames(ChattersGroups groups) {
        Set<String> all = new HashSet<>();
        addLower(all, groups.broadcaster());
        addLower(all, groups.vips());
        addLower(all, groups.moderators());
        addLower(all, groups.staff());
        addLower(all, groups.admins());
        addLower(all, groups.globalMods());
        addLower(all, groups.viewers());
        return Collections.unmodifiableSet(all);
    }

    private void addLower(Set<String> target, List<String> source) {
        if (source != null) source.stream().map(String::toLowerCase).forEach(target::add);
    }

    private record CachedChatters(Set<String> usernames, Instant fetchedAt) {
        boolean isExpired() {
            return Instant.now().isAfter(fetchedAt.plus(CACHE_TTL));
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChattersResponse(
            @JsonProperty("chatter_count") int chatterCount,
            ChattersGroups chatters
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChattersGroups(
            List<String> broadcaster,
            List<String> vips,
            List<String> moderators,
            List<String> staff,
            List<String> admins,
            @JsonProperty("global_mods") List<String> globalMods,
            List<String> viewers
    ) {}
}
