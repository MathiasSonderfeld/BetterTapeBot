package bettertapebot.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "better-tape-bot")
public class BotProperties {

    @NotNull
    Duration activationCodeTTL = Duration.ofHours(24);
    @NotNull
    Locale activationCodeFormatLocale = Locale.GERMANY;
    
    @NotBlank
    String gdprResourceName = "dsgvo.txt";
    
    @NotBlank
    String acceptGdprText = "Akzeptieren";
    
    @NotBlank
    String denyGdprText = "Ablehnen";
    
    @NotBlank
    String defaultUserForTapes = "anonymous";
    
    @NotNull
    Duration tapeCacheTTL = Duration.ofHours(1);
    
    @NotNull
    SubscriptionProperties subscription = new SubscriptionProperties();
    
    @NotNull
    TelegramProperties telegram = new TelegramProperties();
    
    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SubscriptionProperties {
        @NotEmpty
        List<String> countsAsYes;
        @NotEmpty
        List<String> countsAsNo;
        
        public Boolean interpretStatus(String input){
            if(countsAsYes.contains(input)){
                return true;
            }
            else if(countsAsNo.contains(input)){
                return false;
            }
            else {
                return null;
            }
        }
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TelegramProperties {
        @NotBlank
        String token;

        @Positive
        int messageLengthLimit = 4096;
        
        /*
         * Telegram API defines limit at 1 msg per second
         */
        @NotNull
        Duration delayBetweenMessagesForSameChat = Duration.ofSeconds(1);
        
        @Positive
        int retryCountInCaseOfTooManyRequests = 5;
    }
}
