package bettertapebot.handler.general;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.Command;
import bettertapebot.handler.CommandHandler;
import bettertapebot.repository.UserStateRepository;
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
