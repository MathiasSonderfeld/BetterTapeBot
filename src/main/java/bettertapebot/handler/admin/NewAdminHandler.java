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

import java.util.Objects;
import java.util.Set;

@CustomLog
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NewAdminHandler implements CommandHandler, StateHandler {
    
    UserRepository userRepository;
    ResponseService responseService;
    
    @Override
    public @NonNull Command forCommand() {
        return Command.NEW_ADMIN;
    }
    
    @Override
    public @NonNull Set<UserState> forStates() {
        return Set.of(UserState.NEW_ADMIN_USER_GET_USERNAME);
    }
    
    @Override
    @Transactional
    public void handleMessage(@NonNull UserStateEntity userStateEntity, long chatId, String message) {
        if (!userStateEntity.getUserState().isAdmin()) {
            responseService.send(chatId, "Nur Admins dürfen neue Admins festlegen");
            return;
        }
        
        if(!StringUtils.hasText(message)){
            userStateEntity.setUserState(UserState.NEW_ADMIN_USER_GET_USERNAME);
            responseService.send(userStateEntity.getChatId(), "Wer soll denn der neue Admin werden?");
            return;
        }
        
        var givenUsername = MessageCleaner.getFirstWord(message);
        if(Objects.equals(givenUsername, userStateEntity.getOwner().getUsername())){
            userStateEntity.setUserState(UserState.ADMIN);
            responseService.send(userStateEntity.getChatId(), "Du bist schon Admin");
            return;
        }
        
        var user = userRepository.findById(givenUsername);
        if(user.isEmpty()){
            userStateEntity.setUserState(UserState.NEW_ADMIN_USER_GET_USERNAME);
            responseService.send(userStateEntity.getChatId(), "Den Benutzer gibt es nicht. Probiers nochmal");
            return;
        }
        
        var userEntity = user.get();
        if(userEntity.getIsAdmin()){
            userStateEntity.setUserState(UserState.ADMIN);
            responseService.send(userStateEntity.getChatId(), userEntity.getUsername() + " ist schon Admin");
            return;
        }
        
        userEntity.setIsAdmin(true);
        userStateEntity.setUserState(UserState.ADMIN);
        responseService.send(userStateEntity.getChatId(),  userEntity.getUsername() + " ist nun Admin");
    }
}
