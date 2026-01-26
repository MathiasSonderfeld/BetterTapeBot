package bettertapebot.handler;

import bettertapebot.repository.entity.UserState;
import bettertapebot.testutil.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.telegram.telegrambots.longpolling.starter.TelegramBotInitializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class StateHandlerTest {
    
    @MockitoBean
    TelegramBotInitializer telegramBotInitializer; //disable the telegram api
    
    @Autowired
    List<StateHandler> stateHandlers;
    
    @Test
    @DisplayName("verify that every state, that needs a handler, has exactly one handler")
    void testExactlyOneHandlerPerRelevantState(){
        Map<UserState, List<StateHandler>> stateHandlerMap = new HashMap<>();
        for (StateHandler stateHandler : stateHandlers) {
            for (UserState forState : stateHandler.forStates()) {
                stateHandlerMap.computeIfAbsent(forState, _ -> new ArrayList<>())
                    .add(stateHandler);
            }
        }
        
        HashSet<UserState> relevantKeys = new HashSet<>(Arrays.asList(UserState.values()));
        relevantKeys.remove(UserState.LOGGED_OUT);
        relevantKeys.remove(UserState.LOGGED_IN);
        relevantKeys.remove(UserState.NEW_CHAT);
        
        assertThat(stateHandlerMap).isNotNull()
            .containsOnlyKeys(relevantKeys)
            .allSatisfy((_, handlers) ->
                assertThat(handlers).isNotNull()
                    .hasSize(1));
    }
}