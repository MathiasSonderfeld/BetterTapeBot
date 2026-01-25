package bettertapebot.handler.admin;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.Command;
import bettertapebot.repository.UserStateRepository;
import bettertapebot.repository.entity.UserState;
import bettertapebot.repository.entity.UserStateEntity;
import bettertapebot.testutil.TestcontainersConfiguration;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({TestcontainersConfiguration.class, BroadcastHandler.class})
class BroadcastHandlerTest {
    
    @Autowired
    BroadcastHandler broadcastHandler;
    
    @MockitoSpyBean
    UserStateRepository userStateRepository;
    
    @MockitoBean
    ResponseService responseService;
    
    @BeforeEach
    void reset(){
        Mockito.reset(
            userStateRepository,
            responseService
        );
    }
    
    @AfterEach
    void cleanUp(){
        userStateRepository.deleteAll();
    }
    
    @Test
    public void registersForCorrectCommand(){
        assertThat(broadcastHandler.forCommand()).isEqualTo(Command.BROADCAST);
    }
    
    @Test
    public void registersForCorrectStates(){
        assertThat(broadcastHandler.forStates()).containsExactlyInAnyOrder(UserState.BROADCAST_AWAIT_MESSAGE);
    }
    
    @Test
    public void notAdminChatGetsDenied(){
        long chatId = 1234L;
        broadcastHandler.handleCommand(chatId, "testmessage");
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Nur Admins können Broadcasts senden");
    }
    
    @Test
    public void broadcastCommandWithMessageGetsSent(){
        long chatId = 2345L;
        Long loggedInChatId = 1234L;
        Long loggedOutChatId = 9876L;
        String message = "testmessage";
        var state = userStateRepository.save(UserStateEntity.builder()
            .userState(UserState.ADMIN)
            .owner(null)
            .chatId(chatId)
            .build());
        userStateRepository.save(UserStateEntity.builder()
            .userState(UserState.LOGGED_IN)
            .owner(null)
            .chatId(loggedInChatId)
            .build());
        userStateRepository.save(UserStateEntity.builder()
            .userState(UserState.LOGGED_OUT)
            .owner(null)
            .chatId(loggedOutChatId)
            .build());
        
        broadcastHandler.handleCommand(chatId, message);
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        
        //noinspection unchecked
        ArgumentCaptor<List<Long>> listCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).broadcast(listCaptor.capture(), textCaptor.capture());
        
        var lists = listCaptor.getAllValues();
        assertThat(lists).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.COLLECTION)
            .hasSize(2)
            .containsExactlyInAnyOrder(chatId, loggedInChatId)
            .doesNotContain(loggedOutChatId);
        
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains(message);
        
        assertThat(state)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.ADMIN);
    }
    
    @Test
    public void broadcastCommandWithoutMessageGetsAskedForMessage(){
        long chatId = 3456L;
        userStateRepository.save(UserStateEntity.builder()
            .userState(UserState.ADMIN)
            .owner(null)
            .chatId(chatId)
            .build());
        
        broadcastHandler.handleCommand(chatId, null);
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Wie lautet die Nachricht?");
        
        var userStateEntity = userStateRepository.findById(chatId);
        assertThat(userStateEntity).isPresent().get()
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.BROADCAST_AWAIT_MESSAGE);
    }
    
    @Test
    public void BroadcastMessageGetsSent(){
        long chatId = 4567L;
        Long loggedInChatId = 1234L;
        Long loggedOutChatId = 9876L;
        String message = "testmessage";
        var state = userStateRepository.save(UserStateEntity.builder()
            .userState(UserState.BROADCAST_AWAIT_MESSAGE)
            .owner(null)
            .chatId(chatId)
            .build());
        userStateRepository.save(UserStateEntity.builder()
            .userState(UserState.LOGGED_IN)
            .owner(null)
            .chatId(loggedInChatId)
            .build());
        userStateRepository.save(UserStateEntity.builder()
            .userState(UserState.LOGGED_OUT)
            .owner(null)
            .chatId(loggedOutChatId)
            .build());
        
        broadcastHandler.handleMessage(state, chatId, message);
        //noinspection unchecked
        ArgumentCaptor<List<Long>> listCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).broadcast(listCaptor.capture(), textCaptor.capture());
        
        var lists = listCaptor.getAllValues();
        assertThat(lists).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.COLLECTION)
            .hasSize(2)
            .containsExactlyInAnyOrder(chatId, loggedInChatId)
            .doesNotContain(loggedOutChatId);
        
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains(message);
        
        assertThat(state)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.ADMIN);
    }
}