package bettertapebot.cache;

import bettertapebot.properties.BotProperties;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig
class TapeCacheTest {
    
    TapeCache tapeCache;
    BotProperties botProperties;
    
    @BeforeEach
    void setup(){
        botProperties = new BotProperties();
        tapeCache = new TapeCache(botProperties);
    }
    
    @Test
    void testPutAndGet(){
        var title = "test";
        var id = 1L;
        Assertions.assertDoesNotThrow(() -> tapeCache.put(id, title));
        var fetch = Assertions.assertDoesNotThrow(() -> tapeCache.get(id));
        assertThat(fetch).isNotNull();
        assertThat(fetch.tapeTitle()).isNotNull().isEqualTo(title);
        assertThat(fetch.dateAdded()).isNotNull().isAfter(Instant.now().minusSeconds(1));
    }
    
    @Test
    void testRemove(){
        var title = "test";
        var id = 1L;
        Assertions.assertDoesNotThrow(() -> tapeCache.put(id, title));
        Assertions.assertDoesNotThrow(() -> tapeCache.remove(id));
        var fetch = Assertions.assertDoesNotThrow(() -> tapeCache.get(id));
        assertThat(fetch).isNull();
    }
    
    @Test
    void testCleanUpDeletesOldData(){
        botProperties.setTapeCacheTTL(Duration.ofMillis(1));
        var title = "test";
        var id = 1L;
        Assertions.assertDoesNotThrow(() -> tapeCache.put(id, title));
        Awaitility.await().pollDelay(Duration.ofMillis(10)).until(() -> true); //wait 10 milliseconds
        tapeCache.cleanOldTapeNames();
        var fetch = Assertions.assertDoesNotThrow(() -> tapeCache.get(id));
        assertThat(fetch).isNull();
    }
    
    @Test
    void testCleanUpPreservesNewData(){
        botProperties.setTapeCacheTTL(Duration.ofMillis(100));
        var title = "test";
        var id = 1L;
        Assertions.assertDoesNotThrow(() -> tapeCache.put(id, title));
        Awaitility.await().pollDelay(Duration.ofMillis(10)).until(() -> true); //wait 10 milliseconds
        tapeCache.cleanOldTapeNames();
        var fetch = Assertions.assertDoesNotThrow(() -> tapeCache.get(id));
        assertThat(fetch).isNotNull();
    }
}