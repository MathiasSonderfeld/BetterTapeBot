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
@Import({TestcontainersConfiguration.class, NewAdminHandler.class})
class NewAdminHandlerTest {
    
    @Autowired
    NewAdminHandler newAdminHandler;
    
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
        assertThat(newAdminHandler.forCommand()).isEqualTo(Command.NEW_ADMIN);
        assertThat(newAdminHandler.forStates()).containsExactlyInAnyOrder(UserState.NEW_ADMIN_USER_GET_USERNAME);
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
        newAdminHandler.handleMessage(userStateEntity, chatId, "testmessage");
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Nur Admins dürfen neue Admins festlegen");
        Mockito.verifyNoInteractions(userRepository);
    }
    
    @Test
    public void newAdminwithoutUsernameGetsAskedForUsername(){
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
        newAdminHandler.handleMessage(userStateEntity, chatId, null);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Wer soll denn der neue Admin werden?");
        Mockito.verifyNoInteractions(userRepository);
        
        assertThat(userStateEntity)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.NEW_ADMIN_USER_GET_USERNAME);
    }
    
    @Test
    public void newAdminCommandWithSelfGetsAborted(){
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
        
        Mockito.reset(userRepository, responseService);
        newAdminHandler.handleMessage(userStateEntity, chatId, userEntity.getUsername());
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Du bist schon Admin");
        Mockito.verifyNoInteractions(userRepository);
        
        assertThat(userStateEntity)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.LOGGED_IN);
    }
    
    @Test
    public void newAdminWithUnknownUsernameGetsAskedAgain() {
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
        newAdminHandler.handleMessage(userStateEntity, chatId, unknownUsername);
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
            .isEqualTo(UserState.NEW_ADMIN_USER_GET_USERNAME);
    }
    
    @Test
    public void newAdminCommandWithKnownAdminGetsAborted(){
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
            .isAdmin(true)
            .build());
        
        Mockito.reset(userRepository, responseService);
        newAdminHandler.handleMessage(userStateEntity, chatId, otherAdmin.getUsername());
        Mockito.verify(userRepository, Mockito.times(1)).findById(otherAdmin.getUsername());
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains(otherAdmin.getUsername())
            .contains("ist schon Admin");
        
        assertThat(userStateEntity)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.LOGGED_IN);
    }
    
    @Test
    public void newAdminCommandWithKnownUserGetsPromoted(){
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
            .isAdmin(false)
            .build());
        
        Mockito.reset(userRepository, responseService);
        newAdminHandler.handleMessage(userStateEntity, chatId, otherUser.getUsername());
        Mockito.verify(userRepository, Mockito.times(1)).findById(otherUser.getUsername());
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains(otherUser.getUsername())
            .contains("ist nun Admin");
        
        assertThat(otherUser).extracting(UserEntity::getIsAdmin).isEqualTo(true);
        
        assertThat(userStateEntity)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.LOGGED_IN);
    }
}