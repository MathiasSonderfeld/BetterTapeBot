package bettertapebot.handler.loggedin;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.Command;
import bettertapebot.handler.CommandHandler;
import bettertapebot.handler.StateHandler;
import bettertapebot.properties.BotProperties;
import bettertapebot.repository.TapeRepository;
import bettertapebot.repository.UserRepository;
import bettertapebot.repository.entity.UserState;
import bettertapebot.repository.entity.UserStateEntity;
import bettertapebot.util.MessageCleaner;
import bettertapebot.util.TapeFormatter;
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
public class StarringHandler implements CommandHandler, StateHandler {

    ResponseService responseService;
    UserRepository userRepository;
    TapeRepository tapeRepository;
     BotProperties botProperties;

    @Override
    public @NonNull Command forCommand() {
        return Command.STARRING;
    }
    
    @Override
    public @NonNull Set<UserState> forStates() {
        return Set.of(UserState.STARRING_GET_USERNAME);
    }

    @Override
    @Transactional
    public void handleMessage(@NonNull UserStateEntity userStateEntity, String message) {
        if (!userStateEntity.getUserState().isLoggedIn()) {
            responseService.send(userStateEntity.getChatId(), "Nur eingeloggte User können Tapes abfragen");
            return;
        }
        
        if(!StringUtils.hasText(message)){
            userStateEntity.setUserState(UserState.STARRING_GET_USERNAME);
            responseService.send(userStateEntity.getChatId(), "Wessen Darbietungen möchtest du begutachten?");
            return;
        }
        
        var givenUsername = MessageCleaner.getFirstWord(message);
        var starOptional = userRepository.findById(givenUsername);
        if(starOptional.isEmpty()){
            userStateEntity.setUserState(UserState.STARRING_GET_USERNAME);
            responseService.send(userStateEntity.getChatId(), "Den Benutzer gibt es nicht. Probiers nochmal");
            return;
        }
        
        var star = starOptional.get();
        var tapes = tapeRepository.findAllByStar(star);
        boolean isAdmin = userStateEntity.isAdminModeActive();
        var response = TapeFormatter.formatTapes(tapes, botProperties.getOutputTimezone(), isAdmin);
        responseService.send(userStateEntity.getChatId(), response);
        userStateEntity.setUserState(UserState.LOGGED_IN);
    }
}
