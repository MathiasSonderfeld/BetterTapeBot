package eu.sonderfeld.mathias.bettertapebot.handler.general;

import eu.sonderfeld.mathias.bettertapebot.bot.ResponseService;
import eu.sonderfeld.mathias.bettertapebot.handler.Command;
import eu.sonderfeld.mathias.bettertapebot.handler.CommandHandler;
import eu.sonderfeld.mathias.bettertapebot.handler.StateHandler;
import eu.sonderfeld.mathias.bettertapebot.repository.UserRepository;
import eu.sonderfeld.mathias.bettertapebot.repository.UserStateRepository;
import eu.sonderfeld.mathias.bettertapebot.repository.entity.UserState;
import eu.sonderfeld.mathias.bettertapebot.repository.entity.UserStateEntity;
import eu.sonderfeld.mathias.bettertapebot.util.MessageCleaner;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Set;

@CustomLog
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LoginHandler implements CommandHandler, StateHandler {

    ResponseService responseService;
    UserStateRepository userStateRepository;
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
    public void handleCommand(long chatId, String message) {
        var state = userStateRepository.findById(chatId);
        
        var alreadyLoggedIn = state.map(UserStateEntity::getUserState)
            .map(UserState::isLoggedIn)
            .orElse(false);
        if(alreadyLoggedIn){
            responseService.send(chatId, "du bist schon eingeloggt als " + state.get().getOwner().getUsername());
            return;
        }
        
        UserStateEntity userStateEntity = state
            .orElseGet(() -> userStateRepository.save(
                UserStateEntity.builder()
                    .chatId(chatId)
                    .build()));
        
        //user hat username direkt mit angegeben
        if(StringUtils.hasText(message)){
            var givenName = MessageCleaner.getFirstWord(message);
            verifyAndSetUsername(chatId, givenName, userStateEntity);
            return;
        }
        
        userStateEntity.setUserState(UserState.LOGIN_VALIDATE_USERNAME);
        responseService.send(chatId, "Wie lautet dein Benutzername?");
    }
    
    @Override
    @Transactional
    public void handleMessage(long chatId, String message) {
        var userStateEntity = userStateRepository.findById(chatId).orElseThrow();
        if(userStateEntity.getUserState() == UserState.LOGIN_VALIDATE_USERNAME){
            var cleanedName = MessageCleaner.getFirstWord(message);
            verifyAndSetUsername(chatId, cleanedName, userStateEntity);
            return;
        }
        
        if(userStateEntity.getUserState() != UserState.LOGIN_VALIDATE_PIN) {
            log.error("state not implemented in this handler, this should never happen");
            responseService.send(chatId, "da ist etwas bei mir schiefgegangen. Bitte informiere einen Admin und benutze /reset, falls das Problem persistiert");
            return;
        }
        
        var pin = MessageCleaner.getFirstWord(message);
        var userEntity = userStateEntity.getOwner();
        
        if(Objects.equals(pin, userEntity.getPin())){
            userStateEntity.setUserState(UserState.LOGGED_IN);
            responseService.send(chatId, "du wurdest erfolgreich eingeloggt");
        }
        else {
            responseService.send(chatId, "PIN inkorrekt, versuchs nochmal");
        }
    }
    
    private void verifyAndSetUsername(long chatId, String username, UserStateEntity userStateEntity){
        //if user is already correctly assigned, we can move on with PIN validation
        if(userStateEntity.getOwner() != null && Objects.equals(username, userStateEntity.getOwner().getUsername())){
            requestPin(chatId, userStateEntity);
            return;
        }
        
        var userEntityOptional = userRepository.findById(username);
        if(userEntityOptional.isEmpty()){
            responseService.send(chatId, "der angegebene benutzername " + username + " ist unbekannt");
            return;
        }
        var userEntity = userEntityOptional.get();
        userStateEntity.setOwner(userEntity);
        requestPin(chatId, userStateEntity);
    }
    
    private void requestPin(long chatId, UserStateEntity userStateEntity){
        userStateEntity.setUserState(UserState.LOGIN_VALIDATE_PIN);
        responseService.send(chatId, "Wie lautet deine PIN?");
    }
}
