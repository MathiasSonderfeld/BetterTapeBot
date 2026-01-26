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
    UserRepository userRepository;
    
    @MockitoBean
    ResponseService responseService;
    
    @Autowired
    UserStateRepository userStateRepository;
    
    @AfterEach
    void cleanUp(){
        userStateRepository.deleteAll();
        userRepository.deleteAll();
    }
    
    @Test
    public void registersForCorrectCommandAndStates(){
        assertThat(deleteUserHandler.forCommand()).isEqualTo(Command.DELETE_USER);
        assertThat(deleteUserHandler.forStates()).containsExactlyInAnyOrder(UserState.DELETE_USER_GET_USERNAME);
    }
    
    @Test
    public void notAdminChatGetsDenied(){
        long chatId = 1234L;
        var userEntity = userRepository.save(UserEntity.builder()
            .username("user")
            .pin("1234")
            .isAdmin(true)
            .build());
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.LOGGED_IN)
            .owner(userEntity)
            .build());
        
        Mockito.reset(userRepository, responseService);
        deleteUserHandler.handleMessage(userStateEntity, chatId, "testmessage");
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Nur Admins können Benutzer löschen");
        Mockito.verifyNoInteractions(userRepository);
    }
    
    @Test
    public void deleteUserWithoutUsernameGetsAskedForUsername(){
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
        
        Mockito.reset(userRepository, responseService);
        deleteUserHandler.handleMessage(userStateEntity, chatId, null);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Wie lautet der Benutzername?");
        Mockito.verifyNoInteractions(userRepository);
        
        assertThat(userStateEntity)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.DELETE_USER_GET_USERNAME);
    }
    
    @Test
    public void deleteUserWithUnknownUsernameGetsAskedAgain(){
        long chatId = 3456L;
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
        String usernameToDelete = "unknown";
        
        Mockito.reset(userRepository, responseService);
        deleteUserHandler.handleMessage(userStateEntity, chatId, usernameToDelete);
        Mockito.verify(userRepository, Mockito.times(1)).deleteByUsername(usernameToDelete);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Den Benutzer gibt es nicht. Probiers nochmal");
        
        assertThat(userStateEntity)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.DELETE_USER_GET_USERNAME);
    }
    
    @Test
    public void deleteUserCommandWithKnownUserGetsDeleted(){
        long chatId = 4567L;
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
        
        var userToRemove = userRepository.save(UserEntity.builder()
            .username("user")
            .pin("1234")
            .isAdmin(false)
            .build());
        
        Mockito.reset(userRepository, responseService);
        deleteUserHandler.handleMessage(userStateEntity, chatId, userToRemove.getUsername());
        Mockito.verify(userRepository, Mockito.times(1)).deleteByUsername(userToRemove.getUsername());
        Mockito.verifyNoInteractions(responseService);
        
        var deleted = userRepository.findById(userToRemove.getUsername());
        assertThat(deleted).isNotNull().isEmpty();
        
        assertThat(userStateEntity)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.LOGGED_IN);
    }
}