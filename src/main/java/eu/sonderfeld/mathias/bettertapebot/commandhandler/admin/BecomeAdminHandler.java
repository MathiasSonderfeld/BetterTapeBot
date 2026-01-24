package eu.sonderfeld.mathias.bettertapebot.commandhandler.admin;

import eu.sonderfeld.mathias.bettertapebot.bot.ResponseService;
import eu.sonderfeld.mathias.bettertapebot.commandhandler.Command;
import eu.sonderfeld.mathias.bettertapebot.commandhandler.CommandHandler;
import eu.sonderfeld.mathias.bettertapebot.repository.UserStateRepository;
import eu.sonderfeld.mathias.bettertapebot.repository.entity.UserState;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@CustomLog
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BecomeAdminHandler implements CommandHandler {

    UserStateRepository userStateRepository;
    ResponseService responseService;

    @Override
    public Command forCommand() {
        return Command.ADMIN;
    }

    @Override
    @Transactional
    public void handleMessage(long chatId, String message) {
        var state = userStateRepository.findById(chatId);
        if(state.isEmpty() || !state.get().getUserState().isLoggedIn()){
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
