package bettertapebot.handler.admin;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.Command;
import bettertapebot.repository.UserStateRepository;
import bettertapebot.repository.entity.UserState;
import bettertapebot.repository.entity.UserStateEntity;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

@CustomLog
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExitAdminHandler extends AbstractAdminHandler {
    
    public ExitAdminHandler(UserStateRepository userStateRepository, ResponseService responseService) {
        super(userStateRepository, responseService);
    }
    
    @Override
    public @NonNull Command forCommand() {
        return Command.EXIT;
    }
    
    @Override
    protected @NonNull String getErrorMessage() {
        return "Du bist gar nicht im Admin-Modus";
    }
    
    @Override
    protected void handleCommandWithMessage(@NonNull UserStateEntity userStateEntity, @NonNull String message) {
        handleCommandWithoutMessage(userStateEntity);
    }
    
    @Override
    protected void handleCommandWithoutMessage(@NonNull UserStateEntity userStateEntity) {
        userStateEntity.setUserState(UserState.LOGGED_IN);
        responseService.send(userStateEntity.getChatId(), "Du hast den Admin-Modus verlassen");
    }
}
