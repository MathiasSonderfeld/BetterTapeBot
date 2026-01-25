package bettertapebot.handler.admin;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.CommandHandler;
import bettertapebot.repository.UserStateRepository;
import bettertapebot.repository.entity.UserState;
import bettertapebot.repository.entity.UserStateEntity;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public abstract class AbstractAdminHandler implements CommandHandler {
    
    UserStateRepository userStateRepository;
    ResponseService responseService;
    
    @Override
    public void handleCommand(long chatId, String message) {
        var stateOptional = userStateRepository.findById(chatId);
        var knownAndAdmin = stateOptional.map(UserStateEntity::getUserState)
            .map(UserState::isAdmin)
            .orElse(false);
        if (!knownAndAdmin) {
            responseService.send(chatId, getErrorMessage());
            return;
        }
        var userStateEntity = stateOptional.get();
        if(StringUtils.hasText(message)){
            handleCommandWithMessage(userStateEntity, message);
            return;
        }
        handleCommandWithoutMessage(userStateEntity);
    }
    
    protected abstract @NonNull String getErrorMessage();
    protected abstract void handleCommandWithMessage(@NonNull UserStateEntity userStateEntity, @NonNull String message);
    protected abstract void handleCommandWithoutMessage(@NonNull UserStateEntity userStateEntity);
}
