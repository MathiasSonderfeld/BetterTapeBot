package bettertapebot.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Data
@Validated
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "better-tape-bot")
public class BotProperties {
    
    @NotBlank
    String defaultUserForTapes = "anonymous";
    
    @NotNull
    Duration tapeCacheTtl = Duration.ofHours(1);
    
    @NotNull
    ZoneId outputTimezone = ZoneId.of("Europe/Berlin");
    
    
    @NotNull
    InputValidationProperties inputValidation = new InputValidationProperties();
    
    @NotNull
    GdprProperties gdpr = new GdprProperties();
    
    @NotNull
    ActivationCodeProperties activationCode = new ActivationCodeProperties();
    
    @NotNull
    SubscriptionProperties subscription = new SubscriptionProperties();
    
    @NotNull
    TelegramProperties telegram = new TelegramProperties();
    
    @Data
    @Validated
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class InputValidationProperties {
        @NotNull
        Pattern username;
        
        @NotNull
        Pattern pin;
    }
    
    
    @Data
    @Validated
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class GdprProperties {
        @NotBlank
        String resource = "dsgvo.txt";
        
        @NotBlank
        String acceptText = "Akzeptieren";
        
        @NotBlank
        String denyText = "Ablehnen";
    }
    
    @Data
    @Validated
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ActivationCodeProperties {
        @NotNull
        Duration ttl = Duration.ofHours(24);
        
        @NotNull
        Locale formatLocale = Locale.GERMANY;
    }
    
    @Data
    @Validated
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
    @Validated
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
