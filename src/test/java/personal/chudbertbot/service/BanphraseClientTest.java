package personal.chudbertbot.service;

import org.junit.jupiter.api.Test;
import personal.chudbertbot.config.TwitchProperties;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BanphraseClientTest {

    private BanphraseClient clientWith(String pajbotUrl, String streamerId) {
        TwitchProperties props = new TwitchProperties(
                "bot", "token", "id", "secret", List.of(), pajbotUrl, streamerId);
        return new BanphraseClient(props);
    }

    @Test
    void isSafe_returnsTrueWhenNeitherApiIsConfigured() {
        // Fail-open: if no banphrase API is configured, allow all messages.
        BanphraseClient client = clientWith("", "");
        assertThat(client.isSafe("Hello world")).isTrue();
    }

    @Test
    void isSafe_returnsTrueWhenBothUrlsAreNull() {
        BanphraseClient client = clientWith(null, null);
        assertThat(client.isSafe("Hello world")).isTrue();
    }
}
