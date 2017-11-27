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

package org.mycore.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.BuddhistCalendar;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.CopticCalendar;
import com.ibm.icu.util.EthiopicCalendar;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.HebrewCalendar;
import com.ibm.icu.util.IslamicCalendar;
import com.ibm.icu.util.JapaneseCalendar;

/**
 * This class implements all methods for handling calendars in MyCoRe objects
 * and data models. It is licensed by <a href="http://source.icu-project.org/repos/icu/icu/trunk/license.html">ICU License</a>.
 *
 * @author Jens Kupferschmidt
 * @author Thomas Junge
 * @version $Revision: 1.8 $ $Date: 2008/05/28 13:43:31 $
 * @see <a href="http://site.icu-project.org/home">http://site.icu-project.org/home</a>
 */
public class MCRCalendar {

    /** Logger */
    static Logger LOGGER = LogManager.getLogger(MCRCalendar.class.getName());

    /** Tag for Buddhistic calendar */
    public static final String TAG_BUDDHIST = "buddhist";

    /** Tag for Chinese calendar */
    public static final String TAG_CHINESE = "chinese";

    /** Tag for Coptic calendar */
    public static final String TAG_COPTIC = "coptic";

    /** Tag for Ethiopic calendar */
    public static final String TAG_ETHIOPIC = "ethiopic";

    /** Tag for Gregorian calendar */
    public static final String TAG_GREGORIAN = "gregorian";

    /** Tag for Hebrew calendar */
    public static final String TAG_HEBREW = "hebrew";

    /** Tag for Islamic calendar */
    public static final String TAG_ISLAMIC = "islamic";

    /** Tag for Japanese calendar */
    public static final String TAG_JAPANESE = "japanese";

    /** Tag for Julian calendar */
    public static final String TAG_JULIAN = "julian";

    /** Tag for Persic calendar */
    public static final String TAG_PERSIC = "persic";

    /** Tag for Armenian calendar */
    public static final String TAG_ARMENIAN = "armenian";

    /** Tag for Egyptian calendar */
    public static final String TAG_EGYPTIAN = "egyptian";

    /** Minimum Julian Day number is 0 = 01.01.4713 BC */
    public static final int MIN_JULIAN_DAY_NUMBER = 0;

    /** Maximum Julian Day number is 3182057 = 28.01.4000 */
    public static final int MAX_JULIAN_DAY_NUMBER = 3182057;

    /** all available calendars of ICU as String area */
    public static final String CALENDARS_ICU[] = { TAG_BUDDHIST, TAG_CHINESE, TAG_COPTIC, TAG_ETHIOPIC, TAG_GREGORIAN,
        TAG_HEBREW, TAG_ISLAMIC, TAG_JAPANESE };

    /** a list of calendar tags they are supported in this class */
    public static final List<String> CALENDARS_LIST = Collections
        .unmodifiableList(new ArrayList<>(
            Arrays.asList(TAG_GREGORIAN, TAG_JULIAN, TAG_ISLAMIC, TAG_BUDDHIST, TAG_COPTIC, TAG_ETHIOPIC, TAG_PERSIC,
                TAG_JAPANESE, TAG_ARMENIAN, TAG_EGYPTIAN)));

    /**
     * This method check a ancient date string for the given calendar. For
     * syntax of the date string see javadocs of calendar methods.
     *
     * @param date_string
     *            the date as string.
     * @param last
     *            the value is true if the date should be filled with the
     *            highest value of month or day like 12 or 31 else it fill the
     *            date with the lowest value 1 for month and day.
     * @param calendar_string
     *            the calendar name as String, kind of the calendars are
     *            ('gregorian', 'julian', 'islamic', 'buddhist', 'coptic',
     *            'ethiopic', 'persic', 'japanese', 'armenian' or 'egyptian' )
     *
     * @return the ICU Calendar instance of the concrete calendar type or null if an error was occurred.
     * @exception MCRException if parsing has an error
     */
    public static Calendar getHistoryDateAsCalendar(String date_string, boolean last, String calendar_string)
        throws MCRException {
        Calendar out = null;
        // check date_string
        LOGGER.debug("Input of getHistoryDateAsCalendar: {}  {}  {}", date_string, calendar_string,
            Boolean.toString(last));
        if (date_string == null || date_string.trim().length() == 0) {
            throw new MCRException("The ancient date string is null or empty");
        }
        date_string = date_string.trim();
        if (calendar_string == null || calendar_string.trim().length() == 0) {
            throw new MCRException("The calendar string is null or empty");
        }
        if (date_string.equals("4713-01-01 BC")) {
            LOGGER.debug("Date string contains MIN_JULIAN_DAY_NUMBER");
            out = new GregorianCalendar();
            out.set(Calendar.JULIAN_DAY, MCRCalendar.MIN_JULIAN_DAY_NUMBER);
            return out;
        }
        if (date_string.equals("4000-01-28 AD")) {
            LOGGER.debug("Date string contains MAX_JULIAN_DAY_NUMBER");
            out = new GregorianCalendar();
            out.set(Calendar.JULIAN_DAY, MCRCalendar.MAX_JULIAN_DAY_NUMBER);
            return out;
        }
        // Check calendar string
        if (!CALENDARS_LIST.contains(calendar_string)) {
            throw new MCRException("The calendar string " + calendar_string + " is not supported");
        }
        // select for calendar
        if (calendar_string.equals(TAG_GREGORIAN)) {
            out = getCalendarFromGregorianDate(date_string, last);
        }
        if (calendar_string.equals(TAG_JULIAN)) {
            out = getCalendarFromJulianDate(date_string, last);
        }
        if (calendar_string.equals(TAG_ISLAMIC)) {
            out = getCalendarFromIslamicDate(date_string, last);
        }
        if (calendar_string.equals(TAG_COPTIC)) {
            out = getCalendarFromCopticDate(date_string, last);
        }
        if (calendar_string.equals(TAG_ETHIOPIC)) {
            out = getCalendarFromEthiopicDate(date_string, last);
        }
        if (calendar_string.equals(TAG_BUDDHIST)) {
            out = getCalendarFromBuddhistDate(date_string, last);
        }
        if (calendar_string.equals(TAG_PERSIC)) {
            out = getCalendarFromPersicDate(date_string, last);
        }
        if (calendar_string.equals(TAG_ARMENIAN)) {
            out = getCalendarFromArmenianDate(date_string, last);
        }
        if (calendar_string.equals(TAG_EGYPTIAN)) {
            out = getCalendarFromEgyptianDate(date_string, last);
        }
        if (calendar_string.equals(TAG_JAPANESE)) {
            out = getCalendarFromJapaneseDate(date_string, last);
        }
        if (calendar_string.equals(TAG_HEBREW)) {
            out = getCalendarFromHebrewDate(date_string, last);
        }
        LOGGER.debug("Output of getHistoryDateAsCalendar: {}", getCalendarDateToFormattedString(out));
        return out;
    }

    /**
     * Check the date string for julian or gregorian calendar
     *
     * @param date_string the date string
     * @param last the flag for first / last day
     * @return an integer array with [0] = year; [1] = month; [2] = day; [3] = era : -1 = BC : +1 = AC
     * @throws Exception
     */
    private static int[] checkDateStringForJulianCalendar(String date_string, boolean last) throws Exception {
        int[] fields = new int[4];
        // look for BC
        date_string = date_string.toUpperCase(Locale.ROOT);
        boolean bc = false;
        int start = 0;
        int ende = date_string.length();
        if (date_string.substring(0, 1).equals("-")) {
            bc = true;
            start = 1;
        } else {
            if (date_string.length() > 2) {
                int i = date_string.indexOf("AD");
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
                i = date_string.indexOf("BC");
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
            if (date_string.length() > 7) {
                int i = date_string.indexOf("N. CHR");
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
                i = date_string.indexOf("V. CHR");
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
        date_string = date_string.substring(start, ende).trim();

        // German, English or ISO?
        start = 0;
        boolean iso = false;
        String token = ".";
        if (date_string.indexOf("-", start + 1) != -1) {
            iso = true;
            token = "-";
        }
        if (date_string.indexOf("/", start + 1) != -1) {
            token = "/";
        }

        // only a year?
        int firstdot = date_string.indexOf(token, start + 1);
        int secdot = -1;
        if (firstdot != -1) {
            secdot = date_string.indexOf(token, firstdot + 1);
        }
        int day = 1;
        int mon = 0;
        int year = 0;
        if (secdot != -1) {
            if (iso) {
                year = Integer.parseInt(date_string.substring(start, firstdot));
                mon = Integer.parseInt(date_string.substring(firstdot + 1, secdot)) - 1;
                day = Integer.parseInt(date_string.substring(secdot + 1, date_string.length()));
            } else {
                day = Integer.parseInt(date_string.substring(start, firstdot));
                mon = Integer.parseInt(date_string.substring(firstdot + 1, secdot)) - 1;
                year = Integer.parseInt(date_string.substring(secdot + 1, date_string.length()));
            }
        } else {
            if (firstdot != -1) {
                if (iso) {
                    year = Integer.parseInt(date_string.substring(start, firstdot));
                    mon = Integer.parseInt(date_string.substring(firstdot + 1, date_string.length())) - 1;
                } else {
                    mon = Integer.parseInt(date_string.substring(start, firstdot)) - 1;
                    year = Integer.parseInt(date_string.substring(firstdot + 1, date_string.length()));
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

                }
            } else {
                year = Integer.parseInt(date_string.substring(start, date_string.length()));
                if (last) {
                    mon = 11;
                    day = 31;
                }
            }
        }

        // test of the monthly
        if (mon > 11 || mon < 0) {
            throw new MCRException("The month of the date is inadmissible.");
        }

        // Test of the daily
        if ((mon == 0 || mon == 2 || mon == 4 || mon == 6 || mon == 7 || mon == 9 || mon == 11) && day > 31
            || (mon == 3 || mon == 5 || mon == 8 || mon == 10) && day > 30 || mon == 1 && day > 29 && year % 4 == 0
            || mon == 1 && day > 28 && year % 4 > 0 || day < 1) {
            throw new MCRException("The day of the date is inadmissible.");
        }
        fields[0] = year;
        fields[1] = mon;
        fields[2] = day;
        fields[3] = bc ? -1 : 1;
        return fields;
    }

    /**
     * This method convert a ancient date to a general Calendar value. The
     * syntax for the gregorian input is: <br>
     * <ul>
     * <li> [[[t]t.][m]m.][yyy]y [v. Chr.]</li>
     * <li> [[[t]t.][m]m.][yyy]y [AD|BC]</li>
     * <li> [-|AD|BC] [[[t]t.][m]m.][yyy]y</li>
     * <li> [[[t]t/][m]m/][yyy]y [AD|BC]</li>
     * <li> [-|AD|BC] [[[t]t/][m]m/][yyy]y</li>
     * <li> y[yyy][-m[m][-t[t]]] [v. Chr.]</li>
     * <li> y[yyy][-m[m][-t[t]]] [AD|BC]</li>
     * <li> [-|AD|BC] y[yyy][-m[m][-t[t]]]</li>
     * </ul>
     *
     * @param date_string
     *            the date as string.
     * @param last
     *            the value is true if the date should be filled with the
     *            highest value of month or day like 12 or 31 else it fill the
     *            date with the lowest value 1 for month and day.
     *
     * @return the GregorianCalendar date value or null if an error was
     *         occurred.
     * @exception MCRException if parsing has an error
     */
    protected static GregorianCalendar getCalendarFromGregorianDate(String date_string, boolean last)
        throws MCRException {
        try {
            int fields[] = checkDateStringForJulianCalendar(date_string, last);
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.set(fields[0], fields[1], fields[2]);
            if (fields[3] == -1) {
                calendar.set(GregorianCalendar.ERA, GregorianCalendar.BC);
            } else {
                calendar.set(GregorianCalendar.ERA, GregorianCalendar.AD);
            }
            return calendar;
        } catch (Exception e) {
            throw new MCRException("The ancient gregorian date is false.", e);
        }
    }

    /**
     * This method convert a JulianCalendar date to a general Calendar value.
     * The syntax for the julian input is: <br>
     * <ul>
     * <li> [[[t]t.][m]m.][yyy]y [v. Chr.|n. Chr.]</li>
     * <li> [[[t]t.][m]m.][yyy]y [AD|BC]</li>
     * <li> [-|AD|BC] [[[t]t.][m]m.][yyy]y</li>
     * <li> [[[t]t/][m]m/][yyy]y [AD|BC]</li>
     * <li> [-|AD|BC] [[[t]t/][m]m/][yyy]y</li>
     * <li> y[yyy][-m[m][-t[t]]] [v. Chr.|n. Chr.]</li>
     * <li> y[yyy][-m[m][-t[t]]] [AD|BC]</li>
     * <li> [-|AD|BC] y[yyy][-m[m][-t[t]]]</li>
     * </ul>
     *
     * @param date_string
     *            the date as string.
     * @param last
     *            the value is true if the date should be filled with the
     *            highest value of month or day like 12 or 31 else it fill the
     *            date with the lowest value 1 for month and day.
     *
     * @return the GregorianCalendar date value or null if an error was
     *         occurred.
     * @exception MCRException if parsing has an error
     */
    protected static Calendar getCalendarFromJulianDate(String date_string, boolean last) throws MCRException {
        try {
            int fields[] = checkDateStringForJulianCalendar(date_string, last);
            Calendar calendar = new GregorianCalendar();
            calendar.set(fields[0], fields[1], fields[2]);
            if (fields[3] == -1) {
                calendar.set(Calendar.ERA, GregorianCalendar.BC);
            } else {
                calendar.set(Calendar.ERA, GregorianCalendar.AD);
            }
            // correct data
            int julian_day = calendar.get(Calendar.JULIAN_DAY);
            if (fields[0] == 1582 && fields[1] == 9 && fields[2] == 6 && fields[3] == 1) {
                julian_day = 2299162;
            }
            if (fields[0] == 1582 && fields[1] == 9 && fields[2] == 7 && fields[3] == 1) {
                julian_day = 2299163;
            }
            if (fields[0] == 1582 && fields[1] == 9 && fields[2] == 8 && fields[3] == 1) {
                julian_day = 2299164;
            }
            if (fields[0] == 1582 && fields[1] == 9 && fields[2] == 9 && fields[3] == 1) {
                julian_day = 2299165;
            }
            if (fields[0] == 1582 && fields[1] == 9 && fields[2] == 10 && fields[3] == 1) {
                julian_day = 2299166;
            }
            if (fields[0] == 1582 && fields[1] == 9 && fields[2] == 11 && fields[3] == 1) {
                julian_day = 2299167;
            }
            if (fields[0] == 1582 && fields[1] == 9 && fields[2] == 12 && fields[3] == 1) {
                julian_day = 2299168;
            }
            if (fields[0] == 1582 && fields[1] == 9 && fields[2] == 13 && fields[3] == 1) {
                julian_day = 2299169;
            }
            if (fields[0] == 1582 && fields[1] == 9 && fields[2] == 14 && fields[3] == 1) {
                julian_day = 2299170;
            }
            if (fields[0] == 1582 && fields[1] == 9 && fields[2] == 15 && fields[3] == 1) {
                julian_day = 2299171;
            }
            if ((fields[0] > 1582 || (fields[0] == 1582 && fields[1] > 9)
                || (fields[0] == 1582 && fields[1] == 9 && fields[2] > 15))
                && fields[3] == 1) {
                julian_day += 10;
            }
            if ((fields[0] > 1700 || (fields[0] == 1700 && fields[1] >= 2)) && fields[3] == 1) {
                julian_day += 1;
            }
            if ((fields[0] > 1800 || (fields[0] == 1800 && fields[1] >= 2)) && fields[3] == 1) {
                julian_day += 1;
            }
            if ((fields[0] > 1900 || (fields[0] == 1900 && fields[1] >= 2)) && fields[3] == 1) {
                julian_day += 1;
            }
            if ((fields[0] > 2100 || (fields[0] == 2100 && fields[1] >= 2)) && fields[3] == 1) {
                julian_day += 1;
            }
            calendar.set(Calendar.JULIAN_DAY, julian_day);
            return calendar;
        } catch (Exception e) {
            throw new MCRException("The ancient julian date is false.", e);
        }
    }

    /**
     * This method convert a islamic calendar date to a IslamicCalendar valuei civil mode.
     * The syntax for the islamic input is: <br>
     * <ul>
     * <li> [[[t]t.][m]m.][yyy]y [H.|h.]</li>
     * <li> [.\u0647 | .\u0647 .\u0642] [[[t]t.][m]m.][yyy]y</li>
     * <li> y[yyy][-m[m][-t[t]]] H.|h.</li>
     * </ul>
     *
     * @param date_string
     *            the date as string.
     * @param last
     *            the value is true if the date should be filled with the
     *            highest value of month or day like 12 or 30 else it fill the
     *            date with the lowest value 1 for month and day.
     *
     * @return the IslamicCalendar date value or null if an error was occurred.
     * @exception MCRException if parsing has an error
     */
    protected static IslamicCalendar getCalendarFromIslamicDate(String date_string, boolean last) {
        try {
            date_string = date_string.toUpperCase(Locale.ROOT);
            int start = 0;
            int ende = date_string.length();
            int i = date_string.indexOf("H.");
            if (i != -1) {
                ende = i;
            }
            if (date_string.length() > 10) {
                i = date_string.indexOf(".\u0647.\u0642");
                if (i != -1) {
                    start = 3;
                } else {
                    i = date_string.indexOf(".\u0647");
                    if (i != -1) {
                        start = 2;
                    }
                }
            }

            date_string = date_string.substring(start, ende).trim();

            // german or ISO?
            start = 0;
            boolean iso = false;
            String token = ".";

            if (date_string.indexOf("-", start + 1) != -1) {
                iso = true;
                token = "-";
            }
            //
            int firstdot = date_string.indexOf(token, start + 1);
            int secdot = -1;

            if (firstdot != -1) {
                secdot = date_string.indexOf(token, firstdot + 1);
            }

            int day = 1;
            int mon = 0;
            int year = 0;
            if (secdot != -1) { // day month year
                if (iso) {
                    year = Integer.parseInt(date_string.substring(start, firstdot));
                    mon = Integer.parseInt(date_string.substring(firstdot + 1, secdot)) - 1;
                    day = Integer.parseInt(date_string.substring(secdot + 1, date_string.length()));
                } else {
                    day = Integer.parseInt(date_string.substring(start, firstdot));
                    mon = Integer.parseInt(date_string.substring(firstdot + 1, secdot)) - 1;
                    year = Integer.parseInt(date_string.substring(secdot + 1, date_string.length()));
                }
            } else {
                if (firstdot != -1) { // month year
                    if (iso) {
                        year = Integer.parseInt(date_string.substring(start, firstdot));
                        mon = Integer.parseInt(date_string.substring(firstdot + 1, date_string.length())) - 1;
                    } else {
                        mon = Integer.parseInt(date_string.substring(start, firstdot)) - 1;
                        year = Integer.parseInt(date_string.substring(firstdot + 1, date_string.length()));
                    }

                    if (last) {
                        if (mon % 2 == 0) {
                            day = 30;
                        }
                        if (mon % 2 == 1) {
                            day = 29;
                        }

                    }
                } else { // year
                    year = Integer.parseInt(date_string.substring(start, date_string.length()));

                    if (last) {
                        mon = 11;
                        day = 29;
                    }
                }
            }
            // test of the monthly
            if (mon > 11 || mon < 0) {
                throw new MCRException("The month of the date is inadmissible.");
            }
            // Test of the daily
            if (day > 30 || mon % 2 == 1 && mon < 11 && day > 29 || day < 1) {
                throw new MCRException("The day of the date is inadmissible.");
            }
            IslamicCalendar calendar = new IslamicCalendar();
            calendar.setCivil(true);
            calendar.set(year, mon, day);
            return calendar;
        } catch (Exception e) {
            throw new MCRException("The ancient islamic date is false.", e);
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
     * @exception MCRException if parsing has an error
     */

    protected static HebrewCalendar getCalendarFromHebrewDate(String datestr, boolean last) {
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
     * Check the date string for ethiopic or coptic calendar
     *
     * @param date_string the date string
     * @param last the flag for first / last day
     * @return an integer array with [0] = year; [1] = month; [2] = day; [3] = era : -1 = B.M.: +1 = A.M.
     * @throws Exception
     */
    private static int[] checkDateStringForCopticCalendar(String date_string, boolean last) {
        int[] fields = new int[4];
        date_string = date_string.trim();
        // test before Martyrium
        boolean bm = false;
        int start = 0;
        int ende = date_string.length();
        ende = date_string.length();
        if (date_string.length() > 4) {
            int i = date_string.indexOf("A.M.");
            if (i != -1) {
                start = 0;
                ende = i - 1;
            }
            i = date_string.indexOf("a.M.");
            if (i != -1) {
                start = 0;
                ende = i - 1;
            }
            i = date_string.indexOf("E.E.");
            if (i != -1) {
                start = 0;
                ende = i - 1;
            }
        }
        date_string = date_string.substring(start, ende).trim();

        // german or ISO?
        start = 0;
        boolean iso = false;
        String token = ".";

        if (date_string.indexOf("-", start + 1) != -1) {
            iso = true;
            token = "-";
        }
        //
        int firstdot = date_string.indexOf(token, start + 1);
        int secdot = -1;

        if (firstdot != -1) {
            secdot = date_string.indexOf(token, firstdot + 1);
        }
        int day = 1;
        int mon = 0;
        int year = 0;
        if (secdot != -1) { // day, mon, year
            if (iso) {
                year = Integer.parseInt(date_string.substring(start, firstdot));
                mon = Integer.parseInt(date_string.substring(firstdot + 1, secdot)) - 1;
                day = Integer.parseInt(date_string.substring(secdot + 1, date_string.length()));
            } else {
                day = Integer.parseInt(date_string.substring(start, firstdot));
                mon = Integer.parseInt(date_string.substring(firstdot + 1, secdot)) - 1;
                year = Integer.parseInt(date_string.substring(secdot + 1, date_string.length()));
            }
        } else {
            if (firstdot != -1) { // mon, year
                if (iso) {
                    year = Integer.parseInt(date_string.substring(start, firstdot));
                    mon = Integer.parseInt(date_string.substring(firstdot + 1, date_string.length())) - 1;
                } else {
                    mon = Integer.parseInt(date_string.substring(start, firstdot)) - 1;
                    year = Integer.parseInt(date_string.substring(firstdot + 1, date_string.length()));
                }

                if (last) {
                    if (mon <= 11) {
                        day = 30;
                    } else {
                        day = 5;
                    }
                }
            } else { // year
                year = Integer.parseInt(date_string.substring(start, date_string.length()));

                if (last) {
                    mon = 12;
                    day = 5;
                }
            }
        }
        // test of the monthly
        if (mon > 12 || mon < 0) {
            throw new MCRException("The month of the date is inadmissible.");
        }
        // Test of the daily
        if (day > 30 || day < 1 || day > 6 && mon == 12) {
            throw new MCRException("The day of the date is inadmissible.");
        }
        if (bm) {
            year = -year + 1; // if before Matyrium
        }
        fields[0] = year;
        fields[1] = mon;
        fields[2] = day;
        return fields;
    }

    /**
     * This method convert a CopticCalendar date to a CopticCalendar value. The
     * syntax for the coptic input is: <br>
     * <ul>
     * <li> [[[t]t.][m]m.][yyy]y [[A.|a.]M.]</li>
     * <li> y[yyy][-m[m][-t[t]]] [A.|a.]M.]</li>
     * </ul>
     *
     * @param date_string
     *            the date as string.
     * @param last
     *            the value is true if the date should be filled with the
     *            highest value of month or day like 12 or 30 else it fill the
     *            date with the lowest value 1 for month and day.
     *
     * @return the CopticCalendar date value or null if an error was occurred.
     * @exception MCRException if parsing has an error
     */
    protected static CopticCalendar getCalendarFromCopticDate(String date_string, boolean last) {
        try {
            int fields[] = checkDateStringForCopticCalendar(date_string, last);
            CopticCalendar calendar = new CopticCalendar();
            calendar.set(fields[0], fields[1], fields[2]);
            return calendar;
        } catch (Exception e) {
            throw new MCRException("The ancient coptic calendar date is false.", e);
        }
    }

    /**
     * This method convert a EthiopicCalendar date to a EthiopicCalendar value.
     * The syntax for the ethiopic input is: <br>
     * <ul>
     * <li> [[[t]t.][m]m.][yyy]y [E.E.]</li>
     * <li> y[yyy][-m[m][-t[t]]] [E.E.]</li>
     * </ul>
     *
     * @param date_string
     *            the date as string.
     * @param last
     *            the value is true if the date should be filled with the
     *            highest value of month or day like 13 or 30 else it fill the
     *            date with the lowest value 1 for month and day.
     *
     * @return the EthiopicCalendar date value or null if an error was occurred.
     * @exception MCRException if parsing has an error
     */
    protected static EthiopicCalendar getCalendarFromEthiopicDate(String date_string, boolean last) {
        try {
            int fields[] = checkDateStringForCopticCalendar(date_string, last);
            EthiopicCalendar calendar = new EthiopicCalendar();
            calendar.set(fields[0], fields[1], fields[2]);
            return calendar;
        } catch (Exception e) {
            throw new MCRException("The ancient ethiopic calendar date is false.", e);
        }
    }

    /**
     * This method convert a JapaneseCalendar date to a JapaneseCalendar value.
     * The syntax for the japanese input is: <br>
     * <ul>
     * <li> [[[t]t.][m]m.][H|M|S|T][yyy]y <br>
     * H: Heisei; M: Meiji, S: Showa, T: Taiso
     * </li>
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
     * @exception MCRException if parsing has an error
     */
    protected static JapaneseCalendar getCalendarFromJapaneseDate(String datestr, boolean last) {
        try {
            datestr = datestr.trim();

            // boolean bm = false;
            int start = 0;

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

            int era;
            switch (syear.substring(0, 1)) {
                case "H":
                    era = 235;
                    break;
                case "S":
                    era = 234;
                    break;
                case "T":
                    era = 233;
                    break;
                case "M":
                    era = 232;
                    break;
                default:
                    era = 0;
            }
            year = Integer.parseInt(syear.substring(1).trim());
            // test of the monthly
            if (mon > 12 || mon < 0) {
                throw new MCRException("The month of the date is inadmissible.");
            }
            // Test of the daily
            if (day > 30 || day < 1 || day > 6 && mon == 12) {
                throw new MCRException("The day of the date is inadmissible.");
            }

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
     * This method convert a BuddhistCalendar date to a IslamicCalendar value.
     * The syntax for the buddhist input is: <br>
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
     * @exception MCRException if parsing has an error
     */

    protected static BuddhistCalendar getCalendarFromBuddhistDate(String datestr, boolean last) {
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
            if ((mon == 0 || mon == 2 || mon == 4 || mon == 6 || mon == 7 || mon == 9 || mon == 11) && day > 31
                || (mon == 3 || mon == 5 || mon == 8 || mon == 10) && day > 30 || mon == 1 && day > 29
                    && year % 4 == 0
                || mon == 1 && day > 28 && year % 4 > 0 || day < 1) {
                throw new MCRException("The day of the date is inadmissible.");
            }
            if (bb) {
                year = -year + 1; // if before Buddha
            }

            if (year == 2125 && mon == 9 && day >= 5 && day < 15) {
                day = 15;
            }

            budcal.set(year, mon, day);
            return budcal;
        } catch (Exception e) {
            throw new MCRException("The ancient buddhist date is false.", e);
        }
    }

    /**
     * This method convert a PersicCalendar date to a GregorianCalendar value.
     * The The syntax for the persian input is: <br>
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
     * @exception MCRException if parsing has an error
     */
    protected static GregorianCalendar getCalendarFromPersicDate(String datestr, boolean last) {
        try {
            datestr = datestr.trim();
            // test before
            boolean bb = false;
            int start = 0;
            if (datestr.substring(0, 1).equals("-")) {
                bb = true;
                start = 1;
                datestr = datestr.substring(start).trim();
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
            if (bb) {
                year = -year + 1;
            }
            njahr = year + 621;

            GregorianCalendar newdate = new GregorianCalendar();
            newdate.set(njahr, 2, 20); // yearly beginning to 20.3.
            // beginning of the month (day to year)
            int begday = 0;
            if (mon == 1) {
                begday = 31;
            }
            if (mon == 2) {
                begday = 62;
            }
            if (mon == 3) {
                begday = 93;
            }
            if (mon == 4) {
                begday = 124;
            }
            if (mon == 5) {
                begday = 155;
            }
            if (mon == 6) {
                begday = 186;
            }
            if (mon == 7) {
                begday = 216;
            }
            if (mon == 8) {
                begday = 246;
            }
            if (mon == 9) {
                begday = 276;
            }
            if (mon == 10) {
                begday = 306;
            }
            if (mon == 11) {
                begday = 336;
            }
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
                newdate.set(1582, mon - 1, 15);
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
     * @exception MCRException if parsing has an error
     */
    protected static GregorianCalendar getCalendarFromArmenianDate(String datestr, boolean last) {
        try {
            datestr = datestr.trim();
            // test before
            boolean ba = false;
            int start = 0;
            if (datestr.substring(0, 1).equals("-")) {
                ba = true;
                start = 1;
                datestr = datestr.substring(start).trim();
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
                        if (mon <= 12) {
                            day = 30;
                        }
                        if (mon == 13) {
                            day = 5;
                        }
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
            if (mon > 13 || mon < 1) {
                throw new MCRException("The month of the date is inadmissible.");
            }
            // Test of the daily
            if (day > 30 || day < 1 || day > 5 && mon == 13) {
                throw new MCRException("The day of the date is inadmissible.");
            }
            int difyear;
            int difday;
            int jhd = 1600;
            int ndifday = 0;
            if (ba) {
                year = -year + 1;
            }
            GregorianCalendar ecal = new GregorianCalendar();
            if (year * 10000 + mon * 100 + day >= 10311214) {// Jahr >
                // 14.12.1031
                difyear = year - 1031;
                difday = difyear * 365 + (mon - 1) * 30 + day - 344;
                ecal.set(1582, 9, 15);
                ecal.add(Calendar.DATE, difday);
            }
            if (year * 10000 + mon * 100 + day < 10311214 && year * 10000 + mon * 100 + day > 10311204) { //
                ecal.set(1582, 9, 15);
            }
            if (year * 10000 + mon * 100 + day <= 10311204) {// Jahr <
                // 5.10.1592
                ecal.set(1582, 9, 15);
                difyear = year - 1031;
                int daysyear = 36525;
                difday = difyear * 365 + (mon - 1) * 30 + day - 334;

                if (difday <= -30168) {
                    ndifday = ndifday - 30168;
                    jhd = 1500;
                    difday = difday + 30167;
                    while (difday < 0) {
                        if (difday < -daysyear) { // 36525
                            ndifday = ndifday - daysyear;
                            jhd = jhd - 100;
                            if (jhd == 0) {
                                jhd = -1;
                            }
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
     * The The syntax for the egyptian (Nabonassar) input is: <br>
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
     * @exception MCRException if parsing has an error
     */
    protected static GregorianCalendar getCalendarFromEgyptianDate(String datestr, boolean last) {
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
                        if (mon <= 12) {
                            day = 30;
                        }
                        if (mon == 13) {
                            day = 5;
                        }
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
            if (mon > 13 || mon < 1) {
                throw new MCRException("The month of the date is inadmissible.");
            }
            // Test of the daily
            if (day > 30 || day < 1 || day > 5 && mon == 13) {
                throw new MCRException("The day of the date is inadmissible.");
            }
            int difyear;
            int difday;
            int jhd = 1600;
            int ndifday = 0;
            if (ba) {
                year = -year + 1;
            }
            GregorianCalendar ecal = new GregorianCalendar();
            if (year * 10000 + mon * 100 + day >= 23310314) {// Jahr >
                // 15.10.1592
                difyear = year - 2331;
                difday = difyear * 365 + (mon - 1) * 30 + day - 74;
                ecal.set(1582, 9, 15);
                ecal.add(Calendar.DATE, difday);
            }

            if (year * 10000 + mon * 100 + day < 23310314 && year * 10000 + mon * 100 + day >= 23310304) { //
                ecal.set(1582, 9, 15);
            }
            if (year * 10000 + mon * 100 + day < 23310304) {// Jahr <
                // 5.10.1592
                ecal.set(1582, 9, 15);
                difyear = year - 2331;
                int daysyear = 36525;
                difday = difyear * 365 + (mon - 1) * 30 + day - 64;

                if (difday <= -30168) {
                    ndifday = ndifday - 30168;
                    jhd = 1500;
                    difday = difday + 30167;
                    while (difday < 0) {
                        if (difday < -daysyear) { // days of 100 years 36525
                            ndifday = ndifday - daysyear;
                            jhd = jhd - 100;
                            if (jhd == 0) {
                                jhd = -1;
                            }
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
     * @return the Julian Day number as Integer
     */
    public static int getJulianDayNumber(Calendar input_calendar) {
        return input_calendar.get(Calendar.JULIAN_DAY);
    }

    /**
     * This method return the Julian Day number for a given Calendar instance.
     *
     * @return the Julian Day number as String
     */
    public static String getJulianDayNumberAsString(Calendar input_calendar) {
        return Integer.toString(input_calendar.get(Calendar.JULIAN_DAY));
    }

    /**
     * This method get the Gregorian calendar form a given calendar
     *
     * @param input_calendar
     *            an instance of a Calendar
     * @return a Gregorian calendar
     */
    public static GregorianCalendar getGregorianCalendarOfACalendar(Calendar input_calendar) {
        int julian_day = getJulianDayNumber(input_calendar);
        GregorianCalendar ret = new GregorianCalendar();
        ret.set(Calendar.JULIAN_DAY, julian_day);
        return ret;
    }

    /**
     * This method returns the date as string in format 'yy-MM-dd G'.
     *
     * @return the date string
     */
    public static String getCalendarDateToFormattedString(Calendar calendar) {
        if (calendar instanceof IslamicCalendar) {
            return getCalendarDateToFormattedString(calendar, "dd.MM.yyyy");
        } else if (calendar instanceof GregorianCalendar) {
            return getCalendarDateToFormattedString(calendar, "yyyy-MM-dd G");
        }
        return getCalendarDateToFormattedString(calendar, "yyyy-MM-dd G");
    }

    /**
     * This method returns the date as string.
     *
     * @param calendar
     *            the Calendar date
     * @param format
     *            the format of the date as String
     *
     * @return the date string in the format. If the format is wrong dd.MM.yyyy
     *         G is set. If the date is wrong an empty string will be returned.
     *         The output is depending on calendar type. For Calendar it will use
     *         the Julian Calendar to 05.10.1582. Then it use the Gregorian Calendar.
     */
    public static String getCalendarDateToFormattedString(Calendar calendar, String format) {
        if (calendar == null || format == null || format.trim().length() == 0) {
            return "";
        }
        SimpleDateFormat formatter = null;
        try {
            if (calendar instanceof IslamicCalendar) {
                formatter = new SimpleDateFormat(format, new Locale("en"));
            } else if (calendar instanceof GregorianCalendar) {
                formatter = new SimpleDateFormat(format, new Locale("en"));
            } else {
                formatter = new SimpleDateFormat(format, new Locale("en"));
            }
        } catch (Exception e) {
            formatter = new SimpleDateFormat("dd.MM.yyyy G", new Locale("en"));
        }
        try {
            formatter.setCalendar(calendar);
            if (calendar instanceof IslamicCalendar) {
                return formatter.format(calendar.getTime()) + " h.";
            } else if (calendar instanceof CopticCalendar) {
                return formatter.format(calendar.getTime()) + " A.M.";
            } else if (calendar instanceof EthiopicCalendar) {
                return formatter.format(calendar.getTime()) + " E.E.";
            } else {
                return formatter.format(calendar.getTime());
            }
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * This method returns the calendar type as string.
     *
     * @param calendar
     *            the Calendar date
     * @return The clendar type as string. If Calendar is empty an empty string will be returned.
     */
    public static String getCalendarTypeString(Calendar calendar) {
        if (calendar == null) {
            return "";
        }
        if (calendar instanceof IslamicCalendar) {
            return TAG_ISLAMIC;
        } else if (calendar instanceof CopticCalendar) {
            return TAG_COPTIC;
        } else if (calendar instanceof EthiopicCalendar) {
            return TAG_ETHIOPIC;
        } else if (calendar instanceof GregorianCalendar) {
            return TAG_GREGORIAN;
        } else {
            return TAG_JULIAN;
        }
    }

}
