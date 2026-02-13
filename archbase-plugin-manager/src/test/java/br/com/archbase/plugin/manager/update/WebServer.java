package br.com.archbase.plugin.manager.update;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.ResourceFactory;


public class WebServer {

    private static final int DEFAULT_PORT = 8081;
    private static final String DEFAULT_RESOURCE_BASE = "./downloads/";

    private int port = DEFAULT_PORT;
    private String resourceBase = DEFAULT_RESOURCE_BASE;
    private Server server;

    public static void main(String[] args) {
        try {
            new WebServer().start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getPort() {
        return port;
    }

    public WebServer setPort(int port) {
        this.port = port;

        return this;
    }

    public String getResourceBase() {
        return resourceBase;
    }

    public WebServer setResourceBase(String resourceBase) {
        this.resourceBase = resourceBase;

        return this;
    }

    public void start() throws Exception {
        server = new Server(port);
        server.setStopAtShutdown(true);

        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setBaseResource(ResourceFactory.root().newResource(resourceBase));
        resourceHandler.setDirAllowed(true);

        server.setHandler(resourceHandler);

        server.start();
    }

    public void shutdown() {
        if (server != null && server.isRunning()) {
            try {
                server.stop();
            } catch (Exception e) {
                throw new RuntimeException("Falha ao parar servidor", e);
            }
        }
    }

}
