package org.mycore.iiif.common;

import javax.ws.rs.ApplicationPath;

import org.apache.logging.log4j.LogManager;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.restapi.MCRJerseyRestApp;

/**
 * @author Sebastian Hofmann
 */
@ApplicationPath("/api/iiif")
public class MCRIIIFApp extends MCRJerseyRestApp {

    public MCRIIIFApp() {
        super();
        register(MCRIIIFBaseURLFilter.class);
    }

    @Override
    protected void initAppName() {
        setApplicationName("MyCoRe IIIF-API " + getVersion());
        LogManager.getLogger().info("Initiialize {}", getApplicationName());
    }

    @Override
    protected String getVersion() {
        return "2.0";
    }

    @Override
    protected String[] getRestPackages() {
        return MCRConfiguration2.getOrThrow("MCR.IIIF.API.Resource.Packages", MCRConfiguration2::splitValue)
            .toArray(String[]::new);
    }
}
