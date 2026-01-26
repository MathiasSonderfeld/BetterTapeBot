package bettertapebot.handler.loggedin;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.Command;
import bettertapebot.handler.CommandHandler;
import bettertapebot.repository.TapeRepository;
import bettertapebot.repository.entity.TapeEntity;
import bettertapebot.repository.entity.UserStateEntity;
import bettertapebot.util.TapeFormatter;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@CustomLog
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetAllHandler implements CommandHandler {

    ResponseService responseService;
    TapeRepository tapeRepository;

    @Override
    public @NonNull Command forCommand() {
        return Command.ALL;
    }

    @Override
    @Transactional
    public void handleMessage(@NonNull UserStateEntity userStateEntity, long chatId, String message) {
        if(!userStateEntity.getUserState().isLoggedIn()){
            responseService.send(chatId, "Nur eingeloggte User können Tapes abfragen");
            return;
        }
        
        List<TapeEntity> tapes = tapeRepository.findAllByOrderByDateAddedDesc();
        if(tapes.isEmpty()){
            responseService.send(chatId, "Es gibt noch keine Einträge");
            return;
        }
        
        boolean isAdmin = userStateEntity.getUserState().isAdmin();
        StringBuilder stringBuilder = new StringBuilder();
        for (TapeEntity tape : tapes) {
            stringBuilder.append(TapeFormatter.formatTape(tape, isAdmin))
                .append("\n\n");
        }
        responseService.send(chatId, stringBuilder.toString());
    }
}
