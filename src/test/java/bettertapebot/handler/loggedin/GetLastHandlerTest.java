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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
@Import({TestcontainersConfiguration.class, GetLastHandler.class})
class GetLastHandlerTest {
    
    @Autowired
    GetLastHandler getLastHandler;
    
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
            userStateRepository,
            userRepository,
            tapeRepository,
            responseService
        );
    }
    
    @AfterEach
    void cleanUp(){
        userStateRepository.deleteAll();
        tapeRepository.deleteAll();
        userRepository.deleteAll();
    }
    
    @Test
    public void registersForCorrectCommand(){
        assertThat(getLastHandler.forCommand()).isEqualTo(Command.LAST);
    }
    
    @Test
    public void notLoggedInUserGetsDenied(){
        Long chatId = 1234L;
        getLastHandler.handleCommand(chatId, null);
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Nur eingeloggte User können Tapes abfragen");
    }
    
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testGetsListOfTapes(boolean isAdmin){
        Long chatId = 2345L;
        ZonedDateTime time1 = ZonedDateTime.of(2025, 9, 1,12,0,0,0, ZoneId.systemDefault());
        ZonedDateTime time2 = ZonedDateTime.of(2024, 9, 1,12,0,0,0, ZoneId.systemDefault());
        ZonedDateTime time3 = ZonedDateTime.of(2023, 9, 1,12,0,0,0, ZoneId.systemDefault());
        String expectedTime = "01.09.25 12:00";
        var requestor = userRepository.save(UserEntity.builder()
            .username("requestor")
            .pin("1234")
            .isAdmin(isAdmin)
            .build());
        
        var star = userRepository.save(UserEntity.builder()
            .username("star")
            .pin("9876")
            .isAdmin(false)
            .build());
        
        var director = userRepository.save(UserEntity.builder()
            .username("director")
            .pin("8765")
            .isAdmin(false)
            .build());
        
        userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .owner(requestor)
            .userState(isAdmin ? UserState.ADMIN : UserState.LOGGED_IN)
            .build());
        
        var tape1 = tapeRepository.save(TapeEntity.builder()
            .title("tape 1")
            .star(star)
            .director(director)
            .dateAdded(time1.toInstant())
            .build());
        
        tapeRepository.save(TapeEntity.builder()
            .title("tape 2")
            .star(star)
            .director(director)
            .dateAdded(time2.toInstant())
            .build());
        
        tapeRepository.save(TapeEntity.builder()
            .title("tape 3")
            .star(star)
            .director(director)
            .dateAdded(time3.toInstant())
            .build());
        
        Mockito.reset(userStateRepository, userRepository, tapeRepository);
        getLastHandler.handleCommand(chatId, null);
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        Mockito.verify(tapeRepository, Mockito.times(1)).findTopByOrderByDateAddedDesc();
        Mockito.verifyNoInteractions(userRepository);
        
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1);
        var text = texts.getFirst();
        assertThat(text)
            .contains(star.getUsername())
            .contains(director.getUsername())
            .contains(tape1.getTitle())
            .contains(expectedTime);
        
        if(isAdmin){
            assertThat(text).contains(tape1.getId().toString());
        }
    }
}