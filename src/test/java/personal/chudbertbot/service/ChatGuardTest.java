package personal.chudbertbot.service;

import org.junit.jupiter.api.Test;
import personal.chudbertbot.exception.MessageGuardException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

class ChatGuardTest {

    private final ChatGuard guard = new ChatGuard();

    // --- Emoji limit ---

    @Test
    void validate_allowsMessageWithNoEmojis() {
        assertThatCode(() -> guard.validate("Hello chat!")).doesNotThrowAnyException();
    }

    @Test
    void validate_allowsMessageWithExactlyMaxEmojis() {
        String message = "😀".repeat(ChatGuard.MAX_EMOJIS); // 30 × 😀
        assertThatCode(() -> guard.validate(message)).doesNotThrowAnyException();
    }

    @Test
    void validate_throwsWhenEmojiLimitExceeded() {
        String message = "😀".repeat(ChatGuard.MAX_EMOJIS + 1); // 31 × 😀
        assertThatThrownBy(() -> guard.validate(message))
                .isInstanceOf(MessageGuardException.class)
                .hasMessageContaining("Emoji limit");
    }

    @Test
    void countEmojis_countsCorrectly() {
        String three = "😀🎮🔥"; // 😀🎮🔥
        assertThat(guard.countEmojis(three)).isEqualTo(3);
    }

    @Test
    void countEmojis_ignoresPlainText() {
        assertThat(guard.countEmojis("Hello world!")).isEqualTo(0);
    }

    // --- Mass ping limit ---

    @Test
    void validate_allowsSingleMention() {
        assertThatCode(() -> guard.validate("Good game @someuser!")).doesNotThrowAnyException();
    }

    @Test
    void validate_throwsOnMultipleMentions() {
        assertThatThrownBy(() -> guard.validate("@user1 @user2 hello"))
                .isInstanceOf(MessageGuardException.class)
                .hasMessageContaining("Mass ping");
    }

    @Test
    void countMentions_countsCorrectly() {
        assertThat(guard.countMentions("hey @alice and @bob")).isEqualTo(2);
    }

    @Test
    void countMentions_returnsZeroForNoMentions() {
        assertThat(guard.countMentions("no pings here")).isEqualTo(0);
    }
}
