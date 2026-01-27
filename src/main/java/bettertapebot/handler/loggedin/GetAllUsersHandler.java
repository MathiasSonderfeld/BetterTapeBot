package bettertapebot.handler.loggedin;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.Command;
import bettertapebot.handler.CommandHandler;
import bettertapebot.repository.UserRepository;
import bettertapebot.repository.entity.UserEntity;
import bettertapebot.repository.entity.UserStateEntity;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@CustomLog
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetAllUsersHandler implements CommandHandler {

    UserRepository userRepository;
    ResponseService responseService;

    @Override
    public @NonNull Command forCommand() {
        return Command.USERS;
    }

    @Override
    @Transactional
    public void handleMessage(@NonNull UserStateEntity userStateEntity, long chatId, String message) {
        if(!userStateEntity.getUserState().isLoggedIn()){
            responseService.send(chatId, "Nur eingeloggte User können andere User sehen");
            return;
        }

        var allUsernames = userRepository.findAll().stream()
            .map(UserEntity::getUsername)
            .filter(u -> !"anonymous".equalsIgnoreCase(u)) //TODO make default user configurable
            .collect(Collectors.toSet());

        responseService.send(chatId, "Folgende User sind registriert: " + String.join(", ", allUsernames));
    }
}
