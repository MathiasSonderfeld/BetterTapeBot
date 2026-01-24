package eu.sonderfeld.mathias.bettertapebot.handler;

import org.jspecify.annotations.NonNull;

public interface CommandHandler {

    @NonNull Command forCommand();
    void handleCommand(long chatId, String message);
}
