package bettertapebot.handler.admin;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.Command;
import bettertapebot.handler.StateHandler;
import bettertapebot.repository.UserStateRepository;
import bettertapebot.repository.entity.UserState;
import bettertapebot.repository.entity.UserStateEntity;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

@CustomLog
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BroadcastHandler extends AbstractAdminHandler implements StateHandler {
    
    public BroadcastHandler(UserStateRepository userStateRepository, ResponseService responseService) {
        super(userStateRepository, responseService);
    }
    
    @Override
    public @NonNull Command forCommand() {
        return Command.BROADCAST;
    }
    
    @Override
    public @NonNull Set<UserState> forStates() {
        return Set.of(UserState.BROADCAST_AWAIT_MESSAGE);
    }
    
    @Override
    protected @NonNull String getErrorMessage(){
        return "Nur Admins können Broadcasts senden";
    }
    
    @Override
    public void handleMessage(@NonNull UserStateEntity userStateEntity, long chatId, String message) {
        handleCommandWithMessage(userStateEntity, message);
    }
    
    @Override
    protected void handleCommandWithoutMessage(@NonNull UserStateEntity userStateEntity) {
        userStateEntity.setUserState(UserState.BROADCAST_AWAIT_MESSAGE);
        responseService.send(userStateEntity.getChatId(), "Wie lautet die Nachricht?");
    }
    
    @Override
    protected void handleCommandWithMessage(@NonNull UserStateEntity userStateEntity, @NonNull String message) {
        var loggedInStates = Arrays.stream(UserState.values())
            .filter(UserState::isLoggedIn)
            .toList();
        
        var chats = userStateRepository.findUserStateEntitiesByUserStateIn(loggedInStates)
            .stream().map(UserStateEntity::getChatId).toList();
        
        userStateEntity.setUserState(UserState.ADMIN);
        responseService.broadcast(chats, message);
    }
}
