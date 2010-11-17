package org.mycore.frontend.editor.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

public abstract class MCRCombinedValidatorBase extends MCRConfigurableBase {

    private final static Logger LOGGER = Logger.getLogger(MCRCombinedValidatorBase.class);

    protected List<MCRConfigurable> validators = new ArrayList<MCRConfigurable>();

    public void setProperty(String name, String value) {
        super.setProperty(name, value);
        for (MCRConfigurable validator : validators)
            validator.setProperty(name, value);
    }

    public boolean hasRequiredProperties() {
        for (MCRConfigurable validator : validators) {
            if (validator.hasRequiredProperties())
                return true;
        }
        return false;
    }

    protected void reportValidationResult(MCRConfigurable validator, boolean success, String... input) {
        if (!LOGGER.isDebugEnabled())
            return;

        StringBuffer buffer = new StringBuffer();

        buffer.append(validator.getClass().getSimpleName());
        buffer.append(" validation ");
        buffer.append(success ? "succesful" : "failed");
        buffer.append(": ");

        for (String s : input)
            buffer.append("\"").append(s).append("\" ");

        Properties properties = getProperties();
        if (!properties.isEmpty())
            for (Object name : properties.keySet())
                buffer.append(name).append("=\"").append(properties.get(name)).append("\" ");

        LOGGER.debug(buffer.toString());
    }
}