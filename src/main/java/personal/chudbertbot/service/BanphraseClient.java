package personal.chudbertbot.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import personal.chudbertbot.config.TwitchProperties;

import java.util.List;

/**
 * Checks outgoing messages against pajbot banphrases before they are sent.
 *
 * Priority:
 *   1. Channel-specific pajbot  →  POST {TWITCH_PAJBOT_URL}/api/v1/banphrases/test
 *   2. paj.pajbot.com central   →  GET  paj.pajbot.com/api/channel/{TWITCH_STREAMER_ID}/moderation/check_message
 *   3. Neither configured       →  warn once, allow all messages (fail-open)
 *
 * Both APIs fail-open on network error so a temporary outage doesn't silence the bot.
 *
 * Configuration:
 *   TWITCH_PAJBOT_URL   — e.g. https://nymn.pajbot.com  (no trailing slash)
 *   TWITCH_STREAMER_ID  — Twitch user ID of the streamer, e.g. 62300805
 */
@Service
public class BanphraseClient {

    private static final Logger log = LoggerFactory.getLogger(BanphraseClient.class);

    private final TwitchProperties properties;
    private final RestClient restClient;

    public BanphraseClient(TwitchProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    /**
     * Returns true if the message is safe to send (not matched by any banphrase).
     * Returns true on API failure (fail-open) with a warning log.
     */
    public boolean isSafe(String message) {
        String pajbotUrl = properties.pajbotUrl();
        String streamerId = properties.streamerId();

        if (pajbotUrl != null && !pajbotUrl.isBlank()) {
            return checkChannelPajbot(pajbotUrl, message);
        }
        if (streamerId != null && !streamerId.isBlank()) {
            return checkPajApi(streamerId, message);
        }

        log.warn("No banphrase API configured — set TWITCH_PAJBOT_URL or TWITCH_STREAMER_ID. "
                + "Messages are sent without banphrase checking.");
        return true;
    }

    // --- Channel-specific pajbot (POST /api/v1/banphrases/test) ---

    private boolean checkChannelPajbot(String baseUrl, String message) {
        try {
            V1Response response = restClient.post()
                    .uri(baseUrl + "/api/v1/banphrases/test")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new V1Request(message))
                    .retrieve()
                    .body(V1Response.class);

            if (response == null) {
                log.warn("Null response from {} — allowing (fail-open)", baseUrl);
                return true;
            }
            if (response.banned()) {
                String name = response.banphraseData() != null ? response.banphraseData().name() : "unknown";
                int id   = response.banphraseData() != null ? response.banphraseData().id()   : -1;
                log.warn("Banphrase blocked: '{}' (id={})", name, id);
            }
            return !response.banned();

        } catch (Exception e) {
            log.warn("Pajbot check failed ({}): {} — allowing (fail-open)", baseUrl, e.getMessage());
            return true;
        }
    }

    private record V1Request(String message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record V1Response(
            boolean banned,
            @JsonProperty("banphrase_data") BanphraseData banphraseData
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BanphraseData(int id, String name) {}

    // --- paj.pajbot.com central API (GET /api/channel/{id}/moderation/check_message) ---

    private boolean checkPajApi(String streamerId, String message) {
        try {
            PajResponse response = restClient.get()
                    .uri("https://paj.pajbot.com/api/channel/{id}/moderation/check_message?message={msg}",
                            streamerId, message)
                    .retrieve()
                    .body(PajResponse.class);

            if (response == null) {
                log.warn("Null response from paj.pajbot.com — allowing (fail-open)");
                return true;
            }
            if (response.banned() && response.filterData() != null && !response.filterData().isEmpty()) {
                log.warn("Banphrase blocked via paj API: {}", response.filterData().get(0).reason());
            }
            return !response.banned();

        } catch (Exception e) {
            log.warn("Paj banphrase check failed: {} — allowing (fail-open)", e.getMessage());
            return true;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PajResponse(
            boolean banned,
            @JsonProperty("filter_data") List<FilterData> filterData
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FilterData(String reason) {}
}
