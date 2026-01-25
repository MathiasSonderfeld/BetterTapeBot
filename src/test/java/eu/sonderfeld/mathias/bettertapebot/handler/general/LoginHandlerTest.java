package eu.sonderfeld.mathias.bettertapebot.handler.general;

import eu.sonderfeld.mathias.bettertapebot.bot.ResponseService;
import eu.sonderfeld.mathias.bettertapebot.handler.Command;
import eu.sonderfeld.mathias.bettertapebot.repository.UserRepository;
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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({TestcontainersConfiguration.class, LoginHandler.class})
class LoginHandlerTest {
    
    @Autowired
    LoginHandler loginHandler;
    
    @MockitoSpyBean
    UserStateRepository userStateRepository;
    
    @MockitoSpyBean
    UserRepository userRepository;
    
    @MockitoBean
    ResponseService responseService;
    
    @Test
    public void registersForCorrectCommand(){
        assertThat(loginHandler.forCommand()).isEqualTo(Command.LOGIN);
    }
    
    @Test
    public void registersForCorrectStates(){
        assertThat(loginHandler.forStates()).containsExactlyInAnyOrder(
            UserState.LOGIN_VALIDATE_USERNAME,
            UserState.LOGIN_VALIDATE_PIN
        );
    }
    
    @Test
    public void alreadyLoggedInUserGetsInfo(){
        Long chatId = 1234L;
        String username = "username";
        UserState state = UserState.LOGGED_IN;
        
        UserEntity userEntity = userRepository.save(UserEntity.builder()
            .username(username)
            .pin("1234")
            .build());
        
        userStateRepository.save(UserStateEntity.builder()
            .userState(state)
            .owner(userEntity)
            .chatId(chatId)
            .build());
        
        loginHandler.handleCommand(chatId, username);
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("du bist schon eingeloggt als")
            .contains(username);
    }
    
    @Test
    public void loggedOutUserWithGivenUsernameGetsAskedForPIN(){
        Long chatId = 2345L;
        String username = "username";
        UserState userState = UserState.LOGGED_OUT;
        
        UserEntity userEntity = userRepository.save(UserEntity.builder()
            .username(username)
            .pin("1234")
            .build());
        
        userStateRepository.save(UserStateEntity.builder()
            .userState(userState)
            .owner(userEntity)
            .chatId(chatId)
            .build());
        
        loginHandler.handleCommand(chatId, username);
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Wie lautet deine PIN?");
        
        var updatedState = userStateRepository.findById(chatId);
        assertThat(updatedState).isNotEmpty().get()
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.LOGIN_VALIDATE_PIN);
        
        assertThat(updatedState).isNotEmpty().get()
            .extracting(UserStateEntity::getOwner).isNotNull()
            .extracting(UserEntity::getUsername)
            .isEqualTo(username);
    }
    
    @Test
    public void newUserWithGivenUsernameGetsAskedForPIN(){
        Long chatId = 3456L;
        String username = "username";
        UserState userState = UserState.LOGGED_OUT;
        
        UserEntity oldEntity = userRepository.save(UserEntity.builder()
            .username("oldname")
            .pin("1234")
            .build());
        
        userRepository.save(UserEntity.builder()
            .username(username)
            .pin("1234")
            .build());
        
        userStateRepository.save(UserStateEntity.builder()
            .userState(userState)
            .owner(oldEntity)
            .chatId(chatId)
            .build());
        
        loginHandler.handleCommand(chatId, username);
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Wie lautet deine PIN?");
        
        var updatedState = userStateRepository.findById(chatId);
        assertThat(updatedState).isNotEmpty().get()
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.LOGIN_VALIDATE_PIN);
        
        assertThat(updatedState).isNotEmpty().get()
            .extracting(UserStateEntity::getOwner).isNotNull()
            .extracting(UserEntity::getUsername)
            .isEqualTo(username);
    }
    
    @Test
    public void loggedOutUserGetsAskedForUsername(){
        Long chatId = 4567L;
        String username = "username";
        UserState userState = UserState.LOGGED_OUT;
        
        UserEntity userEntity = userRepository.save(UserEntity.builder()
            .username(username)
            .pin("1234")
            .build());
        
        userStateRepository.save(UserStateEntity.builder()
            .userState(userState)
            .owner(userEntity)
            .chatId(chatId)
            .build());
        
        loginHandler.handleCommand(chatId, null);
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Wie lautet dein Benutzername?");
        
        var updatedState = userStateRepository.findById(chatId);
        assertThat(updatedState).isNotEmpty().get()
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.LOGIN_VALIDATE_USERNAME);
        
        assertThat(updatedState).isNotEmpty().get()
            .extracting(UserStateEntity::getOwner).isNotNull()
            .extracting(UserEntity::getUsername)
            .isEqualTo(username);
    }
    
    @Test
    public void sendingUsernameOnRequestGetsAskedForPIN(){
        Long chatId = 5678L;
        String username = "username";
        UserState userState = UserState.LOGIN_VALIDATE_USERNAME;
        
        UserEntity userEntity = userRepository.save(UserEntity.builder()
            .username(username)
            .pin("1234")
            .build());
        
        userStateRepository.save(UserStateEntity.builder()
            .userState(userState)
            .owner(userEntity)
            .chatId(chatId)
            .build());
        
        loginHandler.handleMessage(chatId, username);
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Wie lautet deine PIN?");
        
        var updatedState = userStateRepository.findById(chatId);
        assertThat(updatedState).isNotEmpty().get()
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.LOGIN_VALIDATE_PIN);
        
        assertThat(updatedState).isNotEmpty().get()
            .extracting(UserStateEntity::getOwner).isNotNull()
            .extracting(UserEntity::getUsername)
            .isEqualTo(username);
    }
    
    @Test
    public void sendingNewUsernameOnRequestGetsAskedForPIN(){
        Long chatId = 5678L;
        String username = "username";
        UserState userState = UserState.LOGIN_VALIDATE_USERNAME;
        
        UserEntity oldEntity = userRepository.save(UserEntity.builder()
            .username("old name")
            .pin("1234")
            .build());
        
        userRepository.save(UserEntity.builder()
            .username(username)
            .pin("1234")
            .build());
        
        userStateRepository.save(UserStateEntity.builder()
            .userState(userState)
            .owner(oldEntity)
            .chatId(chatId)
            .build());
        
        loginHandler.handleMessage(chatId, username);
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Wie lautet deine PIN?");
        
        var updatedState = userStateRepository.findById(chatId);
        assertThat(updatedState).isNotEmpty().get()
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.LOGIN_VALIDATE_PIN);
        
        assertThat(updatedState).isNotEmpty().get()
            .extracting(UserStateEntity::getOwner).isNotNull()
            .extracting(UserEntity::getUsername)
            .isEqualTo(username);
    }
    
    @Test
    public void sendingWrongPINOnRequestGetsAskedToRepeat(){
        Long chatId = 6789L;
        String username = "username";
        String pin = "1234";
        UserState userState = UserState.LOGIN_VALIDATE_PIN;
        
        UserEntity userEntity = userRepository.save(UserEntity.builder()
            .username(username)
            .pin(pin)
            .build());
        
        userStateRepository.save(UserStateEntity.builder()
            .userState(userState)
            .owner(userEntity)
            .chatId(chatId)
            .build());
        
        loginHandler.handleMessage(chatId, "wrong pin");
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("PIN inkorrekt, versuchs nochmal");
        
        var updatedState = userStateRepository.findById(chatId);
        assertThat(updatedState).isNotEmpty().get()
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.LOGIN_VALIDATE_PIN);
    }
    
    @Test
    public void sendingCorrectPINOnRequestGetsLoggedIn(){
        Long chatId = 6789L;
        String username = "username";
        String pin = "1234";
        UserState userState = UserState.LOGIN_VALIDATE_PIN;
        
        UserEntity userEntity = userRepository.save(UserEntity.builder()
            .username(username)
            .pin(pin)
            .build());
        
        userStateRepository.save(UserStateEntity.builder()
            .userState(userState)
            .owner(userEntity)
            .chatId(chatId)
            .build());
        
        loginHandler.handleMessage(chatId, pin);
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("du wurdest erfolgreich eingeloggt");
        
        var updatedState = userStateRepository.findById(chatId);
        assertThat(updatedState).isNotEmpty().get()
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.LOGGED_IN);
    }
}