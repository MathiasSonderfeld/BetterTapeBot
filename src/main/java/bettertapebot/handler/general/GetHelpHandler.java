package bettertapebot.handler.general;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.Command;
import bettertapebot.handler.CommandHandler;
import bettertapebot.repository.UserStateRepository;
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
    public void handleCommand(long chatId, String message) {
        var stateOptional = userStateRepository.findById(chatId);
        
        var commandsMap = Arrays.stream(Command.values())
            .collect(Collectors.groupingBy(Command::getCommandLevel));
        
        StringBuilder sb = new StringBuilder()
            .append("Grundfunktionen:")
            .append("\n");
        commandsMap.get(Command.CommandLevel.GENERAL).stream()
            .map(Command::getFormattedHelpText)
            .forEach(c -> sb.append(c).append("\n"));

        if(stateOptional.isEmpty()){
            responseService.send(chatId, sb.toString());
            return;
        }
        var userState = stateOptional.get().getUserState();

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
