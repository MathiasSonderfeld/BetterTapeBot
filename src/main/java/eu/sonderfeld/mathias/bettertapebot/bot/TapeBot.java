package eu.sonderfeld.mathias.bettertapebot.bot;

import eu.sonderfeld.mathias.bettertapebot.commandhandler.Command;
import eu.sonderfeld.mathias.bettertapebot.commandhandler.CommandHandler;
import eu.sonderfeld.mathias.bettertapebot.properties.BotProperties;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@CustomLog
@RequiredArgsConstructor
@EnableConfigurationProperties(BotProperties.class)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TapeBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    BotProperties botProperties;
    Set<CommandHandler> commandHandlers;

    @NonFinal
    Map<Command, CommandHandler> commandHandlerMap;

    @PostConstruct
    private void postconstruct(){
        commandHandlerMap = commandHandlers.stream()
            .collect(Collectors.toMap(CommandHandler::forCommand, Function.identity()));
    }

    @Override
    public String getBotToken() {
        return botProperties.getTelegram().getToken();
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {

        if(!update.hasMessage() || !update.getMessage().hasText()){
            log.warn("update was ignored {}", update);
            return;
        }

        long chatId = update.getMessage().getChatId();
        String receivedText = update.getMessage().getText();
        String commandText = update.getMessage().getEntities().getFirst().getText();


//
//        // If input is stateless command
//        Handler commandHandler = CommandHandlerFactory.getCommandHandler(receivedText);
//        if (commandHandler != null) {
//            commandHandler.handle(context);
//            return;
//        }
//
//        // Else input belongs to current state
//        Handler handler;
//        try {
//            handler = StateHandlerFactory.getStateHandler(context);
//        } catch (ApplicationException e) {
//            e.handle(context);
//            return;
//        }
//        handler.handle(context);
    }
}