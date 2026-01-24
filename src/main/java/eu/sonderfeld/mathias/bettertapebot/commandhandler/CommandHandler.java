package eu.sonderfeld.mathias.bettertapebot.commandhandler;

public interface CommandHandler {

    Command forCommand();
    void handleMessage(long chatId, String message);
}
