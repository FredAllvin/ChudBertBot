package personal.chudbertbot.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import personal.chudbertbot.config.TwitchProperties;
import personal.chudbertbot.service.CommandRegistry;

import java.util.List;

@Controller
public class DashboardController {

    private final CommandRegistry commandRegistry;
    private final TwitchProperties twitchProperties;

    public DashboardController(CommandRegistry commandRegistry, TwitchProperties twitchProperties) {
        this.commandRegistry = commandRegistry;
        this.twitchProperties = twitchProperties;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        model.addAttribute("username", authentication.getName());

        String picture = null;
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            picture = oidcUser.getPicture();
        }
        model.addAttribute("profilePicture", picture);
        model.addAttribute("commands", commandRegistry.getAll());

        List<String> channels = twitchProperties.channels();
        model.addAttribute("channels", channels == null ? 0 : channels.size());

        return "dashboard";
    }
}
