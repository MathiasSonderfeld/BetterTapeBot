package eu.sonderfeld.mathias.bettertapebot.handler.loggedin;

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
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({TestcontainersConfiguration.class, GetAllHandler.class})
class GetAllHandlerTest {
    
    @Autowired
    GetAllHandler getAllHandler;
    
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
        userRepository.deleteAll();
        tapeRepository.deleteAll();
    }
    
    @Test
    public void registersForCorrectCommand(){
        assertThat(getAllHandler.forCommand()).isEqualTo(Command.ALL);
    }
    
    @Test
    public void notLoggedInUserGetsDenied(){
        Long chatId = 1234L;
        getAllHandler.handleCommand(chatId, null);
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
    
    @Test
    public void normalUserGetsListOfTapes(){
        Long chatId = 2345L;
        ZonedDateTime time = ZonedDateTime.of(2025, 9, 1,12,0,0,0, ZoneId.systemDefault());
        String expectedTime = "01.09.25 12:00";
        var requestor = userRepository.save(UserEntity.builder()
            .username("requestor")
            .pin("1234")
            .isAdmin(false)
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
            .userState(UserState.LOGGED_IN)
            .build());
        
        var tape1 = tapeRepository.save(TapeEntity.builder()
            .title("tape 1")
            .star(star)
            .director(director)
            .dateAdded(time.toInstant())
            .build());
        
        var tape2 = tapeRepository.save(TapeEntity.builder()
            .title("tape 2")
            .star(star)
            .director(director)
            .dateAdded(time.toInstant())
            .build());
        
        var tape3 = tapeRepository.save(TapeEntity.builder()
            .title("tape 3")
            .star(star)
            .director(director)
            .dateAdded(time.toInstant())
            .build());
        
        Mockito.reset(userStateRepository, userRepository, tapeRepository);
        getAllHandler.handleCommand(chatId, null);
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        Mockito.verify(tapeRepository, Mockito.times(1)).findAll(ArgumentMatchers.any(Sort.class));
        Mockito.verifyNoInteractions(userRepository);
        
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains(star.getUsername())
            .contains(director.getUsername())
            .contains(tape1.getTitle())
            .contains(tape2.getTitle())
            .contains(tape3.getTitle())
            .contains(expectedTime);
    }
}