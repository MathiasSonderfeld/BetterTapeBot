package bettertapebot.handler.loggedin;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.Command;
import bettertapebot.handler.CommandHandler;
import bettertapebot.handler.StateHandler;
import bettertapebot.properties.BotProperties;
import bettertapebot.repository.TapeRepository;
import bettertapebot.repository.UserRepository;
import bettertapebot.repository.UserStateRepository;
import bettertapebot.repository.entity.TapeEntity;
import bettertapebot.repository.entity.UserState;
import bettertapebot.repository.entity.UserStateEntity;
import bettertapebot.util.MessageCleaner;
import bettertapebot.util.TapeFormatter;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@CustomLog
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AddTapeHandler implements CommandHandler, StateHandler {
    
    HashMap<Long, Map.Entry<Instant, String>> tapeCache = new HashMap<>();

    ResponseService responseService;
    UserRepository userRepository;
    TapeRepository tapeRepository;
    BotProperties botProperties;
    UserStateRepository userStateRepository;
    
    //check cache every 10 mins for old entries and remove them
    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.MINUTES)
    void cleanOldTapeNames() {
        for (var cacheEntry : tapeCache.entrySet()) {
            var chatId = cacheEntry.getKey();
            var createTime = cacheEntry.getValue().getKey();
            var age = Duration.between(Instant.now(), createTime);
            //age - ttl > 0 <=> age > ttl
            if(age.minus(botProperties.getTapeCacheTTL()).isPositive()){
                tapeCache.remove(chatId);
            }
        }
    }
    
    @Override
    public @NonNull Command forCommand() {
        return Command.ADD;
    }
    
    @Override
    public @NonNull Set<UserState> forStates() {
        return Set.of(UserState.ADD_TAPE_GET_TITLE, UserState.ADD_TAPE_GET_STAR);
    }

    @Override
    @Transactional
    public void handleMessage(@NonNull UserStateEntity userStateEntity, String message) {
        if (!userStateEntity.getUserState().isLoggedIn()) {
            responseService.send(userStateEntity.getChatId(), "Nur eingeloggte User können Tapes eintragen");
            return;
        }
        
        if(userStateEntity.getUserState() == UserState.ADD_TAPE_GET_STAR){
            handleStar(userStateEntity, message);
            return;
        }
        
        if(!StringUtils.hasText(message)){
            userStateEntity.setUserState(UserState.ADD_TAPE_GET_TITLE);
            responseService.send(userStateEntity.getChatId(), "Welchen Titel soll das Werk tragen?");
            return;
        }
        tapeCache.put(userStateEntity.getChatId(), Map.entry(Instant.now(), message));
        userStateEntity.setUserState(UserState.ADD_TAPE_GET_STAR);
        responseService.send(userStateEntity.getChatId(), "Wer ist der Star dieses Meisterwerks?");
    }
    
    private void handleStar(UserStateEntity userStateEntity, String message) {
        var username = MessageCleaner.getFirstWord(message);
        var starOptional = userRepository.findById(username);
        if(starOptional.isEmpty()){
            responseService.send(userStateEntity.getChatId(), "Den Benutzer gibt es nicht. Probiers nochmal");
            return;
        }
        var star = starOptional.get();
        var director = userStateEntity.getOwner();
        var tapeCacheEntry = tapeCache.get(userStateEntity.getChatId());
        tapeCache.remove(userStateEntity.getChatId());
        
        var tapeEntity = tapeRepository.save(TapeEntity.builder()
            .title(tapeCacheEntry.getValue())
            .star(star)
            .director(director)
            .dateAdded(tapeCacheEntry.getKey())
            .build());
        
        userStateEntity.setUserState(UserState.LOGGED_IN);
        var formattedTape = TapeFormatter.formatTape(tapeEntity, false);
        var activeIds = userStateRepository.findAllByUserStateIsInAndOwner_WantsAbonnement(UserState.LOGGED_IN_STATES, true)
            .stream()
            .map(UserStateEntity::getChatId)
            .toList();
        responseService.broadcast(activeIds, formattedTape);
    }
}
