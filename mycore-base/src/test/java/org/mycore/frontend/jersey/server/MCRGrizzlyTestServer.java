package org.mycore.frontend.jersey.server;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.UriBuilder;

import org.apache.log4j.Logger;

import com.sun.grizzly.http.SelectorThread;
import com.sun.jersey.api.container.grizzly.GrizzlyWebContainerFactory;

public class MCRGrizzlyTestServer {

    static Logger LOGGER = Logger.getLogger(MCRGrizzlyTestServer.class.getName());

    public URI BASE_URI;

    private SelectorThread threadSelector;

    private String packageName;

    private int getPort(int defaultPort) {
        String port = System.getenv("JERSEY_HTTP_PORT");
        if (null != port)
            try {
                return Integer.parseInt(port);
            } catch (NumberFormatException e) {
            }
        return defaultPort;
    }

    public URI getBaseURI() {
        if (this.BASE_URI == null) {
            this.BASE_URI = UriBuilder.fromUri("http://localhost/").port(getPort(9998)).build(new Object[0]);
        }
        return this.BASE_URI;
    }

    public MCRGrizzlyTestServer(String packageName) {
        this.packageName = packageName;
    }

    public void start() {
        Map<String, String> initParams = new HashMap<String, String>();

        initParams.put("com.sun.jersey.config.property.packages", this.packageName);
        initParams.put("com.sun.jersey.config.feature.DisableXmlSecurity", "true");

        LOGGER.info("Starting grizzly...");
        try {
            this.threadSelector = GrizzlyWebContainerFactory.create(getBaseURI(), initParams);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if (this.threadSelector != null) {
            LOGGER.info("Stopping grizzly...");
            this.threadSelector.stopEndpoint();
            LOGGER.info("grizzly... stopped");
        }
    }
}
