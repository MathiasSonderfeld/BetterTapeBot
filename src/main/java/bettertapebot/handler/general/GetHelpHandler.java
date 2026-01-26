package bettertapebot.handler.general;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.Command;
import bettertapebot.handler.CommandHandler;
import bettertapebot.repository.entity.UserStateEntity;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.stream.Collectors;

@CustomLog
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetHelpHandler implements CommandHandler {

    ResponseService responseService;

    @Override
    public @NonNull Command forCommand() {
        return Command.HELP;
    }

    @Override
    @Transactional
    public void handleMessage(@NonNull UserStateEntity userStateEntity, long chatId, String message) {
        var userState = userStateEntity.getUserState();
        
        var commandsMap = Arrays.stream(Command.values())
            .collect(Collectors.groupingBy(Command::getCommandLevel));
        
        StringBuilder sb = new StringBuilder()
            .append("Grundfunktionen:")
            .append("\n");
        
        commandsMap.get(Command.CommandLevel.GENERAL).stream()
            .map(Command::getFormattedHelpText)
            .forEach(c -> sb.append(c).append("\n"));
        
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
