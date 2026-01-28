package bettertapebot.config;

import bettertapebot.properties.BotProperties;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Random;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PasscodeGenerator {
    
    BotProperties botProperties;
    Random random = new Random();
    
    @NonFinal
    @Setter(AccessLevel.PROTECTED)
    Instant lastTimeGenerated = Instant.MIN;
    @NonFinal
    int currentPasscode = 0;

    public int generatePasscode() {
        Instant now = Instant.now();
        if (now.isAfter(lastTimeGenerated.plus(botProperties.getActivationCode().getTtl()))) {
            currentPasscode = random.nextInt(1000,10000); //generate 4 digit code
            lastTimeGenerated = now;
            return currentPasscode;
        }
        return currentPasscode;
    }

    public boolean validatePasscode(int code) {
        if (Instant.now().isAfter(lastTimeGenerated.plus(botProperties.getActivationCode().getTtl())) || (lastTimeGenerated == Instant.MIN && code == 0)) {
            return false;
        }
        return code == currentPasscode;
    }
}