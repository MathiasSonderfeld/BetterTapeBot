package bettertapebot.handler.general;

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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({TestcontainersConfiguration.class, ResetStateHandler.class})
class ResetStateHandlerTest {
    
    @Autowired
    ResetStateHandler resetStateHandler;
    
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
        assertThat(resetStateHandler.forCommand()).isEqualTo(Command.RESET);
    }
    
    @Test
    public void unknownChatGetsSkipped(){
        long chatId = 1234L;
        resetStateHandler.handleCommand(chatId, "testmessage");
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("chat unbekannt, kein reset nötig");
    }
    
    @Test
    public void chatGetsReset(){
        long chatId = 2345L;
        
        userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.LOGGED_IN)
            .build());
        
        resetStateHandler.handleCommand(chatId, "testmessage");
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Chat wurde zurückgesetzt");
        
        var expected = userStateRepository.findById(chatId);
        assertThat(expected).isNotNull().isEmpty();
    }
}