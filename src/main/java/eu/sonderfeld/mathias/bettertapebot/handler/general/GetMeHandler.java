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
    @Transactional
    public void handleCommand(long chatId, String message) {
        var state = userStateRepository.findById(chatId);
        if(state.isEmpty()){
            responseService.send(chatId, String.format("chatId: %d, keine weiteren daten zu dem chat gefunden", chatId));
            if(log.isDebugEnabled()){
                log.debug("could not find userstate for chatId {}", chatId);
            }
            return;
        }

        var userState = state.get();
        var username = userState.getUser().getUsername();
        var stateName = userState.getUserState().name();

        responseService.send(chatId, String.format("chatId: %d, username: %s, user state: %s", chatId, username, stateName));
    }
}
