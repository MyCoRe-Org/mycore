/**
 * 
 */
package org.mycore.common.log4j2.lookups;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.mycore.common.config.MCRConfiguration;

/**
 * Lookup a value in {@link MCRConfiguration}. Uses <code>key</code> as property key.
 * 
 * @author Thomas Scheffler
 */
@Plugin(
    name = "mcrcfg", category = StrLookup.CATEGORY)
public class MCRConfigurationLookup implements StrLookup {

    @Override
    public String lookup(String key) {
        String value = MCRConfiguration.instance().getString(key, null);
        return value;
    }

    @Override
    public String lookup(LogEvent event, String key) {
        return lookup(key);
    }

}
