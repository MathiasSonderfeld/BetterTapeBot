package eu.sonderfeld.mathias.bettertapebot.util;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig
class PasscodeGeneratorTest {
    
    @Test
    void testCodeWithinTheSame24hStaysTheSame(){
        var code = PasscodeGenerator.generatePasscode();
        assertThat(code).isPositive();
        for (int i = 0; i < 1000; i++) {
            assertThat(PasscodeGenerator.generatePasscode()).isEqualTo(code);
            assertThat(PasscodeGenerator.validatePasscode(code+1)).isFalse();
            assertThat(PasscodeGenerator.validatePasscode(code)).isTrue();
        }
    }
    
    @Test
    void testCodeOlderThan24hoursGetReplaced(){
        int count = 1000;
        List<Integer> codes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            PasscodeGenerator.setLastTimeGenerated(Instant.MIN);
            var code = PasscodeGenerator.generatePasscode();
            codes.add(code);
            assertThat(PasscodeGenerator.validatePasscode(code+1)).isFalse();
            assertThat(PasscodeGenerator.validatePasscode(code)).isTrue();
        }
        assertThat(codes).hasSize(count);
        var uniqueCodes = new HashSet<>(codes);
        assertThat(uniqueCodes).hasSizeGreaterThan(10); //randomly generated can vary heavily
    }
}