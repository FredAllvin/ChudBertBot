package personal.chudbertbot.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@ConfigurationProperties(prefix = "twitch")
@Validated
public record TwitchProperties(
        @NotBlank String botUsername,
        @NotBlank String oauthToken,
        @NotBlank String clientId,
        @NotBlank String clientSecret,
        List<String> channels
) {}
