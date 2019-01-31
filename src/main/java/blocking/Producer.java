package blocking;

import async.events.Event;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
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
    private static final ObjectMapper jsonMapper = new ObjectMapper();
    public static final String EXTERNAL_PRODUCER_EXECUTABLE_PATH = "vendor/generator-linux-amd64";

    private final BlockingQueue<Event> outputQueue;
    private int eventsLost = 0;

    public Producer(BlockingQueue<Event> outputQueue) {
        this.outputQueue = outputQueue;
    }

    public void start() {
        new Thread(this::workerThread).start();
    }

    private void workerThread() {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process process = runtime.exec(EXTERNAL_PRODUCER_EXECUTABLE_PATH);
            try (BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                for (; ; ) {
                    String line = stdout.readLine();
                    try {
                        Event event = jsonMapper.readValue(line, Event.class);

                        // if we use outputQueue.put() here we can block the external process
                        // (effectively applying back-pressure). We choose to drop events instead
                        // since the external process behavior in this case was not defined.
                        if (!outputQueue.offer(event)) {
                            eventsLost++;
                            logger.log(Level.WARNING, "Producer output queue full. Total events lost so far " + eventsLost);
                        }
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "failed to parse " + line);
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
