package bettertapebot.util;

import bettertapebot.handler.Command;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.NonNull;
import org.springframework.util.StringUtils;

@UtilityClass
public class MessageCleaner {
    
    public String cleanup(String input){
        if(!StringUtils.hasText(input)){
            return "";
        }
        return input.trim();
    }
    
    public String[] getAllWords(String message){
        var trimmed = cleanup(message);
        return trimmed.split("\\s+");
    }
    
    public String getFirstWord(String message){
        return getAllWords(message)[0];
    }
    
    public String removeCommand(@NonNull String message, @NonNull Command command) {
        return message.substring(command.getCommand().length()).trim();
    }
}
