package bettertapebot.util;

import bettertapebot.repository.entity.TapeEntity;
import lombok.experimental.UtilityClass;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@UtilityClass
public class TapeFormatter {
    private static final String TEMPLATE = """
        <b>%s</b> <i>mit</i> %s
        <i>von</i> %s am %s
        """.trim();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");
    
    public String formatTape(TapeEntity tape, boolean addId){
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
}
