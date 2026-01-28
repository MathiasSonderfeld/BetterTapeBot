package bettertapebot.handler.loggedin;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.Command;
import bettertapebot.repository.TapeRepository;
import bettertapebot.repository.UserRepository;
import bettertapebot.repository.UserStateRepository;
import bettertapebot.repository.entity.TapeEntity;
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

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({TestcontainersConfiguration.class, StarringHandler.class})
class StarringHandlerTest {
    
    @Autowired
    StarringHandler starringHandler;
    
    @MockitoSpyBean
    UserRepository userRepository;
    
    @MockitoSpyBean
    TapeRepository tapeRepository;
    
    @MockitoBean
    ResponseService responseService;
    
    @Autowired
    UserStateRepository userStateRepository;
    
    @BeforeEach
    void reset(){
        Mockito.reset(userRepository, tapeRepository, responseService);
    }
    
    @AfterEach
    void cleanUp(){
        userStateRepository.deleteAll();
        tapeRepository.deleteAll();
        userRepository.deleteAll();
    }
    
    @Test
    public void registersForCorrectCommandAndStates(){
        assertThat(starringHandler.forCommand()).isEqualTo(Command.STARRING);
        assertThat(starringHandler.forStates()).containsExactlyInAnyOrder(UserState.STARRING_GET_USERNAME);
    }
    
    @Test
    public void notLoggedInUserGetsDenied(){
        long chatId = 1234L;
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.NEW_CHAT)
            .build());
        
        Mockito.reset(userRepository, tapeRepository, responseService);
        starringHandler.handleMessage(userStateEntity, null);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Nur eingeloggte User können Tapes abfragen");
        Mockito.verifyNoInteractions(userRepository, tapeRepository);
    }
    
    @Test
    public void starringWithoutUsernameGetsAskedForUsername(){
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
        
        Mockito.reset(userRepository, tapeRepository, responseService);
        starringHandler.handleMessage(userStateEntity, null);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Wessen Darbietungen möchtest du begutachten?");
        Mockito.verifyNoInteractions(userRepository, tapeRepository);
        
        assertThat(userStateEntity)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.STARRING_GET_USERNAME);
    }
    
    @Test
    public void starringWithUnknownUsernameGetsAskedAgain() {
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
        
        Mockito.reset(userRepository, tapeRepository, responseService);
        starringHandler.handleMessage(userStateEntity, unknownUsername);
        Mockito.verify(userRepository, Mockito.times(1)).findById(unknownUsername);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Den Benutzer gibt es nicht. Probiers nochmal");
        Mockito.verifyNoInteractions(tapeRepository);
        
        assertThat(userStateEntity)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.STARRING_GET_USERNAME);
    }
    
    @Test
    public void starringWithValidUsernameGetsDelivered(){
        ZonedDateTime time = ZonedDateTime.of(2026, 2, 1,12,0,0,0, ZoneId.systemDefault());
        String expectedTime = "01.02.26 12:00";
        long chatId = 6789L;
        var requestor = userRepository.save(UserEntity.builder()
            .username("requestor")
            .pin("1234")
            .isAdmin(false)
            .build());
        var requestorState = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.LOGGED_IN)
            .owner(requestor)
            .build());
        
        var star = userRepository.save(UserEntity.builder()
            .username("star")
            .pin("9876")
            .isAdmin(false)
            .build());
        var tape1 = tapeRepository.save(TapeEntity.builder()
            .title("tape1")
            .star(star)
            .director(requestor)
            .dateAdded(time.toInstant())
            .build());
        var tape2 = tapeRepository.save(TapeEntity.builder()
            .title("tape2")
            .star(star)
            .director(requestor)
            .dateAdded(time.toInstant())
            .build());
        var tape3 = tapeRepository.save(TapeEntity.builder()
            .title("tape3")
            .star(star)
            .director(requestor)
            .dateAdded(time.toInstant())
            .build());
        var tapeIgnored = tapeRepository.save(TapeEntity.builder()
            .title("tapeIgnored")
            .star(requestor)
            .director(star)
            .dateAdded(time.toInstant())
            .build());
        
        Mockito.reset(userRepository, tapeRepository, responseService);
        starringHandler.handleMessage(requestorState, star.getUsername());
        Mockito.verify(userRepository, Mockito.times(1)).findById(star.getUsername());
        Mockito.verify(tapeRepository, Mockito.times(1)).findAllByStar(star);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains(star.getUsername())
            .contains(expectedTime)
            .contains(tape1.getTitle())
            .contains(tape2.getTitle())
            .contains(tape3.getTitle())
            .doesNotContain(tapeIgnored.getTitle());
        
        assertThat(requestorState)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.LOGGED_IN);
    }

}