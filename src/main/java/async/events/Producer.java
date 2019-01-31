package async.events;

import io.vertx.core.eventbus.MessageProducer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Producer runs an external process generating events.
 * Each event is a newline terminated json fields matching Event class
 * Lines containing malformed json will be discarded.
 *
 * Producer deserializes events into an event queue which
 * may be consumed asynchronously by another thread.
 *
 * In order not to block the generating process (which may cause events to
 * be lost if that process internally has no back-pressure mechanism), we choose
 * to drop events if the queue is full.
 */
public class Producer {
    private static final Logger logger = Logger.getLogger(Producer.class.getSimpleName());
    public static final String EXTERNAL_PRODUCER_EXECUTABLE_PATH = "vendor/generator-linux-amd64";

    private final MessageProducer<String> producer;

    public Producer(MessageProducer<String> producer) {
        this.producer = producer;
    }

    public Future<?> start() {
        return Executors.newSingleThreadExecutor().submit(this::workerThread);
    }

    private void workerThread() {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process process = runtime.exec(EXTERNAL_PRODUCER_EXECUTABLE_PATH);
            try (BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                for (; ; ) {
                    String line = stdout.readLine();
                    if (!producer.writeQueueFull()) {
                        producer.write(line);
                    } else {
                        // when consumers cannot keep up, we choose to drop the messages in order not to block the source
                        logger.log(Level.SEVERE, "Output queue full, dropping message");
                    }
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to execute subprocess or subprocess terminated unexpectedly", e);
        }

        logger.log(Level.SEVERE, "Terminating producer worker thread.");
        System.exit(1);
    }
}
