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
public class RemoveAdminHandler implements CommandHandler, StateHandler {

    ResponseService responseService;
    UserRepository userRepository;

    @Override
    public @NonNull Command forCommand() {
        return Command.REMOVE_ADMIN;
    }
    
    @Override
    public @NonNull Set<UserState> forStates() {
        return Set.of(UserState.REMOVE_ADMIN_USER_GET_USERNAME);
    }

    @Override
    @Transactional
    public void handleMessage(@NonNull UserStateEntity userStateEntity, long chatId, String message) {
        if (!userStateEntity.isAdminModeActive()) {
            responseService.send(chatId, "Nur Admins dürfen andere Admins entfernen");
            return;
        }
        
        if(!StringUtils.hasText(message)){
            userStateEntity.setUserState(UserState.REMOVE_ADMIN_USER_GET_USERNAME);
            responseService.send(userStateEntity.getChatId(), "Welcher Admin soll wieder eingeschränkt werden?");
            return;
        }
        
        var givenUsername = MessageCleaner.getFirstWord(message);
        var user = userRepository.findById(givenUsername);
        if(user.isEmpty()){
            userStateEntity.setUserState(UserState.REMOVE_ADMIN_USER_GET_USERNAME);
            responseService.send(userStateEntity.getChatId(), "Den Benutzer gibt es nicht. Probiers nochmal");
            return;
        }
        
        var userEntity = user.get();
        if(!userEntity.getIsAdmin()){
            userStateEntity.setUserState(UserState.LOGGED_IN);
            responseService.send(userStateEntity.getChatId(), userEntity.getUsername() + " ist nicht Admin");
            return;
        }
        
        userEntity.setIsAdmin(false);
        userStateEntity.setUserState(UserState.LOGGED_IN);
        responseService.send(userStateEntity.getChatId(),  userEntity.getUsername() + " ist nicht mehr Admin");
    }
}
