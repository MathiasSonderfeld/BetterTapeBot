package bettertapebot.handler.admin;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.Command;
import bettertapebot.repository.UserRepository;
import bettertapebot.repository.UserStateRepository;
import bettertapebot.repository.entity.UserEntity;
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
@Import({TestcontainersConfiguration.class, DeleteUserHandler.class})
class DeleteUserHandlerTest {
    
    @Autowired
    DeleteUserHandler deleteUserHandler;
    
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
        assertThat(deleteUserHandler.forCommand()).isEqualTo(Command.DELETE_USER);
    }
    
    @Test
    public void registersForCorrectStates(){
        assertThat(deleteUserHandler.forStates()).containsExactlyInAnyOrder(UserState.DELETE_USER_GET_USERNAME);
    }
    
    @Test
    public void notAdminChatGetsDenied(){
        Long chatId = 1234L;
        deleteUserHandler.handleCommand(chatId, "testmessage");
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Nur Admins können Benutzer löschen");
    }
    
    @Test
    public void deleteUserCommandWithUnknownUserGetsAskedAgain(){
        Long chatId = 3456L;
        String usernameToDelete = "redundantuser";
        var admin = userRepository.save(UserEntity.builder()
            .username("username")
            .pin("1234")
            .isAdmin(true)
            .build());
        
        var state = userStateRepository.save(UserStateEntity.builder()
            .userState(UserState.ADMIN)
            .owner(admin)
            .chatId(chatId)
            .build());
        
        Mockito.reset(userRepository, userStateRepository);
        deleteUserHandler.handleCommand(chatId, usernameToDelete);
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        Mockito.verify(userRepository, Mockito.times(1)).deleteByUsername(usernameToDelete);
        
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Den Benutzer gibt es nicht. Probiers nochmal");
        
        assertThat(state)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.DELETE_USER_GET_USERNAME);
    }
    
    @Test
    public void deleteUserCommandWithoutUserGetsAskedForUsername(){
        Long chatId = 3456L;
        var admin = userRepository.save(UserEntity.builder()
            .username("username")
            .pin("1234")
            .isAdmin(true)
            .build());
        
        var state = userStateRepository.save(UserStateEntity.builder()
            .userState(UserState.ADMIN)
            .owner(admin)
            .chatId(chatId)
            .build());
        
        Mockito.reset(userRepository, userStateRepository);
        deleteUserHandler.handleCommand(chatId, null);
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Wie lautet der Benutzername?");
        Mockito.verifyNoInteractions(userRepository);
        
        assertThat(state)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.DELETE_USER_GET_USERNAME);
    }
    
    @Test
    public void deleteUserCommandWithKnownUserGetsDeleted(){
        Long chatId = 3456L;
        String usernameToDelete = "redundantuser";
        var admin = userRepository.save(UserEntity.builder()
            .username("username")
            .pin("1234")
            .isAdmin(true)
            .build());
        
        var state = userStateRepository.save(UserStateEntity.builder()
            .userState(UserState.ADMIN)
            .owner(admin)
            .chatId(chatId)
            .build());
        
        userRepository.save(UserEntity.builder()
            .username(usernameToDelete)
            .pin("9876")
            .isAdmin(false)
            .build());
        
        Mockito.reset(userRepository, userStateRepository);
        deleteUserHandler.handleCommand(chatId, usernameToDelete);
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        Mockito.verify(userRepository, Mockito.times(1)).deleteByUsername(usernameToDelete);
        Mockito.verifyNoInteractions(responseService);
        
        var deleted = userRepository.findById(usernameToDelete);
        assertThat(deleted).isNotNull().isEmpty();
        
        assertThat(state)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.ADMIN);
    }
    
    @Test
    public void messageWithKnownUserGetsDeleted(){
        long chatId = 3456L;
        String usernameToDelete = "redundantuser";
        var admin = userRepository.save(UserEntity.builder()
            .username("username")
            .pin("1234")
            .isAdmin(true)
            .build());
        
        var state = userStateRepository.save(UserStateEntity.builder()
            .userState(UserState.ADMIN)
            .owner(admin)
            .chatId(chatId)
            .build());
        
        userRepository.save(UserEntity.builder()
            .username(usernameToDelete)
            .pin("9876")
            .isAdmin(false)
            .build());
        
        Mockito.reset(userRepository, userStateRepository);
        deleteUserHandler.handleMessage(state, chatId, usernameToDelete);
        Mockito.verify(userRepository, Mockito.times(1)).deleteByUsername(usernameToDelete);
        Mockito.verifyNoInteractions(userStateRepository, responseService);
        
        var deleted = userRepository.findById(usernameToDelete);
        assertThat(deleted).isNotNull().isEmpty();
        
        assertThat(state)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.ADMIN);
    }
    
    @Test
    public void messageWithUnknownUserGetsAskedAgain(){
        Long chatId = 3456L;
        String usernameToDelete = "redundantuser";
        var admin = userRepository.save(UserEntity.builder()
            .username("username")
            .pin("1234")
            .isAdmin(true)
            .build());
        
        var state = userStateRepository.save(UserStateEntity.builder()
            .userState(UserState.ADMIN)
            .owner(admin)
            .chatId(chatId)
            .build());
        
        Mockito.reset(userRepository, userStateRepository);
        deleteUserHandler.handleMessage(state, chatId, usernameToDelete);
        Mockito.verify(userRepository, Mockito.times(1)).deleteByUsername(usernameToDelete);
        
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Den Benutzer gibt es nicht. Probiers nochmal");
        Mockito.verifyNoInteractions(userStateRepository);
        
        assertThat(state)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.DELETE_USER_GET_USERNAME);
    }
}