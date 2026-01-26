package bettertapebot.config;

import bettertapebot.properties.BotProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig
class PasscodeGeneratorTest {
    
    PasscodeGenerator passcodeGenerator;
    
    @BeforeEach
    void setup(){
        passcodeGenerator = new PasscodeGenerator(new BotProperties());
    }
    
    @Test
    void testCodeWithinTheSame24hStaysTheSame(){
        var code = passcodeGenerator.generatePasscode();
        assertThat(code).isPositive();
        for (int i = 0; i < 1000; i++) {
            assertThat(passcodeGenerator.generatePasscode()).isEqualTo(code);
            assertThat(passcodeGenerator.validatePasscode(code+1)).isFalse();
            assertThat(passcodeGenerator.validatePasscode(code)).isTrue();
        }
    }
    
    @Test
    void testCodeOlderThan24hoursGetReplaced(){
        int count = 1000;
        List<Integer> codes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            passcodeGenerator.setLastTimeGenerated(Instant.MIN);
            var code = passcodeGenerator.generatePasscode();
            codes.add(code);
            assertThat(passcodeGenerator.validatePasscode(code+1)).isFalse();
            assertThat(passcodeGenerator.validatePasscode(code)).isTrue();
        }
        assertThat(codes).hasSize(count);
        var uniqueCodes = new HashSet<>(codes);
        assertThat(uniqueCodes).hasSizeGreaterThan(10); //randomly generated can vary heavily
    }
}