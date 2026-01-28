package bettertapebot.handler.general;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.Command;
import bettertapebot.repository.UserStateRepository;
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

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({TestcontainersConfiguration.class, GetHelpHandler.class})
class GetHelpHandlerTest {

    @Autowired
    GetHelpHandler getHelpHandler;

    @MockitoBean
    ResponseService responseService;
    
    @Autowired
    UserStateRepository userStateRepository;
    
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
        long chatId = 1234L;
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.NEW_CHAT)
            .build());
        
        Mockito.reset(responseService);
        getHelpHandler.handleMessage(userStateEntity, "testmessage");
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
        long chatId = 2345L;
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.LOGGED_IN)
            .build());
        
        Mockito.reset(responseService);
        getHelpHandler.handleMessage(userStateEntity, "testmessage");
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
        long chatId = 3456L;
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.LOGGED_IN)
            .adminMode(true)
            .build());
        
        Mockito.reset(responseService);
        getHelpHandler.handleMessage(userStateEntity, "testmessage");
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