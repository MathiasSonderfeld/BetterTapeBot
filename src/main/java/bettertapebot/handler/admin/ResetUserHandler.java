package bettertapebot.handler.admin;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.Command;
import bettertapebot.handler.CommandHandler;
import bettertapebot.handler.StateHandler;
import bettertapebot.repository.UserRepository;
import bettertapebot.repository.UserStateRepository;
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
public class ResetUserHandler implements CommandHandler, StateHandler {

    ResponseService responseService;
    UserStateRepository userStateRepository;
    UserRepository userRepository;

    @Override
    public @NonNull Command forCommand() {
        return Command.RESET_USER;
    }
    
    @Override
    public @NonNull Set<UserState> forStates() {
        return Set.of(UserState.RESET_USER_GET_USERNAME);
    }

    @Override
    @Transactional
    public void handleMessage(@NonNull UserStateEntity userStateEntity, long chatId, String message) {
        if (!userStateEntity.isAdminModeActive()) {
            responseService.send(chatId, "Nur Admins dürfen Benutzer zurücksetzen");
            return;
        }
        
        if(!StringUtils.hasText(message)){
            userStateEntity.setUserState(UserState.RESET_USER_GET_USERNAME);
            responseService.send(userStateEntity.getChatId(), "Welcher User soll zurückgesetzt werden?");
            return;
        }
        
        var givenUsername = MessageCleaner.getFirstWord(message);
        var user = userRepository.findById(givenUsername);
        if(user.isEmpty()){
            userStateEntity.setUserState(UserState.RESET_USER_GET_USERNAME);
            responseService.send(userStateEntity.getChatId(), "Den Benutzer gibt es nicht. Probiers nochmal");
            return;
        }
        
        var userEntity = user.get();
        long count = userStateRepository.deleteUserStateEntitiesByOwner(userEntity);
        var response = String.format("%s in %d chats zurückgesetzt", givenUsername, count);
        userStateEntity.setUserState(UserState.LOGGED_IN);
        responseService.send(userStateEntity.getChatId(), response);
    }
}
