package eu.sonderfeld.mathias.bettertapebot.util;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig
class TextSplitterTest {
    
    @Test
    void testShortTextGetsThrough(){
        String input = "input";
        var result = TextSplitter.splitTextSmart(input, 10);
        assertThat(result).isNotNull()
            .hasSize(1)
            .element(0)
            .isEqualTo(input);
    }
    
    @Test
    void testLongTextWithoutLinebreaksGetsCutAtLimit(){
        String input = "input";
        int repeatCount = 10;
        String repeated = input.repeat(repeatCount);
        var result = TextSplitter.splitTextSmart(repeated, input.length());
        assertThat(result).isNotNull()
            .hasSize(repeatCount)
            .allSatisfy(t -> assertThat(t).isEqualTo(input));
    }
    
    @Test
    void testLongTextWithLinebreaksGetsCutAtLinebreak(){
        String input = "in\nput";
        int repeatCount = 5;
        String repeated = input.repeat(repeatCount);
        
        var expected = List.of(
            "in",
            "putin",
            "putin",
            "putin",
            "putin",
            "put"
        );
        var result = TextSplitter.splitTextSmart(repeated, 6);
        assertThat(result).isNotNull()
            .isEqualTo(expected);
    }
    
    
    @Test
    void testLongTextWithDobuleLinebreaksGetsCutAtDoubleLinebreak(){
        String input = "in\nput\n\n";
        int repeatCount = 5;
        String repeated = input.repeat(repeatCount);
        
        var expected = List.of(
            "in\nput",
            "in\nput",
            "in\nput",
            "in\nput",
            "in\nput"
        );
        var result = TextSplitter.splitTextSmart(repeated, 6);
        assertThat(result).isNotNull()
            .isEqualTo(expected);
    }
    
    @Test
    void testFindSmartSplitPointFindsDNothing(){
        var text = "this is a test, this is a new Line";
        var result = TextSplitter.findSmartSplitPoint(text, text.length());
        assertThat(result).isEqualTo(text.length());
    }
    
    @Test
    void testFindSmartSplitPointFindsNewLine(){
        var text = "this is a test\nthis is a new Line";
        var result = TextSplitter.findSmartSplitPoint(text, text.length());
        var expected = text.lastIndexOf("this");
        assertThat(result).isEqualTo(expected);
    }
    
    @Test
    void testFindSmartSplitPointFindsDoubleNewLine(){
        var text = "this is a test\n\nthis is a\n new Line";
        var result = TextSplitter.findSmartSplitPoint(text, text.length());
        var expected = text.lastIndexOf("this");
        assertThat(result).isEqualTo(expected);
    }
}