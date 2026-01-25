package eu.sonderfeld.mathias.bettertapebot.handler.loggedin;

import eu.sonderfeld.mathias.bettertapebot.bot.ResponseService;
import eu.sonderfeld.mathias.bettertapebot.handler.Command;
import eu.sonderfeld.mathias.bettertapebot.handler.CommandHandler;
import eu.sonderfeld.mathias.bettertapebot.handler.StateHandler;
import eu.sonderfeld.mathias.bettertapebot.repository.UserRepository;
import eu.sonderfeld.mathias.bettertapebot.repository.UserStateRepository;
import eu.sonderfeld.mathias.bettertapebot.repository.entity.UserState;
import eu.sonderfeld.mathias.bettertapebot.repository.entity.UserStateEntity;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.Set;

@CustomLog
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AddTapeHandler implements CommandHandler, StateHandler { //TODO implement

    ResponseService responseService;
    UserStateRepository userStateRepository;
    UserRepository userRepository;

    @Override
    public @NonNull Command forCommand() {
        return Command.ADD;
    }
    
    @Override
    public @NonNull Set<UserState> forStates() {
        return Set.of(UserState.ADD_TAPE_GET_TITLE, UserState.ADD_TAPE_GET_STAR);
    }

    @Override
    public void handleCommand(long chatId, String message) {
    
    }
    
    @Override
    public void handleMessage(@NonNull UserStateEntity userStateEntity, long chatId, String message) {
    
    }
}
