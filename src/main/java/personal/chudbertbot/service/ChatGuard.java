package personal.chudbertbot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import personal.chudbertbot.exception.MessageGuardException;

import java.util.regex.Pattern;

/**
 * Validates outgoing chat messages against channel rules before they are sent.
 * All messages must pass through this guard via BotChatService.sendMessage().
 *
 * Rules enforced:
 *   - Max 30 emojis per message (channel rule)
 *   - Max 1 @mention per message (mass ping prevention)
 */
@Service
public class ChatGuard {

    private static final Logger log = LoggerFactory.getLogger(ChatGuard.class);

    static final int MAX_EMOJIS = 30;
    static final int MAX_MENTIONS = 1;

    private static final Pattern MENTION_PATTERN = Pattern.compile("@\\w+");

    /**
     * Validates a message. Throws {@link MessageGuardException} if it violates any rule.
     * The caller (BotChatService) catches this and suppresses the send.
     */
    public void validate(String message) {
        int emojiCount = countEmojis(message);
        if (emojiCount > MAX_EMOJIS) {
            throw new MessageGuardException(
                "Emoji limit exceeded: " + emojiCount + "/" + MAX_EMOJIS);
        }

        int mentionCount = countMentions(message);
        if (mentionCount > MAX_MENTIONS) {
            throw new MessageGuardException(
                "Mass ping blocked: " + mentionCount + " @mentions (max " + MAX_MENTIONS + ")");
        }
    }

    /**
     * Counts Unicode emoji characters using Java 21's Character.isEmojiPresentation().
     * Covers all standard emoji (😀🎮🔥 etc.) but excludes text symbols that can
     * optionally render as emoji (e.g., ♥ without a variation selector).
     */
    int countEmojis(String text) {
        return (int) text.codePoints()
                .filter(Character::isEmojiPresentation)
                .count();
    }

    int countMentions(String text) {
        return (int) MENTION_PATTERN.matcher(text).results().count();
    }
}
