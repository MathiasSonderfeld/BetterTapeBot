package eu.sonderfeld.mathias.bettertapebot.handler.general;

import eu.sonderfeld.mathias.bettertapebot.bot.ResponseService;
import eu.sonderfeld.mathias.bettertapebot.handler.Command;
import eu.sonderfeld.mathias.bettertapebot.handler.CommandHandler;
import eu.sonderfeld.mathias.bettertapebot.repository.UserStateRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@CustomLog
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetHelpHandler implements CommandHandler {

    UserStateRepository userStateRepository;
    ResponseService responseService;

    @Override
    public @NonNull Command forCommand() {
        return Command.HELP;
    }

    @Override
    @Transactional
    public void handleCommand(long chatId, String message) {
        var state = userStateRepository.findById(chatId);

        var commandsMap = Arrays.stream(Command.values())
            .collect(Collectors.groupingBy(Command::getCommandLevel));

        StringBuilder sb = new StringBuilder()
            .append("Grundfunktionen:")
            .append("\n");
        commandsMap.get(Command.CommandLevel.GENERAL).stream()
            .map(Command::getFormattedHelpText)
            .forEach(c -> sb.append(c).append("\n"));

        if(state.isEmpty()){
            responseService.send(chatId, sb.toString());
            return;
        }
        var userState = state.get().getUserState();

        //user commands
        if(userState.isLoggedIn()){
            sb.append("\n")
                .append("Befehle für eingeloggte User:")
                .append("\n");

            commandsMap.get(Command.CommandLevel.LOGGEDIN).stream()
                .map(Command::getFormattedHelpText)
                .forEach(c -> sb.append(c).append("\n"));
        }

        //admin commands
        if(userState.isAdmin()){
            sb.append("\n")
                .append("Befehle für Admins:")
                .append("\n");

            commandsMap.get(Command.CommandLevel.ADMIN).stream()
                .map(Command::getFormattedHelpText)
                .forEach(c -> sb.append(c).append("\n"));
        }

        responseService.send(chatId, sb.toString());
    }
}
