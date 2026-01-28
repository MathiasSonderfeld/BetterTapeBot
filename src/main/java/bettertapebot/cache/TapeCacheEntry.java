package bettertapebot.cache;

import java.time.Instant;

public record TapeCacheEntry(String tapeTitle, Instant dateAdded){}
