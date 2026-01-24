package eu.sonderfeld.mathias.bettertapebot.commandhandler.loggedin;

import eu.sonderfeld.mathias.bettertapebot.bot.ResponseService;
import eu.sonderfeld.mathias.bettertapebot.commandhandler.Command;
import eu.sonderfeld.mathias.bettertapebot.commandhandler.CommandHandler;
import eu.sonderfeld.mathias.bettertapebot.repository.UserRepository;
import eu.sonderfeld.mathias.bettertapebot.repository.UserStateRepository;
import eu.sonderfeld.mathias.bettertapebot.repository.entity.UserEntity;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@CustomLog
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetAllUsersHandler implements CommandHandler {

    UserStateRepository userStateRepository;
    UserRepository userRepository;
    ResponseService responseService;

    @Override
    public Command forCommand() {
        return Command.USERS;
    }

    @Override
    @Transactional
    public void handleMessage(long chatId, String message) {
        var state = userStateRepository.findById(chatId);
        if(state.isEmpty() || !state.get().getUserState().isLoggedIn()){
            responseService.send(chatId, "Nur eingeloggte User können andere User sehen");
            return;
        }

        var allUsernames = userRepository.findAll().stream()
            .map(UserEntity::getUsername)
            .collect(Collectors.toSet());

        responseService.send(chatId, "Folgende User sind registriert: " + String.join(", ", allUsernames));
    }
}
