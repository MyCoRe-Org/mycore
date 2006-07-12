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

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Builds a new, unique NISS based on the current date and time expressed
 * in seconds. The resulting NISS is non-speaking, but unique and somewhat
 * optimized for the nbn:de checksum algorithm. Only one NISS per second 
 * will be generated.
 * 
 * @author Frank Lützenkirchen
 */
public class MCRNISSBuilderFL implements MCRNISSBuilder {
    private String last;

    public void init(String configID) {
    }

    public synchronized String buildNISS() {
        Calendar now = new GregorianCalendar();
        int yyy = 2268 - now.get(Calendar.YEAR);
        int ddd = 500 - now.get(Calendar.DAY_OF_YEAR);
        int hh = now.get(Calendar.HOUR_OF_DAY);
        int mm = now.get(Calendar.MINUTE);
        int ss = now.get(Calendar.SECOND);
        int sss = 99999 - ((hh * 3600) + (mm * 60) + ss);

        String DDDDD = String.valueOf((yyy * 366) + ddd);

        StringBuffer buffer = new StringBuffer();
        buffer.append(DDDDD.charAt(4));
        buffer.append(DDDDD.charAt(2));
        buffer.append(DDDDD.charAt(1));
        buffer.append(DDDDD.charAt(3));
        buffer.append(DDDDD.charAt(0));
        buffer.append(sss);
        String niss = buffer.toString();

        if (niss.equals(last)) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }

            return buildNISS();
        } else {
            last = niss;
            return niss;
        }
    }
}
