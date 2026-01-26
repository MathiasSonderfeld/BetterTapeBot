package bettertapebot.handler.general;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.Command;
import bettertapebot.handler.CommandHandler;
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
public class GetMeHandler implements CommandHandler {

    ResponseService responseService;

    @Override
    public @NonNull Command forCommand() {
        return Command.ME;
    }

    @Override
    @Transactional
    public void handleMessage(@NonNull UserStateEntity userStateEntity, long chatId, String message) {
        var owner = userStateEntity.getOwner();
        var username = owner != null ? owner.getUsername() : "unknown";
        var stateName = userStateEntity.getUserState().name();
        responseService.send(chatId, String.format("chatId: %d, username: %s, user state: %s", chatId, username, stateName));
    }
}
