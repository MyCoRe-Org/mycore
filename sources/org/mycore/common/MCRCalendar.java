/**
 * 
 * $Revision: 1.8 $ $Date: 2008/05/28 13:43:31 $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.common;

import java.util.Locale;

import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.BuddhistCalendar;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.CopticCalendar;
import com.ibm.icu.util.EthiopicCalendar;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.HebrewCalendar;
import com.ibm.icu.util.IslamicCalendar;
import com.ibm.icu.util.JapaneseCalendar;

import org.apache.log4j.Logger;
import org.mycore.common.MCRException;

/**
 * This class implements all methods for handling calendars in MyCoRe objects
 * and data models. It use the GPL licensed ICU library of IBM.
 * 
 * @author Jens Kupferschmidt
 * @author Thomas Junge
 * @version $Revision: 1.8 $ $Date: 2008/05/28 13:43:31 $
 * @see http://icu.sourceforge.net/
 */
public class MCRCalendar {

    /** Logger */
    static Logger LOGGER = Logger.getLogger(MCRCalendar.class.getName());

    /** Tag for calendars */
    public static String TAG_BUDDHIST = "buddhist";

    public static String TAG_CHINESE = "chinese";

    public static String TAG_COPTIC = "coptic";

    public static String TAG_ETHIOPIC = "ethiopic";

    public static String TAG_GREGORIAN = "gregorian";

    public static String TAG_HEBREW = "hebrew";

    public static String TAG_ISLAMIC = "islamic";

    public static String TAG_ISLAMIC_CIVIL = "islamic-civil";

    public static String TAG_JAPANESE = "japanese";

    public static String TAG_JULIAN = "julian";

    public static String TAG_PERSIC = "persic";

    public static String TAG_ARMENIAN = "armenian";

    public static String TAG_EGYPTIAN = "egyptian";

    /** Minimum Julian Day number is 0 = 01.01.4713 BC */
    public static int MIN_JULIAN_DAY_NUMBER = 0;

    /** Maximum Julian Day number is 3182057 = 31.12.3999 */
    public static int MAX_JULIAN_DAY_NUMBER = 3182057;

    /** all available calendars of ICU */
    public static String CALENDARS_ICU[] = { TAG_BUDDHIST, TAG_CHINESE, TAG_COPTIC, TAG_ETHIOPIC, TAG_GREGORIAN, TAG_HEBREW, TAG_ISLAMIC, TAG_ISLAMIC_CIVIL, TAG_JAPANESE };

    /** convert following calendars from input to gregorian */
    public static String CALENDARS_INPUT[] = { TAG_GREGORIAN, TAG_JULIAN, TAG_ISLAMIC, TAG_BUDDHIST, TAG_COPTIC, TAG_ETHIOPIC, TAG_PERSIC, TAG_JAPANESE, TAG_ARMENIAN, TAG_EGYPTIAN };

    /**
     * This method convert a ancient date to a GregorianCalendar value. For
     * syntax of the string see javadocs of calendar methods.
     * 
     * @param datestr
     *            the date as string.
     * @param last
     *            the value is true if the date should be filled with the
     *            highest value of month or day like 12 or 31 else it fill the
     *            date with the lowest value 1 for month and day.
     * 
     * @return the GregorianCalendar date value or null if an error was
     *         occurred.
     * @exception a
     *                MCRException if parsing has an error
     */
    public static final GregorianCalendar getGregorianHistoryDate(String datestr, boolean last) throws MCRException {
        return getGregorianHistoryDate(datestr, last, TAG_GREGORIAN);
    }

    /**
     * This method convert a ancient date to a GregorianCalendar value. For
     * syntax of the string see javadocs of calendar methods.
     * 
     * @param datestr
     *            the date as string.
     * @param last
     *            the value is true if the date should be filled with the
     *            highest value of month or day like 12 or 31 else it fill the
     *            date with the lowest value 1 for month and day.
     * @param calstr
     *            the calendar as String, kind of the calendar name
     *            ('gregorian', 'julian', 'islamic', 'coptic', 'ethiopic',
     *            'buddhist', 'japanese', 'persic', 'armenian', 'egyptian')
     * 
     * @return the GregorianCalendar date value or null if an error was
     *         occurred.
     * @exception a
     *                MCRException if parsing has an error
     */
    public static final GregorianCalendar getGregorianHistoryDate(String datestr, boolean last, String calstr) throws MCRException {
        // check input
        String calstrtmp = checkCalendarName(calstr);
        Calendar cal = checkHistoryDate(datestr, last, calstrtmp);
        
        
        GregorianCalendar gcal = new GregorianCalendar();
        int year = 0;
        int mon = 0;
        int day = 0;
        int area = 0;

        try {
            if (cal instanceof GregorianCalendar && (calstrtmp.equals(TAG_GREGORIAN) || calstrtmp.equals(TAG_JULIAN) || calstrtmp.equals(TAG_PERSIC) || calstrtmp.equals(TAG_ARMENIAN) || calstrtmp.equals(TAG_EGYPTIAN))) {
                gcal = (GregorianCalendar) cal;
            } else if (cal instanceof IslamicCalendar) {
                gcal.setTime(cal.getTime());

                year = gcal.get(Calendar.YEAR);
                mon = gcal.get(Calendar.MONTH) + 1;
                day = gcal.get(Calendar.DATE);
                area = gcal.get(Calendar.ERA);

                if ((10000 * year + 100 * mon + day) <= (15821004)) {
                    // Change Julian to Gregorian
                    int TD = 0;
                    int jh;
                    if (year % 100 == 0 && mon <= 2)
                        jh = (year - 1) / 100;
                    else
                        jh = year / 100;
                    int a = jh / 4;
                    int b = jh % 4;
                    TD = 3 * a + b - 2;
                    if (area == 1) { // AD
                        if (year == 1582 && mon == 9 && day > 24) {
                            gcal.set(year, mon - 1, day + TD + 35 - day);
                            TD = 0;
                        }
                        if (year == 1582 && mon == 10 && day < 5) {
                            gcal.set(year, mon - 1, day + TD + 5 - day);
                            TD = 0;
                        }
                        if (year == 1582 && mon == 10 && day >= 5 && day < 15) {
                            gcal.set(year, mon - 1, day + TD);
                            TD = 0;
                        }
                    } else
                        TD = -TD - 4; // BC
                    gcal.add(Calendar.DATE, TD);
                }
            } else if (cal instanceof CopticCalendar && calstrtmp.equals(TAG_COPTIC)) {
                gcal.setTime(cal.getTime());
                year = gcal.get(Calendar.YEAR);
                mon = gcal.get(Calendar.MONTH) + 1;
                day = gcal.get(Calendar.DATE);
                area = gcal.get(Calendar.ERA);
                if ((10000 * year + 100 * mon + day) <= (15821004)) {
                    // Change julian to gregorian
                    int TD = 0;
                    int jh;
                    if (year % 100 == 0 && mon <= 2)
                        jh = (year - 1) / 100;
                    else
                        jh = year / 100;
                    int a = jh / 4;
                    int b = jh % 4;
                    TD = 3 * a + b - 2;
                    if (area == 1) { // AD
                        if (year == 1582 && mon == 9 && day > 24) {
                            gcal.set(year, mon - 1, day + TD + 35 - day);
                            TD = 0;
                        }
                        if (year == 1582 && mon == 10 && day < 5) {
                            gcal.set(year, mon - 1, day + TD + 5 - day);
                            TD = 0;
                        }
                        if (year == 1582 && mon == 10 && day >= 5 && day < 15) {
                            gcal.set(year, mon - 1, day + TD);
                            TD = 0;
                        }
                    } else
                        TD = -TD - 4; // BC
                    gcal.add(Calendar.DATE, TD);
                }
            } else if (cal instanceof EthiopicCalendar && calstrtmp.equals(TAG_ETHIOPIC)) {
                gcal.setTime(cal.getTime());
                year = gcal.get(Calendar.YEAR);
                mon = gcal.get(Calendar.MONTH) + 1;
                day = gcal.get(Calendar.DATE);
                area = gcal.get(Calendar.ERA);
                if ((10000 * year + 100 * mon + day) <= (15821004)) {
                    // Change julian to gregorian
                    int TD = 0;
                    int jh;
                    if (year % 100 == 0 && mon <= 2)
                        jh = (year - 1) / 100;
                    else
                        jh = year / 100;
                    int a = jh / 4;
                    int b = jh % 4;
                    TD = 3 * a + b - 2;
                    if (area == 1) { // AD
                        if (year == 1582 && mon == 9 && day > 24) {
                            gcal.set(year, mon - 1, day + TD + 35 - day);
                            TD = 0;
                        }
                        if (year == 1582 && mon == 10 && day < 5) {
                            gcal.set(year, mon - 1, day + TD + 5 - day);
                            TD = 0;
                        }
                        if (year == 1582 && mon == 10 && day >= 5 && day < 15) {
                            gcal.set(year, mon - 1, day + TD);
                            TD = 0;
                        }
                    } else
                        TD = -TD - 4; // BC
                    gcal.add(Calendar.DATE, TD);
                }
            } else if (cal instanceof BuddhistCalendar && calstrtmp.equals(TAG_BUDDHIST)) {
                gcal.setTime(cal.getTime());
                gcal.add(Calendar.DATE, 0);
            } else if (cal instanceof GregorianCalendar && calstrtmp.equals(TAG_JAPANESE)) {
                gcal.setTime(cal.getTime());
                gcal.add(Calendar.DATE, 0);
            } else if (cal instanceof HebrewCalendar && calstrtmp.equals(TAG_HEBREW)) {
                gcal.setTime(cal.getTime());
                year = gcal.get(Calendar.YEAR);
                mon = gcal.get(Calendar.MONTH) + 1;
                day = gcal.get(Calendar.DATE);
                if ((10000 * year + 100 * mon + day) <= (15821004)) {
                    // 
                    int TD = 0;
                    int jh;
                    if (year % 100 == 0 && mon <= 2)
                        jh = (year - 1) / 100;
                    else
                        jh = year / 100;
                    int a = jh / 4;
                    int b = jh % 4;
                    TD = 3 * a + b - 2;
                    if (year == 1582 && mon == 9 && day > 24) {
                        gcal.set(year, mon - 1, day + TD + 35 - day);
                        TD = 0;
                    }
                    if (year == 1582 && mon == 10 && day < 5) {
                        gcal.set(year, mon - 1, day + TD + 5 - day);
                        TD = 0;
                    }
                    if (year == 1582 && mon == 10 && day >= 5 && day < 15) {
                        gcal.set(year, mon - 1, day + TD);
                        TD = 0;
                    }
                    gcal.add(Calendar.DATE, TD);
                }
            }

        } catch (MCRException ex) {
        }
        return gcal;
    }

    /**
     * This method test a ancient date string. For syntax of the string see
     * javadocs of calendar methods.
     * 
     * @param datestr
     *            the date as string.
     * @param last
     *            the value is true if the date should be filled with the
     *            highest value of month or day like 12 or 31 else it fill the
     *            date with the lowest value 1 for month and day.
     * @param calstr
     *            the calendar as String, kind of the calendar ('gregorian',
     *            'julian', 'islamic', 'coptic', 'ethiopic', 'buddhist',
     *            'japanese', 'persic', ''armenian, 'egyptian')
     * 
     * @return the GregorianCalendar date value or null if an error was
     *         occurred.
     */
    public static final boolean testHistoryDate(String datestr, boolean last, String calstr) {
        try {
            Calendar cal = checkHistoryDate(datestr, last, calstr);
            if (cal == null)
                return false;
            return true;
        } catch (MCRException ex) {
            return false;
        }
    }

    /**
     * This method check the String of the calendar name.
     * 
     * @param calstr
     *            the calendar name as String, kind of the calendars are
     *            ('gregorian', 'julian', 'islamic', 'buddhist', 'coptic',
     *            'ethiopic', 'persic', 'japanese', 'armenian' or 'egyptian' )
     * @return the calendar string or gregorian if an error was occurred
     * @exception a
     *                MCRException if parsing has an error
     */
    private static final String checkCalendarName(String calstr) {
        if ((calstr == null) || (calstr.trim().length() == 0)) {
            throw new MCRException("The calendar name is null or empty.");
        }
        for (int i = 0; i < CALENDARS_INPUT.length; i++) {
            if (CALENDARS_INPUT[i].equals(calstr)) {
                return calstr;
            }
        }
        throw new MCRException("Can't find the calendar name " + calstr + ".");
    }

    /**
     * This method check a ancient date string for the given calendar. For
     * syntax of the string see javadocs of calendar methods.
     * 
     * @param datestr
     *            the date as string.
     * @param last
     *            the value is true if the date should be filled with the
     *            highest value of month or day like 12 or 31 else it fill the
     *            date with the lowest value 1 for month and day.
     * @param calstr
     *            the calendar name as String, kind of the calendars are
     *            ('gregorian', 'julian', 'islamic', 'buddhist', 'coptic',
     *            'ethiopic', 'persic', 'japanese', 'armenian' or 'egyptian' )
     * 
     * @return the ICU Calendar date value or null if an error was occurred.
     * @exception a
     *                MCRException if parsing has an error
     */
    private static final Calendar checkHistoryDate(String datestr, boolean last, String calstr) throws MCRException {
        Calendar out = null;
        // check datestr String
        LOGGER.debug("Input checkHistoryDate " + datestr + "  " + calstr + "  " + Boolean.toString(last));
        if ((datestr == null) || (datestr.trim().length() == 0)) {
            throw new MCRException("The ancient date string is null or empty");
        }
        // Check calendar string
        String caltmp = checkCalendarName(calstr);
        // select for calendar
        if (caltmp.equals(TAG_GREGORIAN)) {
            return getCalendarFromGregorianDate(datestr, last);
        }
        if (caltmp.equals(TAG_JULIAN)) {
            return getCalendarFromJulianDate(datestr, last);
        }
        if (caltmp.equals(TAG_ISLAMIC)) {
            return getCalendarFromIslamicDate(datestr, last);
        }
        if (caltmp.equals(TAG_COPTIC)) {
            return getCalendarFromCopticDate(datestr, last);
        }
        if (caltmp.equals(TAG_ETHIOPIC)) {
            return getCalendarFromEthiopicDate(datestr, last);
        }
        if (caltmp.equals(TAG_BUDDHIST)) {
            return getCalendarFromBuddhistDate(datestr, last);
        }
        if (caltmp.equals(TAG_PERSIC)) {
            return getCalendarFromPersicDate(datestr, last);
        }
        if (caltmp.equals(TAG_ARMENIAN)) {
            return getCalendarFromArmenianDate(datestr, last);
        }
        if (caltmp.equals(TAG_EGYPTIAN)) {
            return getCalendarFromEgyptianDate(datestr, last);
        }
        if (caltmp.equals(TAG_JAPANESE)) {
            return getCalendarFromJapaneseDate(datestr, last);
        }
        if (caltmp.equals(TAG_HEBREW)) {
            return getCalendarFromHebrewDate(datestr, last);
        }
        return out;
    }

    /**
     * This method convert a ancient date to a GregorianCalendar value. The
     * syntax for the gregorian input is: <br />
     * <ul>
     * <li> [[[t]t.][m]m.][yyy]y [v. Chr.]</li>
     * <li> [[[t]t.][m]m.][yyy]y [AD|BC]</li>
     * <li> [-|AD|BC] [[[t]t.][m]m.][yyy]y</li>
     * <li> y[yyy][-m[m][-t[t]]] [v. Chr.]</li>
     * <li> y[yyy][-m[m][-t[t]]] [AD|BC]</li>
     * <li> [-|AD|BC] y[yyy][-m[m][-t[t]]]</li>
     * </ul>
     * 
     * @param indatestr
     *            the date as string.
     * @param last
     *            the value is true if the date should be filled with the
     *            highest value of month or day like 12 or 31 else it fill the
     *            date with the lowest value 1 for month and day.
     * 
     * @return the GregorianCalendar date value or null if an error was
     *         occurred.
     * @exception a
     *                MCRException if parsing has an error
     */
    private static final GregorianCalendar getCalendarFromGregorianDate(String datestr, boolean last) throws MCRException {
        try {
            // look for BC
            datestr = datestr.toUpperCase();
            boolean bc = false;
            int start = 0;
            int ende = datestr.length();
            if (datestr.substring(0, 1).equals("-")) {
                bc = true;
                start = 1;
            } else {
                if (datestr.length() > 2) {
                    int i = datestr.indexOf("AD");
                    if (i != -1) {
                        if (i == 0) {
                            bc = false;
                            start = 2;
                        } else {
                            bc = false;
                            start = 0;
                            ende = i - 1;
                        }
                    }
                    i = datestr.indexOf("BC");
                    if (i != -1) {
                        if (i == 0) {
                            bc = true;
                            start = 2;
                        } else {
                            bc = true;
                            start = 0;
                            ende = i - 1;
                        }
                    }
                }
                if (datestr.length() > 7) {
                    int i = datestr.indexOf("N. CHR");
                    if (i != -1) {
                        if (i == 0) {
                            bc = false;
                            start = 7;
                        } else {
                            bc = false;
                            start = 0;
                            ende = i - 1;
                        }
                    }
                    i = datestr.indexOf("V. CHR");
                    if (i != -1) {
                        if (i == 0) {
                            bc = true;
                            start = 7;
                        } else {
                            bc = true;
                            start = 0;
                            ende = i - 1;
                        }
                    }
                }
            }
            datestr = datestr.substring(start, ende).trim();
            
           // german or ISO?
            start = 0;
            boolean iso = false;
            String token = ".";
            if (datestr.indexOf("-", start + 1) != -1) {
                iso = true;
                token = "-";
            }
            // only a year?
            int firstdot = datestr.indexOf(token, start + 1);
            int secdot = -1;
            if (firstdot != -1) {
                secdot = datestr.indexOf(token, firstdot + 1);
            }
            int day = 0;
            int mon = 0;
            int year = 0;
            if (secdot != -1) {
                if (iso) {
                    year = Integer.parseInt(datestr.substring(start, firstdot));
                    mon = Integer.parseInt(datestr.substring(firstdot + 1, secdot)) - 1;
                    day = Integer.parseInt(datestr.substring(secdot + 1, datestr.length()));
                } else {
                    day = Integer.parseInt(datestr.substring(start, firstdot));
                    mon = Integer.parseInt(datestr.substring(firstdot + 1, secdot)) - 1;
                    year = Integer.parseInt(datestr.substring(secdot + 1, datestr.length()));
                }

                if (year == 1582 && mon == 9 && day >= 5 && day < 15) {
                    day = 15;
                } // the problem on the year 1582
            } else {
                if (firstdot != -1) {
                    if (iso) {
                        year = Integer.parseInt(datestr.substring(start, firstdot));
                        mon = Integer.parseInt(datestr.substring(firstdot + 1, datestr.length())) - 1;
                    } else {
                        mon = Integer.parseInt(datestr.substring(start, firstdot)) - 1;
                        year = Integer.parseInt(datestr.substring(firstdot + 1, datestr.length()));
                    }
                    if (last) {
                        if (mon == 0 || mon == 2 || mon == 4 || mon == 6 || mon == 7 || mon == 9 || mon == 11) {
                            day = 31;
                        }
                        if (mon == 1) {
                            day = 28;
                        }

                        if (mon == 3 || mon == 5 || mon == 8 || mon == 10) {
                            day = 30;
                        }

                    } else {
                        day = 1;
                    }
                } else {
                    year = Integer.parseInt(datestr.substring(start, datestr.length()));
                    if (last) {
                        mon = 11;
                        day = 31;
                    } else {
                        mon = 0;
                        day = 1;
                    }
                }
            }
            GregorianCalendar newdate = new GregorianCalendar();
            // test of the monthly
            if (mon > 11 || mon < 0) {

                throw new MCRException("The month of the date is inadmissible.");
            }

            // Test of the daily
            if (((mon == 0 || mon == 2 || mon == 4 || mon == 6 || mon == 7 || mon == 9 || mon == 11) && day > 31) || ((mon == 3 || mon == 5 || mon == 8 || mon == 10) && day > 30) || (mon == 1 && day > 29 && year % 4 == 0) || (mon == 1 && day > 28 && year % 4 > 0) || day < 1) {
                throw new MCRException("The day of the date is inadmissible.");
            }
            // set AD/BC
            newdate.set(year, mon, day);
            if (bc) {
                newdate.set(GregorianCalendar.ERA, GregorianCalendar.BC);
            } else {
                newdate.set(GregorianCalendar.ERA, GregorianCalendar.AD);
            }
            newdate.add(Calendar.DATE, 0);
            return newdate;
        } catch (Exception e) {
            throw new MCRException("The ancient gregorian date is false.", e);
        }
    }

    /**
     * This method convert a JulianCalendar date to a GregorianCalendar value.
     * The syntax for the julian input is: <br />
     * <ul>
     * <li> [[[t]t.][m]m.][yyy]y [v.Chr.]</li>
     * <li> [[[t]t.][m]m.][yyy]y [AD|BC]</li>
     * <li> [-|AD|BC] [[[t]t.][m]m.][yyy]y</li>
     * <li> y[yyy][-m[m][-t[t]]] [v.Chr.]</li>
     * <li> y[yyy][-m[m][-t[t]]] [AD|BC]</li>
     * <li> [-|AD|BC] y[yyy][-m[m][-t[t]]]</li>
     * </ul>
     * 
     * @param datestr
     *            the date as string.
     * @param last
     *            the value is true if the date should be filled with the
     *            highest value of month or day like 12 or 31 else it fill the
     *            date with the lowest value 1 for month and day.
     * 
     * @return the GregorianCalendar date value or null if an error was
     *         occurred.
     * @exception a
     *                MCRException if parsing has an error
     */
    private static final GregorianCalendar getCalendarFromJulianDate(String datestr, boolean last) throws MCRException {
        try {
            // look for v. Chr.
            datestr = datestr.toUpperCase();
            boolean bc = false;
            int start = 0;
            int ende = datestr.length();
            if (datestr.substring(0, 1).equals("-")) {
                bc = true;
                start = 1;
            } else {
                if (datestr.length() > 2) {
                    int i = datestr.indexOf("AD");
                    if (i != -1) {
                        if (i == 0) {
                            bc = false;
                            start = 2;
                        } else {
                            bc = false;
                            start = 0;
                            ende = i - 1;
                        }
                    }
                    i = datestr.indexOf("BC");
                    if (i != -1) {
                        if (i == 0) {
                            bc = true;
                            start = 2;
                        } else {
                            bc = true;
                            start = 0;
                            ende = i - 1;
                        }
                    }
                }
                if (datestr.length() > 7) {
                    int i = datestr.indexOf("N. CHR");
                    if (i != -1) {
                        if (i == 0) {
                            bc = false;
                            start = 7;
                        } else {
                            bc = false;
                            start = 0;
                            ende = i - 1;
                        }
                    }
                    i = datestr.indexOf("V. CHR");
                    if (i != -1) {
                        if (i == 0) {
                            bc = true;
                            start = 7;
                        } else {
                            bc = true;
                            start = 0;
                            ende = i - 1;
                        }
                    }
                }
            }
            datestr = datestr.substring(start, ende).trim();

            // german or ISO?
            start = 0;
            boolean iso = false;
            String token = ".";

            if (datestr.indexOf("-", start + 1) != -1) {
                iso = true;
                token = "-";
            }

            // only a year
            int firstdot = datestr.indexOf(token, start + 1);
            int secdot = -1;
            if (firstdot != -1) {
                secdot = datestr.indexOf(token, firstdot + 1);
            }

            int day = 0;
            int mon = 0;
            int year = 0;
            int TD = 0; // day for correction
            if (secdot != -1) {
                try {
                    if (iso) {
                        year = Integer.parseInt(datestr.substring(start, firstdot));
                        mon = Integer.parseInt(datestr.substring(firstdot + 1, secdot)) - 1;
                        day = Integer.parseInt(datestr.substring(secdot + 1, datestr.length()));
                    } else {
                        day = Integer.parseInt(datestr.substring(start, firstdot));
                        mon = Integer.parseInt(datestr.substring(firstdot + 1, secdot)) - 1;
                        year = Integer.parseInt(datestr.substring(secdot + 1, datestr.length()));
                    }
                    // Change Julian to Gregorian
                    int jh;
                    if (year % 100 == 0 && mon <= 2)
                        jh = (year - 1) / 100;
                    else
                        jh = year / 100;
                    int a = jh / 4;
                    int b = jh % 4;
                    TD = 3 * a + b - 2;
                    if (year == 1582 && mon == 8 && day > 24) {
                        mon = 9;
                        day = day + TD + 5 - day;
                        TD = 0;
                    }
                    if (year == 1582 && mon == 9 && day < 5) {
                        day = day + TD + 5 - day;
                        TD = 0;
                    }
                    if (year == 1582 && mon == 9 && day >= 5 && day < 15) {
                        day = day + TD;
                        TD = 0;
                    }
                } catch (Exception ex) {
                }
            } else {
                if (firstdot != -1) {
                    if (iso) {
                        year = Integer.parseInt(datestr.substring(start, firstdot));
                        mon = Integer.parseInt(datestr.substring(firstdot + 1, datestr.length())) - 1;
                    } else {
                        mon = Integer.parseInt(datestr.substring(start, firstdot)) - 1;
                        year = Integer.parseInt(datestr.substring(firstdot + 1, datestr.length()));
                    }

                    if (last) {
                        if (mon == 0 || mon == 2 || mon == 4 || mon == 6 || mon == 7 || mon == 9 || mon == 11) {
                            day = 31;
                        }

                        if (mon == 1) {
                            day = 28;
                        }

                        if (mon == 3 || mon == 5 || mon == 8 || mon == 10) {
                            day = 30;
                        }

                    } else {
                        day = 1;
                    }

                } else {
                    year = Integer.parseInt(datestr.substring(start, datestr.length()));

                    if (last) {
                        mon = 11;
                        day = 31;
                    } else {
                        mon = 0;
                        day = 1;
                    }
                }
            }
            // test of the monthly
            if (mon > 11 || mon < 0) {
                throw new MCRException("The month of the date is inadmissible.");
            }

            // Test of the daily
            if (((mon == 0 || mon == 2 || mon == 4 || mon == 6 || mon == 7 || mon == 9 || mon == 11) && day > 31) || ((mon == 3 || mon == 5 || mon == 8 || mon == 10) && day > 30) || (mon == 1 && day > 29 && year % 4 == 0) || (mon == 1 && day > 28 && year % 4 > 0) || day < 1) {
                throw new MCRException("The day of the date is inadmissible.");
            }
            // set AD/BC
            GregorianCalendar newdate = new GregorianCalendar();
            newdate.set(year, mon, day);
            if (bc == true) {
                newdate.set(GregorianCalendar.ERA, GregorianCalendar.BC);
                TD = -TD - 4; // Calendar correction reversible
            } else {
                newdate.set(GregorianCalendar.ERA, GregorianCalendar.AD);

            }

            newdate.add(Calendar.DATE, TD); // Calendar correction
            return (GregorianCalendar) newdate;
        } catch (Exception e) {
            throw new MCRException("The ancient julian date is false.", e);
        }
    }

    /**
     * This method convert a IslamicCalendar date to a IslamicCalendar value.
     * The syntax for the islamic input is: <br />
     * <ul>
     * <li> [[[t]t.][m]m.][yyy]y [v.]H.</li>
     * <li> [[[t]t.][m]m.][yyy]y [b.]H.</li>
     * <li> [-] [[[t]t.][m]m.][yyy]y</li>
     * <li> [.\u0647 | .\u0647 .\u0642] [[[t]t.][m]m.][yyy]y</li>
     * <li> y[yyy][-m[m][-t[t]]] [v.]H.</li>
     * <li> y[yyy][-m[m][-t[t]]] [b.]H.</li>
     * <li> [-] y[yyy][-m[m][-t[t]]]</li>
     * </ul>
     * 
     * @param datestr
     *            the date as string.
     * @param last
     *            the value is true if the date should be filled with the
     *            highest value of month or day like 12 or 30 else it fill the
     *            date with the lowest value 1 for month and day.
     * 
     * @return the IslamicCalendar date value or null if an error was occurred.
     * @exception a
     *                MCRException if parsing has an error
     */
    private static final IslamicCalendar getCalendarFromIslamicDate(String datestr, boolean last) {
        try {
            // test before Hidschra
            boolean bh = false;
            int start = 0;
            int ende = datestr.length();
            if (datestr.substring(0, 1).equals("-")) {
                bh = true;
                start = 1;
            } else {
                if (datestr.length() > 4) {
                    int i = datestr.indexOf("v.H.");
                    if (i != -1) {
                        bh = true;
                        start = 0;
                        ende = i - 1;
                    }
                    i = datestr.indexOf("b.H.");
                    if (i != -1) {
                        bh = true;
                        start = 0;
                        ende = i - 1;
                    }
                    if (!bh) {
                        i = datestr.indexOf("H.");
                        if (i != -1) {
                            bh = false;
                            start = 0;
                            ende = i;
                        }
                        if (datestr.length() > 10) {

                            i = datestr.indexOf(".\u0647.\u0642");
                            if (i != -1) {
                                if (i == 0) {
                                    bh = true;
                                    start = 4;
                                }
                            }
                        }
                        if (!bh) {
                            i = datestr.indexOf(".\u0647");
                            if (i != -1) {
                                if (i == 0) {
                                    bh = false;
                                    start = 2;
                                }
                            }
                        }
                    }
                }
            }

            datestr = datestr.substring(start, ende).trim();

            // german or ISO?
            start = 0;
            boolean iso = false;
            String token = ".";

            if (datestr.indexOf("-", start + 1) != -1) {
                iso = true;
                token = "-";
            }
            // 
            int firstdot = datestr.indexOf(token, start + 1);
            int secdot = -1;

            if (firstdot != -1) {
                secdot = datestr.indexOf(token, firstdot + 1);
            }

            int day = 0;
            int mon = 0;
            int year = 0;
            if (secdot != -1) { // day month year
                if (iso) {
                    year = Integer.parseInt(datestr.substring(start, firstdot));
                    mon = Integer.parseInt(datestr.substring(firstdot + 1, secdot)) - 1;
                    day = Integer.parseInt(datestr.substring(secdot + 1, datestr.length()));
                } else {
                    day = Integer.parseInt(datestr.substring(start, firstdot));
                    mon = Integer.parseInt(datestr.substring(firstdot + 1, secdot)) - 1;
                    year = Integer.parseInt(datestr.substring(secdot + 1, datestr.length()));
                }
            } else {
                if (firstdot != -1) { // month year
                    if (iso) {
                        year = Integer.parseInt(datestr.substring(start, firstdot));
                        mon = Integer.parseInt(datestr.substring(firstdot + 1, datestr.length())) - 1;
                    } else {
                        mon = Integer.parseInt(datestr.substring(start, firstdot)) - 1;
                        year = Integer.parseInt(datestr.substring(firstdot + 1, datestr.length()));
                    }

                    if (last) {
                        if (mon % 2 == 0)
                            day = 30;
                        if (mon % 2 == 1)
                            day = 29;

                    } else {
                        day = 1;
                    }
                } else { // year
                    year = Integer.parseInt(datestr.substring(start, datestr.length()));

                    if (last) {
                        mon = 11;
                        day = 29;
                    } else {
                        mon = 0;
                        day = 1;
                    }
                }
            }
            // test of the monthly
            if (mon > 11 || mon < 0)
                throw new MCRException("The month of the date is inadmissible.");
            // Test of the daily
            if (day > 30 || (mon % 2 == 1 && mon < 11 && day > 29) || (day < 1))
                throw new MCRException("The day of the date is inadmissible.");
            if (bh)
                year = -year + 1; // if before Hidschra
            IslamicCalendar ical = new IslamicCalendar();
            ical.set(year, mon, day);
            ical.add(Calendar.DATE, 0); // Calendar correction

            return ical;

        } catch (Exception e) {
            throw new MCRException("The ancient islamic is false.", e);
        }

    }

    /**
     * This method convert a HebrewCalendar date to a HebrewCalendar value. The
     * syntax for the hebrew input is [[t]t.][m]m.][yyy]y] or
     * [[yyy]y-[[m]m]-[[t]t].
     * 
     * @param datestr
     *            the date as string.
     * @param last
     *            the value is true if the date should be filled with the
     *            highest value of month or day like 13 or 30 else it fill the
     *            date with the lowest value 1 for month and day.
     * 
     * @return the HebewCalendar date value or null if an error was occurred.
     * @exception a
     *                MCRException if parsing has an error
     */

    private static final HebrewCalendar getCalendarFromHebrewDate(String datestr, boolean last) {
        try {
            int start = 0;
            datestr = datestr.trim();

            // german or ISO?
            start = 0;
            boolean iso = false;
            String token = ".";

            if (datestr.indexOf("-", start + 1) != -1) {
                iso = true;
                token = "-";
            }
            // 
            int firstdot = datestr.indexOf(token, start + 1);
            int secdot = -1;

            if (firstdot != -1) {
                secdot = datestr.indexOf(token, firstdot + 1);
            }

            int day = 0;
            int mon = 0;
            int year = 0;

            if (secdot != -1) {
                if (iso) {
                    year = Integer.parseInt(datestr.substring(start, firstdot));
                    mon = Integer.parseInt(datestr.substring(firstdot + 1, secdot)) - 1;
                    day = Integer.parseInt(datestr.substring(secdot + 1, datestr.length()));
                } else {
                    day = Integer.parseInt(datestr.substring(start, firstdot));
                    mon = Integer.parseInt(datestr.substring(firstdot + 1, secdot)) - 1;
                    year = Integer.parseInt(datestr.substring(secdot + 1, datestr.length()));
                }
            } else {
                if (firstdot != -1) {
                    if (iso) {
                        year = Integer.parseInt(datestr.substring(start, firstdot));
                        mon = Integer.parseInt(datestr.substring(firstdot + 1, datestr.length())) - 1;
                    } else {
                        mon = Integer.parseInt(datestr.substring(start, firstdot)) - 1;
                        year = Integer.parseInt(datestr.substring(firstdot + 1, datestr.length()));
                    }

                    if (last) {
                        if (mon == 0 || mon == 4 || mon == 7 || mon == 9 || mon == 11) {
                            day = 30;
                        } else {
                            day = 29;
                        }
                    } else {
                        day = 1;
                    }
                } else {
                    year = Integer.parseInt(datestr.substring(start, datestr.length()));

                    if (last) {
                        mon = 11;
                        day = 29;
                    } else {
                        mon = 0;
                        day = 1;
                    }
                }
            }
            HebrewCalendar hcal = new HebrewCalendar();
            hcal.set(year, mon, day);
            return hcal;

        } catch (Exception e) {
            throw new MCRException("The ancient hebrew date is false.", e);
        }
    }

    /**
     * This method convert a CopticCalendar date to a CopticCalendar value. The
     * syntax for the coptic input is: <br />
     * <ul>
     * <li> [[[t]t.][m]m.][yyy]y [[A.|a.]M.]</li>
     * <li> [[[t]t.][m]m.][yyy]y [B.M.]</li>
     * <li> [-] [[[t]t.][m]m.][yyy]y</li>
     * <li> y[yyy][-m[m][-t[t]]] [A.|a.]M.]</li>
     * <li> y[yyy][-m[m][-t[t]]] [B.M.]</li>
     * <li> [-] y[yyy][-m[m][-t[t]]]</li>
     * </ul>
     * 
     * @param datestr
     *            the date as string.
     * @param last
     *            the value is true if the date should be filled with the
     *            highest value of month or day like 12 or 30 else it fill the
     *            date with the lowest value 1 for month and day.
     * 
     * @return the CopticCalendar date value or null if an error was occurred.
     * @exception a
     *                MCRException if parsing has an error
     */
    private static final CopticCalendar getCalendarFromCopticDate(String datestr, boolean last) {
        try {
            datestr = datestr.trim();
            // test before Martyrium
            boolean bm = false;
            int start = 0;
            int ende = datestr.length();
            if (datestr.substring(0, 1).equals("-")) {
                bm = true;
                start = 1;
            }
            datestr = datestr.substring(start).trim();
            start = 0;
            ende = datestr.length();
            if (datestr.length() > 4) {
                int i = datestr.indexOf("B.M.");
                if (i != -1) {
                    bm = true;
                    start = 0;
                    ende = i - 1;
                }
                i = datestr.indexOf("A.M.");
                if (i != -1) {
                    start = 0;
                    ende = i - 1;
                }
                i = datestr.indexOf("a.M.");
                if (i != -1) {
                    start = 0;
                    ende = i - 1;
                }
            }

            datestr = datestr.substring(start, ende).trim();

            // german or ISO?
            start = 0;
            boolean iso = false;
            String token = ".";

            if (datestr.indexOf("-", start + 1) != -1) {
                iso = true;
                token = "-";
            }
            // 
            int firstdot = datestr.indexOf(token, start + 1);
            int secdot = -1;

            if (firstdot != -1) {
                secdot = datestr.indexOf(token, firstdot + 1);
            }

            int day = 0;
            int mon = 0;
            int year = 0;

            if (secdot != -1) { // day, mon, year
                if (iso) {
                    year = Integer.parseInt(datestr.substring(start, firstdot));
                    mon = Integer.parseInt(datestr.substring(firstdot + 1, secdot)) - 1;
                    day = Integer.parseInt(datestr.substring(secdot + 1, datestr.length()));
                } else {
                    day = Integer.parseInt(datestr.substring(start, firstdot));
                    mon = Integer.parseInt(datestr.substring(firstdot + 1, secdot)) - 1;
                    year = Integer.parseInt(datestr.substring(secdot + 1, datestr.length()));
                }
            } else {
                if (firstdot != -1) { // mon, year
                    if (iso) {
                        year = Integer.parseInt(datestr.substring(start, firstdot));
                        mon = Integer.parseInt(datestr.substring(firstdot + 1, datestr.length())) - 1;
                    } else {
                        mon = Integer.parseInt(datestr.substring(start, firstdot)) - 1;
                        year = Integer.parseInt(datestr.substring(firstdot + 1, datestr.length()));
                    }

                    if (last) {
                        if (mon <= 11) {
                            day = 30;
                        } else {
                            day = 5;
                        }
                    } else {
                        day = 1;
                    }
                } else { // year
                    year = Integer.parseInt(datestr.substring(start, datestr.length()));

                    if (last) {
                        mon = 12;
                        day = 5;
                    } else {
                        mon = 0;
                        day = 1;
                    }
                }
            }
            // test of the monthly
            if (mon > 12 || mon < 0)
                throw new MCRException("The month of the date is inadmissible.");
            // Test of the daily
            if (day > 30 || (day < 1) || (day > 6 && mon == 12))
                throw new MCRException("The day of the date is inadmissible.");
            if (bm)
                year = -year + 1; // if before Matyrium
            CopticCalendar ccal = new CopticCalendar();
            ccal.set(year, mon, day);

            ccal.add(Calendar.DATE, 0); // Calendar correction

            return ccal;

        } catch (Exception e) {
            throw new MCRException("The ancient coptic date is false.", e);
        }
    }

    /**
     * This method convert a JapaneseCalendar date to a JapaneseCalendar value.
     * The syntax for the japanese input is: <br />
     * <ul>
     * <li> [[[t]t.][m]m.][H|M|S|T][yyy]y</li>
     * H: Heisei; M: Meiji, S: Showa, T: Taiso
     * <li> [H|M|S|T]y[yyy][-m[m][-t[t]]]</li>
     * </ul>
     * 
     * @param datestr
     *            the date as string.
     * @param last
     *            the value is true if the date should be filled with the
     *            highest value of month or day like 12 or 30 else it fill the
     *            date with the lowest value 1 for month and day.
     * 
     * @return the JapaneseCalendar date value or null if an error was occurred.
     * @exception a
     *                MCRException if parsing has an error
     */
    private static final JapaneseCalendar getCalendarFromJapaneseDate(String datestr, boolean last) {
        try {
            datestr = datestr.trim();

            // boolean bm = false;
            int era = 0;
            int start = 0;
            int ende = datestr.length();

            // german or ISO?
            start = 0;
            boolean iso = false;
            String token = ".";

            if (datestr.indexOf("-", start + 1) != -1) {
                iso = true;
                token = "-";
            }
            // 
            int firstdot = datestr.indexOf(token, start + 1);
            int secdot = -1;

            if (firstdot != -1) {
                secdot = datestr.indexOf(token, firstdot + 1);
            }

            int day = 0;
            int mon = 0;
            int year = 0;
            String syear = "";
            if (secdot != -1) { // day, mon, year
                if (iso) {
                    syear = datestr.substring(start, firstdot);
                    mon = Integer.parseInt(datestr.substring(firstdot + 1, secdot)) - 1;
                    day = Integer.parseInt(datestr.substring(secdot + 1, datestr.length()));
                } else {
                    day = Integer.parseInt(datestr.substring(start, firstdot));
                    mon = Integer.parseInt(datestr.substring(firstdot + 1, secdot)) - 1;
                    syear = datestr.substring(secdot + 1, datestr.length());
                }
            } else {
                if (firstdot != -1) { // mon, year
                    if (iso) {
                        syear = datestr.substring(start, firstdot);
                        mon = Integer.parseInt(datestr.substring(firstdot + 1, datestr.length())) - 1;
                    } else {
                        mon = Integer.parseInt(datestr.substring(start, firstdot)) - 1;
                        syear = datestr.substring(firstdot + 1, datestr.length());
                    }

                    if (last) {
                        if (mon <= 11) {
                            day = 30;
                        } else {
                            day = 5;
                        }
                    } else {
                        day = 1;
                    }
                } else { // year
                    syear = datestr.substring(start, datestr.length());

                    if (last) {
                        mon = 12;
                        day = 5;
                    } else {
                        mon = 0;
                        day = 1;
                    }
                }
            }

            if (syear.substring(0, 1).equals("H"))
                era = 235;
            else if (syear.substring(0, 1).equals("S"))
                era = 234;
            else if (syear.substring(0, 1).equals("T"))
                era = 233;
            else if (syear.substring(0, 1).equals("M"))
                era = 232;
            year = Integer.parseInt(syear.substring(1).trim());
            // test of the monthly
            if (mon > 12 || mon < 0)
                throw new MCRException("The month of the date is inadmissible.");
            // Test of the daily
            if (day > 30 || (day < 1) || (day > 6 && mon == 12))
                throw new MCRException("The day of the date is inadmissible.");

            JapaneseCalendar jcal = new JapaneseCalendar();
            // GregorianCalendar jcal = new GregorianCalendar();
            jcal.set(year, mon, day);
            jcal.set(JapaneseCalendar.ERA, era);
            jcal.add(Calendar.DATE, 0); // Calendar correction
            GregorianCalendar xcal = new GregorianCalendar();
            xcal.setTime(jcal.getTime());

            return jcal;

        } catch (Exception e) {
            throw new MCRException("The ancient jacanese date is false.", e);
        }
    }

    /**
     * This method convert a EthiopicCalendar date to a EthiopicCalendar value.
     * The syntax for the ethiopic input is: <br />
     * <ul>
     * <li> [[[t]t.][m]m.][yyy]y [E.E.]</li>
     * <li> [-] [[[t]t.][m]m.][yyy]y</li>
     * <li> y[yyy][-m[m][-t[t]]] [E.E.]</li>
     * <li> [-] y[yyy][-m[m][-t[t]]]</li>
     * </ul>
     * 
     * @param datestr
     *            the date as string.
     * @param last
     *            the value is true if the date should be filled with the
     *            highest value of month or day like 13 or 30 else it fill the
     *            date with the lowest value 1 for month and day.
     * 
     * @return the EthiopicCalendar date value or null if an error was occurred.
     * @exception a
     *                MCRException if parsing has an error
     */
    private static final EthiopicCalendar getCalendarFromEthiopicDate(String datestr, boolean last) {
        try {
            datestr = datestr.trim();
            // test before Christi
            boolean bc = false;
            int start = 0;
            int ende = datestr.length();
            if (datestr.substring(0, 1).equals("-")) {
                bc = true;
                start = 1;
            }
            datestr = datestr.substring(start).trim();
            start = 0;
            ende = datestr.length();
            if (datestr.length() > 4) {
                int i = datestr.indexOf("E.E.");
                if (i != -1) {
                    start = 0;
                    ende = i - 1;
                }
                datestr = datestr.substring(start, ende).trim();
            }

            // german or ISO?
            start = 0;
            boolean iso = false;
            String token = ".";

            if (datestr.indexOf("-", start + 1) != -1) {
                iso = true;
                token = "-";
            }
            // 
            int firstdot = datestr.indexOf(token, start + 1);
            int secdot = -1;

            if (firstdot != -1) {
                secdot = datestr.indexOf(token, firstdot + 1);
            }

            int day = 0;
            int mon = 0;
            int year = 0;

            if (secdot != -1) { // day, mon, year
                if (iso) {
                    year = Integer.parseInt(datestr.substring(start, firstdot));
                    mon = Integer.parseInt(datestr.substring(firstdot + 1, secdot)) - 1;
                    day = Integer.parseInt(datestr.substring(secdot + 1, datestr.length()));
                } else {
                    day = Integer.parseInt(datestr.substring(start, firstdot));
                    mon = Integer.parseInt(datestr.substring(firstdot + 1, secdot)) - 1;
                    year = Integer.parseInt(datestr.substring(secdot + 1, datestr.length()));
                }
            } else {
                if (firstdot != -1) { // mon, year
                    if (iso) {
                        year = Integer.parseInt(datestr.substring(start, firstdot));
                        mon = Integer.parseInt(datestr.substring(firstdot + 1, datestr.length())) - 1;
                    } else {
                        mon = Integer.parseInt(datestr.substring(start, firstdot)) - 1;
                        year = Integer.parseInt(datestr.substring(firstdot + 1, datestr.length()));
                    }

                    if (last) {
                        if (mon <= 11) {
                            day = 30;
                        } else {
                            day = 5;
                        }
                    } else {
                        day = 1;
                    }
                } else { // year
                    year = Integer.parseInt(datestr.substring(start, datestr.length()));

                    if (last) {
                        mon = 12;
                        day = 5;
                    } else {
                        mon = 0;
                        day = 1;
                    }
                }
            }
            // test of the monthly
            if (mon > 12 || mon < 0)
                throw new MCRException("The month of the date is inadmissible.");
            // Test of the daily
            if (day > 30 || (day < 1) || (day > 6 && mon == 12))
                throw new MCRException("The day of the date is inadmissible.");
            if (bc)
                year = -year + 1; // if before Christi
            EthiopicCalendar ecal = new EthiopicCalendar();
            ecal.set(year, mon, day);

            ecal.add(Calendar.DATE, 0); // Calendar correction

            return ecal;

        } catch (Exception e) {
            throw new MCRException("The ancient ethiopic date is false.", e);
        }
    }

    /**
     * This method convert a BuddhistCalendar date to a IslamicCalendar value.
     * The syntax for the buddhist input is: <br />
     * <ul>
     * <li> [-][[[t]t.][m]m.][yyy]y [B.E.]</li>
     * <li> [-] [[[t]t.][m]m.][yyy]y</li>
     * <li> [-] y[yyy][-m[m][-t[t]]] [B.E.]</li>
     * <li> [-] y[yyy][-m[m][-t[t]]]</li>
     * </ul>
     * 
     * @param datestr
     *            the date as string.
     * @param last
     *            the value is true if the date should be filled with the
     *            highest value of month or day like 12 or 31 else it fill the
     *            date with the lowest value 1 for month and day.
     * 
     * @return the BuddhistCalendar date value or null if an error was occurred.
     * @exception a
     *                MCRException if parsing has an error
     */

    private static final BuddhistCalendar getCalendarFromBuddhistDate(String datestr, boolean last) {
        try {
            datestr = datestr.trim();
            // test before Buddhas
            boolean bb = false;
            int start = 0;
            int ende = datestr.length();
            if (datestr.substring(0, 1).equals("-")) {
                bb = true;
                start = 1;
                datestr = datestr.substring(start).trim();
                ende = datestr.length();
            }
            start = 0;
            if (datestr.length() > 4) {
                int i = datestr.indexOf("B.E.");
                if (i != -1) {
                    start = 0;
                    ende = i;
                }
            }
            datestr = datestr.substring(start, ende).trim();

            // german oder ISO?
            start = 0;
            boolean iso = false;
            String token = ".";

            if (datestr.indexOf("-", start + 1) != -1) {
                iso = true;
                token = "-";
            }
            // 
            int firstdot = datestr.indexOf(token, start + 1);
            int secdot = -1;

            if (firstdot != -1) {
                secdot = datestr.indexOf(token, firstdot + 1);
            }

            int day = 0;
            int mon = 0;
            int year = 0;

            if (secdot != -1) { // day, month, year
                if (iso) {
                    year = Integer.parseInt(datestr.substring(start, firstdot));
                    mon = Integer.parseInt(datestr.substring(firstdot + 1, secdot)) - 1;
                    day = Integer.parseInt(datestr.substring(secdot + 1, datestr.length()));
                } else {
                    day = Integer.parseInt(datestr.substring(start, firstdot));
                    mon = Integer.parseInt(datestr.substring(firstdot + 1, secdot)) - 1;
                    year = Integer.parseInt(datestr.substring(secdot + 1, datestr.length()));
                }
            } else {
                if (firstdot != -1) { // month, year
                    if (iso) {
                        year = Integer.parseInt(datestr.substring(start, firstdot));
                        mon = Integer.parseInt(datestr.substring(firstdot + 1, datestr.length())) - 1;
                    } else {
                        mon = Integer.parseInt(datestr.substring(start, firstdot)) - 1;
                        year = Integer.parseInt(datestr.substring(firstdot + 1, datestr.length()));
                    }

                    if (last) {
                        if (mon == 0 || mon == 2 || mon == 4 || mon == 6 || mon == 7 || mon == 9 || mon == 11) {
                            day = 31;
                        }
                        if (mon == 1) {
                            day = 28;
                        }
                        if (mon == 3 || mon == 5 || mon == 8 || mon == 10) {
                            day = 30;
                        }
                    } else {
                        day = 1;
                    }
                } else { // year
                    year = Integer.parseInt(datestr.substring(start, datestr.length()));

                    if (last) {
                        mon = 11;
                        day = 29;
                    } else {
                        mon = 0;
                        day = 1;
                    }
                }
            }
            BuddhistCalendar budcal = new BuddhistCalendar();
            // test of the monthly
            if (mon > 11 || mon < 0) {
                throw new MCRException("The month of the date is inadmissible.");
            }

            // Test of the daily
            if (((mon == 0 || mon == 2 || mon == 4 || mon == 6 || mon == 7 || mon == 9 || mon == 11) && day > 31) || ((mon == 3 || mon == 5 || mon == 8 || mon == 10) && day > 30) || (mon == 1 && day > 29 && year % 4 == 0) || (mon == 1 && day > 28 && year % 4 > 0) || day < 1) {
                throw new MCRException("The day of the date is inadmissible.");
            }
            if (bb)
                year = -year + 1; // if before Buddha

            if (year == 2125 && mon == 9 && day >= 5 && day < 15) {
                day = 15;
            }

            budcal.set(year, mon, day);
            return (BuddhistCalendar) budcal;
        } catch (Exception e) {
            throw new MCRException("The ancient buddhist date is false.", e);
        }
    }

    /**
     * This method convert a PersicCalendar date to a GregorianCalendar value.
     * The The syntax for the persian input is: <br />
     * <ul>
     * <li> [-] [[[t]t.][m]m.][yyy]y</li>
     * <li> [-] y[yyy][-m[m][-t[t]]]</li>
     * </ul>
     * 
     * @param datestr
     *            the date as string.
     * @param last
     *            the value is true if the date should be filled with the
     *            highest value of month or day like 13 or 30 else it fill the
     *            date with the lowest value 1 for month and day.
     * 
     * @return the GregorianCalendar date value or null if an error was
     *         occurred.
     * @exception a
     *                MCRException if parsing has an error
     */
    private static final GregorianCalendar getCalendarFromPersicDate(String datestr, boolean last) {
        try {
            datestr = datestr.trim();
            // test before
            boolean bb = false;
            int start = 0;
            int ende = datestr.length();
            if (datestr.substring(0, 1).equals("-")) {
                bb = true;
                start = 1;
                datestr = datestr.substring(start).trim();
                ende = datestr.length();
            }

            // german or ISO?
            start = 0;
            boolean iso = false;
            String token = ".";

            if (datestr.indexOf("-", start + 1) != -1) {
                iso = true;
                token = "-";
            }

            int firstdot = datestr.indexOf(token, start + 1);
            int secdot = -1;

            if (firstdot != -1) {
                secdot = datestr.indexOf(token, firstdot + 1);
            }

            int day = 0;
            int mon = 0;
            int year = 0;

            if (secdot != -1) { // year, month, day
                if (iso) {
                    year = Integer.parseInt(datestr.substring(start, firstdot));
                    mon = Integer.parseInt(datestr.substring(firstdot + 1, secdot)) - 1;
                    day = Integer.parseInt(datestr.substring(secdot + 1, datestr.length()));
                } else {
                    day = Integer.parseInt(datestr.substring(start, firstdot));
                    mon = Integer.parseInt(datestr.substring(firstdot + 1, secdot)) - 1;
                    year = Integer.parseInt(datestr.substring(secdot + 1, datestr.length()));
                }
            } else {
                if (firstdot != -1) { // year, month
                    if (iso) {
                        year = Integer.parseInt(datestr.substring(start, firstdot));
                        mon = Integer.parseInt(datestr.substring(firstdot + 1, datestr.length())) - 1;
                    } else {
                        mon = Integer.parseInt(datestr.substring(start, firstdot)) - 1;
                        year = Integer.parseInt(datestr.substring(firstdot + 1, datestr.length()));
                    }

                    if (last) {
                        if (mon == 0 || mon == 1 || mon == 2 || mon == 3 || mon == 4 || mon == 5) {
                            day = 31;
                        }
                        if (mon == 6 || mon == 7 || mon == 8 || mon == 9 || mon == 10) {
                            day = 30;
                        }
                        if (mon == 11) {
                            day = 29;
                        }

                    } else {
                        day = 1;
                    }
                } else { // year
                    year = Integer.parseInt(datestr.substring(start, datestr.length()));

                    if (last) {
                        mon = 11;
                        day = 29;
                    } else {
                        mon = 0;
                        day = 1;
                    }
                }
            }
            int njahr = 0;
            if (bb)
                year = -year + 1;
            njahr = year + 621;

            GregorianCalendar newdate = new GregorianCalendar();
            newdate.set(njahr, 2, 20); // yearly beginning to 20.3.
            // beginning of the month (day to year)
            int begday = 0;
            if (mon == 1)
                begday = 31;
            if (mon == 2)
                begday = 62;
            if (mon == 3)
                begday = 93;
            if (mon == 4)
                begday = 124;
            if (mon == 5)
                begday = 155;
            if (mon == 6)
                begday = 186;
            if (mon == 7)
                begday = 216;
            if (mon == 8)
                begday = 246;
            if (mon == 9)
                begday = 276;
            if (mon == 10)
                begday = 306;
            if (mon == 11)
                begday = 336;
            begday += day - 1;

            int jh = njahr / 100; // century
            int b = jh % 4;
            int c = njahr % 100; // year of the century
            int d = c / 4; // count leap year of the century

            int min;
            if (njahr >= 0) {
                min = b * 360 + 350 * c - d * 1440 + 720; // minute
                newdate.add(Calendar.MINUTE, min); // minute of day
                newdate.add(Calendar.DATE, begday); // day of the year
            } else {
                min = b * 360 + 350 * c - d * 1440 + 720; // minute
                newdate.add(Calendar.DATE, begday + 2); // day of the year
                newdate.add(Calendar.MINUTE, min); // minute of day
            }

            // problem 1582
            year = newdate.get(Calendar.YEAR);
            mon = newdate.get(Calendar.MONTH) + 1;
            if (year == 1582 && mon == 10 && day >= 5 && day < 15) {
                newdate.set(year, mon - 1, 15);
            }

            return newdate;

        } catch (Exception e) {
            throw new MCRException("The ancient persian date is false.", e);
        }
    }

    /**
     * This method convert a ArmenianCalendar date to a GregorianCalendar value.
     * The syntax for the Armenian input is [-][[t]t.][m]m.][yyy]y] or
     * [-][[yyy]y-[[m]m]-[[t]t].
     * 
     * <ul>
     * <li> [-] [[[t]t.][m]m.][yyy]y</li>
     * <li> [-] y[yyy][-m[m][-t[t]]]</li>
     * </ul>
     * 
     * @param datestr
     *            the date as string.
     * @param last
     *            the value is true if the date should be filled with the
     *            highest value of month or day like 13 or 30 else it fill the
     *            date with the lowest value 1 for month and day.
     * 
     * @return the GregorianCalendar date value or null if an error was
     *         occurred.
     * @exception a
     *                MCRException if parsing has an error
     */
    private static final GregorianCalendar getCalendarFromArmenianDate(String datestr, boolean last) {
        try {
            datestr = datestr.trim();
            // test before
            boolean ba = false;
            int start = 0;
            int ende = datestr.length();
            if (datestr.substring(0, 1).equals("-")) {
                ba = true;
                start = 1;
                datestr = datestr.substring(start).trim();
                ende = datestr.length();
            }

            // german or ISO?
            start = 0;
            boolean iso = false;
            String token = ".";

            if (datestr.indexOf("-", start + 1) != -1) {
                iso = true;
                token = "-";
            }

            int firstdot = datestr.indexOf(token, start + 1);
            int secdot = -1;

            if (firstdot != -1) {
                secdot = datestr.indexOf(token, firstdot + 1);
            }
            int day = 0;
            int mon = 0;
            int year = 0;

            if (secdot != -1) { // year, month, day
                if (iso) {
                    year = Integer.parseInt(datestr.substring(start, firstdot));
                    mon = Integer.parseInt(datestr.substring(firstdot + 1, secdot));
                    day = Integer.parseInt(datestr.substring(secdot + 1, datestr.length()));
                } else {
                    day = Integer.parseInt(datestr.substring(start, firstdot));
                    mon = Integer.parseInt(datestr.substring(firstdot + 1, secdot));
                    year = Integer.parseInt(datestr.substring(secdot + 1, datestr.length()));
                }
            } else {
                if (firstdot != -1) { // year, month
                    if (iso) {
                        year = Integer.parseInt(datestr.substring(start, firstdot));
                        mon = Integer.parseInt(datestr.substring(firstdot + 1, datestr.length()));
                    } else {
                        mon = Integer.parseInt(datestr.substring(start, firstdot));
                        year = Integer.parseInt(datestr.substring(firstdot + 1, datestr.length()));
                    }

                    if (last) {
                        if (mon <= 12)
                            day = 30;
                        if (mon == 13)
                            day = 5;
                    } else {
                        mon = 1;
                        day = 1;
                    }
                } else { // year
                    year = Integer.parseInt(datestr.substring(start, datestr.length()));

                    if (last) {
                        mon = 13;
                        day = 5;
                    } else {
                        mon = 1;
                        day = 1;
                    }
                }
            }
            // test of the monthly
            if (mon > 13 || mon < 1)
                throw new MCRException("The month of the date is inadmissible.");
            // Test of the daily
            if (day > 30 || (day < 1) || (day > 5 && mon == 13))
                throw new MCRException("The day of the date is inadmissible.");
            int difyear;
            int difday;
            int jhd = 1600;
            int ndifday = 0;
            if (ba)
                year = -year + 1;
            GregorianCalendar ecal = new GregorianCalendar();
            if ((year * 10000 + mon * 100 + day) >= 10311214) {// Jahr >
                // 14.12.1031
                difyear = year - 1031;
                difday = (difyear * 365) + (mon - 1) * 30 + day - 344;
                ecal.set(1582, 9, 15);
                ecal.add(Calendar.DATE, difday);
            }
            if ((year * 10000 + mon * 100 + day) < 10311214 && (year * 10000 + mon * 100 + day) > 10311204) { // 
                ecal.set(1582, 9, 15);
            }
            if ((year * 10000 + mon * 100 + day) <= 10311204) {// Jahr <
                // 5.10.1592
                ecal.set(1582, 9, 15);
                difyear = year - 1031;
                int daysyear = 36525;
                difday = (difyear * 365) + (mon - 1) * 30 + day - 334;

                if (difday <= -30168) {
                    ndifday = ndifday - 30168;
                    jhd = 1500;
                    difday = difday + 30167;
                    while (difday < 0) {
                        if (difday < -daysyear) { // 36525
                            ndifday = ndifday - daysyear;
                            jhd = jhd - 100;
                            if (jhd == 0)
                                jhd = -1;
                            if (jhd == -1) {
                                jhd = 0;
                            } else {
                                daysyear = 36525;
                            }
                            if (jhd % 400 == 0) {
                                difday = difday + daysyear;
                            } else {
                                difday = difday + daysyear - 1;
                            }
                        } else {
                            ndifday = ndifday + difday;
                            difday = 0;
                        }
                    }
                    ecal.add(Calendar.DATE, ndifday);
                } else {
                    ecal.add(Calendar.DATE, difday);
                }
            }
            return ecal;

        } catch (Exception e) {
            throw new MCRException("The ancient armenian date is false.", e);
        }
    }

    /**
     * This method convert a EgyptianCalendar date to a GregorianCalendar value.
     * The The syntax for the egyptian (Nabonassar) input is: <br />
     * <ul>
     * <li> [-][[[t]t.][m]m.][yyy]y [A.N.]</li>
     * <li> [-] [[[t]t.][m]m.][yyy]y</li>
     * <li> [-] y[yyy][-m[m][-t[t]]] [A.N.]</li>
     * <li> [-] y[yyy][-m[m][-t[t]]]</li>
     * </ul>
     * 
     * @param datestr
     *            the date as string.
     * @param last
     *            the value is true if the date should be filled with the
     *            highest value of month or day like 13 or 30 else it fill the
     *            date with the lowest value 1 for month and day.
     * 
     * @return the GregorianCalendar date value or null if an error was
     *         occurred.
     * @exception a
     *                MCRException if parsing has an error
     */
    private static final GregorianCalendar getCalendarFromEgyptianDate(String datestr, boolean last) {
        try {
            datestr = datestr.trim();
            // test before
            boolean ba = false;
            int start = 0;
            int ende = datestr.length();
            if (datestr.substring(0, 1).equals("-")) {
                ba = true;
                start = 1;
                datestr = datestr.substring(start).trim();
                ende = datestr.length();
            }
            start = 0;
            if (datestr.length() > 4) {
                int i = datestr.indexOf("A.N.");
                if (i != -1) {
                    start = 0;
                    ende = i;
                }
            }
            datestr = datestr.substring(start, ende).trim();
            // german or ISO?
            start = 0;
            boolean iso = false;
            String token = ".";

            if (datestr.indexOf("-", start + 1) != -1) {
                iso = true;
                token = "-";
            }

            int firstdot = datestr.indexOf(token, start + 1);
            int secdot = -1;

            if (firstdot != -1) {
                secdot = datestr.indexOf(token, firstdot + 1);
            }

            int day = 0;
            int mon = 0;
            int year = 0;

            if (secdot != -1) { // year, month, day
                if (iso) {
                    year = Integer.parseInt(datestr.substring(start, firstdot));
                    mon = Integer.parseInt(datestr.substring(firstdot + 1, secdot));
                    day = Integer.parseInt(datestr.substring(secdot + 1, datestr.length()));
                } else {
                    day = Integer.parseInt(datestr.substring(start, firstdot));
                    mon = Integer.parseInt(datestr.substring(firstdot + 1, secdot));
                    year = Integer.parseInt(datestr.substring(secdot + 1, datestr.length()));
                }
            } else {
                if (firstdot != -1) { // year, month
                    if (iso) {
                        year = Integer.parseInt(datestr.substring(start, firstdot));
                        mon = Integer.parseInt(datestr.substring(firstdot + 1, datestr.length()));
                    } else {
                        mon = Integer.parseInt(datestr.substring(start, firstdot));
                        year = Integer.parseInt(datestr.substring(firstdot + 1, datestr.length()));
                    }

                    if (last) {
                        if (mon <= 12)
                            day = 30;
                        if (mon == 13)
                            day = 5;
                    } else {
                        mon = 1;
                        day = 1;
                    }
                } else { // year
                    year = Integer.parseInt(datestr.substring(start, datestr.length()));

                    if (last) {
                        mon = 13;
                        day = 5;
                    } else {
                        mon = 1;
                        day = 1;
                    }
                }
            }
            // test of the monthly
            if (mon > 13 || mon < 1)
                throw new MCRException("The month of the date is inadmissible.");
            // Test of the daily
            if (day > 30 || (day < 1) || (day > 5 && mon == 13))
                throw new MCRException("The day of the date is inadmissible.");
            int difyear;
            int difday;
            int jhd = 1600;
            int ndifday = 0;
            if (ba)
                year = -year + 1;
            GregorianCalendar ecal = new GregorianCalendar();
            if ((year * 10000 + mon * 100 + day) >= 23310314) {// Jahr >
                // 15.10.1592
                difyear = year - 2331;
                difday = (difyear * 365) + (mon - 1) * 30 + day - 74;
                ecal.set(1582, 9, 15);
                ecal.add(Calendar.DATE, difday);
            }

            if ((year * 10000 + mon * 100 + day) < 23310314 && (year * 10000 + mon * 100 + day) >= 23310304) { // 
                ecal.set(1582, 9, 15);
            }
            if ((year * 10000 + mon * 100 + day) < 23310304) {// Jahr <
                // 5.10.1592
                ecal.set(1582, 9, 15);
                difyear = year - 2331;
                int daysyear = 36525;
                difday = (difyear * 365) + ((mon - 1) * 30 + day) - 64;

                if (difday <= -30168) {
                    ndifday = ndifday - 30168;
                    jhd = 1500;
                    difday = difday + 30167;
                    while (difday < 0) {
                        if (difday < -daysyear) { // days of 100 years 36525
                            ndifday = ndifday - daysyear;
                            jhd = jhd - 100;
                            if (jhd == 0)
                                jhd = -1;
                            if (jhd == -1) {
                                jhd = 0;
                            }
                            // else {daysyear=36525;
                            // }
                            if (jhd % 400 == 0) {
                                difday = difday + daysyear;
                            } else {
                                difday = difday + daysyear - 1;
                            }
                        } else {
                            ndifday = ndifday + difday;
                            difday = 0;
                        }
                    }
                    ecal.add(Calendar.DATE, ndifday);
                } else {
                    ecal.add(Calendar.DATE, difday);
                }
            }
            return ecal;

        } catch (Exception e) {
            throw new MCRException("The ancient egyptian date is false.", e);
        }
    }

    /**
     * This method return the Julian Day number for a given Calendar instance.
     * 
     * @param date
     *            an instance of a Calendar
     * @return the Julian Day number
     */
    public static final int getJulianDayNumber(Calendar date) {
        return date.get(Calendar.JULIAN_DAY);
    }

    /**
     * This method returns the date as string in format 'dd.MM.yyyy G'.
     * 
     * @param date
     *            the GregorianCalendar date
     * 
     * @return the date string
     */
    public static final String getDateToFormattedString(Calendar date) {
        return getDateToFormattedString(date, "dd.MM.yyyy G");
    }

    /**
     * This method returns the date as string.
     * 
     * @param date
     *            the GregorianCalendar date
     * @param format
     *            the format of the date as String
     * 
     * @return the date string in the format. If the format is wrong dd.MM.yyyy
     *         G is set. If the date is wrong an empty string will be returned.
     */
    public static final String getDateToFormattedString(Calendar date, String format) {
        if ((date == null) || (format == null) || (format.trim().length() == 0)) {
            return "";
        }
        SimpleDateFormat formatter = null;
        try {
            formatter = new SimpleDateFormat(format, (new Locale("en")));
        } catch (Exception e) {
            formatter = new SimpleDateFormat("dd.MM.yyyy G", (new Locale("en")));
        }
        try {
            formatter.setCalendar(date);
            return formatter.format(date.getTime());
        } catch (Exception e) {
            return "";
        }
    }

}