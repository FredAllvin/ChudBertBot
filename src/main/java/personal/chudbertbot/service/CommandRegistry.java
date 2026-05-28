package personal.chudbertbot.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import personal.chudbertbot.handler.CommandHandler;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CommandRegistry {

    private final List<CommandHandler> commands;
    private final Map<String, CommandHandler> byTrigger;

    // ObjectProvider safely yields an empty stream when no CommandHandler beans exist yet.
    public CommandRegistry(ObjectProvider<CommandHandler> handlerProvider) {
        this.commands = handlerProvider.orderedStream().toList();
        this.byTrigger = commands.stream()
                .collect(Collectors.toUnmodifiableMap(CommandHandler::getTrigger, h -> h));
    }

    public List<CommandHandler> getAll() {
        return commands;
    }

    public Optional<CommandHandler> findByTrigger(String trigger) {
        return Optional.ofNullable(byTrigger.get(trigger));
    }
}
