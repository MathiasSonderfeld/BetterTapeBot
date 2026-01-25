package eu.sonderfeld.mathias.bettertapebot.handler.loggedin;

import eu.sonderfeld.mathias.bettertapebot.bot.ResponseService;
import eu.sonderfeld.mathias.bettertapebot.handler.Command;
import eu.sonderfeld.mathias.bettertapebot.handler.CommandHandler;
import eu.sonderfeld.mathias.bettertapebot.repository.TapeRepository;
import eu.sonderfeld.mathias.bettertapebot.repository.UserStateRepository;
import eu.sonderfeld.mathias.bettertapebot.repository.entity.TapeEntity;
import eu.sonderfeld.mathias.bettertapebot.repository.entity.UserState;
import eu.sonderfeld.mathias.bettertapebot.repository.entity.UserStateEntity;
import eu.sonderfeld.mathias.bettertapebot.util.TapeFormatter;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;

@CustomLog
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetAllHandler implements CommandHandler { //TODO implement, gib also Id if isAdmin

    ResponseService responseService;
    UserStateRepository userStateRepository;
    TapeRepository tapeRepository;

    @Override
    public @NonNull Command forCommand() {
        return Command.ALL;
    }

    @Override
    public void handleCommand(long chatId, String message) {
        var stateOptional = userStateRepository.findById(chatId);
        var knownAndLoggedIn = stateOptional
            .map(UserStateEntity::getUserState)
            .map(UserState::isLoggedIn)
            .orElse(false);
        if(!knownAndLoggedIn){
            responseService.send(chatId, "Nur eingeloggte User können Tapes abfragen");
            return;
        }
        boolean isAdmin = stateOptional.get().getUserState().isAdmin();
        var sort = Sort.by(Sort.Direction.DESC, TapeEntity.Fields.dateAdded);
        List<TapeEntity> tapes = tapeRepository.findAll(sort);
        
        StringBuilder stringBuilder = new StringBuilder();
        for (TapeEntity tape : tapes) {
            stringBuilder.append(TapeFormatter.formatTape(tape, isAdmin))
                .append("\n\n");
        }
        responseService.send(chatId, stringBuilder.toString());
    }
}
