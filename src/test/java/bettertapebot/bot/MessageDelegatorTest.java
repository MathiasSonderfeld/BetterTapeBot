package bettertapebot.bot;

import bettertapebot.handler.Command;
import bettertapebot.handler.CommandHandler;
import bettertapebot.handler.StateHandler;
import bettertapebot.properties.BotProperties;
import bettertapebot.repository.UserStateRepository;
import bettertapebot.repository.entity.UserState;
import bettertapebot.repository.entity.UserStateEntity;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig
class MessageDelegatorTest {
    private static final Command HANDLED_COMMAND = Command.LOGOUT;
    private static final UserState HANDLED_STATE = UserState.LOGGED_OUT;
    private static final UserStateEntity USER_STATE_ENTITY = UserStateEntity.builder()
        .chatId(1234L)
        .userState(UserState.NEW_CHAT)
        .build();

    BotProperties botProperties;
    ResponseService responseService;
    UserStateRepository userStateRepository;
    CommandHandler commandHandler;
    StateHandler stateHandler;
    MessageDelegator messageDelegator;
    
    private static Update createUpdate(long chatId, String text){
        ArrayList<MessageEntity> botCommands = new ArrayList<>();
        
        // Bot-Commands erkennen: beginnt mit /
        Pattern commandPattern = Pattern.compile("/\\S+");
        Matcher matcher = commandPattern.matcher(text);
        
        while (matcher.find()) {
            botCommands.add(MessageEntity.builder()
                .type("bot_command")
                .offset(matcher.start())
                .length(matcher.end() - matcher.start())
                .build());
        }
        
        Message message = Message.builder()
            .chat(Chat.builder()
                .id(chatId)
                .type("private")
                .build())
            .text(text)
            .entities(botCommands)
            .build();
        
        Update update = new Update();
        update.setMessage(message);
        return update;
    }
    
    @BeforeEach
    void setup(){
        botProperties = new BotProperties();
        botProperties.getTelegram().setToken("token");
        
        responseService = Mockito.mock(ResponseService.class);
        
        
        userStateRepository = Mockito.mock(UserStateRepository.class);
        Mockito.when(userStateRepository.findById(ArgumentMatchers.any())).thenReturn(Optional.of(USER_STATE_ENTITY));
        
        commandHandler = Mockito.mock(CommandHandler.class);
        Mockito.when(commandHandler.forCommand()).thenReturn(HANDLED_COMMAND);
        
        stateHandler = Mockito.mock(StateHandler.class);
        Mockito.when(stateHandler.forStates()).thenReturn(Set.of(HANDLED_STATE));
        
        messageDelegator = new MessageDelegator(responseService, userStateRepository, Set.of(commandHandler), Set.of(stateHandler));
        messageDelegator.postConstruct();
        Mockito.reset(commandHandler, stateHandler);
    }
    
    @Test
    void testEmptyUpdateGetsIgnored(){
        Message message = Message.builder()
            .chat(Chat.builder()
                .id(123L)
                .type("private")
                .build())
            .build();
        Update update = new Update();
        update.setMessage(message);
        
        Assertions.assertDoesNotThrow(() -> messageDelegator.processUpdate(update));
        Mockito.verifyNoInteractions(responseService, userStateRepository, commandHandler, stateHandler);
    }
    
    @Test
    void testMessageWithInvalidCommandGetsInvalidResponse(){
        long chatId = 234L;
        Update update = createUpdate(chatId, "/unknown command");
        
        Assertions.assertDoesNotThrow(() -> messageDelegator.processUpdate(update));
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        Mockito.verifyNoInteractions(commandHandler, stateHandler);
        Mockito.verify(responseService, Mockito.times(1))
            .send(ArgumentMatchers.eq(chatId), ArgumentMatchers.isNull(), ArgumentMatchers.contains("ungültiger Bot-Befehl"));
    }
    
    @Test
    void testMessageWithValidCommandGetsHandledByCommandHandler(){
        long chatId = 345L;
        var text = "data";
        Update update = createUpdate(chatId, HANDLED_COMMAND.getCommand() + " "  + text);
        Assertions.assertDoesNotThrow(() -> messageDelegator.processUpdate(update));
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        Mockito.verifyNoInteractions(responseService, stateHandler);
        Mockito.verify(commandHandler, Mockito.times(1)).handleMessage(USER_STATE_ENTITY, text);
    }
    
    @Test
    void testMessageWithoutCommandAndUnknownStateGetsAskedToLoginOrRegister(){
        long chatId = 456L;
        var text = "data";
        Update update = createUpdate(chatId, text);
        var userStateEntity = UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.NEW_CHAT)
            .build();
        Mockito.when(userStateRepository.findById(chatId)).thenReturn(Optional.of(userStateEntity));
        Assertions.assertDoesNotThrow(() -> messageDelegator.processUpdate(update));
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        Mockito.verifyNoInteractions(commandHandler, stateHandler);
        
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1))
            .send(ArgumentMatchers.eq(chatId), ArgumentMatchers.isNull(), captor.capture());
        
        var captures = captor.getAllValues();
        assertThat(captures).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Hi")
            .contains("/login")
            .contains("/register");
    }
    
    @Test
    void testMessageWithKnownStateButWithoutHandlerGetsWelcomeMessage(){
        long chatId = 567L;
        var text = "data";
        Update update = createUpdate(chatId, text);
        Assertions.assertDoesNotThrow(() -> messageDelegator.processUpdate(update));
        Mockito.verifyNoInteractions(commandHandler, stateHandler);
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1))
            .send(ArgumentMatchers.eq(chatId), ArgumentMatchers.isNull(), captor.capture());
        
        var captures = captor.getAllValues();
        assertThat(captures).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Hi")
            .contains("/login")
            .contains("/register");
    }
    
    @Test
    void testMessageWithKnownStateGetsHandled(){
        long chatId = 678L;
        var text = "data";
        Update update = createUpdate(chatId, text);
        var userStateEntity = UserStateEntity.builder()
            .chatId(chatId)
            .userState(HANDLED_STATE)
            .build();
        Mockito.when(userStateRepository.findById(chatId)).thenReturn(Optional.of(userStateEntity));
        Assertions.assertDoesNotThrow(() -> messageDelegator.processUpdate(update));
        Mockito.verifyNoInteractions(commandHandler);
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        Mockito.verify(stateHandler, Mockito.times(1))
            .handleMessage(ArgumentMatchers.any(), ArgumentMatchers.eq(text));
    }
}