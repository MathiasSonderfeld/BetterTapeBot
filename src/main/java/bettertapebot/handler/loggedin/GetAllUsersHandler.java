package bettertapebot.handler.loggedin;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.Command;
import bettertapebot.handler.CommandHandler;
import bettertapebot.repository.UserRepository;
import bettertapebot.repository.UserStateRepository;
import bettertapebot.repository.entity.UserEntity;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
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
    public @NonNull Command forCommand() {
        return Command.USERS;
    }

    @Override
    public void handleCommand(long chatId, String message) {
        var stateOptional = userStateRepository.findById(chatId);
        if(stateOptional.isEmpty() || !stateOptional.get().getUserState().isLoggedIn()){
            responseService.send(chatId, "Nur eingeloggte User können andere User sehen");
            return;
        }

        var allUsernames = userRepository.findAll().stream()
            .map(UserEntity::getUsername)
            .collect(Collectors.toSet());

        responseService.send(chatId, "Folgende User sind registriert: " + String.join(", ", allUsernames));
    }
}
