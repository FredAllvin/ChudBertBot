# ChudBertBot — Developer Guide

ChudBertBot is a Spring Boot 4.0 / Java 21 Twitch chatbot. It connects to Twitch chat via IRC/WebSocket using [Twitch4J](https://github.com/twitch4j/twitch4j), joins configured channels, and listens to messages. Command handling will be added in future iterations.

---

## Architecture Overview

```
personal.chudbertbot
├── config/
│   ├── TwitchProperties.java    — @ConfigurationProperties record; validates all credentials at startup
│   └── BotConfiguration.java   — @Configuration; wires TwitchClient bean (skipped in tests via @ConditionalOnMissingBean)
├── service/
│   ├── TwitchClientService.java — owns TwitchClient lifecycle: joins/leaves channels, graceful shutdown
│   └── ChatEventService.java   — registers event listeners (@PostConstruct), then triggers channel join
└── handler/
    └── ChatMessageHandler.java  — receives every ChannelMessageEvent; extension point for future commands
```

**Startup flow:** `BotConfiguration` creates the `TwitchClient` bean → `ChatEventService.init()` registers the message listener → joins configured channels. Listeners are always registered *before* joining to prevent missed messages.

**Shutdown flow:** Spring calls `TwitchClientService.destroy()` (via `@PreDestroy` / `DisposableBean`), which leaves all channels then closes the client cleanly.

---

## Environment Variables

All five variables must be set before running. The bot refuses to start if any required variable is missing or blank (`@NotBlank` validation).

| Variable | Required | Description |
|---|---|---|
| `TWITCH_BOT_USERNAME` | Yes | Twitch account name the bot runs as |
| `TWITCH_OAUTH_TOKEN` | Yes | User Access Token for the bot account (without `oauth:` prefix — Twitch4J adds it) |
| `TWITCH_CLIENT_ID` | Yes | Client ID from the Twitch Developer Console |
| `TWITCH_CLIENT_SECRET` | Yes | Client Secret from the Twitch Developer Console |
| `TWITCH_CHANNELS` | No | Comma-separated channels to join (e.g., `mychannel,otherchannel`). If absent, bot starts but joins no channel. |

**Never put real values in `application.properties`** — that file is committed to git.

---

## Obtaining Credentials

1. **Register an application** at [dev.twitch.tv/console](https://dev.twitch.tv/console). Set the OAuth Redirect URL to `http://localhost`.

2. **Generate an OAuth token** for the bot's Twitch account (log in as the bot account, not the streamer account):
   - Quick dev option: [twitchapps.com/tmi/](https://twitchapps.com/tmi/)
   - More controlled: [Twitch CLI](https://dev.twitch.tv/docs/cli/) → `twitch token --user-token --scopes "chat:read chat:edit"`

3. Required OAuth **scopes**: `chat:read` and `chat:edit` (principle of least privilege — do not request more).

4. Copy the token **without** the `oauth:` prefix into `TWITCH_OAUTH_TOKEN`.

---

## Running Locally (PowerShell)

```powershell
$env:TWITCH_BOT_USERNAME  = "ChudBertBot"
$env:TWITCH_OAUTH_TOKEN   = "your_token_here"
$env:TWITCH_CLIENT_ID     = "your_client_id_here"
$env:TWITCH_CLIENT_SECRET = "your_client_secret_here"
$env:TWITCH_CHANNELS      = "your_channel_name"

.\mvnw.cmd spring-boot:run
```

**Expected startup logs:**
```
INFO  Started ChudbertBotApplication in X.XXX seconds
INFO  p.c.service.ChatEventService - ChatEventService initialized. Bot is connected and listening.
INFO  p.c.service.TwitchClientService - Joining channel: your_channel_name
```

**To trace individual messages** (local only, never in production):
```powershell
$env:LOGGING_LEVEL_PERSONAL_CHUDBERTBOT = "DEBUG"
```

**Graceful shutdown:** `Ctrl+C` triggers `@PreDestroy`, logs `Shutting down TwitchClientService` then `TwitchClient closed cleanly`.

---

## Adding a Command — Step-by-Step

Commands are added in `ChatMessageHandler` without touching Spring configuration.

**1. Create a handler class** in `personal.chudbertbot.handler`:

```java
@Component
public class PingCommandHandler {
    public void execute(ChannelMessageEvent event) {
        event.getChannel(); // use Twitch4J chat to respond
        // twitchClient.getChat().sendMessage(channel, "Pong!");
    }
}
```

**2. Inject it into `ChatMessageHandler`** and add dispatch logic:

```java
@Component
public class ChatMessageHandler {

    private static final String PREFIX = "!";

    private final PingCommandHandler pingCommandHandler;

    public ChatMessageHandler(PingCommandHandler pingCommandHandler) {
        this.pingCommandHandler = pingCommandHandler;
    }

    public void handle(ChannelMessageEvent event) {
        String message = event.getMessage();
        if (message == null || !message.startsWith(PREFIX)) return;

        String command = message.substring(PREFIX.length()).split(" ")[0].toLowerCase();
        switch (command) {
            case "ping" -> pingCommandHandler.execute(event);
            // add more cases here
        }
    }
}
```

**3. Write a unit test** in `src/test/java/personal/chudbertbot/handler/`.

**Rate limiting:** Standard Twitch accounts are limited to **20 messages per 30 seconds** (100 if the bot is a moderator in the channel). Never send responses in a tight loop without throttling.

---

## Running Tests

```powershell
.\mvnw.cmd test
```

Tests use a deep-stub `TwitchClient` mock — no real Twitch connection is made. The `contextLoads` integration test verifies the full Spring context wires up correctly with fake credentials.

---

## CI/CD Pipeline (GitHub Actions)

Three configuration files in `.github/`:

| File | Purpose |
|---|---|
| `workflows/ci.yml` | Runs on every push and PR: `mvn test` → `mvn package` → uploads fat JAR as artifact |
| `workflows/codeql.yml` | SAST scan on pushes to main/master and weekly; results appear in GitHub Security tab |
| `dependabot.yml` | Weekly PRs to update Maven dependencies and GitHub Actions versions |

**CI workflow triggers:** every push to any branch, every pull request.

**Artifacts:** The fat JAR is uploaded with a 14-day retention as `chudbertbot-<git-sha>`. Download it from the Actions run page to run the bot manually.

**To activate:** Push the repo to GitHub. Workflows run automatically — no additional configuration needed for CI. CodeQL requires the repository to have the Security tab enabled (default for public repos; enable under Settings → Security → Code security for private repos).

**Adding secrets for future CD:** If you add a deployment step later (e.g., SSH to a VPS), add the credentials under Settings → Secrets and variables → Actions, then reference them as `${{ secrets.MY_SECRET }}` in the workflow. Never commit secrets to the workflow file directly.

---

## Security Considerations

| Concern | Mitigation |
|---|---|
| Credentials in git | `application.properties` uses `${ENV_VAR}` references only |
| Missing credentials | `@NotBlank` on `TwitchProperties` causes hard startup failure |
| Credential logging | Never log `oauthToken`, `clientId`, or `clientSecret` anywhere |
| User input in channel names | `sanitizeChannelName()` in `TwitchClientService` strips non-`[a-z0-9_]` characters — apply this pattern to all future user-supplied input |
| OAuth scope creep | Token uses `chat:read` + `chat:edit` only |
| Library log leakage | `com.github.twitch4j` and `com.github.philippheuer` suppressed to `WARN` |
| `.env` files | `*.env` is in `.gitignore` |

---

## Pre-Production Security Audit Checklist

Before using the bot in a live channel, verify:

- [ ] No real credentials appear in any git commit: `git log -S "oauth"` and `git log -S "client_secret"` return no results
- [ ] `application.properties` in git history contains only `${ENV_VAR}` placeholders
- [ ] Log files (if any) contain no token fragments — check with `Select-String "oauth" *.log`
- [ ] OAuth token rotated if it was ever logged or exposed accidentally
- [ ] Bot account has only the minimum required Twitch permissions
- [ ] Any future command arguments sourced from chat users pass through input sanitization before use (length limits, character whitelisting, injection prevention)
- [ ] Twitch4J library is on the latest stable version (check [Maven Central](https://mvnrepository.com/artifact/com.github.twitch4j/twitch4j) for updates)
- [ ] No hardcoded strings in code that look like tokens, client IDs, or secrets

---

## Key Dependencies

| Dependency | Version | Purpose |
|---|---|---|
| Spring Boot | 4.0.6 | Application framework, lifecycle, DI |
| Twitch4J | 1.25.0 (see `pom.xml`) | Twitch IRC/WebSocket chat client |
| spring-boot-starter-validation | (BOM managed) | `@NotBlank` credential validation |
| Java | 21 | Language runtime |
