package async.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class Event {
    public final String eventType;
    public final String data;
    public final long timestamp;

    public Event(@JsonProperty("event_type") String eventType,
                 @JsonProperty("data") String data,
                 @JsonProperty("timestamp") long timestamp) {
        this.eventType = eventType;
        this.data = data;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Event{" + eventType + ":'"  + data + "' " + Instant.ofEpochSecond(timestamp) + '}';
    }
}
