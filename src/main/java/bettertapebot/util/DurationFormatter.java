package bettertapebot.util;

import com.ibm.icu.text.MeasureFormat;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.util.Locale;
import java.util.stream.Stream;

@UtilityClass
public class DurationFormatter {
    
    public String format(@NonNull Duration duration, @NonNull Locale locale){
        long days = duration.toDays();
        long hours = duration.minusDays(days).toHours();
        long minutes = duration.minusDays(days).minusHours(hours).toMinutes();
        long seconds = duration.minusDays(days).minusHours(hours).minusMinutes(minutes).getSeconds();
        long millis = duration.minusDays(days).minusHours(hours).minusMinutes(minutes).minusSeconds(seconds).toMillis();
        MeasureFormat format = MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.WIDE);
        Measure[] measures = Stream.of(
                new Measure(days, MeasureUnit.DAY),
                new Measure(hours, MeasureUnit.HOUR),
                new Measure(minutes, MeasureUnit.MINUTE),
                new Measure(seconds, MeasureUnit.SECOND),
                new Measure(millis, MeasureUnit.MILLISECOND))
            .filter(m -> m.getNumber().floatValue() >= 1)
            .toArray(Measure[]::new);
        return format.formatMeasures(measures);
    }
}
