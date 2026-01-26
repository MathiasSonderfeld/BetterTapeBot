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
@Import({TestcontainersConfiguration.class, RemoveAdminHandler.class})
class RemoveAdminHandlerTest {
    
    @Autowired
    RemoveAdminHandler removeAdminHandler;
    
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
        assertThat(removeAdminHandler.forCommand()).isEqualTo(Command.REMOVE_ADMIN);
        assertThat(removeAdminHandler.forStates()).containsExactlyInAnyOrder(UserState.REMOVE_ADMIN_USER_GET_USERNAME);
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
        removeAdminHandler.handleMessage(userStateEntity, chatId, "testmessage");
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Nur Admins dürfen andere Admins entfernen");
        Mockito.verifyNoInteractions(userRepository);
    }
    
    @Test
    public void removeAdminwithoutUsernameGetsAskedForUsername(){
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
        removeAdminHandler.handleMessage(userStateEntity, chatId, null);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Welcher Admin soll wieder eingeschränkt werden?");
        Mockito.verifyNoInteractions(userRepository);
        
        assertThat(userStateEntity)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.REMOVE_ADMIN_USER_GET_USERNAME);
    }
    
    @Test
    public void removeAdminWithUnknownUsernameGetsAskedAgain() {
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
        String unknownUsername = "unknown";
        
        Mockito.reset(userRepository, responseService);
        removeAdminHandler.handleMessage(userStateEntity, chatId, unknownUsername);
        Mockito.verify(userRepository, Mockito.times(1)).findById(unknownUsername);
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
            .isEqualTo(UserState.REMOVE_ADMIN_USER_GET_USERNAME);
    }
    
    @Test
    public void removeAdminCommandWithKnownUserGetsAborted(){
        long chatId = 5678L;
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
        var otherAdmin = userRepository.save(UserEntity.builder()
            .username("otherAdmin")
            .pin("9876")
            .isAdmin(false)
            .build());
        
        Mockito.reset(userRepository, responseService);
        removeAdminHandler.handleMessage(userStateEntity, chatId, otherAdmin.getUsername());
        Mockito.verify(userRepository, Mockito.times(1)).findById(otherAdmin.getUsername());
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains(otherAdmin.getUsername())
            .contains("ist nicht Admin");
        
        assertThat(userStateEntity)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.LOGGED_IN);
    }
    
    @Test
    public void removeAdminCommandWithKnownAdminGetsDemoted(){
        long chatId = 6789L;
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
        var otherUser = userRepository.save(UserEntity.builder()
            .username("otherUser")
            .pin("9876")
            .isAdmin(true)
            .build());
        
        Mockito.reset(userRepository, responseService);
        removeAdminHandler.handleMessage(userStateEntity, chatId, otherUser.getUsername());
        Mockito.verify(userRepository, Mockito.times(1)).findById(otherUser.getUsername());
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains(otherUser.getUsername())
            .contains("ist nicht mehr Admin");
        
        assertThat(otherUser).extracting(UserEntity::getIsAdmin).isEqualTo(false);
        
        assertThat(userStateEntity)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.LOGGED_IN);
    }
}