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

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Builds a new, unique NISS based on the current date and time expressed
 * in seconds. The resulting NISS is non-speaking, but unique and somewhat
 * optimized for the nbn:de checksum algorithm. Only one NISS per second
 * will be generated.
 *
 * @author Frank Lützenkirchen
 */
public class MCRFLURNGenerator extends MCRDNBURNGenerator {
    private String last;

    public MCRFLURNGenerator(String generatorID) {
        super(generatorID);
    }

    protected synchronized String buildNISS(MCRObjectID mcrID, String additional) {
        Calendar now = new GregorianCalendar(TimeZone.getTimeZone("GMT+01:00"), Locale.ENGLISH);
        int yyy = 2268 - now.get(Calendar.YEAR);
        int ddd = 500 - now.get(Calendar.DAY_OF_YEAR);
        int hh = now.get(Calendar.HOUR_OF_DAY);
        int mm = now.get(Calendar.MINUTE);
        int ss = now.get(Calendar.SECOND);
        int sss = 99999 - (hh * 3600 + mm * 60 + ss);

        String DDDDD = String.valueOf(yyy * 366 + ddd);

        String niss = String.valueOf(DDDDD.charAt(4)) + DDDDD.charAt(2) + DDDDD.charAt(1) + DDDDD.charAt(3)
            + DDDDD.charAt(0)
            + sss;

        if (niss.equals(last)) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }

            return buildNISS(mcrID, additional);
        } else {
            last = niss;
            return niss;
        }
    }

}
