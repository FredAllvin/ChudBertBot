package personal.chudbertbot.config;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TwitchProperties.class)
public class BotConfiguration {

    @Bean
    @ConditionalOnMissingBean(TwitchClient.class)
    public TwitchClient twitchClient(TwitchProperties props) {
        OAuth2Credential credential = new OAuth2Credential("twitch", props.oauthToken());
        return TwitchClientBuilder.builder()
                .withClientId(props.clientId())
                .withClientSecret(props.clientSecret())
                .withEnableChat(true)
                .withChatAccount(credential)
                .build();
    }
}
