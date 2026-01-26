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

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({TestcontainersConfiguration.class, ResetUserHandler.class})
class ResetUserHandlerTest {
    
    @Autowired
    ResetUserHandler resetUserHandler;
    
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
        assertThat(resetUserHandler.forCommand()).isEqualTo(Command.RESET_USER);
        assertThat(resetUserHandler.forStates()).containsExactlyInAnyOrder(UserState.RESET_USER_GET_USERNAME);
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
        resetUserHandler.handleMessage(userStateEntity, chatId, "testmessage");
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Nur Admins dürfen Benutzer zurücksetzen");
        Mockito.verifyNoInteractions(userRepository);
    }
    
    @Test
    public void resetUserwithoutUsernameGetsAskedForUsername(){
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
        resetUserHandler.handleMessage(userStateEntity, chatId, null);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Welcher User soll zurückgesetzt werden?");
        Mockito.verifyNoInteractions(userRepository);
        
        assertThat(userStateEntity)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.RESET_USER_GET_USERNAME);
    }
    
    @Test
    public void resetUserWithUnknownUsernameGetsAskedAgain() {
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
        resetUserHandler.handleMessage(userStateEntity, chatId, unknownUsername);
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
            .isEqualTo(UserState.RESET_USER_GET_USERNAME);
    }
    
    @Test
    public void resetUserCommandWithValidUserGetsReset(){
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
        userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.LOGGED_IN)
            .adminMode(true)
            .owner(otherUser)
            .build());
        
        Mockito.reset(userRepository, responseService);
        resetUserHandler.handleMessage(userStateEntity, chatId, otherUser.getUsername());
        Mockito.verify(userRepository, Mockito.times(1)).findById(otherUser.getUsername());
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains(otherUser.getUsername())
            .contains("in 1 chats zurückgesetzt");
        
        var allChats = userStateRepository.findAll();
        assertThat(allChats).allMatch(e -> !Objects.equals(e.getOwner().getUsername(), otherUser.getUsername()));
        
        assertThat(userStateEntity)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.LOGGED_IN);
    }
}