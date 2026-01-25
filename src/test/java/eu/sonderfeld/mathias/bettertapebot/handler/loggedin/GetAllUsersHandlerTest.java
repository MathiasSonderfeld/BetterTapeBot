package eu.sonderfeld.mathias.bettertapebot.handler.loggedin;

import eu.sonderfeld.mathias.bettertapebot.bot.ResponseService;
import eu.sonderfeld.mathias.bettertapebot.handler.Command;
import eu.sonderfeld.mathias.bettertapebot.repository.UserRepository;
import eu.sonderfeld.mathias.bettertapebot.repository.UserStateRepository;
import eu.sonderfeld.mathias.bettertapebot.repository.entity.UserEntity;
import eu.sonderfeld.mathias.bettertapebot.repository.entity.UserState;
import eu.sonderfeld.mathias.bettertapebot.repository.entity.UserStateEntity;
import eu.sonderfeld.mathias.bettertapebot.testutil.TestcontainersConfiguration;
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
@Import({TestcontainersConfiguration.class, GetAllUsersHandler.class})
class GetAllUsersHandlerTest {
    
    @Autowired
    GetAllUsersHandler getAllUsersHandler;
    
    @MockitoSpyBean
    UserStateRepository userStateRepository;
    
    @MockitoSpyBean
    UserRepository userRepository;
    
    @MockitoBean
    ResponseService responseService;
    
    @BeforeEach
    void reset(){
        Mockito.reset(
            userRepository,
            userStateRepository,
            responseService
        );
    }
    
    @AfterEach
    void cleanUp(){
        userStateRepository.deleteAll();
        userRepository.deleteAll();
    }
    
    @Test
    public void registersForCorrectCommand(){
        assertThat(getAllUsersHandler.forCommand()).isEqualTo(Command.USERS);
    }
    
    @Test
    public void unknownUserGetsDenied(){
        Long chatId = 1234L;
        getAllUsersHandler.handleCommand(chatId, "testmessage");
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Nur eingeloggte User können andere User sehen");
    }
    
    @Test
    public void loggedInUserGetsAllUsers(){
        Long chatId = 2345L;
        var user1 = userRepository.save(UserEntity.builder()
            .username("user1")
            .pin("1234")
            .build());
        
        var user2 = userRepository.save(UserEntity.builder()
            .username("user2")
            .pin("1234")
            .build());
        
        var user3 = userRepository.save(UserEntity.builder()
            .username("user3")
            .pin("1234")
            .build());
        
        userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.LOGGED_IN)
            .owner(user1)
            .build());
        
        getAllUsersHandler.handleCommand(chatId, "testmessage");
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Folgende User sind registriert:")
            .contains(user1.getUsername(), user2.getUsername(), user3.getUsername());
    }
}