package eu.sonderfeld.mathias.bettertapebot.handler.general;

import eu.sonderfeld.mathias.bettertapebot.bot.ResponseService;
import eu.sonderfeld.mathias.bettertapebot.handler.Command;
import eu.sonderfeld.mathias.bettertapebot.handler.CommandHandler;
import eu.sonderfeld.mathias.bettertapebot.properties.BotProperties;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@CustomLog
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetDsgvoHandler implements CommandHandler {

    ResponseService responseService;
    BotProperties botProperties;

    @Override
    public @NonNull Command forCommand() {
        return Command.DSGVO;
    }

    @Override
    public void handleCommand(long chatId, String message) {
        try {
            var dsgoFile = Thread.currentThread().getContextClassLoader()
                .getResource(botProperties.getDsgvoResourceName());

            if(dsgoFile == null) {
                log.error("cant load dsgvo resource, the url is null");
                return;
            }

            String dsgvo = Files.readString(Path.of(dsgoFile.getPath()));
            responseService.send(chatId, dsgvo);
        } catch (IOException e) {
         log.error("cant load dsgvo resouce, error during file access", e);
        }
    }
}
