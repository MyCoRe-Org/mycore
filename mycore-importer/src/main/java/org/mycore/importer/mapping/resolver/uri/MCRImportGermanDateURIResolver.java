package org.mycore.importer.mapping.resolver.uri;

/**
 * This is only a sample implementation of an date resolver. Extend this
 * or write an own date resolver to support your formats.
 * 
 * @author Matthias Eichner
 */
public class MCRImportGermanDateURIResolver implements MCRImportURIResolver {

    public String resolve(String uri, String oldValue) {
        // resolve format DD.MM.YYYY
        String[] split = oldValue.split(".");
        String day = split[0];
        String month = split[1];
        String year = split[2];

        if(day.length() == 1)
            day = "0" + day;
        if(month.length() == 1)
            month = "0" + month;
        if(year.length() == 2)
            year = "19" + year;
        return year + "-" + month + "-" + day;
    }
}
