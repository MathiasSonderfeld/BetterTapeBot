package bettertapebot.handler.admin;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.Command;
import bettertapebot.handler.StateHandler;
import bettertapebot.repository.UserRepository;
import bettertapebot.repository.UserStateRepository;
import bettertapebot.repository.entity.UserState;
import bettertapebot.repository.entity.UserStateEntity;
import bettertapebot.util.MessageCleaner;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.Set;

@CustomLog
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeleteUserHandler  extends AbstractAdminHandler implements StateHandler {

    UserRepository userRepository;
    
    public DeleteUserHandler(UserStateRepository userStateRepository, ResponseService responseService, UserRepository userRepository) {
        super(userStateRepository, responseService);
        this.userRepository = userRepository;
    }
    
    @Override
    public @NonNull Command forCommand() {
        return Command.DELETE_USER;
    }
    
    @Override
    public @NonNull Set<UserState> forStates() {
        return Set.of(UserState.DELETE_USER_GET_USERNAME);
    }
    
    @Override
    protected @NonNull String getErrorMessage() {
        return "Nur Admins können Benutzer löschen";
    }
    
    @Override
    public void handleMessage(@NonNull UserStateEntity userStateEntity, long chatId, String message) {
        handleCommandWithMessage(userStateEntity, message);
    }
    
    @Override
    protected void handleCommandWithoutMessage(@NonNull UserStateEntity userStateEntity) {
        userStateEntity.setUserState(UserState.DELETE_USER_GET_USERNAME);
        responseService.send(userStateEntity.getChatId(), "Wie lautet der Benutzername?");
    }
    
    @Override
    protected void handleCommandWithMessage(@NonNull UserStateEntity userStateEntity, @NonNull String message) {
        var usernameToRemove = MessageCleaner.getFirstWord(message);
        
        var deletedEntity = userRepository.deleteByUsername(usernameToRemove);
        if(deletedEntity.isPresent()){
            log.info("deleting user with username {} on request of {}", usernameToRemove, userStateEntity.getOwner().getUsername());
            userStateEntity.setUserState(UserState.ADMIN);
        }
        else {
            userStateEntity.setUserState(UserState.DELETE_USER_GET_USERNAME);
            responseService.send(userStateEntity.getChatId(), "Den Benutzer gibt es nicht. Probiers nochmal");
        }
    }
}
