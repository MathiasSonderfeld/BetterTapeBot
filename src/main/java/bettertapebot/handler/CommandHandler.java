package bettertapebot.handler;

import org.jspecify.annotations.NonNull;

public interface CommandHandler extends Handler {

    @NonNull Command forCommand();
}
