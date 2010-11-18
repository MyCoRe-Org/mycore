package org.mycore.frontend.editor.validation;

import java.util.Properties;

import org.apache.log4j.Logger;

public abstract class MCRValidatorBase implements MCRValidator {

    private final static Logger LOGGER = Logger.getLogger(MCRValidatorBase.class);

    private Properties properties = new Properties();

    public void setProperty(String name, String value) {
        properties.setProperty(name, value);
    }

    public String getProperty(String name) {
        return properties.getProperty(name);
    }

    protected boolean hasProperty(String name) {
        return properties.containsKey(name);
    }

    public abstract boolean hasRequiredProperties();

    public boolean isValid(Object... input) {
        boolean isValid;

        try {
            isValid = isValidOrDie(input);
        } catch (Exception ex) {
            logException(ex);
            isValid = false;
        }

        if (LOGGER.isDebugEnabled())
            LOGGER.debug(buildLogInfo(isValid, input));

        return isValid;
    }

    protected abstract boolean isValidOrDie(Object... input) throws Exception;

    private void logException(Throwable ex) {
        LOGGER.debug(getClass().getSimpleName() + " caught " + ex.toString());
        ex = ex.getCause();
        if (ex != null)
            logException(ex);
    }

    private String buildLogInfo(boolean isValid, Object... input) {
        StringBuffer buffer = new StringBuffer();

        buffer.append(getClass().getSimpleName());
        buffer.append(" validation ");
        buffer.append(isValid ? "successful" : "failed");
        buffer.append(": ");

        if (input != null)
            for (Object value : input)
                buffer.append("\"").append(value).append("\" ");

        if (!properties.isEmpty())
            for (Object name : properties.keySet())
                buffer.append(name).append("=\"").append(properties.get(name)).append("\" ");

        return buffer.toString();
    }
}
