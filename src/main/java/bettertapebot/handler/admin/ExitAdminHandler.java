package bettertapebot.handler.admin;

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
public class ExitAdminHandler implements CommandHandler {
    
    ResponseService responseService;
    
    @Override
    public @NonNull Command forCommand() {
        return Command.EXIT;
    }
    
    @Override
    @Transactional
    public void handleMessage(@NonNull UserStateEntity userStateEntity, String message) {
        if (!userStateEntity.isAdminModeActive()) {
            responseService.send(userStateEntity.getChatId(), "Du bist gar nicht im Admin-Modus");
            return;
        }
        userStateEntity.setUserState(UserState.LOGGED_IN);
        responseService.send(userStateEntity.getChatId(), "Du hast den Admin-Modus verlassen");
    }
}
