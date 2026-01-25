package bettertapebot.handler.admin;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.Command;
import bettertapebot.handler.StateHandler;
import bettertapebot.repository.TapeRepository;
import bettertapebot.repository.UserStateRepository;
import bettertapebot.repository.entity.UserState;
import bettertapebot.repository.entity.UserStateEntity;
import bettertapebot.util.MessageCleaner;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@CustomLog
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeleteTapeHandler extends AbstractAdminHandler implements StateHandler {

    TapeRepository tapeRepository;
    
    public DeleteTapeHandler(UserStateRepository userStateRepository, ResponseService responseService, TapeRepository tapeRepository) {
        super(userStateRepository, responseService);
        this.tapeRepository = tapeRepository;
    }
    
    @Override
    public @NonNull Command forCommand() {
        return Command.DELETE_TAPE;
    }
    
    @Override
    public @NonNull Set<UserState> forStates() {
        return Set.of(UserState.DELETE_TAPE_GET_TAPE_ID);
    }
    
    @Override
    protected @NonNull String getErrorMessage(){
        return "Nur Admins können Tapes löschen";
    }
    
    @Override
    public void handleMessage(@NonNull UserStateEntity userStateEntity, long chatId, String message) {
        handleCommandWithMessage(userStateEntity, message);
    }
    
    @Override
    protected void handleCommandWithoutMessage(@NonNull UserStateEntity userStateEntity) {
        userStateEntity.setUserState(UserState.DELETE_TAPE_GET_TAPE_ID);
        responseService.send(userStateEntity.getChatId(), "Wie lautet die Tape-ID?");
    }
    
    @Override
    protected void handleCommandWithMessage(@NonNull UserStateEntity userStateEntity, @NonNull String message) {
        var uuidString = MessageCleaner.getFirstWord(message);
        UUID id;
        try{
            id = UUID.fromString(uuidString);
        }
        catch (IllegalArgumentException e){
            log.warn("failed to parse UUID from string {}, cut out from user input {}", uuidString, message, e);
            userStateEntity.setUserState(UserState.DELETE_TAPE_GET_TAPE_ID);
            responseService.send(userStateEntity.getChatId(), "Die ID konnte ich nicht parsen, probiers nochmal");
            return;
        }
        var deleteOptional = tapeRepository.deleteTapeEntityById(id);
        if(deleteOptional.isPresent()){
            log.info("deleting tape with id {} on request of {}", id, userStateEntity.getOwner().getUsername());
            userStateEntity.setUserState(UserState.ADMIN);
        }
        else {
            userStateEntity.setUserState(UserState.DELETE_TAPE_GET_TAPE_ID);
            responseService.send(userStateEntity.getChatId(), "Zu der ID konnte ich keinen Eintrag finden, probiers nochmal");
        }
    }
}
