package bettertapebot.bot;

import bettertapebot.handler.Command;
import bettertapebot.handler.CommandHandler;
import bettertapebot.handler.StateHandler;
import bettertapebot.properties.BotProperties;
import bettertapebot.repository.UserStateRepository;
import bettertapebot.repository.entity.UserState;
import bettertapebot.repository.entity.UserStateEntity;
import bettertapebot.util.MessageCleaner;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Component
@CustomLog
@RequiredArgsConstructor
@EnableConfigurationProperties(BotProperties.class)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TapeBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    BotProperties botProperties;
    ResponseService responseService;
    UserStateRepository userStateRepository;
    
    Set<CommandHandler> commandHandlers;
    Set<StateHandler> stateHandlers;

    @NonFinal
    Map<Command, CommandHandler> commandHandlerMap;
    
    @NonFinal
    Map<UserState, StateHandler> stateHandlerMap;
    
    @PostConstruct
    void postConstruct(){
        commandHandlerMap = new EnumMap<>(Command.class);
        for (CommandHandler commandHandler : commandHandlers) {
            commandHandlerMap.put(commandHandler.forCommand(), commandHandler);
        }
        
        stateHandlerMap = new EnumMap<>(UserState.class);
        for (StateHandler stateHandler : stateHandlers) {
            for (UserState userState : stateHandler.forStates()) {
                stateHandlerMap.put(userState, stateHandler);
            }
        }
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
    @Transactional
    public void consume(Update update) {
        if(!update.hasMessage() || !update.getMessage().hasText()){
            log.warn("update was ignored as it has no messsage for chatid {} - {}", update.getMessage().getChatId(), update);
            return;
        }
        
        Message message = update.getMessage();
        long chatId = message.getChatId();
        String receivedText = message.getText();
        String botCommand = Optional.ofNullable(message.getEntities())
            .stream().flatMap(Collection::stream)
            .filter(e -> Objects.equals("bot_command", e.getType()))
            .findFirst()
            .map(MessageEntity::getText)
            .orElse(null);
        Command command = Command.fromCommandString(botCommand);
        
        //if command is unknown, reject it
        if(botCommand != null && command == null){
            responseService.send(chatId, null, "ungültiger bot-befehl, benutze " + Command.HELP.getCommand() + " für eine Liste der möglichen Befehle");
            return;
        }
        
        //if command is known, process it
        if(command != null){
            receivedText = MessageCleaner.removeCommand(receivedText, command);
            var handler = commandHandlerMap.get(command);
            if(handler == null){
                log.error("missing handler for registered command {}", botCommand);
            }
            else {
                handler.handleCommand(chatId, receivedText);
            }
            return;
        }
        
        //no command was given, processing based on state
        var stateOptional = userStateRepository.findById(chatId);
        if(stateOptional.isEmpty()){
            responseService.send(chatId, null, "Hi, gib /login zum einloggen oder /register zum registrieren ein.");
            return;
        }
        UserStateEntity userStateEntity = stateOptional.get();
        var handler = stateHandlerMap.get(userStateEntity.getUserState());
        if(handler != null){
            handler.handleMessage(userStateEntity, chatId, receivedText);
        }
    }
}