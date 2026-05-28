package personal.chudbertbot;

import com.github.twitch4j.TwitchClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "twitch.bot-username=testbot",
        "twitch.oauth-token=fake_token_for_test",
        "twitch.client-id=fake_client_id",
        "twitch.client-secret=fake_client_secret",
        "twitch.channels="
})
class ChudbertBotApplicationTests {

    @TestConfiguration
    static class TestConfig {
        // Overrides BotConfiguration.twitchClient() with a deep-stub mock so
        // the context loads without attempting a real Twitch connection.
        @Bean
        TwitchClient twitchClient() {
            return Mockito.mock(TwitchClient.class, Mockito.RETURNS_DEEP_STUBS);
        }
    }

    @Test
    void contextLoads() {
    }
}
