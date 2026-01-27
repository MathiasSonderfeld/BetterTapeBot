package bettertapebot.bot;

import bettertapebot.properties.BotProperties;
import bettertapebot.util.TextSplitter;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;

import java.util.List;

@CustomLog
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ResponseService {
    
    BotProperties botProperties;
    AsyncTelegramClient asyncTelegramClient;

    public void send(long chatId, String text) {
        send(chatId, new ReplyKeyboardRemove(true), text);
    }
    
    public void send(long chatId, ReplyKeyboard replyKeyboard, String text) {
        List<String> chunks = TextSplitter.splitTextSmart(text, botProperties.getTelegram().getMessageLengthLimit());
        sendChunks(chatId, chunks, replyKeyboard);
    }
    
    public void broadcast(List<Long> chatIds, String text) {
        List<String> chunks = TextSplitter.splitTextSmart(text, botProperties.getTelegram().getMessageLengthLimit());
        for (Long chatId : chatIds) {
            sendChunks(chatId, chunks, null);
        }
    }
    
    private void sendChunks(long chatId, List<String> chunks, ReplyKeyboard markup){
        for (int i = 0; i < chunks.size(); i++) {
            SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(chunks.get(i))
                .parseMode("HTML")
                .build();
            
            // Nur erster Chunk bekommt das ReplyKeyboard
            if (i == 0 && markup != null) {
                message.setReplyMarkup(markup);
            }
            
            asyncTelegramClient.sendMessage(chatId, message);
            if (log.isDebugEnabled()) {
                log.debug("Message sent to ChatId '{}': {}", chatId, chunks.get(i));
            }
        }
    }
}