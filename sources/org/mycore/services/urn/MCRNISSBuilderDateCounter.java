/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.services.urn;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.mycore.common.MCRConfiguration;

/**
 * Builds a new, unique NISS based on the current date and/or time
 * in combination with a counter. The date/time can be formatted with
 * a Java SimpleDateFormat pattern, the counter ca be formatted with
 * a Java DecimalFormat pattern. The property "NISSPattern" is used 
 * for configuring the instance. Example configuration:
 * 
 * MCR.URN.SubNamespace.Essen.Prefix=urn:nbn:de:465-miless-
 * MCR.URN.SubNamespace.Essen.NISSBuilder=org.mycore.services.urn.MCRNISSBuilderDateCounter
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
public class MCRNISSBuilderDateCounter implements MCRNISSBuilder {
    
    private String lastDate;

    private String lastNISS;

    private int counter = 1;

    private SimpleDateFormat fmtDate;

    private DecimalFormat fmtCount;

    public void init(String configID) {
        String property = "MCR.URN.SubNamespace." + configID + ".NISSPattern";
        String pattern = MCRConfiguration.instance().getString(property);
        String patternDate = pattern;
        String patternCounter = "";

        int pos1 = pattern.indexOf("0");
        int pos2 = pattern.indexOf("#");
        if ((pos1 >= 0) || (pos2 >= 0)) {
            int pos;

            if (pos1 == -1)
                pos = pos2;
            else if (pos2 == -1)
                pos = pos1;
            else
                pos = Math.min(pos1, pos2);

            patternDate = pattern.substring(0, pos);
            patternCounter = pattern.substring(pos);
        }

        if (patternDate.length() > 0)
            fmtDate = new SimpleDateFormat(patternDate);

        if (patternCounter.length() > 0)
            fmtCount = new DecimalFormat(patternCounter);
    }

    public synchronized String buildNISS() {
        String niss;

        do {
            StringBuffer sb = new StringBuffer();

            if (fmtDate != null) {
                Calendar now = new GregorianCalendar();
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
        } while (niss.equals(lastNISS));

        lastNISS = niss;
        return niss;
    }
}
