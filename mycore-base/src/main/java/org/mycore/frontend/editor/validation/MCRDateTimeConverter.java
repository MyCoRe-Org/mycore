package org.mycore.frontend.editor.validation;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.mycore.common.MCRException;

public class MCRDateTimeConverter {

    private List<DateFormat> formats = new ArrayList<DateFormat>();

    public MCRDateTimeConverter(String patterns) {
        for (String pattern : patterns.split(";"))
            formats.add(getDateFormat(pattern.trim()));
    }

    protected DateFormat getDateFormat(String pattern) {
        DateFormat df = new SimpleDateFormat(pattern);
        df.setLenient(false);
        return df;
    }

    public Date string2date(String input) throws MCRException {
        for (DateFormat format : formats) {
            try {
                return format.parse(input);
            } catch (Exception ignored) {
            }
        }
        throw new MCRException("DateTime value can not be parsed: " + input);
    }
}
