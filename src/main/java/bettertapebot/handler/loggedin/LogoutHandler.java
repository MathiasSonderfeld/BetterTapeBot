package bettertapebot.handler.loggedin;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.Command;
import bettertapebot.handler.CommandHandler;
import bettertapebot.repository.entity.UserState;
import bettertapebot.repository.entity.UserStateEntity;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@CustomLog
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LogoutHandler implements CommandHandler {

    ResponseService responseService;

    @Override
    public @NonNull Command forCommand() {
        return Command.LOGOUT;
    }

    @Override
    @Transactional
    public void handleMessage(@NonNull UserStateEntity userStateEntity, long chatId, String message) {
        if(!userStateEntity.getUserState().isLoggedIn()){
            responseService.send(chatId, "Du bist nicht eingeloggt");
            return;
        }
        userStateEntity.setUserState(UserState.LOGGED_OUT);
        responseService.send(chatId, "Ich melde dich ab");
    }
}
