package eu.sonderfeld.mathias.bettertapebot.handler.admin;

import eu.sonderfeld.mathias.bettertapebot.bot.ResponseService;
import eu.sonderfeld.mathias.bettertapebot.handler.Command;
import eu.sonderfeld.mathias.bettertapebot.handler.CommandHandler;
import eu.sonderfeld.mathias.bettertapebot.handler.StateHandler;
import eu.sonderfeld.mathias.bettertapebot.repository.UserStateRepository;
import eu.sonderfeld.mathias.bettertapebot.repository.entity.UserState;
import eu.sonderfeld.mathias.bettertapebot.repository.entity.UserStateEntity;
import jakarta.transaction.Transactional;
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

    ResponseService responseService;
    UserStateRepository userStateRepository;

    @Override
    public @NonNull Command forCommand() {
        return Command.BROADCAST;
    }
    
    @Override
    public @NonNull Set<UserState> forStates() {
        return Set.of(UserState.BROADCAST_AWAIT_MESSAGE);
    }

    @Override
    @Transactional
    public void handleCommand(long chatId, String message) {
        var stateOptional = userStateRepository.findById(chatId);
        var knownAndAdmin = stateOptional.map(UserStateEntity::getUserState)
            .map(UserState::isAdmin)
            .orElse(false);
        if(!knownAndAdmin){
            responseService.send(chatId, "Nur Admins können Broadcasts senden");
            return;
        }
        var userState = stateOptional.get();
        if(StringUtils.hasText(message)){
          broadcast(message);
          return;
        }
        userState.setUserState(UserState.BROADCAST_AWAIT_MESSAGE);
        responseService.send(chatId, "Wie lautet die Nachricht?");
    }
    
    @Override
    @Transactional
    public void handleMessage(long chatId, String message) {
        broadcast(message);
    }
    
    private void broadcast(String message) {
        var loggedInStates = Arrays.stream(UserState.values())
            .filter(UserState::isLoggedIn)
            .toList();
        
        var chats = userStateRepository.findUserStateEntitiesByUserStateIn(loggedInStates)
            .stream().map(UserStateEntity::getChatId).toList();
        
        responseService.broadcast(chats, message);
    }
}
