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

package org.mycore.frontend.jersey;

import org.apache.logging.log4j.LogManager;
import org.glassfish.jersey.server.ResourceConfig;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;

/**
 * Entry point for mycore jersey configuration. Loads the {@link MCRJerseyConfiguration} defined in
 * 'MCR.Jersey.Configuration' or the default {@link MCRJerseyDefaultConfiguration}.
 * 
 * @author Matthias Eichner
 */
public class MCRJerseyResourceConfig extends ResourceConfig {

    public MCRJerseyResourceConfig() {
        super();
        LogManager.getLogger().info("Loading jersey resource config...");
        MCRJerseyConfiguration configuration;
        try {
            configuration = MCRConfiguration.instance().getInstanceOf("MCR.Jersey.Configuration",
                new MCRJerseyDefaultConfiguration());
        } catch (MCRConfigurationException exc) {
            LogManager.getLogger().error("Unable to initialize jersey.", exc);
            return;
        }
        try {
            configuration.configure(this);
        } catch (Exception exc) {
            LogManager.getLogger().error("Unable to configure jersey.", exc);
        }
    }

}
