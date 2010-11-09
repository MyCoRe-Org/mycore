package org.mycore.frontend.editor.validation;

import java.util.Properties;

public abstract class MCRValidator {

    private Properties properties = new Properties();

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperty(String name, String value) {
        properties.setProperty(name, value);
    }

    public String getProperty(String name) {
        return properties.getProperty(name);
    }

    public boolean hasProperty(String name) {
        return properties.containsKey(name);
    }

    public abstract boolean hasRequiredPropertiesForValidation();

    public abstract boolean isValid(String input) throws Exception;

    public boolean isValidExceptionsCatched(String input) {
        try {
            return isValid(input);
        } catch (Exception ex) {
            return false;
        }
    }
}
