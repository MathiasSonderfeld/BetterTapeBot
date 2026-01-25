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
public class ResetStateHandler implements CommandHandler {

    UserStateRepository userStateRepository;
    ResponseService responseService;

    @Override
    public @NonNull Command forCommand() {
        return Command.RESET;
    }

    @Override
    @Transactional
    public void handleCommand(long chatId, String message) {
        var stateOptional = userStateRepository.findById(chatId);
        if(stateOptional.isEmpty()){
            responseService.send(chatId, "chat unbekannt, kein reset nötig");
            return;
        }
        userStateRepository.deleteById(chatId);
        responseService.send(chatId, "Chat wurde zurückgesetzt, benutze /start um von vorne zu beginnen");
    }
}
