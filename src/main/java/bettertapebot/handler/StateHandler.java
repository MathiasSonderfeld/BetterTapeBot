package bettertapebot.handler;

import bettertapebot.repository.entity.UserState;
import org.jspecify.annotations.NonNull;

import java.util.Set;

public interface StateHandler extends Handler {
    @NonNull Set<UserState> forStates();
    
}
