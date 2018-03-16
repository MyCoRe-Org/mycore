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

package org.mycore.common.inject;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * Base entry point for guice injection. Resolves all modules from configuration
 * in MCR.Inject.Module.XXX = MyGuiceModule.
 * 
 * @author Matthias Eichner
 */
public class MCRInjectorConfig {

    private static final Logger LOGGER = LogManager.getLogger(MCRInjectorConfig.class);

    private static Injector INJECTOR;

    private static List<Module> MODULES;

    static {
        MODULES = MCRConfiguration2.getPropertiesMap()
            .entrySet()
            .stream()
            .filter(e -> e.getKey().startsWith("MCR.Inject.Module."))
            .map(Map.Entry::getValue)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(MCRInjectorConfig::instantiateModule)
            .collect(Collectors.toList());
        LOGGER.info("Using guice modules: {}", MODULES);
        INJECTOR = Guice.createInjector(MODULES);
    }

    /**
     * Returns the global injector for mycore.
     * 
     * @return returns the guice injector
     */
    public static synchronized Injector injector() {
        return INJECTOR;
    }

    /**
     * Returns a list of all guice modules used by the {@link Injector}. Be aware that this list immutable and changes
     * are not reflect on the injector.
     * 
     * @return list of guice modules
     */
    public static synchronized List<Module> modules() {
        return MODULES;
    }

    private static Module instantiateModule(String classname) {
        LogManager.getLogger().debug("Loading Guice Module: {}", classname);
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Module> forName = (Class<? extends Module>) Class.forName(classname);
            return forName.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new MCRConfigurationException("Could not instantiate Guice Module " + classname, e);
        }
    }

}
