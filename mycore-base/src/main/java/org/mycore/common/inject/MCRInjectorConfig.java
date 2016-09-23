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

    private final static Logger LOGGER = LogManager.getLogger(MCRInjectorConfig.class);

    private static Injector INJECTOR;

    private static List<Module> MODULES;

    static {
        Map<String, String> moduleMap = MCRConfiguration.instance().getPropertiesMap("MCR.inject.module");
        MODULES = Lists.newArrayList();
        moduleMap.keySet().stream().map(propertyName -> {
            Module module = MCRConfiguration.instance().getInstanceOf(propertyName);
            return module;
        }).forEach(MODULES::add);
        LOGGER.info("Using guice modules: " + MODULES);
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
