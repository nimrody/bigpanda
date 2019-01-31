package async.events;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.json.Json;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class TestStatsProcessor {
    private static final String ADDRESS = "consumer";

    private Vertx vertx = Vertx.vertx();
    private StatsProcessor statsProcessor = new StatsProcessor();

    @Test
    public void testCounters() throws Exception {
        vertx.eventBus().consumer(ADDRESS, statsProcessor);

        MessageProducer<String> producer = vertx.eventBus().publisher(ADDRESS);
        Arrays.asList(
                "{ 'event_type': 'foo', 'data': 'lorem', 'timestamp': 1548788801 }",
                "{ 'event_type': 'bar', 'data': 'sit', 'timestamp': 1548788801 }",
                "{ 'event_type': 'bar', 'data': 'ipsum', 'timestamp': 1548788804 }",
                "{ 'event_type': 'foo', 'data': 'dolor', 'timestamp': 1548788804 }")
                .forEach(line -> producer.write(line.replace("\'", "\"")));

        waitForAllEventsToBeHandled();

        StatsProcessor.Stats stats = Json.decodeValue(statsProcessor.currentStats(), StatsProcessor.Stats.class);
        Assert.assertEquals(0, stats.malformedMessagesCount.get());
        Assert.assertEquals(2, stats.eventTypeCounts.get("bar").get());
        Assert.assertEquals(2, stats.eventTypeCounts.get("foo").get());
        Assert.assertEquals(1, stats.dataWordsCounts.get("dolor").get());
        Assert.assertEquals(1, stats.dataWordsCounts.get("lorem").get());
    }

    @Test
    public void testInvalidLines() throws Exception {
        vertx.eventBus().consumer(ADDRESS, statsProcessor);
        MessageProducer<String> producer = vertx.eventBus().publisher(ADDRESS);
        Arrays.asList(
                "{ 'event_type': 'foo', 'data': 'lorem', 'timestamp': 1548788801 }",
                "xxxxxxxxxxxxx",
                "{ 'event_type': 'bar', 'data': 'ipsum', 'timestamp': 1548788804 }",
                "{ 'event_type': 'foo', 'data': 'dolor', 'timestamp': 1548788804 }")
                .forEach(line -> producer.write(line.replace("\'", "\"")));

        waitForAllEventsToBeHandled();

        StatsProcessor.Stats stats = Json.decodeValue(statsProcessor.currentStats(), StatsProcessor.Stats.class);
        Assert.assertEquals(1, stats.malformedMessagesCount.get());
        Assert.assertEquals(1, stats.eventTypeCounts.get("bar").get());
        Assert.assertEquals(2, stats.eventTypeCounts.get("foo").get());
    }

    private void waitForAllEventsToBeHandled() throws InterruptedException {
        // Doesn't seem like Vertx has any method for waiting for the event loop to clear
        // its queue. Vertx.close(handler) closes event processing immediately before deliverying
        // pending messages.
        Thread.sleep(1000);
    }
}
