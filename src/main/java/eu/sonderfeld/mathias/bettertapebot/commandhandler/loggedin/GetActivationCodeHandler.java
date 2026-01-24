package eu.sonderfeld.mathias.bettertapebot.commandhandler.loggedin;

import eu.sonderfeld.mathias.bettertapebot.bot.ResponseService;
import eu.sonderfeld.mathias.bettertapebot.commandhandler.Command;
import eu.sonderfeld.mathias.bettertapebot.commandhandler.CommandHandler;
import eu.sonderfeld.mathias.bettertapebot.repository.UserStateRepository;
import eu.sonderfeld.mathias.bettertapebot.util.PasscodeGenerator;
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
public class GetActivationCodeHandler implements CommandHandler {

    UserStateRepository userStateRepository;
    ResponseService responseService;

    @Override
    public Command forCommand() {
        return Command.CODE;
    }

    @Override
    @Transactional
    public void handleMessage(long chatId, String message) {
        var state = userStateRepository.findById(chatId);
        if(state.isEmpty() || !state.get().getUserState().isLoggedIn()){
            responseService.send(chatId, "Nur eingeloggte User können Codes erzeugen");
            return;
        }

        responseService.send(chatId, "Der aktuelle Freischaltcode lautet: " + PasscodeGenerator.generatePasscode());
    }
}
