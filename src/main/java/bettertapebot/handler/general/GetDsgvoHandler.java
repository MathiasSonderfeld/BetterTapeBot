package bettertapebot.handler.general;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.Command;
import bettertapebot.handler.CommandHandler;
import bettertapebot.properties.BotProperties;
import bettertapebot.repository.entity.UserStateEntity;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public void handleMessage(@NonNull UserStateEntity userStateEntity, long chatId, String message) {
        try {
            var dsgoFile = Thread.currentThread().getContextClassLoader()
                .getResource(botProperties.getGdprResourceName());

            if(dsgoFile == null) {
                log.error("cant load dsgvo resource, the url is null");
                return;
            }

            String dsgvo = Files.readString(Path.of(dsgoFile.getPath()));
            responseService.send(chatId, null, dsgvo);
        } catch (IOException e) {
         log.error("cant load dsgvo resource, error during file access", e);
        }
    }
}
