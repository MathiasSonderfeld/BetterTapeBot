package eu.sonderfeld.mathias.bettertapebot.handler.admin;

import eu.sonderfeld.mathias.bettertapebot.bot.ResponseService;
import eu.sonderfeld.mathias.bettertapebot.handler.Command;
import eu.sonderfeld.mathias.bettertapebot.repository.TapeRepository;
import eu.sonderfeld.mathias.bettertapebot.repository.UserRepository;
import eu.sonderfeld.mathias.bettertapebot.repository.UserStateRepository;
import eu.sonderfeld.mathias.bettertapebot.repository.entity.TapeEntity;
import eu.sonderfeld.mathias.bettertapebot.repository.entity.UserEntity;
import eu.sonderfeld.mathias.bettertapebot.repository.entity.UserState;
import eu.sonderfeld.mathias.bettertapebot.repository.entity.UserStateEntity;
import eu.sonderfeld.mathias.bettertapebot.testutil.TestcontainersConfiguration;
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

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({TestcontainersConfiguration.class, DeleteTapeHandler.class})
class DeleteTapeHandlerTest {
    
    @Autowired
    DeleteTapeHandler deleteTapeHandler;
    
    @MockitoSpyBean
    UserStateRepository userStateRepository;
    
    @MockitoSpyBean
    UserRepository userRepository;
    
    @MockitoSpyBean
    TapeRepository tapeRepository;
    
    @MockitoBean
    ResponseService responseService;
    
    @BeforeEach
    void reset(){
        Mockito.reset(
            userRepository,
            userStateRepository,
            tapeRepository,
            responseService
        );
    }
    
    @AfterEach
    void cleanUp(){
        userStateRepository.deleteAll();
        userRepository.deleteAll();
        tapeRepository.deleteAll();
    }
    
    @Test
    public void registersForCorrectCommand(){
        assertThat(deleteTapeHandler.forCommand()).isEqualTo(Command.DELETE_TAPE);
    }
    
    @Test
    public void registersForCorrectStates(){
        assertThat(deleteTapeHandler.forStates()).containsExactlyInAnyOrder(UserState.DELETE_TAPE_GET_TAPE_ID);
    }
    
    @Test
    public void notAdminChatGetsDenied(){
        Long chatId = 1234L;
        deleteTapeHandler.handleCommand(chatId, "testmessage");
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Nur Admins können Tapes löschen");
    }
    
    @Test
    public void deleteTapeCommandWithValidUUIDGetsDeleted(){
        Long chatId = 2345L;
        
        var user = userRepository.save(UserEntity.builder()
            .username("username")
            .pin("1234")
            .isAdmin(true)
            .build());
        
        var state = userStateRepository.save(UserStateEntity.builder()
            .userState(UserState.ADMIN)
            .owner(user)
            .chatId(chatId)
            .build());
        
        var tape = tapeRepository.save(TapeEntity.builder()
            .title("test tape 1")
            .star(user)
            .director(user)
            .dateAdded(Instant.now())
            .build());
        
        deleteTapeHandler.handleCommand(chatId, tape.getId().toString());
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        Mockito.verify(tapeRepository, Mockito.times(1)).deleteTapeEntityById(tape.getId());
        
        var tapeOptional = tapeRepository.findById(tape.getId());
        assertThat(tapeOptional).isNotNull().isEmpty();
        
        assertThat(state)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.ADMIN);
    }
    
    @Test
    public void deleteTapeCommandWithValidButUnknownUUIDGetsAskedAgain(){
        Long chatId = 2345L;
        
        var user = userRepository.save(UserEntity.builder()
            .username("username")
            .pin("1234")
            .isAdmin(true)
            .build());
        
        var state = userStateRepository.save(UserStateEntity.builder()
            .userState(UserState.ADMIN)
            .owner(user)
            .chatId(chatId)
            .build());
        
        UUID randomId = UUID.randomUUID();
        
        Mockito.reset(userStateRepository, userRepository, tapeRepository);
        deleteTapeHandler.handleCommand(chatId, randomId.toString());
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        Mockito.verify(tapeRepository, Mockito.times(1)).deleteTapeEntityById(randomId);
        
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Zu der ID konnte ich keinen Eintrag finden, probiers nochmal");
        Mockito.verifyNoInteractions(userRepository);
        
        assertThat(state)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.DELETE_TAPE_GET_TAPE_ID);
    }
    
    @Test
    public void deleteTapeCommandWithInvalidUUIDGetsError(){
        Long chatId = 3456L;
        
        var user = userRepository.save(UserEntity.builder()
            .username("username")
            .pin("1234")
            .isAdmin(true)
            .build());
        
        var state = userStateRepository.save(UserStateEntity.builder()
            .userState(UserState.ADMIN)
            .owner(user)
            .chatId(chatId)
            .build());
        
        deleteTapeHandler.handleCommand(chatId, "invalid");
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        Mockito.verifyNoInteractions(tapeRepository);
        
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Die ID konnte ich nicht parsen, probiers nochmal");
        
        assertThat(state)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.DELETE_TAPE_GET_TAPE_ID);
    }
    
    @Test
    public void deleteTapeCommandWithoutIdGetsAskedForId(){
        Long chatId = 3456L;
        
        var user = userRepository.save(UserEntity.builder()
            .username("username")
            .pin("1234")
            .isAdmin(true)
            .build());
        
        var state = userStateRepository.save(UserStateEntity.builder()
            .userState(UserState.ADMIN)
            .owner(user)
            .chatId(chatId)
            .build());
        
        deleteTapeHandler.handleCommand(chatId, null);
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        Mockito.verifyNoInteractions(tapeRepository);
        
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Wie lautet die Tape-ID?");
        
        assertThat(state)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.DELETE_TAPE_GET_TAPE_ID);
    }
    
    @Test
    public void deleteTapeMessageWithValidButUnknownUUIDGetsAskedAgain(){
        long chatId = 2345L;
        
        var user = userRepository.save(UserEntity.builder()
            .username("username")
            .pin("1234")
            .isAdmin(true)
            .build());
        
        var state = userStateRepository.save(UserStateEntity.builder()
            .userState(UserState.DELETE_TAPE_GET_TAPE_ID)
            .owner(user)
            .chatId(chatId)
            .build());
        
        UUID randomId = UUID.randomUUID();
        
        Mockito.reset(userStateRepository, userRepository, tapeRepository);
        deleteTapeHandler.handleMessage(state, chatId, randomId.toString());
        Mockito.verify(tapeRepository, Mockito.times(1)).deleteTapeEntityById(randomId);
        
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Zu der ID konnte ich keinen Eintrag finden, probiers nochmal");
        Mockito.verifyNoInteractions(userStateRepository, userRepository);
        
        assertThat(state)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.DELETE_TAPE_GET_TAPE_ID);
    }
    
    @Test
    public void deleteTapeMessageWithValidUUIDGetsDeleted(){
        long chatId = 2345L;
        
        var user = userRepository.save(UserEntity.builder()
            .username("username")
            .pin("1234")
            .isAdmin(true)
            .build());
        
        var state = userStateRepository.save(UserStateEntity.builder()
            .userState(UserState.DELETE_TAPE_GET_TAPE_ID)
            .owner(user)
            .chatId(chatId)
            .build());
        
        var tape = tapeRepository.save(TapeEntity.builder()
            .title("test tape 1")
            .star(user)
            .director(user)
            .dateAdded(Instant.now())
            .build());
        
        Mockito.reset(userStateRepository, userRepository, tapeRepository);
        deleteTapeHandler.handleMessage(state, chatId, tape.getId().toString());
        Mockito.verify(tapeRepository, Mockito.times(1)).deleteTapeEntityById(tape.getId());
        
        var tapeOptional = tapeRepository.findById(tape.getId());
        assertThat(tapeOptional).isNotNull().isEmpty();
        
        assertThat(state)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.ADMIN);
        Mockito.verifyNoInteractions(userRepository, userStateRepository, responseService);
    }
    
    @Test
    public void deleteTapeMessageWithInvalidUUIDGetsError(){
        Long chatId = 3456L;
        
        var user = userRepository.save(UserEntity.builder()
            .username("username")
            .pin("1234")
            .isAdmin(true)
            .build());
        
        var state = userStateRepository.save(UserStateEntity.builder()
            .userState(UserState.DELETE_TAPE_GET_TAPE_ID)
            .owner(user)
            .chatId(chatId)
            .build());
        Mockito.reset(userStateRepository, userRepository);
        deleteTapeHandler.handleMessage(state, chatId, "invalid");
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Die ID konnte ich nicht parsen, probiers nochmal");
        
        assertThat(state)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.DELETE_TAPE_GET_TAPE_ID);
        Mockito.verifyNoInteractions(userRepository, userStateRepository, tapeRepository);
    }
    
}