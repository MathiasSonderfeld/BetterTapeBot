package eu.sonderfeld.mathias.bettertapebot.handler;

import eu.sonderfeld.mathias.bettertapebot.repository.entity.UserState;
import org.jspecify.annotations.NonNull;

import java.util.Set;

public interface StateHandler {
    @NonNull Set<UserState> forStates();
    void handleMessage(long chatId, String message);
}
