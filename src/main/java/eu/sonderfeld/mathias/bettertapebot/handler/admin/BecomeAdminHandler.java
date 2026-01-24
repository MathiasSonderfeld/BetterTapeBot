package eu.sonderfeld.mathias.bettertapebot.handler.admin;

import eu.sonderfeld.mathias.bettertapebot.bot.ResponseService;
import eu.sonderfeld.mathias.bettertapebot.handler.Command;
import eu.sonderfeld.mathias.bettertapebot.handler.CommandHandler;
import eu.sonderfeld.mathias.bettertapebot.repository.UserStateRepository;
import eu.sonderfeld.mathias.bettertapebot.repository.entity.UserState;
import eu.sonderfeld.mathias.bettertapebot.repository.entity.UserStateEntity;
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
public class BecomeAdminHandler implements CommandHandler {

    UserStateRepository userStateRepository;
    ResponseService responseService;

    @Override
    public @NonNull Command forCommand() {
        return Command.ADMIN;
    }

    @Override
    @Transactional
    public void handleCommand(long chatId, String message) {
        var state = userStateRepository.findById(chatId);
        
        //if empty, chat is unknown so we default to true
        var unknownOrNotLoggedIn = state.map(UserStateEntity::getUserState)
            .map(UserState::isLoggedIn) //TODO fix already admin gets missing login error
            .orElse(true);
        if(unknownOrNotLoggedIn){
            responseService.send(chatId, "Nur eingeloggte User können in den Admin-Modus wechseln");
            return;
        }
        
        var userStateEntity = state.get();
        if(userStateEntity.getUserState().isAdmin()){
            responseService.send(chatId, "Du bist bereits im Admin-Modus, /help für Befehle");
            return;
        }
        var user = userStateEntity.getUser();
        if(!user.getIsAdmin()){
            responseService.send(chatId, "Du bist kein Admin");
            return;
        }
        userStateEntity.setUserState(UserState.ADMIN);
        responseService.send(chatId, "Du bist in den Admin-Bereich gewechselt, /help für Befehle");
    }
}
