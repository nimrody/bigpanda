package blocking;

import async.events.Event;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class StatsProcessor {
    private static final ObjectWriter jsonWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();

    private static class Stats {
        @JsonProperty("event_count")
        private final Map<String, AtomicLong> eventTypeCounts = new HashMap<>();

        @JsonProperty("word_count")
        private final Map<String, AtomicLong> dataWordsCounts = new HashMap<>();
    }

    /**
     * Stats mutex guards both maps and is used to ensure consistent view when serving the current stats.
     * (i.e., the HTTP endpoint will serve all counts as seen at a specific point in time)
     *
     * If consistent view is of no importance we can do without the lock and the synchronized{} block.
     * We would need to replace the above HashMap-s with ConcurrentHashMap or wrap it with
     * Collections.synchronizedMap() since the map is still accessed by multiple threads.
     */
    private final Stats stats = new Stats();

    public void processEvent(Event event) {
        // synchronized is a reentrant mutex and so as long as there is one consumer thread,
        // it will keep holding the lock and won't block here unless the HTTP endpoint is accessed.
        synchronized (stats) {
            stats.eventTypeCounts.computeIfAbsent(event.eventType, unused -> new AtomicLong()).incrementAndGet();
            stats.dataWordsCounts.computeIfAbsent(event.data, unused -> new AtomicLong()).incrementAndGet();
        }
    }

    public String currentStats() {
        try {
            synchronized (stats) {
                return jsonWriter.writeValueAsString(stats);
            }
        } catch (JsonProcessingException e) {
            return "Unexpected error producing stats Json";
        }
    }
}
