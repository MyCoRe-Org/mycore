package org.mycore.frontend.editor.validation;

import java.util.Properties;

public abstract class MCRValidatorBase implements MCRValidator {

    private Properties properties = new Properties();

    public void setProperty(String name, String value) {
        properties.setProperty(name, value);
    }

    public String getProperty(String name) {
        return properties.getProperty(name);
    }

    protected Properties getProperties() {
        return properties;
    }

    public boolean hasProperty(String name) {
        return properties.containsKey(name);
    }

    public abstract boolean hasRequiredProperties();

    public boolean isValid(Object... input) {
        try {
            return isValidOrDie(input);
        } catch (Exception ex) {
            return false;
        }
    }

    protected abstract boolean isValidOrDie(Object... input) throws Exception;
}
