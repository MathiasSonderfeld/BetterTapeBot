package bettertapebot.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
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
    @Valid
    InputValidationProperties inputValidation = new InputValidationProperties();
    
    @NotNull
    @Valid
    GdprProperties gdpr = new GdprProperties();
    
    @NotNull
    @Valid
    ActivationCodeProperties activationCode = new ActivationCodeProperties();
    
    @NotNull
    @Valid
    SubscriptionProperties subscription = new SubscriptionProperties();
    
    @NotNull
    @Valid
    TelegramProperties telegram = new TelegramProperties();
    
    @Data
    @Validated
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class InputValidationProperties {
        @NotNull
        Pattern username = Pattern.compile("^[A-Za-z0-9+_.-]{2,255}$");
        
        @NotNull
        Pattern pin = Pattern.compile("^[0-9]{4}$");
    }
    
    
    @Data
    @Validated
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class GdprProperties {
        @NotNull
        Resource resource = new ClassPathResource("dsgvo.txt");
        
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
        List<String> countsAsYes = List.of("y", "yes", "j", "ja");
        @NotEmpty
        List<String> countsAsNo = List.of("n", "no", "nein");
        
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
