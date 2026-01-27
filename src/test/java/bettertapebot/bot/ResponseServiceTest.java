package bettertapebot.bot;

import bettertapebot.properties.BotProperties;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig
class ResponseServiceTest {

    BotProperties botProperties;
    TelegramClient telegramClient;
    ResponseService responseService;

    @BeforeEach
    void createClientAndResponseService(){
        botProperties = new BotProperties();
        botProperties.setTelegram(new BotProperties.TelegramProperties());
        botProperties.getTelegram().setMessageLengthLimit(4096);
        botProperties.getTelegram().setDelayBetweenMessagesMs(100);
        telegramClient = Mockito.mock(TelegramClient.class);
        responseService = new ResponseService(telegramClient, botProperties);
        responseService.postConstruct();
    }

    @Test
    @SneakyThrows
    void testSendingWorks() {
        responseService.send(1, "testmessage");
        Awaitility.await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
            Mockito.verify(telegramClient, Mockito.times(1)).execute(captor.capture());

            var allMessages = captor.getAllValues();
            assertThat(allMessages).isNotNull()
                .hasSize(1)
                .element(0)
                .satisfies(sendMessage -> {
                    assertThat(sendMessage).isNotNull();
                    assertThat(sendMessage.getChatId()).isEqualTo("1");
                    assertThat(sendMessage.getText()).isEqualTo("testmessage");
                    assertThat(sendMessage.getReplyMarkup()).isNotNull()
                        .isEqualTo(new ReplyKeyboardRemove(true));
                });
        });
    }

    @Test
    @SneakyThrows
    void testSendingLongMessageGetsSplit() {
        botProperties.getTelegram().setMessageLengthLimit(11);
        responseService.send(1, "testmessageverylong");
        Awaitility.await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
            Mockito.verify(telegramClient, Mockito.times(2)).execute(captor.capture());

            var allMessages = captor.getAllValues();
            assertThat(allMessages).isNotNull()
                .hasSize(2);

            var firstMessage = allMessages.getFirst();
            assertThat(firstMessage).isNotNull();
            assertThat(firstMessage.getChatId()).isEqualTo("1");
            assertThat(firstMessage.getText()).isEqualTo("testmessage");
            assertThat(firstMessage.getReplyMarkup()).isNotNull()
                .isEqualTo(new ReplyKeyboardRemove(true));

            var secondMessage = allMessages.get(1);
            assertThat(secondMessage).isNotNull();
            assertThat(secondMessage.getChatId()).isEqualTo("1");
            assertThat(secondMessage.getText()).isEqualTo("verylong");
            assertThat(secondMessage.getReplyMarkup()).isNull();
        });
    }

    @Test
    @SneakyThrows
    void testSplitTextArrivesInCorrectOrder() {
        String input = "testmessage_verylong";
        botProperties.getTelegram().setMessageLengthLimit(2);
        responseService.send(1, input);
        Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
            Mockito.verify(telegramClient, Mockito.times(10)).execute(captor.capture());

            var allMessages = captor.getAllValues();
            assertThat(allMessages).isNotNull()
                .hasSize(10);
            var allTexts = allMessages.stream()
                .map(SendMessage::getText)
                .toList();

            var joined = String.join("", allTexts);
            assertThat(joined).isEqualTo(input);
        });
    }

    @Test
    @SneakyThrows
    void testExplicitKeyboardGetsOnlyAddedToFirstMessage() {
        botProperties.getTelegram().setMessageLengthLimit(11);
        responseService.send(1, new ReplyKeyboardMarkup(List.of()), "testmessageverylong");
        Awaitility.await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
            Mockito.verify(telegramClient, Mockito.times(2)).execute(captor.capture());

            var allMessages = captor.getAllValues();
            assertThat(allMessages).isNotNull()
                .hasSize(2);

            var firstMessage = allMessages.getFirst();
            assertThat(firstMessage).isNotNull();
            assertThat(firstMessage.getChatId()).isEqualTo("1");
            assertThat(firstMessage.getText()).isEqualTo("testmessage");
            assertThat(firstMessage.getReplyMarkup()).isNotNull();

            var secondMessage = allMessages.get(1);
            assertThat(secondMessage).isNotNull();
            assertThat(secondMessage.getChatId()).isEqualTo("1");
            assertThat(secondMessage.getText()).isEqualTo("verylong");
            assertThat(secondMessage.getReplyMarkup()).isNull();
        });
    }
    @Test
    @SneakyThrows
    void testCanHandleNullKeyboards() {
        botProperties.getTelegram().setMessageLengthLimit(11);
        responseService.send(1, null, "testmessage");
        Awaitility.await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
            Mockito.verify(telegramClient, Mockito.times(1)).execute(captor.capture());
            
            var allMessages = captor.getAllValues();
            assertThat(allMessages).isNotNull()
                .hasSize(1)
                .element(0)
                .satisfies(sendMessage -> {
                    assertThat(sendMessage).isNotNull();
                    assertThat(sendMessage.getChatId()).isEqualTo("1");
                    assertThat(sendMessage.getText()).isEqualTo("testmessage");
                    assertThat(sendMessage.getReplyMarkup()).isNull();
                });
        });
    }

    @Test
    @SneakyThrows
    void testMessagesGetSentWithDelay() {
        String input = "x".repeat(20);
        botProperties.getTelegram().setMessageLengthLimit(5);

        List<Instant> callTimes = new CopyOnWriteArrayList<>();
        Mockito.when(telegramClient.execute(ArgumentMatchers.any(SendMessage.class)))
            .thenAnswer(_ -> {
                callTimes.add(Instant.now());
                return null;
            });

        responseService.send(1, input);
        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
            Mockito.verify(telegramClient, Mockito.times(4)).execute(captor.capture());
        });

        for (int i = 1; i < callTimes.size(); i++) {
            Duration gap = Duration.between(callTimes.get(i-1), callTimes.get(i));
            assertThat(gap.toMillis()).isGreaterThanOrEqualTo(100L);
        }
    }
    
    @Test
    @SneakyThrows
    void testBroadcastsGetSentWithDelayOnlyPerChat() {
        String input = "x".repeat(20);
        botProperties.getTelegram().setMessageLengthLimit(5);
        
        ConcurrentHashMap<String, CopyOnWriteArrayList<Instant>> map = new ConcurrentHashMap<>();
        Mockito.when(telegramClient.execute(ArgumentMatchers.any(SendMessage.class)))
            .thenAnswer(invocation -> {
                var chatId = invocation.getArgument(0, SendMessage.class).getChatId();
                map.computeIfAbsent(chatId, _ -> new CopyOnWriteArrayList<>())
                    .add(Instant.now());
                return null;
            });
        
        responseService.broadcast(List.of(1L,2L,3L), input);
        Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
            Mockito.verify(telegramClient, Mockito.times(12)).execute(captor.capture());
        });
        
        for (var entry : map.entrySet()) {
            var list = entry.getValue();
            for (int i = 1; i < list.size(); i++) {
                Duration gap = Duration.between(list.get(i-1), list.get(i));
                assertThat(gap.toMillis()).isGreaterThanOrEqualTo(100L);
            }
        }
        
        // irgendeine Liste nehmen, um die Länge zu bestimmen
        int size = map.values().iterator().next().size();
        
        for (int i = 0; i < size; i++) {
            ArrayList<Instant> timestamps = new ArrayList<>();
            
            for (CopyOnWriteArrayList<Instant> list : map.values()) {
                timestamps.add(list.get(i));
            }
            
            for (int j = 1; j < timestamps.size(); j++) {
                Duration gap = Duration.between(timestamps.get(j-1), timestamps.get(j));
                assertThat(gap.toMillis()).isLessThanOrEqualTo(30L);
            }
        }
        
    }
}