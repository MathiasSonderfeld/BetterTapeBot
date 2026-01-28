package bettertapebot.handler.admin;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.Command;
import bettertapebot.handler.CommandHandler;
import bettertapebot.handler.StateHandler;
import bettertapebot.repository.UserRepository;
import bettertapebot.repository.entity.UserState;
import bettertapebot.repository.entity.UserStateEntity;
import bettertapebot.util.MessageCleaner;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;

@CustomLog
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeleteUserHandler implements CommandHandler, StateHandler {

    UserRepository userRepository;
    ResponseService responseService;
    
    @Override
    public @NonNull Command forCommand() {
        return Command.DELETE_USER;
    }
    
    @Override
    public @NonNull Set<UserState> forStates() {
        return Set.of(UserState.DELETE_USER_GET_USERNAME);
    }
    
    @Override
    @Transactional
    public void handleMessage(@NonNull UserStateEntity userStateEntity, String message) {
        if (!userStateEntity.isAdminModeActive()) {
            responseService.send(userStateEntity.getChatId(), "Nur Admins können Benutzer löschen");
            return;
        }
        
        if(!StringUtils.hasText(message)){
            userStateEntity.setUserState(UserState.DELETE_USER_GET_USERNAME);
            responseService.send(userStateEntity.getChatId(), "Wie lautet der Benutzername?");
            return;
        }
        
        var usernameToRemove = MessageCleaner.getFirstWord(message);
        var deletedEntity = userRepository.deleteByUsername(usernameToRemove);
        if(deletedEntity.isPresent()){
            log.info("deleting user with username {} on request of {}", usernameToRemove, userStateEntity.getOwner().getUsername());
            userStateEntity.setUserState(UserState.LOGGED_IN);
        }
        else {
            userStateEntity.setUserState(UserState.DELETE_USER_GET_USERNAME);
            responseService.send(userStateEntity.getChatId(), "Den Benutzer gibt es nicht. Probiers nochmal");
        }
    }
}
