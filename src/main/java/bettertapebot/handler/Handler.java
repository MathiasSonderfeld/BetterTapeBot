package bettertapebot.handler;

import bettertapebot.repository.entity.UserStateEntity;
import org.jspecify.annotations.NonNull;

public interface Handler {
    void handleMessage(@NonNull UserStateEntity userStateEntity, long chatId, String message); //TODO refactor - remove chatId as its already in the entity
}
