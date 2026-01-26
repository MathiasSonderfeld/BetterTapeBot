package bettertapebot.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;
import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;

@SpringJUnitConfig
class DurationFormatterTest {

    static Stream<Arguments> provideDurations(){
        return Stream.of(
            Arguments.of(named("hours in EN", Duration.ofHours(12)), Locale.UK, "12 hours"),
            Arguments.of(named("hours in DE", Duration.ofHours(12)), Locale.GERMAN, "12 Stunden"),
            Arguments.of(named("hours in IT", Duration.ofHours(12)), Locale.ITALIAN, "12 ore"),
            Arguments.of(named("one hour in EN", Duration.ofHours(1)), Locale.UK, "1 hour"),
            Arguments.of(named("one hour in DE", Duration.ofHours(1)), Locale.GERMAN, "1 Stunde"),
            Arguments.of(named("one hour in IT", Duration.ofHours(1)), Locale.ITALIAN, "1 ora"),
            Arguments.of(named("one day and 15 mins in EN", Duration.ofDays(1).plusMinutes(15)), Locale.UK, "1 day, 15 minutes"),
            Arguments.of(named("one day and 15 mins in DE", Duration.ofDays(1).plusMinutes(15)), Locale.GERMAN, "1 Tag, 15 Minuten"),
            Arguments.of(named("one day and 15 mins in IT", Duration.ofDays(1).plusMinutes(15)), Locale.ITALIAN, "1 giorno e 15 minuti"),
            Arguments.of(named("two minutes two seconds and 430 milliseconds in EN", Duration.ofMinutes(2).plusSeconds(2).plusMillis(430)), Locale.UK, "2 minutes, 2 seconds, 430 milliseconds"),
            Arguments.of(named("two minutes two seconds and 430 milliseconds in DE", Duration.ofMinutes(2).plusSeconds(2).plusMillis(430)), Locale.GERMAN, "2 Minuten, 2 Sekunden und 430 Millisekunden"),
            Arguments.of(named("two minutes two seconds and 430 milliseconds in IT", Duration.ofMinutes(2).plusSeconds(2).plusMillis(430)), Locale.ITALIAN, "2 minuti, 2 secondi e 430 millisecondi")
        );
    }
    
    @ParameterizedTest(name = "{0}")
    @MethodSource("provideDurations")
    void testFormat(Duration duration, Locale locale, String expected){
        long millis = duration.toMillis();
        var result = DurationFormatter.format(duration, locale);
        assertThat(result).isEqualTo(expected);
        assertThat(millis).isEqualTo(duration.toMillis());
    }
}