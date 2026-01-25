package eu.sonderfeld.mathias.bettertapebot.handler.loggedin;

import eu.sonderfeld.mathias.bettertapebot.bot.ResponseService;
import eu.sonderfeld.mathias.bettertapebot.handler.Command;
import eu.sonderfeld.mathias.bettertapebot.handler.CommandHandler;
import eu.sonderfeld.mathias.bettertapebot.repository.UserRepository;
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
public class GetAllHandler implements CommandHandler { //TODO implement

    ResponseService responseService;
    UserStateRepository userStateRepository;
    UserRepository userRepository;

    @Override
    public @NonNull Command forCommand() {
        return Command.ALL;
    }

    @Override
    @Transactional
    public void handleCommand(long chatId, String message) {
    
    }
}
