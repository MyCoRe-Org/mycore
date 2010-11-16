package org.mycore.frontend.editor.validation;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.mycore.common.MCRException;

public class MCRDateTimeValidator extends MCRValidatorBase {

    @Override
    public boolean hasRequiredProperties() {
        return "datetime".equals(getProperty("type")) && hasProperty("format");
    }

    @Override
    protected boolean isValidOrDie(String input) throws Exception {
        string2date(input);
        return true;
    }

    protected Date string2date(String input) throws MCRException {
        String patterns = getProperty("format");
        for (String pattern : patterns.split(";")) {
            DateFormat df = getDateFormat(pattern.trim());
            try {
                return df.parse(input);
            } catch (Exception ignored) {
            }
        }
        throw new MCRException("DateTime value can not be parsed: " + input);
    }

    protected DateFormat getDateFormat(String pattern) {
        DateFormat df = new SimpleDateFormat(pattern);
        df.setLenient(false);
        return df;
    }
}
