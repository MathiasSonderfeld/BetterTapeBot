package eu.sonderfeld.mathias.bettertapebot.bot;

import eu.sonderfeld.mathias.bettertapebot.properties.BotProperties;
import eu.sonderfeld.mathias.bettertapebot.util.TextSplitter;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
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
    
    @NonFinal
    Duration throttleDuration;
    
    @PostConstruct
    void postConstruct(){
        throttleDuration = Duration.ofMillis(botProperties.getTelegram().getDelayBetweenMessagesMs());
    }
    
    /**
     * sends a chat to the telegram api asynchronously.
     * Splits the message into chunks, if it's longer than the {@link BotProperties.TelegramProperties#getMessageLengthLimit() configured} limit.
     * If multiple messages are sent, a {@link BotProperties.TelegramProperties#getDelayBetweenMessagesMs() delay} is added to throttle the API load.
     * Always removes any existing keyboard, if you don't want this behaviour, call the overload with the keyboard parameter and set it to null.
     * @param chatId the id of the chat to respond to
     * @param text the message to send
     */
    public void send(long chatId, String text) {
        send(chatId, new ReplyKeyboardRemove(true), text);
    }
    
    /**
     * sends a chat to the telegram api asynchronously with the given keyboard.
     * Splits the message into chunks, if it's longer than {@link BotProperties.TelegramProperties#getMessageLengthLimit() configured} limit.
     * Keyboard is only sent with first message.
     * If multiple messages are sent, a {@link BotProperties.TelegramProperties#getDelayBetweenMessagesMs() delay} is added to throttle the API load.
     * @param chatId the id of the chat to respond to
     * @param replyKeyboard the Keyboard to present to the user
     * @param text the message to send
     */
    public void send(long chatId, ReplyKeyboard replyKeyboard, String text) {
        List<String> chunks = TextSplitter.splitTextSmart(text, botProperties.getTelegram().getMessageLengthLimit());
        //send messages async
        sendChunks(chatId, chunks, replyKeyboard).subscribe();
    }
    
    public void broadcast(List<Long> chatIds, String text) {
        List<String> chunks = TextSplitter.splitTextSmart(text, botProperties.getTelegram().getMessageLengthLimit());
        
        //broadcast async
        Flux.fromIterable(chatIds)
            //flatMap does not keep order but there is no order to preserve in the chatIds
            .flatMap(chatId -> sendChunks(chatId, chunks, null))
            .subscribe();
    }
    
    private Flux<Long> sendChunks(long chatId, List<String> chunks, ReplyKeyboard markup){
        return Flux.fromIterable(chunks)
            .index()
            //concatMap ensures sequential processing of chunks
            .concatMap(tuple -> {
                long index = tuple.getT1();
                String chunk = tuple.getT2();
                
                return sendChunk(chatId, chunk, index == 0 ? markup : null)
                    .then(index < chunks.size() - 1 ? Mono.delay(throttleDuration) : Mono.empty());  // Kein delay nach letztem Chunk
            });
    }

    private Mono<Void> sendChunk(long chatId, String chunk, ReplyKeyboard markup) {
        return Mono.fromRunnable(() -> {
            SendMessage message = SendMessage
                .builder()
                .chatId(chatId)
                .text(chunk)
                .parseMode("HTML")
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
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }


}