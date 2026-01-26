package bettertapebot.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "better-tape-bot")
public class BotProperties {

    @NotNull
    TelegramProperties telegram = new TelegramProperties();

    @NotNull
    Duration activationCodeTTL = Duration.ofHours(24);
    
    @NotBlank
    String dsgvoResourceName = "dsgvo.txt";

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TelegramProperties {
        @NotBlank
        String token;

        @Positive
        int messageLengthLimit = 4096;

        @Positive
        int delayBetweenMessagesMs = 900;
    }
}
