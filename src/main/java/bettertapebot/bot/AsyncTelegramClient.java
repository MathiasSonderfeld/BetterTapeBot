package bettertapebot.bot;

import bettertapebot.properties.BotProperties;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@CustomLog
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AsyncTelegramClient {
    //gets accessed from main thread and async threadpool so must be thread-safe!
    private final Map<Long, Sinks.Many<SendMessage>> sinks = new ConcurrentHashMap<>();
    
    TelegramClient telegramClient;
    BotProperties botProperties;
    
    public void sendMessage(long chatId, SendMessage msg) {
        //check if we already have a sink for this chatId, if not create one
        sinks.computeIfAbsent(chatId, this::createSink)
            //add message to sink queue
            .tryEmitNext(msg);
    }
    
    private Sinks.Many<SendMessage> createSink(long chatId) {
        /*
            Telegram API limits bots to 1 msg per second per chat. So if we send more, we need to respect that rate limit.
            But we also don't want to stall the whole bot during that time.
            So we need another thread that sends the messages in this rate and a queue to buffer multiple send requests.
            
            This creates exactly the described setup:
            Sinks are a drop-off point for tasks to run async in a dedicated Threadpool.
            many() means that the sink accepts multiple inputs, in our case multiple messages that get sent to the same chat.
            unicast() means we only have one consumer per sink in that Threadpool. We want this as we want to only send one message per second and keep messages in order.
            This way we can send tons of messages in the main thread and the consumer thread in the threadpool will crunch through them async while main can focus on handling other chats.
            onBackpressureBuffer() means that we actually also want a queue to store messages if the main thread adds new messages to send out faster than the consumer can process them.
            As we don't want to worry about rate limit in the mainthread, this is what will occur a lot and that's where our queue is created to store the messages until they are processed.
            This is all part of the project-reactor reactive programming framework, it's imported as part of the spring-boot-starter-webflux dependency.
         */
        Sinks.Many<SendMessage> sink = Sinks.many().unicast().onBackpressureBuffer();
        
        //Flux is a multi object reactive processing pipeline - like java streams but async and multithreaded
        sink.asFlux()
            //concatMap ensures that input order is processing order, otherwise Flux are executed in parallel so no order guarantee
            .concatMap(msg -> sendWithRetry(msg)
                //send message and wait 1s to respect rate limit
                .delayElement(botProperties.getTelegram().getDelayBetweenMessagesForSameChat()))
            //when no messages are left, free the sink until next user input
            .doFinally(_ -> sinks.remove(chatId))
            //run this async and do not block current thread until its completed (this is important so main thread can continue processing other messages)
            .subscribe();
        
        return sink;
    }
    
    private Mono<Message> sendWithRetry(SendMessage msg) {
        return Mono.fromCallable(() -> telegramClient.execute(msg))
            .onErrorResume(th -> {
                //check if telegram sends HTTP 429 - too many requests
                if(th instanceof TelegramApiRequestException apiRequestException && apiRequestException.getErrorCode() == 429){
                    //if so, read the read_after response parameter and wait that long
                    log.warn("request was denied due to 429 - too many requests, waiting and retrying", apiRequestException);
                    return Mono.delay(Duration.ofSeconds(apiRequestException.getParameters().getRetryAfter()))
                        //then throw this custom exception to trigger the retry
                        .then(Mono.error(new TooManyRequestsException()));
                }
                //in any other case propagate error further
                return Mono.error(th);
            })
            //retry on custom exception. Now bot waits for retry_after seconds and then retries the request
            //we need this workaround with the custom exception as Retry cant access exception data, and so we cant tell it to wait retry_after seconds
            .retryWhen(Retry.max(botProperties.getTelegram().getRetryCountInCaseOfTooManyRequests())
                .filter(err -> err instanceof TooManyRequestsException))
            .doOnError(e -> log.error("failure when sending, message is dropped", e));
    }
    
    private static class TooManyRequestsException extends Exception {}
}
