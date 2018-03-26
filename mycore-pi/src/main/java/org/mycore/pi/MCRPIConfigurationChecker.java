package org.mycore.pi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.events.MCRStartupHandler;

import javax.servlet.ServletContext;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Checks deprecated properties and the configuration of {@link MCRPIService}s on startup
 */
public class MCRPIConfigurationChecker implements MCRStartupHandler.AutoExecutable {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static final List<String> DEPRECATED_PROPERTY_PREFIXES = Stream.of("MCR.PI.MetadataManager.",
            "MCR.PI.Inscriber.", "MCR.PI.Registration.").collect(Collectors.toList());

    @Override
    public String getName() {
        return "ConfigurationChecker";
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void startUp(ServletContext servletContext) {
        LOGGER.info("Check persistent identifier configuration!");
        final List<String> deprecatedPropertyList = Collections.synchronizedList(new ArrayList<>());
        DEPRECATED_PROPERTY_PREFIXES
                .forEach(propPrefix -> {
                    Map<String, String> map = MCRConfiguration.instance().getPropertiesMap(propPrefix);
                    deprecatedPropertyList.addAll(map.keySet());
                });

        if (deprecatedPropertyList.size() > 0) {
            throw new MCRConfigurationException("Deprecated properties found: " + deprecatedPropertyList
                    .stream()
                    .collect(Collectors.joining(System.lineSeparator())));
        }

        // check service configuration
        final MCRPIServiceManager serviceManager = MCRPIServiceManager.getInstance();
        serviceManager.getServiceList().forEach(service->{
            LOGGER.info("Check service: " + service.getServiceID());
            service.checkConfiguration();
        });

    }
}
