package blocking;

import async.events.Event;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Main {
    public static final int SERVER_PORT = 8080;
    public static final int QUEUE_SIZE = 10_000;

    public static void main(String[] args) {

        StatsProcessor stats = new StatsProcessor();
        BlockingQueue<Event> queue = new LinkedBlockingQueue<>(QUEUE_SIZE);

        Server server = new Server(SERVER_PORT, stats);
        server.start();

        Producer producer = new Producer(queue);
        // we currently run a single consumer but if StatsProcessor becomes more computationally demanding, we can
        // start multiple consumers (as long as StatsProcessor is CPU-bound, it only makes sense to start as many
        // consumers as the number of processors)
        Consumer consumer = new Consumer(queue, stats);

        consumer.start();
        producer.start();

        // we never terminate since there are multiple non-daemon threads running (producer / consumer worker threads)
    }
}
