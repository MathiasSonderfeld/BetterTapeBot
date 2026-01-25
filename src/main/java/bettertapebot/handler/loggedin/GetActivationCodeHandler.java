package bettertapebot.handler.loggedin;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.Command;
import bettertapebot.handler.CommandHandler;
import bettertapebot.repository.UserStateRepository;
import bettertapebot.util.PasscodeGenerator;
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
public class GetActivationCodeHandler implements CommandHandler {

    UserStateRepository userStateRepository;
    ResponseService responseService;

    @Override
    public @NonNull Command forCommand() {
        return Command.CODE;
    }

    @Override
    public void handleCommand(long chatId, String message) {
        var stateOptional = userStateRepository.findById(chatId);
        if(stateOptional.isEmpty() || !stateOptional.get().getUserState().isLoggedIn()){
            responseService.send(chatId, "Nur eingeloggte User können Codes erzeugen");
            return;
        }

        responseService.send(chatId, "Der aktuelle Freischaltcode lautet: " + PasscodeGenerator.generatePasscode());
    }
}
