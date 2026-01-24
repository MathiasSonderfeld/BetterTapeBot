package eu.sonderfeld.mathias.bettertapebot.util;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class TextSplitter {

    public List<String> splitTextSmart(String text, int limit) {
        List<String> result = new ArrayList<>();
        if(text.length() <= limit){
            result.add(text);
            return result;
        }

        while (text.length() > limit) {
            int splitIndex = findSmartSplitPoint(text, limit);
            result.add(text.substring(0, splitIndex).trim());
            text = text.substring(splitIndex).trim();
        }
        if (!text.isEmpty()) {
            result.add(text);
        }
        return result;
    }

    public int findSmartSplitPoint(String text, int start) {
        // Versuche, ein Absatzende zu finden
        int index = text.lastIndexOf("\n\n", start);
        if (index != -1) return index + 2;

        // Versuche, einen einfachen Zeilenumbruch zu finden
        index = text.lastIndexOf("\n", start);
        if (index != -1) return index + 1;

        // Kein Zeilenumbruch gefunden
        return start;
    }
}
