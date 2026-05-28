package personal.chudbertbot.service;

import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.chat.TwitchChat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import personal.chudbertbot.config.TwitchProperties;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TwitchClientServiceTest {

    @Mock
    TwitchClient twitchClient;
    @Mock
    TwitchChat twitchChat;
    @Mock
    TwitchProperties properties;
    @InjectMocks
    TwitchClientService service;

    @Test
    void joinConfiguredChannels_joinsEachChannel() {
        when(properties.channels()).thenReturn(List.of("mychannel", "otherchannel"));
        when(twitchClient.getChat()).thenReturn(twitchChat);

        service.joinConfiguredChannels();

        verify(twitchChat).joinChannel("mychannel");
        verify(twitchChat).joinChannel("otherchannel");
    }

    @Test
    void joinConfiguredChannels_skipsEmptyChannelList() {
        when(properties.channels()).thenReturn(List.of());

        service.joinConfiguredChannels();

        verifyNoInteractions(twitchClient);
    }

    @Test
    void sanitizeChannelName_stripsInvalidCharacters() {
        assertThat(service.sanitizeChannelName(" My_Channel! ")).isEqualTo("my_channel");
    }

    @Test
    void sanitizeChannelName_handlesEmptyString() {
        assertThat(service.sanitizeChannelName("")).isEqualTo("");
    }

    @Test
    void sanitizeChannelName_lowercasesInput() {
        assertThat(service.sanitizeChannelName("STREAMER")).isEqualTo("streamer");
    }

    @Test
    void destroy_leavesChannelsAndClosesClient() throws Exception {
        when(properties.channels()).thenReturn(List.of("mychannel"));
        when(twitchClient.getChat()).thenReturn(twitchChat);

        service.destroy();

        verify(twitchChat).leaveChannel("mychannel");
        verify(twitchClient).close();
    }
}
