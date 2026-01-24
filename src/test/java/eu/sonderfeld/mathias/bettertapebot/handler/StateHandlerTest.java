package eu.sonderfeld.mathias.bettertapebot.handler;

import eu.sonderfeld.mathias.bettertapebot.bot.ResponseService;
import eu.sonderfeld.mathias.bettertapebot.handler.general.LoginHandler;
import eu.sonderfeld.mathias.bettertapebot.properties.BotProperties;
import eu.sonderfeld.mathias.bettertapebot.repository.UserRepository;
import eu.sonderfeld.mathias.bettertapebot.repository.UserStateRepository;
import eu.sonderfeld.mathias.bettertapebot.repository.entity.UserState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(classes = {
    LoginHandler.class
})
class StateHandlerTest {
    
    @Autowired
    List<StateHandler> stateHandlers;
    
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
        var stateHandlerMap = stateHandlers.stream()
            .flatMap(h -> h.forStates().stream().map(s -> Map.entry(s, h)))
            .collect(Collectors.groupingBy(Map.Entry::getKey,
                Collectors.mapping(Map.Entry::getValue,
                    Collectors.toCollection(ArrayList::new))));
            
        
        assertThat(stateHandlerMap).isNotNull()
            .hasSize(UserState.values().length) //check that every command has a registered handler
            .allSatisfy((_, handlers) ->
                assertThat(handlers).isNotNull()
                    .hasSize(1));
    }
}