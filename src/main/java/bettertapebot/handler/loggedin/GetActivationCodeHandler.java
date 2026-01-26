package bettertapebot.handler.loggedin;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.Command;
import bettertapebot.handler.CommandHandler;
import bettertapebot.repository.entity.UserStateEntity;
import bettertapebot.config.PasscodeGenerator;
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
public class GetActivationCodeHandler implements CommandHandler {

    ResponseService responseService;
    PasscodeGenerator passcodeGenerator;

    @Override
    public @NonNull Command forCommand() {
        return Command.CODE;
    }

    @Override
    @Transactional
    public void handleMessage(@NonNull UserStateEntity userStateEntity, long chatId, String message) {
        if(!userStateEntity.getUserState().isLoggedIn()){
            responseService.send(chatId, "Nur eingeloggte User können Codes erzeugen");
            return;
        }
        responseService.send(chatId, "Der aktuelle Freischaltcode lautet: " + passcodeGenerator.generatePasscode());
    }
}
