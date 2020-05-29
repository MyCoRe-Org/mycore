package org.mycore.pi.condition;

import java.util.Map;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;

public abstract class MCRPIPredicateBase implements MCRPIPredicate {

    private final String propertyPrefix;

    public MCRPIPredicateBase(String propertyPrefix) {
        this.propertyPrefix = propertyPrefix;
    }

    public String getPropertyPrefix() {
        return propertyPrefix;
    }

    @Override
    public Map<String, String> getProperties() {
        return MCRConfiguration2.getSubPropertiesMap(propertyPrefix);
    }

    protected String requireProperty(String key) {
        final Map<String, String> properties = getProperties();
        if (!properties.containsKey(key)) {
            throw new MCRConfigurationException(getPropertyPrefix() + key + " ist not defined!");
        }
        return properties.get(key);
    }

}
