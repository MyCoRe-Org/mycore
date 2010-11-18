package org.mycore.frontend.editor.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

public class MCRCombinedValidator extends MCRValidatorBase implements MCRValidator {

    private final static Logger LOGGER = Logger.getLogger(MCRCombinedValidator.class);

    protected List<MCRValidator> validators = new ArrayList<MCRValidator>();

    public void addValidator(MCRValidator validator) {
        validators.add(validator);
    }

    public void setProperty(String name, String value) {
        super.setProperty(name, value);
        for (MCRValidator validator : validators)
            validator.setProperty(name, value);
    }

    public boolean hasRequiredProperties() {
        for (MCRValidator validator : validators) {
            if (validator.hasRequiredProperties())
                return true;
        }
        return false;
    }

    protected boolean isValidOrDie(Object... input) {
        for (MCRValidator validator : validators) {
            if (!validator.hasRequiredProperties())
                continue;
            if (!validator.isValid(input)) {
                reportValidationResult(validator, false, input);
                return false;
            }
        }
        reportValidationResult(this, true, input);
        return true;
    }

    private void reportValidationResult(MCRValidator validator, boolean success, Object... input) {
        if (!LOGGER.isDebugEnabled())
            return;

        StringBuffer buffer = new StringBuffer();

        buffer.append(validator.getClass().getSimpleName());
        buffer.append(" validation ");
        buffer.append(success ? "succesful" : "failed");
        buffer.append(": ");

        for (Object value : input)
            buffer.append("\"").append(value).append("\" ");

        Properties properties = getProperties();
        if (!properties.isEmpty())
            for (Object name : properties.keySet())
                buffer.append(name).append("=\"").append(properties.get(name)).append("\" ");

        LOGGER.debug(buffer.toString());
    }
}
