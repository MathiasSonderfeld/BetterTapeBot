
package bettertapebot.handler.general;

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
public class LoginHandler implements CommandHandler, StateHandler {

    ResponseService responseService;
    UserRepository userRepository;

    @Override
    public @NonNull Command forCommand() {
        return Command.LOGIN;
    }
    
    @Override
    public @NonNull Set<UserState> forStates() {
        return Set.of(UserState.LOGIN_VALIDATE_USERNAME, UserState.LOGIN_VALIDATE_PIN);
    }

    @Override
    @Transactional
    public void handleMessage(@NonNull UserStateEntity userStateEntity, String message) {
        if(userStateEntity.getUserState().isLoggedIn()){
            responseService.send(userStateEntity.getChatId(), "du bist schon eingeloggt als " + userStateEntity.getOwner().getUsername());
            return;
        }
        
        if(!StringUtils.hasText(message)){
            userStateEntity.setUserState(UserState.LOGIN_VALIDATE_USERNAME);
            responseService.send(userStateEntity.getChatId(), "Wie lautet dein Benutzername?");
            return;
        }
        
        if(userStateEntity.getUserState() == UserState.LOGIN_VALIDATE_PIN){
            var pin = MessageCleaner.getFirstWord(message);
            var userEntity = userStateEntity.getOwner();
            
            if(Objects.equals(pin, userEntity.getPin())){ //TODO verify PIN schema
                userStateEntity.setUserState(UserState.LOGGED_IN);
                responseService.send(userStateEntity.getChatId(), "du wurdest erfolgreich eingeloggt");
            }
            else {
                responseService.send(userStateEntity.getChatId(), "PIN inkorrekt, versuchs nochmal");
            }
            return;
        }
        
        var cleanedName = MessageCleaner.getFirstWord(message);
        verifyAndSetUsername(cleanedName, userStateEntity);
    }
    
    private void verifyAndSetUsername(String username, UserStateEntity userStateEntity){
        if(userStateEntity.getOwner() != null && Objects.equals(username, userStateEntity.getOwner().getUsername())){
            requestPin(userStateEntity);
            userStateEntity.setUserState(UserState.LOGIN_VALIDATE_PIN);
            return;
        }
        
        var userEntityOptional = userRepository.findById(username);
        if(userEntityOptional.isEmpty()){
            userStateEntity.setUserState(UserState.LOGIN_VALIDATE_USERNAME);
            responseService.send(userStateEntity.getChatId(), "der angegebene benutzername " + username + " ist unbekannt. Probiers nochmal");
            return;
        }
        var userEntity = userEntityOptional.get();
        userStateEntity.setOwner(userEntity);
        requestPin(userStateEntity);
    }
    
    private void requestPin(UserStateEntity userStateEntity){
        userStateEntity.setUserState(UserState.LOGIN_VALIDATE_PIN);
        responseService.send(userStateEntity.getChatId(), "Wie lautet deine PIN?");
    }
}
