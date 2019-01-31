package async.events;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server serves the current stats as a json document
 * For simplicity, no router is defined and the HTTP path is ignored
 */

public class Server {
    private static final Logger logger = Logger.getLogger(Server.class.getSimpleName());
    private final Vertx vertx;
    private final int serverPort;
    private final StatsProcessor stats;

    public Server(Vertx vertx, int serverPort, StatsProcessor stats) {
        this.vertx = vertx;
        this.serverPort = serverPort;
        this.stats = stats;
    }

    public void start() {
        HttpServer server = vertx.createHttpServer().requestHandler(req -> {
            // We can check here req.path() if we need to serve multiple endpoints
            req.response()
                    .putHeader("content-type", "application/json")
                    .end(stats.currentStats());
        });

        // Now bind the server:
        server.listen(serverPort);
        logger.log(Level.INFO, "HTTP server started on " + serverPort);
    }
}
