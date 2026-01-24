package eu.sonderfeld.mathias.bettertapebot.bot;

import eu.sonderfeld.mathias.bettertapebot.properties.BotProperties;
import eu.sonderfeld.mathias.bettertapebot.util.TextSplitter;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;

@CustomLog
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ResponseService {

    TelegramClient telegramClient;
    BotProperties botProperties;

    public void send(long chatId, String text) {
        send(chatId, text, null);
    }

    public void send(long chatId, String text, ReplyKeyboard markup) {
        List<String> chunks = TextSplitter.splitTextSmart(text, botProperties.getTelegram().getMessageLengthLimit());

        //send messages async
        Flux.fromIterable(chunks)
            .index()
            .concatMap(tuple -> {
                long index = tuple.getT1();
                String chunk = tuple.getT2();

                return Mono.fromRunnable(() -> sendChunk(chatId, chunk, index == 0 ? markup : null))
                    .subscribeOn(Schedulers.boundedElastic())
                    .then(index < chunks.size() - 1 ? Mono.delay(Duration.ofMillis(900)) : Mono.empty());  // Kein delay nach letztem Chunk
            })
            .subscribe();
    }

    private void sendChunk(long chatId, String chunk, ReplyKeyboard markup) {
        SendMessage message = SendMessage
            .builder()
            .chatId(chatId)
            .text(chunk)
            .build();

        if(markup != null){
            message.setReplyMarkup(markup);
        }

        try {
            telegramClient.execute(message);
            if(log.isDebugEnabled()){
                log.debug("Message sent to ChatId '{}': {}", chatId, chunk);
            }
        } catch (TelegramApiException e) {
            log.error("Error sending message chunk to chatId '{}': {}", chatId, chunk, e);
        }
    }


}