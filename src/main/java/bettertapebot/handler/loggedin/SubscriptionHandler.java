package bettertapebot.handler.loggedin;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.Command;
import bettertapebot.handler.CommandHandler;
import bettertapebot.handler.StateHandler;
import bettertapebot.properties.BotProperties;
import bettertapebot.repository.entity.UserState;
import bettertapebot.repository.entity.UserStateEntity;
import bettertapebot.util.MessageCleaner;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.Set;

@CustomLog
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubscriptionHandler implements CommandHandler, StateHandler {

    ResponseService responseService;
    BotProperties botProperties;
    
    @NonFinal
    ReplyKeyboardMarkup markup;
    
    @PostConstruct
    void buildKeyboard() {
        var yes = botProperties.getSubscription().getCountsAsYes().getFirst();
        var no = botProperties.getSubscription().getCountsAsNo().getFirst();
        markup = ReplyKeyboardMarkup.builder()
            .keyboardRow(new KeyboardRow(yes, no))
            .oneTimeKeyboard(true)
            .build();
    }

    @Override
    public @NonNull Command forCommand() {
        return Command.SUBSCRIPTION;
    }
    
    @Override
    public @NonNull Set<UserState> forStates() {
        return Set.of(UserState.SUBSCRIPTION_AWAITING_VALUE);
    }

    @Override
    @Transactional
    public void handleMessage(@NonNull UserStateEntity userStateEntity, long chatId, String message) {
        if (!userStateEntity.getUserState().isLoggedIn()) {
            responseService.send(chatId, "Nur eingeloggte User können ihren Updates-Status ändern");
            return;
        }
        
        if(!StringUtils.hasText(message)){
            userStateEntity.setUserState(UserState.SUBSCRIPTION_AWAITING_VALUE);
            responseService.send(userStateEntity.getChatId(), markup, "Möchtest du weiterhin Updates bekommen?");
            return;
        }
        
        var wantedStatus = MessageCleaner.getFirstWord(message);
        Boolean determinedStatus = botProperties.getSubscription().interpretStatus(wantedStatus);
        if(determinedStatus == null){
            userStateEntity.setUserState(UserState.SUBSCRIPTION_AWAITING_VALUE);
            responseService.send(userStateEntity.getChatId(), markup, "Das konnte ich nicht interpretieren. Möchtest du weiterhin Updates bekommen?");
            return;
        }
        
        userStateEntity.getOwner().setWantsAbonnement(determinedStatus);
        userStateEntity.setUserState(UserState.LOGGED_IN);
        if(determinedStatus){
            responseService.send(userStateEntity.getChatId(), "Updates sind aktiviert");
        }
        else {
            responseService.send(userStateEntity.getChatId(), "Updates sind deaktiviert");
        }
    }
}
