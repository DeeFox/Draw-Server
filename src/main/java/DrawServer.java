package main.java;

import main.java.db.DrawDB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.ServerContainer;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

/**
 * Created by DFX on 09.05.2016.
 */

public class DrawServer {
    static final Logger logger = LogManager.getLogger(DrawServer.class);

    public static void main(String[] args) {
        DrawServer server = new DrawServer();
        server.start();
    }

    public DrawServer() {

    }

    public void start() {
        DrawDB db = new DrawDB();
        boolean dbRunning = db.init();
        if(!dbRunning) {
            logger.fatal("Error starting the DrawDB!");
            System.exit(0);
        }

        int port = 8080;
        try {
            port = Integer.valueOf(System.getenv("PORT"));
        } catch(Exception e) {
            logger.info("No port specified, defaulting!");
        }
        logger.info("Starting Server on Port " + port);

        Server server = new Server(port);
        ServerConnector connector = new ServerConnector(server);
        connector.setIdleTimeout(1000 * 60);

        server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        try
        {
            ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext(context);

            // Add WebSocket endpoint to javax.websocket layer
            wscontainer.addEndpoint(DrawServerEndpoint.class);
            wscontainer.setDefaultMaxTextMessageBufferSize(1024 * 1024);

            server.start();
            //server.dump(System.err);
            server.join();
        }
        catch (Throwable t)
        {
            logger.fatal("Failed starting server!", t);
            System.exit(0);
        }
    }
}
