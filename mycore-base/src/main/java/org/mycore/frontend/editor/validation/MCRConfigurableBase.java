package org.mycore.frontend.editor.validation;

import java.util.Properties;

public abstract class MCRConfigurableBase implements MCRConfigurable {

    private Properties properties = new Properties();

    public void setProperty(String name, String value) {
        properties.setProperty(name, value);
    }

    protected String getProperty(String name) {
        return properties.getProperty(name);
    }

    protected boolean hasProperty(String name) {
        return properties.containsKey(name);
    }

    public abstract boolean hasRequiredProperties();

}