package bettertapebot.handler;

import bettertapebot.repository.entity.UserStateEntity;
import org.jspecify.annotations.NonNull;

public interface Handler {
    void handleMessage(@NonNull UserStateEntity userStateEntity, String message);
}
