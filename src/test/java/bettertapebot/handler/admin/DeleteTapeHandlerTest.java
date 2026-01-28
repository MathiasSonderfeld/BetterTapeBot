package bettertapebot.handler.admin;

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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({TestcontainersConfiguration.class, DeleteTapeHandler.class})
class DeleteTapeHandlerTest {
    
    @Autowired
    DeleteTapeHandler deleteTapeHandler;
    
    @MockitoSpyBean
    TapeRepository tapeRepository;
    
    @MockitoBean
    ResponseService responseService;
    
    @Autowired
    UserStateRepository userStateRepository;
    
    @Autowired
    UserRepository userRepository;
    
    @AfterEach
    void cleanUp(){
        userStateRepository.deleteAll();
        tapeRepository.deleteAll();
        userRepository.deleteAll();
    }
    
    @Test
    public void registersForCorrectCommandAndStates(){
        assertThat(deleteTapeHandler.forCommand()).isEqualTo(Command.DELETE_TAPE);
        assertThat(deleteTapeHandler.forStates()).containsExactlyInAnyOrder(UserState.DELETE_TAPE_GET_TAPE_ID);
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
        
        deleteTapeHandler.handleMessage(userStateEntity, "testmessage");
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Nur Admins können Tapes löschen");
        Mockito.verifyNoInteractions(tapeRepository);
    }
    
    @Test
    public void deleteTapeWithoutIdGetsAskedForId(){
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
        
        Mockito.reset(tapeRepository, responseService);
        deleteTapeHandler.handleMessage(userStateEntity, null);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Wie lautet die Tape-ID?");
        
        assertThat(userStateEntity)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.DELETE_TAPE_GET_TAPE_ID);
        Mockito.verifyNoInteractions(tapeRepository);
    }
    
    @Test
    public void deleteTapeWithInvalidUuidGetsErrorAndRetry(){
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
        
        Mockito.reset(tapeRepository, responseService);
        deleteTapeHandler.handleMessage(userStateEntity, "invalid");
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Die ID konnte ich nicht parsen, probiers nochmal");
        
        assertThat(userStateEntity)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.DELETE_TAPE_GET_TAPE_ID);
        Mockito.verifyNoInteractions(tapeRepository);
    }
    
    @Test
    public void deleteTapeWithValidUuidGetsDeleted(){
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
        
        var movieStar = userRepository.save(UserEntity.builder()
            .username("username")
            .pin("1234")
            .isAdmin(true)
            .build());
        
        userStateRepository.save(UserStateEntity.builder()
            .userState(UserState.LOGGED_IN)
            .owner(movieStar)
            .chatId(9876L)
            .build());
        
        var tape = tapeRepository.save(TapeEntity.builder()
            .title("test tape 1")
            .star(movieStar)
            .director(movieStar)
            .dateAdded(Instant.now())
            .build());
        
        Mockito.reset(tapeRepository, responseService);
        deleteTapeHandler.handleMessage(userStateEntity, tape.getId().toString());
        Mockito.verify(tapeRepository, Mockito.times(1)).deleteTapeEntityById(tape.getId());
        
        var tapeOptional = tapeRepository.findById(tape.getId());
        assertThat(tapeOptional).isNotNull().isEmpty();
        
        assertThat(userStateEntity)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.LOGGED_IN);
    }
    
    @Test
    public void deleteTapeValidButUnknownUuidGetsAskedAgain(){
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
        
        Mockito.reset(tapeRepository, responseService);
        UUID randomId = UUID.randomUUID();
        deleteTapeHandler.handleMessage(userStateEntity, randomId.toString());
        Mockito.verify(tapeRepository, Mockito.times(1)).deleteTapeEntityById(randomId);
        
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Zu der ID konnte ich keinen Eintrag finden, probiers nochmal");
        
        assertThat(userStateEntity)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.DELETE_TAPE_GET_TAPE_ID);
    }
    
}