package eu.sonderfeld.mathias.bettertapebot.handler.general;

import eu.sonderfeld.mathias.bettertapebot.bot.ResponseService;
import eu.sonderfeld.mathias.bettertapebot.handler.Command;
import eu.sonderfeld.mathias.bettertapebot.repository.UserStateRepository;
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

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({TestcontainersConfiguration.class, GetHelpHandler.class})
class GetHelpHandlerTest {

    @Autowired
    GetHelpHandler getHelpHandler;

    @MockitoSpyBean
    UserStateRepository userStateRepository;

    @MockitoBean
    ResponseService responseService;
    
    @BeforeEach
    void reset(){
        Mockito.reset(
            userStateRepository,
            responseService
        );
    }
    
    @AfterEach
    void cleanUp(){
        userStateRepository.deleteAll();
    }

    @Test
    public void registersForCorrectCommand(){
        assertThat(getHelpHandler.forCommand()).isEqualTo(Command.HELP);
    }

    @Test
    public void unknownUsersGetGeneralHelpOnly(){
        Long chatId = 1234L;
        getHelpHandler.handleCommand(chatId, "testmessage");
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);

        var expected = Arrays.stream(Command.values())
            .filter(c -> c.getCommandLevel() == Command.CommandLevel.GENERAL)
            .map(Command::getFormattedHelpText)
            .collect(Collectors.toSet());

        var expectedMissing = Arrays.stream(Command.values())
            .filter(c -> c.getCommandLevel() != Command.CommandLevel.GENERAL)
            .map(Command::getFormattedHelpText)
            .collect(Collectors.toSet());

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains(expected)
            .doesNotContain(expectedMissing);
    }

    @Test
    public void loggedInUsersGetGeneralAndUserHelp(){
        Long chatId = 2345L;

        userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.LOGGED_IN)
            .build());
        
        getHelpHandler.handleCommand(chatId, "testmessage");
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);

        var expected = Arrays.stream(Command.values())
            .filter(c -> c.getCommandLevel() != Command.CommandLevel.ADMIN)
            .map(Command::getFormattedHelpText)
            .collect(Collectors.toSet());

        var expectedMissing = Arrays.stream(Command.values())
            .filter(c -> c.getCommandLevel() == Command.CommandLevel.ADMIN)
            .map(Command::getFormattedHelpText)
            .collect(Collectors.toSet());

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains(expected)
            .doesNotContain(expectedMissing);
    }
    
    @Test
    public void adminGetGeneralAll(){
        Long chatId = 3456L;
        
        userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.ADMIN)
            .build());
        
        getHelpHandler.handleCommand(chatId, "testmessage");
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);
        
        var expected = Arrays.stream(Command.values())
            .map(Command::getFormattedHelpText)
            .collect(Collectors.toSet());
        
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains(expected);
    }

}