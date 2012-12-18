package org.mycore.importer.mapping.resolver.metadata;

import org.apache.log4j.Logger;

public class MCRImportISODateResolver extends MCRImportAbstractMetadataResolver {

    private static final Logger LOGGER = Logger.getLogger(MCRImportISODateResolver.class);

    @Override
    protected boolean isValid() {
        String date = saveToElement.getText();
        try {
            // only year
            if(!date.contains("-")) {
                return isYearValid(date);
            }

            String[] split = date.split("-");
            String year = split[0];
            String month = split[1];
            // year-month
            if(date.length() == 7) {
                return isYearValid(year) && isMonthOrDayValid(month);
            }
            // year-month-day
            if(date.length() == 10 && split.length == 3) {
                String day = split[2];
                return  isYearValid(year) &&
                        isMonthOrDayValid(month) &&
                        isMonthOrDayValid(day);
            }
        } catch(Exception exc) {
            LOGGER.warn("Exception while parsing date '" + date + "'!", exc);
        }
        return false;
    }

    protected boolean isYearValid(String year) {
        return areNumbers(year) && year.length() == 4;
    }

    protected boolean isMonthOrDayValid(String monthOrDay) {
        return areNumbers(monthOrDay) && monthOrDay.length() == 2;
    }
    
    protected boolean areNumbers(String isNumber) {
        try {
            Integer.parseInt(isNumber);
        } catch(NumberFormatException exc) {
            return false;
        }
        return true;
    }
}