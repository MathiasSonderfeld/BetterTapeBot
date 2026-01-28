package bettertapebot.bot;

import bettertapebot.properties.BotProperties;
import lombok.SneakyThrows;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.ApiResponse;
import org.telegram.telegrambots.meta.api.objects.ResponseParameters;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig
@TestInstance(value = TestInstance.Lifecycle.PER_CLASS)
class AsyncTelegramClientTest {
    private static final int TEST_DELAY = 100;
    private static final int TEST_RETRY = 3;

    BotProperties botProperties;
    TelegramClient telegramClient;
    AsyncTelegramClient asyncTelegramClient;
    
    
    @BeforeAll
    void setup() {
        botProperties = new BotProperties();
        botProperties.getTelegram().setDelayBetweenMessagesForSameChat(Duration.ofMillis(TEST_DELAY));
        botProperties.getTelegram().setRetryCountInCaseOfTooManyRequests(TEST_RETRY);
        telegramClient = Mockito.mock(TelegramClient.class);
        asyncTelegramClient = new AsyncTelegramClient(telegramClient, botProperties);
    }
    
    @Test
    @SneakyThrows
    void testMessageOrderIsKept(){
        botProperties.getTelegram().setDelayBetweenMessagesForSameChat(Duration.ofMillis(1));
        Mockito.when(telegramClient.execute(ArgumentMatchers.any(SendMessage.class))).thenReturn(new Message());
        int count = 1000;
        for (int i = 0; i < count; i++) {
            final int finalI = i;
            Assertions.assertDoesNotThrow(() -> asyncTelegramClient.sendMessage(1, new SendMessage("1", "" + finalI)));
        }
        
        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        //will time out if botProperties change is not taken over in AsyncTelegramClient
        Awaitility.await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
            Mockito.verify(telegramClient, Mockito.times(count)).execute(messageCaptor.capture()));
        
        var allMessages = messageCaptor.getAllValues();
        assertThat(allMessages).hasSize(count);
        
        for (int i = 1; i < count; i++) {
            var before = Integer.parseInt(allMessages.get(i-1).getText());
            var current = Integer.parseInt(allMessages.get(i).getText());
            assertThat(current - before).isEqualTo(1);
        }
    }
    
    @Test
    @SneakyThrows
    void testRetriesError(){
        ApiResponse<Object> apiResponse = new ApiResponse<>(Boolean.FALSE, 429, "TooManyRequests", new ResponseParameters(0L, 10), new Object());
        TelegramApiRequestException expected = new TelegramApiRequestException("expected", apiResponse);
        Mockito.when(telegramClient.execute(ArgumentMatchers.any(SendMessage.class))).thenThrow(expected);
        Assertions.assertDoesNotThrow(() -> asyncTelegramClient.sendMessage(1, new SendMessage("1", "")));
        Awaitility.await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
            Mockito.verify(telegramClient, Mockito.times(TEST_RETRY)).execute(ArgumentMatchers.any(SendMessage.class)));
    }
    
    @Test
    @SneakyThrows
    void testSendMessageForSameIdGetsDelayedWhileOtherChatsDont() {
        ConcurrentHashMap<String, List<Instant>> timestamps = new ConcurrentHashMap<>();
        Mockito.when(telegramClient.execute(ArgumentMatchers.any(SendMessage.class))).thenAnswer(i -> {
            SendMessage sm = i.getArgument(0);
            timestamps.computeIfAbsent(sm.getChatId(), _ -> new CopyOnWriteArrayList<>())
                .add(Instant.now());
            return new Message();
        });
        
        int size = 3;
        
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                final long finalJ = j; //java demands values in lambdas to be (effectively) final, as j isnt we need to copy it
                Assertions.assertDoesNotThrow(() -> asyncTelegramClient.sendMessage(finalJ, new SendMessage("" + finalJ, "")));
            }
        }
        
        Awaitility.await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
            Mockito.verify(telegramClient, Mockito.times(size*size)).execute(ArgumentMatchers.any(SendMessage.class)));
        
        //check that within each map, all entries are at least TEST_DELAY ms apart
        for (Map.Entry<String, List<Instant>> entry : timestamps.entrySet()) {
            List<Instant> instants = entry.getValue();
            for (int i = 1; i < instants.size(); i++) {
                var diff = Duration.between(instants.get(i - 1), instants.get(i)).toMillis();
                assertThat(diff).isGreaterThanOrEqualTo(TEST_DELAY);
            }
        }
        
        for (int i = 0; i < size; i++) {
            int finalI = i;
            Instant min = timestamps.values().stream().map(l -> l.get(finalI)).min(Instant::compareTo).orElseThrow();
            Instant max = timestamps.values().stream().map(l -> l.get(finalI)).max(Instant::compareTo).orElseThrow();
            long diff = Duration.between(min, max).toMillis();
            assertThat(diff).isLessThanOrEqualTo(TEST_DELAY / 2);
        }
    }
}