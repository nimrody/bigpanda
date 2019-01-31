package async.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.EncodeException;
import io.vertx.core.json.Json;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class StatsProcessor implements Handler<Message<String>> {
    static class Stats {
        @JsonProperty("event_count")
        final Map<String, AtomicLong> eventTypeCounts = new HashMap<>();

        @JsonProperty("word_count")
        final Map<String, AtomicLong> dataWordsCounts = new HashMap<>();

        @JsonProperty("malformed_messages_count")
        AtomicLong malformedMessagesCount = new AtomicLong();
    }

    /**
     * Stats mutex guards both maps and is used to ensure consistent view when serving the current stats.
     * (i.e., the HTTP endpoint will serve all counts as seen at a specific point in time)
     *
     * If consistent view is of no importance we can do without the lock and the synchronized{} block.
     * We would need to replace the above HashMap-s with ConcurrentHashMap since the map is still
     * accessed from multiple threads.
     *
     * Another option would be to call pause() on MessageConsumer-s
     */
    private final Stats stats = new Stats();

    @Override
    public void handle(Message<String> message) {
        try {
            Event event = Json.decodeValue(message.body(), Event.class);

            synchronized (stats) {
                stats.eventTypeCounts.computeIfAbsent(event.eventType, unused -> new AtomicLong()).incrementAndGet();
                stats.dataWordsCounts.computeIfAbsent(event.data, unused -> new AtomicLong()).incrementAndGet();
            }

        } catch (DecodeException e) {
            stats.malformedMessagesCount.incrementAndGet();
        }
    }

    public String currentStats() {
        try {
            synchronized (stats) {
                return Json.encodePrettily(stats);
            }
        } catch (EncodeException e) {
            return "Unexpected error producing stats Json";
        }
    }
}
