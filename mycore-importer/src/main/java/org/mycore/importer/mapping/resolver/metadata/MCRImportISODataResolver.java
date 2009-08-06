package org.mycore.importer.mapping.resolver.metadata;

public class MCRImportISODataResolver extends MCRImportAbstractMetadataResolver {

    @Override
    protected boolean isValid() {
        String date = saveToElement.getText();
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
        return false;
    }

    protected boolean isYearValid(String year) {
        if(areNumbers(year) && year.length() == 4)
            return true;
        return false;
    }

    protected boolean isMonthOrDayValid(String monthOrDay) {
        if(areNumbers(monthOrDay) && monthOrDay.length() == 2)
            return true;
        return false;
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