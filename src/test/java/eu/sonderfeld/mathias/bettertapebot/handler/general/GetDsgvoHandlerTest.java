package eu.sonderfeld.mathias.bettertapebot.handler.general;

import eu.sonderfeld.mathias.bettertapebot.bot.ResponseService;
import eu.sonderfeld.mathias.bettertapebot.handler.Command;
import eu.sonderfeld.mathias.bettertapebot.properties.BotProperties;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(classes = {GetDsgvoHandler.class, BotProperties.class})
class GetDsgvoHandlerTest {

    @Autowired
    GetDsgvoHandler getDsgvoHandler;

    @MockitoBean
    ResponseService responseService;
    
    @Autowired
    private BotProperties botProperties;

    @Test
    public void registersForCorrectCommand(){
        assertThat(getDsgvoHandler.forCommand()).isEqualTo(Command.DSGVO);
    }

    @Test
    void respondsWithDsgvo() {
        getDsgvoHandler.handleCommand(1L, "");
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(1L), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("testdsgvo");
    }

    @Test
    void brokenConfigMeansNoResponse() {
        var before = botProperties.getDsgvoResourceName();
        botProperties.setDsgvoResourceName("invalidFile");
        getDsgvoHandler.handleCommand(1L, "");
        Mockito.verifyNoInteractions(responseService);
        botProperties.setDsgvoResourceName(before);
    }
}