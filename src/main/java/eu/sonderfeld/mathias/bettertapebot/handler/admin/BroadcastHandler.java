package eu.sonderfeld.mathias.bettertapebot.handler.admin;

import eu.sonderfeld.mathias.bettertapebot.bot.ResponseService;
import eu.sonderfeld.mathias.bettertapebot.handler.Command;
import eu.sonderfeld.mathias.bettertapebot.handler.StateHandler;
import eu.sonderfeld.mathias.bettertapebot.repository.UserStateRepository;
import eu.sonderfeld.mathias.bettertapebot.repository.entity.UserState;
import eu.sonderfeld.mathias.bettertapebot.repository.entity.UserStateEntity;
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
