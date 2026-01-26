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
public class BecomeAdminHandler implements CommandHandler {
    
    ResponseService responseService;

    @Override
    public @NonNull Command forCommand() {
        return Command.ADMIN;
    }

    @Override
    @Transactional
    public void handleMessage(@NonNull UserStateEntity userStateEntity, long chatId, String message) {
        if(!userStateEntity.getUserState().isLoggedIn()){
            responseService.send(chatId, "Nur eingeloggte User können in den Admin-Modus wechseln");
            return;
        }
        
        if(userStateEntity.isAdminModeActive()){
            responseService.send(chatId, "Du bist bereits im Admin-Modus, /help für Befehle");
            return;
        }
        
        var user = userStateEntity.getOwner();
        if(!user.getIsAdmin()){
            responseService.send(chatId, "Du bist kein Admin");
            return;
        }
        userStateEntity.setAdminMode(true);
        userStateEntity.setUserState(UserState.LOGGED_IN);
        responseService.send(chatId, "Du bist in den Admin-Bereich gewechselt, /help für Befehle");
    }
}
