package bettertapebot.handler.admin;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.Command;
import bettertapebot.handler.CommandHandler;
import bettertapebot.handler.StateHandler;
import bettertapebot.repository.UserStateRepository;
import bettertapebot.repository.entity.UserState;
import bettertapebot.repository.entity.UserStateEntity;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Set;

@CustomLog
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BroadcastHandler implements CommandHandler, StateHandler {
    
    UserStateRepository userStateRepository;
    ResponseService responseService;
    
    @Override
    public @NonNull Command forCommand() {
        return Command.BROADCAST;
    }
    
    @Override
    public @NonNull Set<UserState> forStates() {
        return Set.of(UserState.BROADCAST_AWAIT_MESSAGE);
    }
    
    @Override
    public void handleMessage(@NonNull UserStateEntity userStateEntity, long chatId, String message) {
        if (!userStateEntity.isAdminModeActive()) {
            responseService.send(chatId, "Nur Admins dürfen Broadcasts senden");
            return;
        }
        
        if(!StringUtils.hasText(message)){
            userStateEntity.setUserState(UserState.BROADCAST_AWAIT_MESSAGE);
            responseService.send(userStateEntity.getChatId(), "Wie lautet die Nachricht?");
            return;
        }
        
        var loggedInStates = Arrays.stream(UserState.values())
            .filter(UserState::isLoggedIn)
            .toList();
        
        var chats = userStateRepository.findUserStateEntitiesByUserStateIn(loggedInStates)
            .stream().map(UserStateEntity::getChatId).toList();
        
        userStateEntity.setUserState(UserState.LOGGED_IN);
        responseService.broadcast(chats, message);
    }
}
