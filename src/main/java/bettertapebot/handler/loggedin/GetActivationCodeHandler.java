package bettertapebot.handler.loggedin;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.Command;
import bettertapebot.handler.CommandHandler;
import bettertapebot.properties.BotProperties;
import bettertapebot.repository.entity.UserStateEntity;
import bettertapebot.config.PasscodeGenerator;
import bettertapebot.util.DurationFormatter;
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
    BotProperties botProperties;

    @Override
    public @NonNull Command forCommand() {
        return Command.CODE;
    }

    @Override
    @Transactional
    public void handleMessage(@NonNull UserStateEntity userStateEntity, String message) {
        if(!userStateEntity.getUserState().isLoggedIn()){
            responseService.send(userStateEntity.getChatId(), "Nur eingeloggte User können Codes erzeugen");
            return;
        }
        
        var ttlFormatted = DurationFormatter.format(botProperties.getActivationCodeTTL(), botProperties.getActivationCodeFormatLocale());
        var response = String.format("Der aktuelle Freischaltcode lautet: %04d, er ist %s gültig", passcodeGenerator.generatePasscode(), ttlFormatted);
        responseService.send(userStateEntity.getChatId(), response);
    }
}
