package eu.sonderfeld.mathias.bettertapebot.commandhandler;

import eu.sonderfeld.mathias.bettertapebot.bot.ResponseService;
import eu.sonderfeld.mathias.bettertapebot.commandhandler.admin.BecomeAdminHandler;
import eu.sonderfeld.mathias.bettertapebot.commandhandler.general.GetDsgvoHandler;
import eu.sonderfeld.mathias.bettertapebot.commandhandler.general.GetHelpHandler;
import eu.sonderfeld.mathias.bettertapebot.commandhandler.general.GetMeHandler;
import eu.sonderfeld.mathias.bettertapebot.commandhandler.general.ResetStateHandler;
import eu.sonderfeld.mathias.bettertapebot.commandhandler.loggedin.GetActivationCodeHandler;
import eu.sonderfeld.mathias.bettertapebot.commandhandler.loggedin.GetAllUsersHandler;
import eu.sonderfeld.mathias.bettertapebot.properties.BotProperties;
import eu.sonderfeld.mathias.bettertapebot.repository.UserRepository;
import eu.sonderfeld.mathias.bettertapebot.repository.UserStateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(classes = {
    BecomeAdminHandler.class,
    GetDsgvoHandler.class,
    GetHelpHandler.class,
    GetMeHandler.class,
    ResetStateHandler.class,
    GetActivationCodeHandler.class,
    GetAllUsersHandler.class
})
class CommandHandlerTest {

    @Autowired
    List<CommandHandler> commandHandlers;
    
    @MockitoBean
    BotProperties botProperties;
    
    @MockitoBean
    UserStateRepository userStateRepository;
    
    @MockitoBean
    UserRepository userRepository;
    
    @MockitoBean
    ResponseService responseService;
    
    @Test
    @DisplayName("verify that there are no two handlers for the same command")
    void testUniqueCommand(){
        var map = commandHandlers.stream()
            .collect(Collectors.groupingBy(CommandHandler::forCommand));

        assertThat(map).isNotNull()
            .hasSize(Command.values().length) //check that every command has a registered handler
            .allSatisfy((_, handlers) ->
                assertThat(handlers).isNotNull()
                .hasSize(1));
    }
}