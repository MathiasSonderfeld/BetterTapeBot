package bettertapebot.handler;

import bettertapebot.testutil.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.telegram.telegrambots.longpolling.starter.TelegramBotInitializer;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CommandHandlerTest {
    
    @MockitoBean
    TelegramBotInitializer telegramBotInitializer; //disable the telegram api

    @Autowired
    List<CommandHandler> commandHandlers;
    
    @Test
    @DisplayName("verify that every command has exactly one handler")
    void testExactlyOneHandlerPerCommand(){
        var map = commandHandlers.stream()
            .collect(Collectors.groupingBy(CommandHandler::forCommand));

        assertThat(map).isNotNull()
            .containsKeys(Command.values())
            .allSatisfy((_, handlers) ->
                assertThat(handlers).isNotNull()
                .hasSize(1));
    }
}