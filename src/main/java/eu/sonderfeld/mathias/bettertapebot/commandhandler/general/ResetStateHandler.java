package eu.sonderfeld.mathias.bettertapebot.commandhandler.general;

import eu.sonderfeld.mathias.bettertapebot.bot.ResponseService;
import eu.sonderfeld.mathias.bettertapebot.commandhandler.Command;
import eu.sonderfeld.mathias.bettertapebot.commandhandler.CommandHandler;
import eu.sonderfeld.mathias.bettertapebot.repository.UserStateRepository;
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
public class ResetStateHandler implements CommandHandler {

    UserStateRepository userStateRepository;
    ResponseService responseService;

    @Override
    public Command forCommand() {
        return Command.RESET;
    }

    @Override
    @Transactional
    public void handleMessage(long chatId, String message) {
        var state = userStateRepository.findById(chatId);
        if(state.isEmpty()){
            responseService.send(chatId, "chat unbekannt, kein reset nötig");
            return;
        }
        userStateRepository.deleteById(chatId);
        responseService.send(chatId, "Chat wurde zurückgesetzt, benutze /start um von vorne zu beginnen");
    }
}
