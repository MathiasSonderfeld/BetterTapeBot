package bettertapebot.handler.general;

import bettertapebot.bot.ResponseService;
import bettertapebot.config.InitialAdminCodeGenerator;
import bettertapebot.handler.Command;
import bettertapebot.handler.CommandHandler;
import bettertapebot.handler.StateHandler;
import bettertapebot.properties.BotProperties;
import bettertapebot.repository.UserRepository;
import bettertapebot.repository.entity.UserEntity;
import bettertapebot.repository.entity.UserState;
import bettertapebot.repository.entity.UserStateEntity;
import bettertapebot.util.MessageCleaner;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;

@CustomLog
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InitHandler implements CommandHandler, StateHandler { //TODO implement tests
    
    BotProperties botProperties;
    ResponseService responseService;
    InitialAdminCodeGenerator initialAdminCodeGenerator;
    UserRepository userRepository;
    
    @Override
    public @NonNull Command forCommand() {
        return Command.INIT;
    }
    
    @Override
    public @NonNull Set<UserState> forStates() {
        return Set.of(UserState.INIT_REQUEST_ADMIN_KEY, UserState.INIT_REQUEST_USERNAME, UserState.INIT_REQUEST_PIN);
    }
    
    @Override
    public void handleMessage(@NonNull UserStateEntity userStateEntity, String message) {
        var cleanedInput = MessageCleaner.getFirstWord(message);
        if(userStateEntity.getUserState() == UserState.INIT_REQUEST_USERNAME){
            handleUsername(userStateEntity, cleanedInput);
            return;
        }
        
        if(userStateEntity.getUserState() == UserState.INIT_REQUEST_PIN){
            handlePin(userStateEntity, cleanedInput);
            return;
        }
        
        if(!StringUtils.hasText(cleanedInput)){
            userStateEntity.setUserState(UserState.INIT_REQUEST_ADMIN_KEY);
            responseService.send(userStateEntity.getChatId(), "Wie lautet der initiale Admin Key des Bots? Du findest ihn in der Log-Ausgabe.");
            return;
        }
        
        int code;
        try{
            code = Integer.parseInt(cleanedInput);
        }
        catch (NumberFormatException e){
            userStateEntity.setUserState(UserState.INIT_REQUEST_ADMIN_KEY);
            responseService.send(userStateEntity.getChatId(), "Der initiale Admin Key ist ungültig");
            return;
        }
        if(!initialAdminCodeGenerator.validatesInitialAdminKey(code)){
            userStateEntity.setUserState(UserState.INIT_REQUEST_ADMIN_KEY);
            responseService.send(userStateEntity.getChatId(), "Der Aktivierungscode ist ungültig");
        }
        else {
            userStateEntity.setUserState(UserState.INIT_REQUEST_USERNAME);
            responseService.send(userStateEntity.getChatId(), "Willkommen auf deinem neuen Bot! 😎");
            responseService.send(userStateEntity.getChatId(), "Wie soll dein Username lauten?");
        }
    }
    
    private void handleUsername(UserStateEntity userStateEntity, String username){
        if(!botProperties.getInputValidation().getUsername().matcher(username).matches()){
            responseService.send(userStateEntity.getChatId(), "Der Username hat ein ungültiges Format. Erlaubte Zeichen sind A-Z, a-z, +, _, ., -");
            return;
        }
        var userEntity = userRepository.save(UserEntity.builder()
            .username(username)
            .pin("xxxx")
            .isAdmin(true)
            .build());
        userStateEntity.setOwner(userEntity);
        userStateEntity.setUserState(UserState.INIT_REQUEST_PIN);
        responseService.send(userStateEntity.getChatId(), "Und eine 4-stellige PIN zum Einloggen");
    }
    
    private void handlePin(UserStateEntity userStateEntity, String pin){
        if(!botProperties.getInputValidation().getPin().matcher(pin).matches()){
            responseService.send(userStateEntity.getChatId(), "Die PIN hat ein ungültiges Format. Ich brauche 4 Ziffern.");
            return;
        }
        userStateEntity.getOwner().setPin(pin);
        userStateEntity.setUserState(UserState.LOGGED_IN);
        responseService.send(userStateEntity.getChatId(), "Willkommen zu deinem neuen Bot");
    }
}
