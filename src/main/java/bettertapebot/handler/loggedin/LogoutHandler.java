package bettertapebot.handler.loggedin;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.Command;
import bettertapebot.handler.CommandHandler;
import bettertapebot.repository.UserStateRepository;
import bettertapebot.repository.entity.UserState;
import bettertapebot.repository.entity.UserStateEntity;
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
public class LogoutHandler implements CommandHandler {

    ResponseService responseService;
    UserStateRepository userStateRepository;

    @Override
    public @NonNull Command forCommand() {
        return Command.LOGOUT;
    }

    @Override
    public void handleCommand(long chatId, String message) {
        var stateOptional = userStateRepository.findById(chatId);
        var knownAndLoggedIn = stateOptional
            .map(UserStateEntity::getUserState)
            .map(UserState::isLoggedIn)
            .orElse(false);
        if(!knownAndLoggedIn){
            responseService.send(chatId, "Du bist nicht eingeloggt");
            return;
        }
        var state = stateOptional.get();
        state.setUserState(UserState.LOGGED_OUT);
        responseService.send(chatId, "Ich melde dich ab");
    }
}
