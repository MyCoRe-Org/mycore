/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.pi;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.events.MCRStartupHandler;

import jakarta.servlet.ServletContext;

/**
 * Checks deprecated properties and the configuration of {@link MCRPIService}s on startup
 */
public class MCRPIConfigurationChecker implements MCRStartupHandler.AutoExecutable {

    protected static final List<String> DEPRECATED_PROPERTY_PREFIXES = Stream.of("MCR.PI.MetadataManager.",
        "MCR.PI.Inscriber.", "MCR.PI.Registration.").collect(Collectors.toList());

    private static final Logger LOGGER = LogManager.getLogger();

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
        final List<String> deprecatedPropertyList = DEPRECATED_PROPERTY_PREFIXES
            .stream()
            .flatMap(propPrefix -> MCRConfiguration2.getPropertiesMap()
                .entrySet()
                .stream()
                .filter(p -> p.getKey().startsWith(propPrefix))
                .map(Map.Entry::getKey))
            .collect(Collectors.toList());

        if (deprecatedPropertyList.size() > 0) {
            throw new MCRConfigurationException("Deprecated properties found: " + deprecatedPropertyList
                .stream()
                .collect(Collectors.joining(System.lineSeparator())));
        }

        // check service configuration
        final MCRPIServiceManager serviceManager = MCRPIServiceManager.getInstance();
        serviceManager.getServiceList().forEach(service -> {
            LOGGER.info("Check service: " + service.getServiceID());
            service.checkConfiguration();
        });

    }
}
