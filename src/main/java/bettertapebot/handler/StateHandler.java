package bettertapebot.handler;

import bettertapebot.repository.entity.UserState;
import bettertapebot.repository.entity.UserStateEntity;
import org.jspecify.annotations.NonNull;

import java.util.Set;

public interface StateHandler {
    @NonNull Set<UserState> forStates();
    void handleMessage(@NonNull UserStateEntity userStateEntity, long chatId, String message);
}
