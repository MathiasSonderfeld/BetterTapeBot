package eu.sonderfeld.mathias.bettertapebot.bot;

import eu.sonderfeld.mathias.bettertapebot.properties.BotProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Configuration
public class ClientConfig {

    @Bean
    public TelegramClient telegramClient(BotProperties botProperties){
        return new OkHttpTelegramClient(botProperties.getTelegram().getToken());
    }
}
