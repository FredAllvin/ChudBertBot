package personal.chudbertbot.handler;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ChatMessageHandlerTest {

    private final ChatMessageHandler handler = new ChatMessageHandler();

    @Test
    void handle_doesNotThrow_forValidEvent() {
        // RETURNS_DEEP_STUBS so event.getUser().getName() and event.getChannel().getName()
        // return non-null strings without needing to import the intermediate types.
        ChannelMessageEvent event = Mockito.mock(ChannelMessageEvent.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(event.getUser().getName()).thenReturn("testuser");
        Mockito.when(event.getChannel().getName()).thenReturn("testchannel");

        handler.handle(event);
    }
}
