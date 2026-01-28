package bettertapebot.handler.general;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.Command;
import bettertapebot.handler.CommandHandler;
import bettertapebot.repository.UserStateRepository;
import bettertapebot.repository.entity.UserState;
import bettertapebot.repository.entity.UserStateEntity;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@CustomLog
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ResetStateHandler implements CommandHandler {

    UserStateRepository userStateRepository;
    ResponseService responseService;

    @Override
    public @NonNull Command forCommand() {
        return Command.RESET;
    }

    @Override
    @Transactional
    public void handleMessage(@NonNull UserStateEntity userStateEntity, String message) {
        if(userStateEntity.getUserState() == UserState.NEW_CHAT){
            responseService.send(userStateEntity.getChatId(), "chat unbekannt, kein reset nötig");
            return;
        }
        userStateRepository.deleteById(userStateEntity.getChatId());
        var response = String.format("Chat wurde zurückgesetzt, benutze %s oder %s um von vorne zu beginnen", Command.REGISTER.getCommand(), Command.LOGIN.getCommand());
        responseService.send(userStateEntity.getChatId(), response);
    }
}
