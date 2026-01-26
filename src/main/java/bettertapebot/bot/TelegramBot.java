package bettertapebot.bot;

import bettertapebot.properties.BotProperties;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@CustomLog
@RequiredArgsConstructor
@EnableConfigurationProperties(BotProperties.class)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TelegramBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    BotProperties botProperties;
    MessageDelegator messageDelegator;

    @Override
    public String getBotToken() {
        return botProperties.getTelegram().getToken();
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    /*
        never access the database in this class unless you know what youre doing
        the method does not get called via its public method api but with some strange fuckery, probably reflection, I didnt look into the lib
        but you cant start a transaction here with the transaction annotation
        You'd need to manually create or join one with the entity manager
        I thought it was easier to put the evaluation logic in another bean
     */
    @Override
    public void consume(Update update) {
        messageDelegator.processUpdate(update);
    }
}