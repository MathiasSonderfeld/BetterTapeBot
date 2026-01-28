package bettertapebot.bot;

import bettertapebot.properties.BotProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig
@TestInstance(value = TestInstance.Lifecycle.PER_CLASS)
class ResponseServiceTest {
    private static final int TEST_MESSAGE_LENGTH = 10;
    
    
    BotProperties botProperties;
    AsyncTelegramClient asyncTelegramClient;
    ResponseService responseService;
    
    @BeforeAll
    void setup(){
        botProperties = new BotProperties();
        botProperties.getTelegram().setMessageLengthLimit(TEST_MESSAGE_LENGTH);
        asyncTelegramClient = Mockito.mock(AsyncTelegramClient.class);
        responseService = new ResponseService(botProperties, asyncTelegramClient);
    }
    
    @AfterEach
    void reset() {
        Mockito.reset(asyncTelegramClient);
    }
    
    @Test
    void testSendMessageButOnlyFirstContainsKeyboard(){
        long chatId = 1;
        int messageLength = 20;
        String message = "m".repeat(messageLength);
        responseService.send(chatId, message);
        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        Mockito.verify(asyncTelegramClient, Mockito.times(2)).sendMessage(ArgumentMatchers.eq(chatId), messageCaptor.capture());
        var sendMessages = messageCaptor.getAllValues();
        assertThat(sendMessages).hasSize(2);
        
        var firstMessage = sendMessages.getFirst();
        assertThat(firstMessage.getChatId()).isEqualTo(Long.toString(chatId));
        assertThat(firstMessage.getText()).isEqualTo("m".repeat(TEST_MESSAGE_LENGTH));
        assertThat(firstMessage.getReplyMarkup()).isEqualTo(new ReplyKeyboardRemove(true));
        assertThat(firstMessage.getParseMode()).isEqualTo("HTML");
        
        var secondMessage = sendMessages.get(1);
        assertThat(secondMessage.getChatId()).isEqualTo(Long.toString(chatId));
        assertThat(secondMessage.getText()).isEqualTo("m".repeat(messageLength - TEST_MESSAGE_LENGTH));
        assertThat(secondMessage.getReplyMarkup()).isNull();
        assertThat(secondMessage.getParseMode()).isEqualTo("HTML");
    }
    
    @Test
    void testBroadcasting(){
        var chatIds = List.of(10L, 11L, 12L);
        String message = "m".repeat(2 * TEST_MESSAGE_LENGTH);
        
        responseService.broadcast(chatIds, message);
        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        Mockito.verify(asyncTelegramClient, Mockito.times(6)).sendMessage(ArgumentMatchers.anyLong(), messageCaptor.capture());
        var sendMessages = messageCaptor.getAllValues();
        assertThat(sendMessages).hasSize(6);
        var idMessageMap = sendMessages.stream().collect(Collectors.groupingBy(SendMessage::getChatId));
        assertThat(idMessageMap).allSatisfy((_, list) ->
            assertThat(list).hasSize(2)
                .allSatisfy(sm -> {
                    assertThat(sm.getChatId()).asLong().isIn(chatIds);
                    assertThat(sm.getText()).isEqualTo("m".repeat(TEST_MESSAGE_LENGTH));
                    assertThat(sm.getParseMode()).isEqualTo("HTML");
                    assertThat(sm.getReplyMarkup()).isNull();
                }));
    }
}