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

package org.mycore.common.events;

import java.util.Collections;

import javax.servlet.ServletContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationDirSetup;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.MCRRuntimeComponentDetector;
import org.mycore.common.xml.MCRURIResolver;

/**
 * Initializes classes that implement {@link AutoExecutable} interface that are defined via
 * <code>MCR.Startup.Class</code> property.
 *
 * @author Thomas Scheffler (yagee)
 */
public class MCRStartupHandler {

    /**
     * Can set <code>true</code> or <code>false</code> as {@link ServletContext#setAttribute(String, Object)} to skip
     * errors on startup.
     */
    public static final String HALT_ON_ERROR = "MCR.Startup.haltOnError";

    private static final Logger LOGGER = LogManager.getLogger();

    public interface AutoExecutable {
        /**
         * returns a name to display on start-up.
         */
        String getName();

        /**
         * If order is important returns as 'heigher' priority.
         */
        int getPriority();

        /**
         * This method get executed by {@link MCRStartupHandler#startUp(ServletContext)}
         */
        void startUp(ServletContext servletContext);
    }

    public static void startUp(ServletContext servletContext) {
        //setup configuration
        MCRConfigurationDirSetup dirSetup = new MCRConfigurationDirSetup();
        dirSetup.startUp(servletContext);
        LOGGER.info("I have these components for you: {}", MCRRuntimeComponentDetector.getAllComponents());
        LOGGER.info("I have these mycore components for you: {}", MCRRuntimeComponentDetector.getMyCoReComponents());
        LOGGER.info("I have these app modules for you: {}", MCRRuntimeComponentDetector.getApplicationModules());
        if (servletContext != null) {
            LOGGER.info("Library order: {}", servletContext.getAttribute(ServletContext.ORDERED_LIBS));
        }

        MCRConfiguration.instance().getStrings("MCR.Startup.Class", Collections.emptyList()).stream()
            .map(MCRStartupHandler::getAutoExecutable)
            //reverse ordering: highest priority first
            .sorted((o1, o2) -> Integer.compare(o2.getPriority(), o1.getPriority()))
            .forEachOrdered(autoExecutable -> startExecutable(servletContext, autoExecutable));
        //initialize MCRURIResolver
        MCRURIResolver.init(servletContext);
    }

    private static void startExecutable(ServletContext servletContext, AutoExecutable autoExecutable) {
        LOGGER.info("{}: Starting {}", autoExecutable.getPriority(), autoExecutable.getName());
        try {
            autoExecutable.startUp(servletContext);
        } catch (ExceptionInInitializerError | RuntimeException e) {
            boolean haltOnError = servletContext == null || servletContext.getAttribute(HALT_ON_ERROR) == null
                || Boolean.parseBoolean((String) servletContext.getAttribute(HALT_ON_ERROR));

            if (haltOnError) {
                throw e;
            }
            LOGGER.warn(e.toString());
        }
    }

    private static AutoExecutable getAutoExecutable(String className) {
        try {
            return (AutoExecutable) Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new MCRConfigurationException("Could not initialize 'MCR.Startup.Class': " + className, e);
        }
    }
}
