package bettertapebot.util;

import lombok.AccessLevel;
import lombok.Setter;
import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;

@UtilityClass
public class PasscodeGenerator {
    public static final int timeWindow = 24;
    
    @Setter(AccessLevel.PROTECTED)
    private Instant lastTimeGenerated = Instant.MIN;
    private int currentPasscode = 0;

    public int generatePasscode() {
        Instant now = Instant.now();
        if (now.isAfter(lastTimeGenerated.plus(timeWindow, ChronoUnit.HOURS))) {
            Random random = new Random();
            currentPasscode = random.nextInt(1000,9000); //generate 4 digit code
            lastTimeGenerated = now;
            return currentPasscode;
        }
        return currentPasscode;
    }

    public boolean validatePasscode(int code) {
        if (Instant.now().isAfter(lastTimeGenerated.plus(timeWindow, ChronoUnit.HOURS)) || (lastTimeGenerated == Instant.MIN && code == 0)) {
            return false;
        }
        return code == currentPasscode;
    }
}