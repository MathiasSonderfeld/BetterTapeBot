package bettertapebot.handler.loggedin;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.Command;
import bettertapebot.handler.CommandHandler;
import bettertapebot.properties.BotProperties;
import bettertapebot.repository.TapeRepository;
import bettertapebot.repository.entity.UserStateEntity;
import bettertapebot.util.TapeFormatter;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@CustomLog
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetLastHandler implements CommandHandler {

    ResponseService responseService;
    TapeRepository tapeRepository;
    BotProperties botProperties;

    @Override
    public @NonNull Command forCommand() {
        return Command.LAST;
    }

    @Override
    @Transactional
    public void handleMessage(@NonNull UserStateEntity userStateEntity, String message) {
        if(!userStateEntity.getUserState().isLoggedIn()){
            responseService.send(userStateEntity.getChatId(), "Nur eingeloggte User können Tapes abfragen");
            return;
        }
        
        var tape = tapeRepository.findTopByOrderByDateAddedDesc();
        if(tape.isEmpty()){
            responseService.send(userStateEntity.getChatId(), "Es gibt noch keine Einträge");
            return;
        }
        boolean isAdmin = userStateEntity.isAdminModeActive();
        responseService.send(userStateEntity.getChatId(), TapeFormatter.formatTape(tape.get(), botProperties.getOutputTimezone(), isAdmin));
    }
}
