package personal.chudbertbot.handler;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import personal.chudbertbot.service.CommandRegistry;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMessageHandlerTest {

    @Mock
    CommandRegistry commandRegistry;
    @InjectMocks
    ChatMessageHandler handler;

    @Test
    void handle_ignoresNonCommandMessages() {
        ChannelMessageEvent event = mock(ChannelMessageEvent.class, Mockito.RETURNS_DEEP_STUBS);
        when(event.getMessage()).thenReturn("hello world");

        handler.handle(event);

        verifyNoInteractions(commandRegistry);
    }

    @Test
    void handle_dispatchesKnownCommand() {
        CommandHandler command = mock(CommandHandler.class);
        ChannelMessageEvent event = mock(ChannelMessageEvent.class, Mockito.RETURNS_DEEP_STUBS);
        when(event.getMessage()).thenReturn("!ping");
        when(event.getUser().getName()).thenReturn("testuser");
        when(event.getChannel().getName()).thenReturn("testchannel");
        when(commandRegistry.findByTrigger("!ping")).thenReturn(Optional.of(command));

        handler.handle(event);

        verify(command).handle(event, "");
    }

    @Test
    void handle_ignoresUnknownCommand() {
        ChannelMessageEvent event = mock(ChannelMessageEvent.class, Mockito.RETURNS_DEEP_STUBS);
        when(event.getMessage()).thenReturn("!unknown");
        when(event.getUser().getName()).thenReturn("testuser");
        when(event.getChannel().getName()).thenReturn("testchannel");
        when(commandRegistry.findByTrigger("!unknown")).thenReturn(Optional.empty());

        handler.handle(event);

        verify(commandRegistry).findByTrigger("!unknown");
    }

    @Test
    void handle_passesArgsToCommand() {
        CommandHandler command = mock(CommandHandler.class);
        ChannelMessageEvent event = mock(ChannelMessageEvent.class, Mockito.RETURNS_DEEP_STUBS);
        when(event.getMessage()).thenReturn("!shoutout somestreamer");
        when(event.getUser().getName()).thenReturn("testuser");
        when(event.getChannel().getName()).thenReturn("testchannel");
        when(commandRegistry.findByTrigger("!shoutout")).thenReturn(Optional.of(command));

        handler.handle(event);

        verify(command).handle(event, "somestreamer");
    }

    @Test
    void handle_sanitizesControlCharactersFromArgs() {
        CommandHandler command = mock(CommandHandler.class);
        ChannelMessageEvent event = mock(ChannelMessageEvent.class, Mockito.RETURNS_DEEP_STUBS);
        when(event.getMessage()).thenReturn("!test helloworld");
        when(event.getUser().getName()).thenReturn("testuser");
        when(event.getChannel().getName()).thenReturn("testchannel");
        when(commandRegistry.findByTrigger("!test")).thenReturn(Optional.of(command));

        handler.handle(event);

        verify(command).handle(event, "helloworld");
    }
}
