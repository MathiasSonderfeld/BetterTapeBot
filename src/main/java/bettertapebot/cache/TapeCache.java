package bettertapebot.cache;


import bettertapebot.properties.BotProperties;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;


@CustomLog
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TapeCache {
    BotProperties botProperties;
    HashMap<Long, TapeCacheEntry> internalStorage = new HashMap<>();
    
    public void put(long chatId, String tapeTitle) {
        internalStorage.put(chatId, new TapeCacheEntry(tapeTitle, Instant.now()));
    }
    
    public TapeCacheEntry get(long chatId) {
        return internalStorage.get(chatId);
    }
    
    public void remove(long chatId) {
        internalStorage.remove(chatId);
    }
    
    
    //check cache every 10 mins for old entries and remove them
    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.MINUTES)
    void cleanOldTapeNames() {
        for (var cacheEntry : internalStorage.entrySet()) {
            var chatId = cacheEntry.getKey();
            var createTime = cacheEntry.getValue().dateAdded();
            var age = Duration.between(createTime, Instant.now());
            //age - ttl > 0 <=> age > ttl
            if(age.minus(botProperties.getTapeCacheTTL()).isPositive()){
                internalStorage.remove(chatId);
            }
        }
    }
}
