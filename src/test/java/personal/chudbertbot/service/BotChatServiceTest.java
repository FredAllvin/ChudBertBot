package personal.chudbertbot.service;

import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.chat.TwitchChat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BotChatServiceTest {

    @Mock TwitchClient twitchClient;
    @Mock TwitchChat twitchChat;
    @Mock ChatGuard chatGuard;
    @InjectMocks BotChatService botChatService;

    @Test
    void sendMessage_sendsValidMessage() {
        when(twitchClient.getChat()).thenReturn(twitchChat);

        botChatService.sendMessage("mychannel", "Hello!");

        verify(chatGuard).validate("Hello!");
        verify(twitchChat).sendMessage("mychannel", "Hello!");
    }

    @Test
    void sendMessage_doesNothingForBlankMessage() {
        botChatService.sendMessage("mychannel", "   ");

        verifyNoInteractions(chatGuard, twitchClient);
    }

    @Test
    void sendMessage_doesNothingForNullMessage() {
        botChatService.sendMessage("mychannel", null);

        verifyNoInteractions(chatGuard, twitchClient);
    }

    @Test
    void sendMessage_dropsMessageWhenGuardThrows() {
        doThrow(new personal.chudbertbot.exception.MessageGuardException("too many emojis"))
                .when(chatGuard).validate(anyString());

        botChatService.sendMessage("mychannel", "Hello!");

        verifyNoInteractions(twitchClient);
    }

    @Test
    void sendMessage_truncatesMessageExceeding500Chars() {
        String longMessage = "x".repeat(510);
        when(twitchClient.getChat()).thenReturn(twitchChat);

        botChatService.sendMessage("mychannel", longMessage);

        verify(twitchChat).sendMessage(eq("mychannel"), argThat(m -> m.length() == 500 && m.endsWith("...")));
    }
}
