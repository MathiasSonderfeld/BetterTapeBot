package bettertapebot.handler.admin;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.Command;
import bettertapebot.handler.CommandHandler;
import bettertapebot.handler.StateHandler;
import bettertapebot.repository.TapeRepository;
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
import java.util.UUID;

@CustomLog
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeleteTapeHandler implements CommandHandler, StateHandler {

    TapeRepository tapeRepository;
    ResponseService responseService;
    
    @Override
    public @NonNull Command forCommand() {
        return Command.DELETE_TAPE;
    }
    
    @Override
    public @NonNull Set<UserState> forStates() {
        return Set.of(UserState.DELETE_TAPE_GET_TAPE_ID);
    }
    
    @Override
    @Transactional
    public void handleMessage(@NonNull UserStateEntity userStateEntity, String message) {
        if (!userStateEntity.isAdminModeActive()) {
            responseService.send(userStateEntity.getChatId(), "Nur Admins können Tapes löschen");
            return;
        }
        
        if(!StringUtils.hasText(message)){
            userStateEntity.setUserState(UserState.DELETE_TAPE_GET_TAPE_ID);
            responseService.send(userStateEntity.getChatId(), "Wie lautet die Tape-ID?");
            return;
        }
        
        UUID id = parseUUID(MessageCleaner.getFirstWord(message));
        if(id == null){
            userStateEntity.setUserState(UserState.DELETE_TAPE_GET_TAPE_ID);
            responseService.send(userStateEntity.getChatId(), "Die ID konnte ich nicht parsen, probiers nochmal");
            return;
        }
        
        var deleteOptional = tapeRepository.deleteTapeEntityById(id);
        if(deleteOptional.isPresent()){
            log.info("deleting tape with id {} on request of {}", id, userStateEntity.getOwner().getUsername());
            userStateEntity.setUserState(UserState.LOGGED_IN);
        }
        else {
            userStateEntity.setUserState(UserState.DELETE_TAPE_GET_TAPE_ID);
            responseService.send(userStateEntity.getChatId(), "Zu der ID konnte ich keinen Eintrag finden, probiers nochmal");
        }
    }
    
    private UUID parseUUID(String uuidString){
        try{
            return UUID.fromString(uuidString);
        }
        catch (IllegalArgumentException e){
            log.warn("failed to parse UUID from string {}", uuidString, e);
            return null;
        }
    }
}
