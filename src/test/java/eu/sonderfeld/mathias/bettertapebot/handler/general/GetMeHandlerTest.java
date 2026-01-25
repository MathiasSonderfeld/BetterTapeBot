package eu.sonderfeld.mathias.bettertapebot.handler.general;

import eu.sonderfeld.mathias.bettertapebot.bot.ResponseService;
import eu.sonderfeld.mathias.bettertapebot.handler.Command;
import eu.sonderfeld.mathias.bettertapebot.repository.UserStateRepository;
import eu.sonderfeld.mathias.bettertapebot.repository.entity.UserEntity;
import eu.sonderfeld.mathias.bettertapebot.repository.entity.UserState;
import eu.sonderfeld.mathias.bettertapebot.repository.entity.UserStateEntity;
import eu.sonderfeld.mathias.bettertapebot.testutil.TestcontainersConfiguration;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({TestcontainersConfiguration.class, GetMeHandler.class})
class GetMeHandlerTest {
    
    @Autowired
    GetMeHandler getMeHandler;
    
    @MockitoSpyBean
    UserStateRepository userStateRepository;
    
    @MockitoBean
    ResponseService responseService;
    
    @Test
    public void registersForCorrectCommand(){
        assertThat(getMeHandler.forCommand()).isEqualTo(Command.ME);
    }
    
    @Test
    public void unknownChatGetsChatIdOnly(){
        Long chatId = 1234L;
        getMeHandler.handleCommand(chatId, null);
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains(String.valueOf(chatId))
            .contains("keine weiteren daten");
    }
    
    @Test
    public void knownChatGetsDebugData(){
        Long chatId = 2345L;
        String username = "user";
        UserState state = UserState.DELETE_USER_GET_USERNAME;
        
        var userEntity = UserEntity.builder()
            .username(username)
            .pin("1234")
            .isAdmin(true)
            .build();
        
        var userStateEntity = UserStateEntity.builder()
            .chatId(chatId)
            .userState(state)
            .owner(userEntity)
            .build();
        Mockito.when(userStateRepository.findById(chatId)).thenReturn(Optional.of(userStateEntity));
        
        getMeHandler.handleCommand(chatId, null);
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains(String.valueOf(chatId))
            .contains(username)
            .contains(state.name());
    }
}