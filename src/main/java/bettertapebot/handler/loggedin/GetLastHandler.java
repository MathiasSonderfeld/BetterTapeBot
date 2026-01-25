package bettertapebot.handler.loggedin;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.Command;
import bettertapebot.handler.CommandHandler;
import bettertapebot.repository.TapeRepository;
import bettertapebot.repository.UserStateRepository;
import bettertapebot.repository.entity.UserState;
import bettertapebot.repository.entity.UserStateEntity;
import bettertapebot.util.TapeFormatter;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

@CustomLog
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetLastHandler implements CommandHandler {

    ResponseService responseService;
    UserStateRepository userStateRepository;
    TapeRepository tapeRepository;

    @Override
    public @NonNull Command forCommand() {
        return Command.LAST;
    }

    @Override
    public void handleCommand(long chatId, String message) {
        var userStateEntityOptional = userStateRepository.findById(chatId);
        var knownAndLoggedIn = userStateEntityOptional
            .map(UserStateEntity::getUserState)
            .map(UserState::isLoggedIn)
            .orElse(false);
        if(!knownAndLoggedIn){
            responseService.send(chatId, "Nur eingeloggte User können Tapes abfragen");
            return;
        }
        
        var tape = tapeRepository.findTopByOrderByDateAddedDesc();
        if(tape.isEmpty()){
            responseService.send(chatId, "Es gibt noch keine Einträge");
            return;
        }
        boolean isAdmin = userStateEntityOptional.get().getUserState().isAdmin();
        responseService.send(chatId, TapeFormatter.formatTape(tape.get(), isAdmin));
    }
}
