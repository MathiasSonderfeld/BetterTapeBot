package bettertapebot.handler.general;

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
@Import({TestcontainersConfiguration.class, LoginHandler.class})
class LoginHandlerTest {
    
    @Autowired
    LoginHandler loginHandler;
    
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
        assertThat(loginHandler.forCommand()).isEqualTo(Command.LOGIN);
        assertThat(loginHandler.forStates()).containsExactlyInAnyOrder(UserState.LOGIN_VALIDATE_USERNAME, UserState.LOGIN_VALIDATE_PIN);
    }
    
    @Test
    public void alreadyLoggedInUserGetsInfo(){
        long chatId = 1234L;
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
        loginHandler.handleMessage(userStateEntity, userEntity.getUsername());
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("du bist schon eingeloggt als")
            .contains(userEntity.getUsername());
        Mockito.verifyNoInteractions(userRepository);
    }
    
    @Test
    public void logInRequestWithoutNameGetsAskedForUsername(){
        long chatId = 2345L;
        var userEntity = userRepository.save(UserEntity.builder()
            .username("admin")
            .pin("1234")
            .isAdmin(true)
            .build());
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.LOGGED_OUT)
            .owner(userEntity)
            .build());
        
        Mockito.reset(userRepository, responseService);
        loginHandler.handleMessage(userStateEntity, null);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Wie lautet dein Benutzername?");
        
        assertThat(userStateEntity)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.LOGIN_VALIDATE_USERNAME);
        Mockito.verifyNoInteractions(userRepository);
    }
    
    @Test
    public void loggedOutUserWithFittingUsernameGetsAskedForPIN(){
        long chatId = 3456L;
        var userEntity = userRepository.save(UserEntity.builder()
            .username("admin")
            .pin("1234")
            .isAdmin(true)
            .build());
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.LOGGED_OUT)
            .owner(userEntity)
            .build());
        
        Mockito.reset(userRepository, responseService);
        loginHandler.handleMessage(userStateEntity, userEntity.getUsername());
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Wie lautet deine PIN?");
        Mockito.verifyNoInteractions(userRepository);
        
        assertThat(userStateEntity)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.LOGIN_VALIDATE_PIN);
        Mockito.verifyNoInteractions(userRepository);
    }
    
    @Test
    public void newUserWithGivenUnknownUsernameGetsAskedAgain(){
        long chatId = 4567L;
        String admin = "admin";
        var oldUser = userRepository.save(UserEntity.builder()
            .username("oldUser")
            .pin("9876")
            .isAdmin(false)
            .build());
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.LOGGED_OUT)
            .owner(oldUser)
            .build());
        
        Mockito.reset(userRepository, responseService);
        loginHandler.handleMessage(userStateEntity, admin);
        Mockito.verify(userRepository, Mockito.times(1)).findById(admin);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("der angegebene benutzername")
            .contains(admin)
            .contains("ist unbekannt. Probiers nochmal");
        
        assertThat(userStateEntity)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.LOGIN_VALIDATE_USERNAME);
        
        assertThat(userStateEntity)
            .extracting(UserStateEntity::getOwner).isNotNull()
            .extracting(UserEntity::getUsername)
            .isEqualTo(oldUser.getUsername());
    }
    
    @Test
    public void newUserWithGivenUsernameGetsAskedForPIN(){
        long chatId = 5678L;
        var oldUser = userRepository.save(UserEntity.builder()
            .username("oldUser")
            .pin("9876")
            .isAdmin(false)
            .build());
        var userEntity = userRepository.save(UserEntity.builder()
            .username("admin")
            .pin("1234")
            .isAdmin(true)
            .build());
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.LOGGED_OUT)
            .owner(oldUser)
            .build());
        
        Mockito.reset(userRepository, responseService);
        loginHandler.handleMessage(userStateEntity, userEntity.getUsername());
        Mockito.verify(userRepository, Mockito.times(1)).findById(userEntity.getUsername());
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Wie lautet deine PIN?");
        
        assertThat(userStateEntity)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.LOGIN_VALIDATE_PIN);
        
        assertThat(userStateEntity)
            .extracting(UserStateEntity::getOwner).isNotNull()
            .extracting(UserEntity::getUsername)
            .isEqualTo(userEntity.getUsername());
    }
    
    @Test
    public void sendingWrongPINOnRequestGetsAskedToRepeat(){
        long chatId = 6789L;
        var userEntity = userRepository.save(UserEntity.builder()
            .username("admin")
            .pin("1234")
            .isAdmin(true)
            .build());
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.LOGIN_VALIDATE_PIN)
            .owner(userEntity)
            .build());
        
        Mockito.reset(userRepository, responseService);
        loginHandler.handleMessage(userStateEntity, "wrong pin");
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("PIN inkorrekt, versuchs nochmal");
        Mockito.verifyNoInteractions(userRepository);
        
        assertThat(userStateEntity)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.LOGIN_VALIDATE_PIN);
    }
    
    @Test
    public void sendingCorrectPINOnRequestGetsLoggedIn(){
        long chatId = 7890L;
        var userEntity = userRepository.save(UserEntity.builder()
            .username("admin")
            .pin("1234")
            .isAdmin(true)
            .build());
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.LOGIN_VALIDATE_PIN)
            .owner(userEntity)
            .build());
        
        Mockito.reset(userRepository, responseService);
        loginHandler.handleMessage(userStateEntity, userEntity.getPin());
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("du wurdest erfolgreich eingeloggt");
        Mockito.verifyNoInteractions(userRepository);
        
        assertThat(userStateEntity)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.LOGGED_IN);
    }
}