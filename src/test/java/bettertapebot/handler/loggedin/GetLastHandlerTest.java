package bettertapebot.handler.loggedin;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.Command;
import bettertapebot.properties.BotProperties;
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
@Import({TestcontainersConfiguration.class, GetLastHandler.class, BotProperties.class})
class GetLastHandlerTest {
    
    @Autowired
    GetLastHandler getLastHandler;
    
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
    public void registersForCorrectCommand(){
        assertThat(getLastHandler.forCommand()).isEqualTo(Command.LAST);
    }
    
    @Test
    public void notLoggedInUserGetsDenied(){
        long chatId = 1234L;
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.NEW_CHAT)
            .build());
        
        Mockito.reset(tapeRepository, responseService);
        getLastHandler.handleMessage(userStateEntity, null);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Nur eingeloggte User können Tapes abfragen");
        Mockito.verifyNoInteractions(tapeRepository);
    }
    
    @Test
    public void loggedInUserGetsSorryIfDatabaseIsEmpty(){
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
        getLastHandler.handleMessage(userStateEntity, null);
        Mockito.verify(tapeRepository, Mockito.times(1)).findTopByOrderByDateAddedDesc();
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Es gibt noch keine Einträge");
    }
    
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testGetsListOfTapes(boolean isAdmin){
        long chatId = 3456L;
        ZonedDateTime time1 = ZonedDateTime.of(2023, 9, 1,12,0,0,0, ZoneId.systemDefault());
        ZonedDateTime time2 = ZonedDateTime.of(2024, 9, 1,12,0,0,0, ZoneId.systemDefault());
        ZonedDateTime time3 = ZonedDateTime.of(2025, 9, 1,12,0,0,0, ZoneId.systemDefault());
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
        
        var requestorStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .owner(requestor)
            .userState(UserState.LOGGED_IN)
            .adminMode(isAdmin)
            .build());
        
        var tape1 = tapeRepository.save(TapeEntity.builder()
            .title("tape 1")
            .star(star)
            .director(director)
            .dateAdded(time1.toInstant())
            .build());
        
        var tape2 = tapeRepository.save(TapeEntity.builder()
            .title("tape 2")
            .star(star)
            .director(director)
            .dateAdded(time2.toInstant())
            .build());
        
        var tape3 = tapeRepository.save(TapeEntity.builder()
            .title("tape 3")
            .star(star)
            .director(director)
            .dateAdded(time3.toInstant())
            .build());
        
        Mockito.reset(tapeRepository, responseService);
        getLastHandler.handleMessage(requestorStateEntity, null);
        Mockito.verify(tapeRepository, Mockito.times(1)).findTopByOrderByDateAddedDesc();
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1);
        var text = texts.getFirst();
        assertThat(text)
            .contains(star.getUsername())
            .contains(director.getUsername())
            .contains(tape3.getTitle())
            .contains(expectedTime)
            .doesNotContain(tape1.getTitle())
            .doesNotContain(tape2.getTitle());
        
        if(isAdmin){
            assertThat(text)
                .contains(tape3.getId().toString())
                .doesNotContain(tape1.getId().toString())
                .doesNotContain(tape2.getId().toString());
        }
    }
}