package blocking;

import async.events.Event;

import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Consumer consumes events from 'inputQueue' and updates 'stats' and
 * never terminates since Producer produces infinite events (i.e., no need to flush events
 * when the producer terminates)
 */
public class Consumer {
    private static final Logger logger = Logger.getLogger(Consumer.class.getSimpleName());

    private final BlockingQueue<Event> inputQueue;
    private final StatsProcessor stats;

    public Consumer(BlockingQueue<Event> inputQueue, StatsProcessor stats) {
        this.inputQueue = inputQueue;
        this.stats = stats;
    }

    public void start() {
        new Thread(this::workerThread).start();
    }

    private void workerThread() {
        Event event;
        for (; ; ) {
            try {
                event = inputQueue.take();
                stats.processEvent(event);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "unexpected", e);
            }
        }

    }
}
