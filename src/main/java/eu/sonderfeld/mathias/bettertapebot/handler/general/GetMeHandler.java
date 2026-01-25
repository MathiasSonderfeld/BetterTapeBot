package eu.sonderfeld.mathias.bettertapebot.handler.general;

import eu.sonderfeld.mathias.bettertapebot.bot.ResponseService;
import eu.sonderfeld.mathias.bettertapebot.handler.Command;
import eu.sonderfeld.mathias.bettertapebot.handler.CommandHandler;
import eu.sonderfeld.mathias.bettertapebot.repository.UserStateRepository;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

@CustomLog
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetMeHandler implements CommandHandler {

    ResponseService responseService;
    UserStateRepository userStateRepository;

    @Override
    public @NonNull Command forCommand() {
        return Command.ME;
    }

    @Override
    public void handleCommand(long chatId, String message) {
        var stateOptional = userStateRepository.findById(chatId);
        if(stateOptional.isEmpty()){
            responseService.send(chatId, String.format("chatId: %d, keine weiteren daten zu dem chat gefunden", chatId));
            return;
        }

        var userState = stateOptional.get();
        var username = userState.getOwner().getUsername();
        var stateName = userState.getUserState().name();

        responseService.send(chatId, String.format("chatId: %d, username: %s, user state: %s", chatId, username, stateName));
    }
}
