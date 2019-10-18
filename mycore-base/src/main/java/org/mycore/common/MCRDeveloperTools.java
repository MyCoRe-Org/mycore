package org.mycore.common;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;

/**
 * @author Sebastian Hofmann
 */
public class MCRDeveloperTools {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * @return true if any override is defined
     */
    public static boolean overrideActive() {
        return MCRConfiguration.instance().getString("MCR.Developer.Resource.Override", null) != null;
    }

    /**
     * Reads the property <code>MCR.Developer.Resource.Override</code> and checks if any of the containing paths
     * contains the path parameter.
     * @param path the resource to override
     * @param webResource if true META-INF/resources will be appended to the paths in the property
     * @return the path to new file
     */
    public static Optional<Path> getOverriddenFilePath(String path, boolean webResource) {
        if (overrideActive()) {
            final String[] pathParts = path.split("/");

            return MCRConfiguration.instance()
                .getStrings("MCR.Developer.Resource.Override")
                .stream()
                .map(Paths::get)
                .map(p -> webResource ? p.resolve("META-INF").resolve("resources") : p)
                .map(p -> {
                    for (String part : pathParts) {
                        p = p.resolve(part);
                    }
                    return p;
                })
                .filter(Files::exists)
                .peek(p -> LOGGER.debug("Found overridden Resource: {}", p.toAbsolutePath().toString()))
                .findFirst();
        }
        return Optional.empty();
    }
}
