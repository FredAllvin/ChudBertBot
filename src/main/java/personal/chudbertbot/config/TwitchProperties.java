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
        List<String> channels,
        // Optional: URL of the channel's own pajbot instance (e.g. https://nymn.pajbot.com).
        // Takes priority over streamerId if both are set.
        String pajbotUrl,
        // Optional: Twitch user ID of the streamer, used with paj.pajbot.com when pajbotUrl is absent.
        String streamerId
) {}
