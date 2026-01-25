package bettertapebot.util;

import bettertapebot.repository.entity.TapeEntity;
import bettertapebot.repository.entity.UserEntity;
import bettertapebot.util.TapeFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig
class TapeFormatterTest {
    
    @Test
    void testNormalUser(){
        UUID uuid = UUID.fromString("ae30228b-1623-41b8-bf49-bcb4a49ff62a");
        var expected = "<b>Inception</b> <i>mit</i> Leonardo DiCaprio\n<i>von</i> Christopher Nolan am 01.02.91 05:41";
        var expectedAdmin = expected + "\n<code>ae30228b-1623-41b8-bf49-bcb4a49ff62a</code>";
        var date = ZonedDateTime.of(1991, 2, 1, 5, 41, 14, 0, ZoneId.systemDefault());
        var leo = UserEntity.builder()
            .username("Leonardo DiCaprio")
            .build();
        var chris = UserEntity.builder()
            .username("Christopher Nolan")
            .build();
        var tape = TapeEntity.builder()
            .id(uuid)
            .title("Inception")
            .star(leo)
            .director(chris)
            .dateAdded(date.toInstant())
            .build();
        assertThat(TapeFormatter.formatTape(tape, false)).isEqualTo(expected);
        assertThat(TapeFormatter.formatTape(tape, true)).isEqualTo(expectedAdmin);
    }
}