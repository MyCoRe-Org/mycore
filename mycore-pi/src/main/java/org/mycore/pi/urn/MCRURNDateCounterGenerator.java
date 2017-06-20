package org.mycore.pi.urn;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Builds a new, unique NISS based on the current date and/or time
 * in combination with a counter. The date/time can be formatted with
 * a Java SimpleDateFormat pattern, the counter can be formatted with
 * a Java DecimalFormat pattern. The property "NISSPattern" is used
 * for configuring the instance. Example configuration:
 *
 * MCR.URN.SubNamespace.Essen.Prefix=urn:nbn:de:465-miless-
 * MCR.URN.SubNamespace.Essen.NISSBuilder=org.mycore.urn.services.MCRNISSBuilderDateCounter
 * MCR.URN.SubNamespace.Essen.NISSPattern=yyyyMMdd-HHmmss-000
 *
 * Subsequent calls to MCRURN.buildURN( "Essen" ) could then generate
 * the following URNs, for example:
 *
 * urn:nbn:de:465-miless-20060622-213404-0017
 * urn:nbn:de:465-miless-20060622-213404-0025
 * urn:nbn:de:465-miless-20060622-213448-0013
 *
 * The last character is the checksum digit.
 * In the first two URNs, the generated date pattern is the same, so
 * the counter is increased (starting at 1). The use of "0" instead of
 * "#" in the pattern produces leading zeros.
 *
 * A pattern might have no date part (only use counter)
 * or no counter part (only use date pattern)
 *
 * @author Frank Lützenkirchen
 */
public class MCRURNDateCounterGenerator extends MCRDNBURNGenerator {

    private DecimalFormat fmtCount;

    private SimpleDateFormat fmtDate;

    private String lastDate;

    private int counter = 1;

    public MCRURNDateCounterGenerator(String generatorID) {
        super(generatorID);

        String pattern = getProperties().get("Pattern");
        String patternDate = pattern;
        String patternCounter = "";

        int pos1 = pattern.indexOf("0");
        int pos2 = pattern.indexOf("#");
        if (pos1 >= 0 || pos2 >= 0) {
            int pos;

            if (pos1 == -1) {
                pos = pos2;
            } else if (pos2 == -1) {
                pos = pos1;
            } else {
                pos = Math.min(pos1, pos2);
            }

            patternDate = pattern.substring(0, pos);
            patternCounter = pattern.substring(pos);
        }

        if (patternDate.length() > 0) {
            fmtDate = new SimpleDateFormat(patternDate, Locale.GERMAN);
        }

        if (patternCounter.length() > 0) {
            fmtCount = new DecimalFormat(patternCounter, DecimalFormatSymbols.getInstance(Locale.GERMAN));
        }
    }

    @Override
    protected String buildNISS() {
        String niss;

        StringBuilder sb = new StringBuilder();

        if (fmtDate != null) {
            Calendar now = new GregorianCalendar(TimeZone.getTimeZone("GMT-1:00"), Locale.GERMAN);
            String date = fmtDate.format(now.getTime());
            sb.append(date);

            if (!date.equals(lastDate)) {
                lastDate = date;
                counter = 1; // reset counter, new date
            }
        }

        if (fmtCount != null) {
            sb.append(fmtCount.format(counter++));
        }

        niss = sb.toString();

        return niss;
    }

}
