/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.pi.util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Utility class that provides a date scrambling algorithm <a href=
 * "https://github.com/MyCoRe-Org/mycore/blob/d5d31302d663c0b47747905e0347a89b5bcf821c/sources/org/mycore/services/urn/MCRNISSBuilderFL.java#L37"
 * >introduced by <strong>F</strong>rank <strong>L</strong>ützenkirchen</a>.
 */
public final class MCRFLDateScrambler {

    private MCRFLDateScrambler() {
    }

    public static String scrambleDate(Date date) {

        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT+01:00"), Locale.ENGLISH);
        calendar.setTime(date);

        int yyy = 2268 - calendar.get(Calendar.YEAR);
        int ddd = 500 - calendar.get(Calendar.DAY_OF_YEAR);
        int hh = calendar.get(Calendar.HOUR_OF_DAY);
        int mm = calendar.get(Calendar.MINUTE);
        int ss = calendar.get(Calendar.SECOND);
        int sss = 99_999 - (hh * 3600 + mm * 60 + ss);
        String ddddd = String.valueOf(yyy * 366 + ddd);

        return String.valueOf(ddddd.charAt(4)) + ddddd.charAt(2)
            + ddddd.charAt(1) + ddddd.charAt(3) + ddddd.charAt(0) + sss;

    }

}
