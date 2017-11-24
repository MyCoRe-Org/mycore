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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;

import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * Base entry point for guice injection. Resolves all modules from configuration
 * in MCR.inject.module.XXX = MyGuiceModule.
 * 
 * @author Matthias Eichner
 */
public class MCRInjectorConfig {

    private static final Logger LOGGER = LogManager.getLogger(MCRInjectorConfig.class);

    private static Injector INJECTOR;

    private static List<Module> MODULES;

    static {
        Map<String, String> moduleMap = MCRConfiguration.instance().getPropertiesMap("MCR.inject.module");
        MODULES = Lists.newArrayList();
        moduleMap.keySet().stream().map(propertyName -> {
            return MCRConfiguration.instance().<Module> getInstanceOf(propertyName);
        }).forEach(MODULES::add);
        LOGGER.info("Using guice modules: {}", MODULES);
        INJECTOR = Guice.createInjector(MODULES);
    }

    /**
     * Returns the global injector for mycore.
     * 
     * @return
     */
    public static synchronized Injector injector() {
        return INJECTOR;
    }

    /**
     * Returns a list of all guice modules used by the {@link Injector}.
     * 
     * @return list of guice modules
     */
    public static synchronized List<Module> modules() {
        return MODULES;
    }

}
