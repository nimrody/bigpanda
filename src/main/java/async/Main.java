package async;

import io.vertx.core.Vertx;
import async.events.Producer;
import async.events.Server;
import async.events.StatsProcessor;

/**
 * Spawn an external process (Producer) supplying newline terminated records.
 * Subscribes a StatsProcessor consumer to parse the lines and update
 * histograms of event types and data words.
 *
 * An HTTP endpoint at :8080 returns the accumulated histograms as a single Json object.
 */
public class Main {
    private static final int SERVER_PORT = 8080;
    private static final String CONSUMERS_ADDRESS = "event_consumers";

    public static void main(String[] args) throws Exception {
        StatsProcessor statsProcessor = new StatsProcessor();
        Vertx vertx = Vertx.vertx();

        Server server = new Server(vertx, SERVER_PORT, statsProcessor);
        server.start();

        Producer producer = new Producer(vertx.eventBus().publisher(CONSUMERS_ADDRESS));
        vertx.eventBus().consumer(CONSUMERS_ADDRESS, statsProcessor);

        // the producer only terminates if the external process dies
        producer.start()
                .get();
    }
}
