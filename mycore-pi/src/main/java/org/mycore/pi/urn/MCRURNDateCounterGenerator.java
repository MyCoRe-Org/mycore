/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.pi.urn;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Builds a new, unique NISS based on the current date and/or time
 * in combination with a counter. The date/time can be formatted with
 * a Java SimpleDateFormat pattern, the counter can be formatted with
 * a Java DecimalFormat pattern. The property "NISSPattern" is used
 * for configuring the instance. Example configuration:
 * <p>
 * MCR.URN.SubNamespace.Essen.Prefix=urn:nbn:de:465-miless-
 * MCR.URN.SubNamespace.Essen.NISSBuilder=org.mycore.urn.services.MCRNISSBuilderDateCounter
 * MCR.URN.SubNamespace.Essen.NISSPattern=yyyyMMdd-HHmmss-000
 * <p>
 * Subsequent calls to MCRURN.buildURN( "Essen" ) could then generate
 * the following URNs, for example:
 * <p>
 * urn:nbn:de:465-miless-20060622-213404-0017
 * urn:nbn:de:465-miless-20060622-213404-0025
 * urn:nbn:de:465-miless-20060622-213448-0013
 * <p>
 * The last character is the checksum digit.
 * In the first two URNs, the generated date pattern is the same, so
 * the counter is increased (starting at 1). The use of "0" instead of
 * "#" in the pattern produces leading zeros.
 * <p>
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
    protected String buildNISS(MCRObjectID mcrID, String additional) {
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
