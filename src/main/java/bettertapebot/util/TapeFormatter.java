package bettertapebot.util;

import bettertapebot.repository.entity.TapeEntity;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.NonNull;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.stream.Collectors;

@UtilityClass
public class TapeFormatter {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");
    private static final String TEMPLATE = """
        <b>%s</b> <i>mit</i> %s
        <i>von</i> %s am %s
        """.trim();
    
    public String formatTape(@NonNull TapeEntity tape, boolean addId){
        var dateString = tape.getDateAdded()
            .atOffset(ZoneOffset.UTC)
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(DATE_FORMAT);
        var formatted = String.format(TEMPLATE, tape.getTitle(), tape.getStar().getUsername(), tape.getDirector().getUsername(), dateString);
        if(addId){
            formatted += String.format("\n<code>%s</code>", tape.getId().toString());
        }
        return formatted;
    }
    
    public String formatTapes(@NonNull Collection<TapeEntity> tapes, boolean addId){
        if(tapes.isEmpty()){
            return "Es gibt noch keine Einträge";
        }
        return tapes.stream().map(t -> formatTape(t, addId))
            .collect(Collectors.joining("\n\n"));
    }
}
