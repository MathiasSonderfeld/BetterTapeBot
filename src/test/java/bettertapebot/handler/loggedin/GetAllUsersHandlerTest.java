package bettertapebot.handler.loggedin;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.Command;
import bettertapebot.properties.BotProperties;
import bettertapebot.repository.UserRepository;
import bettertapebot.repository.UserStateRepository;
import bettertapebot.repository.entity.UserEntity;
import bettertapebot.repository.entity.UserState;
import bettertapebot.repository.entity.UserStateEntity;
import bettertapebot.testutil.TestcontainersConfiguration;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
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
@Import({TestcontainersConfiguration.class, GetAllUsersHandler.class, BotProperties.class})
class GetAllUsersHandlerTest {
    
    @Autowired
    GetAllUsersHandler getAllUsersHandler;
    
    @MockitoSpyBean
    UserRepository userRepository;
    
    @MockitoBean
    ResponseService responseService;
    
    @Autowired
    UserStateRepository userStateRepository;
    
    @Autowired
    BotProperties botProperties;
    
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
        long chatId = 1234L;
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.NEW_CHAT)
            .build());
        
        Mockito.reset(userRepository, responseService);
        getAllUsersHandler.handleMessage(userStateEntity, chatId, "testmessage");
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Nur eingeloggte User können andere User sehen");
        Mockito.verifyNoInteractions(userRepository);
    }
    
    @Test
    public void loggedInUserGetsAllUsers(){
        long chatId = 2345L;
        var userEntity = userRepository.save(UserEntity.builder()
            .username("admin")
            .pin("1234")
            .isAdmin(true)
            .build());
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.LOGGED_IN)
            .adminMode(true)
            .owner(userEntity)
            .build());
        
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
        
        Mockito.reset(userRepository, responseService);
        getAllUsersHandler.handleMessage(userStateEntity, chatId, "testmessage");
        Mockito.verify(userRepository, Mockito.times(1)).findAll();
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Folgende User sind registriert:")
            .contains(user1.getUsername())
            .contains(user2.getUsername())
            .contains(user3.getUsername())
            .doesNotContain(botProperties.getDefaultUserForTapes());
    }
}