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

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
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
import com.ibm.icu.util.ULocale;

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

    /**
     * Logger
     */
    private static final Logger LOGGER = LogManager.getLogger(MCRCalendar.class.getName());

    /**
     * Tag for Buddhistic calendar
     */
    public static final String TAG_BUDDHIST = "buddhist";

    /**
     * Tag for Chinese calendar
     */
    public static final String TAG_CHINESE = "chinese";

    /**
     * Tag for Coptic calendar
     */
    public static final String TAG_COPTIC = "coptic";

    /**
     * Tag for Ethiopic calendar
     */
    public static final String TAG_ETHIOPIC = "ethiopic";

    /**
     * Tag for Gregorian calendar
     */
    public static final String TAG_GREGORIAN = "gregorian";

    /**
     * Tag for Hebrew calendar
     */
    public static final String TAG_HEBREW = "hebrew";

    /**
     * Tag for Islamic calendar
     */
    public static final String TAG_ISLAMIC = "islamic";

    /**
     * Tag for Japanese calendar
     */
    public static final String TAG_JAPANESE = "japanese";

    /**
     * Tag for Julian calendar
     */
    public static final String TAG_JULIAN = "julian";

    /**
     * Tag for Persic calendar
     */
    public static final String TAG_PERSIC = "persic";

    /**
     * Tag for Armenian calendar
     */
    public static final String TAG_ARMENIAN = "armenian";

    /**
     * Tag for Egyptian calendar
     */
    public static final String TAG_EGYPTIAN = "egyptian";

    /**
     * Minimum Julian Day number is 0 = 01.01.4713 BC
     */
    public static final int MIN_JULIAN_DAY_NUMBER = 0;

    /**
     * Maximum Julian Day number is 3182057 = 28.01.4000
     */
    public static final int MAX_JULIAN_DAY_NUMBER = 3182057;

    /**
     * a list of calendar tags they are supported in this class
     */
    public static final List<String> CALENDARS_LIST = List.of(
        TAG_GREGORIAN, TAG_JULIAN, TAG_ISLAMIC, TAG_BUDDHIST, TAG_COPTIC, TAG_ETHIOPIC, TAG_PERSIC, TAG_JAPANESE,
        TAG_ARMENIAN, TAG_EGYPTIAN, TAG_HEBREW);

    /**
     * the Julian day of the first day in the armenian calendar, 1.1.1 arm = 13.7.552 greg
     */
    public static final int FIRST_ARMENIAN_DAY;

    /**
     * the Julian day of the first day in the egyptian calendar, 1.1.1 eg = 18.2.747 BC greg
     */
    public static final int FIRST_EGYPTIAN_DAY;

    static {
        final Calendar firstArmenian = GregorianCalendar.getInstance();
        firstArmenian.set(552, GregorianCalendar.JULY, 13);
        FIRST_ARMENIAN_DAY = firstArmenian.get(Calendar.JULIAN_DAY);

        final Calendar firstEgypt = GregorianCalendar.getInstance();
        firstEgypt.set(747, GregorianCalendar.FEBRUARY, 18);
        firstEgypt.set(Calendar.ERA, GregorianCalendar.BC);
        FIRST_EGYPTIAN_DAY = firstEgypt.get(Calendar.JULIAN_DAY);
    }

    private static final String MSG_CALENDAR_UNSUPPORTED = "Calendar %s is not supported!";

    /**
     * @see #getHistoryDateAsCalendar(String, boolean, String)
     */
    public static Calendar getHistoryDateAsCalendar(String input, boolean last, CalendarType calendarType) {
        LOGGER.debug("Input of getHistoryDateAsCalendar: {}  {}  {}", input, calendarType, Boolean.toString(last));

        final String dateString = StringUtils.trim(input);
        // check dateString
        if (StringUtils.isBlank(dateString)) {
            throw new MCRException("The ancient date string is null or empty");
        }

        final Calendar out;
        if (dateString.equals("4713-01-01 BC")) {
            LOGGER.debug("Date string contains MIN_JULIAN_DAY_NUMBER");
            out = new GregorianCalendar();
            out.set(Calendar.JULIAN_DAY, MCRCalendar.MIN_JULIAN_DAY_NUMBER);
            return out;
        }
        if (dateString.equals("4000-01-28 AD")) {
            LOGGER.debug("Date string contains MAX_JULIAN_DAY_NUMBER");
            out = new GregorianCalendar();
            out.set(Calendar.JULIAN_DAY, MCRCalendar.MAX_JULIAN_DAY_NUMBER);
            return out;
        }

        switch (calendarType) {
        case Armenian: {
            out = getCalendarFromArmenianDate(dateString, last);
            break;
        }
        case Buddhist: {
            out = getCalendarFromBuddhistDate(dateString, last);
            break;
        }
        case Coptic: {
            out = getCalendarFromCopticDate(dateString, last);
            break;
        }
        case Egyptian: {
            out = getCalendarFromEgyptianDate(dateString, last);
            break;
        }
        case Ethiopic: {
            out = getCalendarFromEthiopicDate(dateString, last);
            break;
        }
        case Gregorian: {
            out = getCalendarFromGregorianDate(dateString, last);
            break;
        }
        case Hebrew: {
            out = getCalendarFromHebrewDate(dateString, last);
            break;
        }
        case Islamic: {
            out = getCalendarFromIslamicDate(dateString, last);
            break;
        }
        case Japanese: {
            out = getCalendarFromJapaneseDate(dateString, last);
            break;
        }
        case Julian: {
            out = getCalendarFromJulianDate(dateString, last);
            break;
        }
        case Persic: {
            out = getCalendarFromPersicDate(dateString, last);
            break;
        }
        default: {
            throw new MCRException("Calendar type " + calendarType + " not supported!");
        }
        }

        LOGGER.debug("Output of getHistoryDateAsCalendar: {}", getCalendarDateToFormattedString(out));
        return out;
    }

    /**
     * This method check an ancient date string for the given calendar. For
     * syntax of the date string see javadocs of calendar methods.
     *
     * @param dateString     the date as string.
     * @param last           the value is true if the date should be filled with the
     *                       highest value of month or day like 12 or 31 else it fill the
     *                       date with the lowest value 1 for month and day.
     * @param calendarString the calendar name as String, kind of the calendars are
     *                       ('gregorian', 'julian', 'islamic', 'buddhist', 'coptic',
     *                       'ethiopic', 'persic', 'japanese', 'armenian' or 'egyptian' )
     * @return the ICU Calendar instance of the concrete calendar type or null if an error was occurred.
     * @throws MCRException if parsing has an error
     */
    public static Calendar getHistoryDateAsCalendar(String dateString, boolean last, String calendarString)
        throws MCRException {
        return getHistoryDateAsCalendar(dateString, last, CalendarType.of(calendarString));
    }

    /**
     * Check the date string for julian or gregorian calendar
     *
     * @param dateString the date string
     * @param last       the flag for first / last day
     * @return an integer array with [0] = year; [1] = month; [2] = day; [3] = era : -1 = BC : +1 = AC
     */
    private static int[] checkDateStringForJulianCalendar(String dateString, boolean last, CalendarType calendarType) {
        // look for BC
        final String dateTrimmed = StringUtils.trim(StringUtils.upperCase(dateString, Locale.ROOT));

        final boolean bc = beforeZero(dateTrimmed, calendarType);
        final String cleanDate = cleanDate(dateTrimmed, calendarType);
        final int[] fields = parseDateString(cleanDate, last, calendarType);
        final int year = fields[0];
        final int mon = fields[1];
        final int day = fields[2];
        final int era = bc ? -1 : 1;

        // test of the monthly
        if (mon > GregorianCalendar.DECEMBER || mon < GregorianCalendar.JANUARY) {
            throw new MCRException("The month of the date is inadmissible.");
        }

        // Test of the daily
        if (day > 31) {
            throw new MCRException("The day of the date is inadmissible.");
        } else if ((day > 30) && (mon == GregorianCalendar.APRIL || mon == GregorianCalendar.JUNE ||
            mon == GregorianCalendar.SEPTEMBER || mon == GregorianCalendar.NOVEMBER)) {
            throw new MCRException("The day of the date is inadmissible.");
        } else if ((day > 29) && (mon == GregorianCalendar.FEBRUARY)) {
            throw new MCRException("The day of the date is inadmissible.");
        } else if ((day > 28) && (mon == GregorianCalendar.FEBRUARY) && !isLeapYear(year, calendarType)) {
            throw new MCRException("The day of the date is inadmissible.");
        }

        return new int[] { year, mon, day, era };
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
     * @param dateString
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
    protected static GregorianCalendar getCalendarFromGregorianDate(String dateString, boolean last)
        throws MCRException {
        try {
            int[] fields = checkDateStringForJulianCalendar(dateString, last, CalendarType.Gregorian);
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.set(fields[0], fields[1], fields[2]);
            if (fields[3] == -1) {
                calendar.set(Calendar.ERA, GregorianCalendar.BC);
            } else {
                calendar.set(Calendar.ERA, GregorianCalendar.AD);
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
     * @param dateString
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
    protected static Calendar getCalendarFromJulianDate(String dateString, boolean last) throws MCRException {
        try {
            int[] fields = checkDateStringForJulianCalendar(dateString, last, CalendarType.Julian);
            final Calendar calendar = Calendar.getInstance(CalendarType.Julian.getLocale());
            ((GregorianCalendar) calendar).setGregorianChange(new Date(Long.MAX_VALUE));
            calendar.set(fields[0], fields[1], fields[2]);
            if (fields[3] == -1) {
                calendar.set(Calendar.ERA, GregorianCalendar.BC);
            } else {
                calendar.set(Calendar.ERA, GregorianCalendar.AD);
            }

            return calendar;
        } catch (Exception e) {
            throw new MCRException("The ancient julian date is false.", e);
        }
    }

    /**
     * This method converts an islamic calendar date to a IslamicCalendar value civil mode.
     * The syntax for the islamic input is: <br>
     * <ul>
     * <li> [[[t]t.][m]m.][yyy]y [H.|h.]</li>
     * <li> [.\u0647 | .\u0647 .\u0642] [[[t]t.][m]m.][yyy]y</li>
     * <li> y[yyy][-m[m][-t[t]]] H.|h.</li>
     * </ul>
     *
     * @param dateString
     *            the date as string.
     * @param last
     *            the value is true if the date should be filled with the
     *            highest value of month or day like 12 or 30 else it fill the
     *            date with the lowest value 1 for month and day.
     *
     * @return the IslamicCalendar date value or null if an error was occurred.
     * @exception MCRException if parsing has an error
     */
    protected static IslamicCalendar getCalendarFromIslamicDate(String dateString, boolean last) {
        final String dateTrimmed = StringUtils.trim(StringUtils.upperCase(dateString, Locale.ROOT));

        final boolean before = beforeZero(dateTrimmed, CalendarType.Islamic);
        final String cleanDate = cleanDate(dateTrimmed, CalendarType.Islamic);
        final int[] fields = parseDateString(cleanDate, last, CalendarType.Islamic);
        int year = fields[0];
        final int mon = fields[1];
        final int day = fields[2];

        if (before) {
            year = -year + 1;
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
     * @return the HebrewCalendar date value or null if an error was occurred.
     * @exception MCRException if parsing has an error
     */

    protected static HebrewCalendar getCalendarFromHebrewDate(String datestr, boolean last) {
        final String dateTrimmed = StringUtils.trim(StringUtils.upperCase(datestr, Locale.ROOT));

        final boolean before = beforeZero(dateTrimmed, CalendarType.Hebrew);
        if (before) {
            throw new MCRException("Dates before 1 not supported in Hebrew calendar!");
        }

        final String cleanDate = cleanDate(dateTrimmed, CalendarType.Hebrew);
        final int[] fields = parseDateString(cleanDate, last, CalendarType.Hebrew);
        final int year = fields[0];
        final int mon = fields[1];
        final int day = fields[2];

        HebrewCalendar hcal = new HebrewCalendar();
        hcal.set(year, mon, day);

        return hcal;
    }

    /**
     * Check the date string for ethiopic or coptic calendar
     *
     * @param dateString the date string
     * @param last       the flag for first / last day
     * @return an integer array with [0] = year; [1] = month; [2] = day; [3] = era : -1 = B.M.: +1 = A.M.
     */
    private static int[] checkDateStringForCopticCalendar(String dateString, boolean last, CalendarType calendarType) {
        final String dateTrimmed = StringUtils.trim(StringUtils.upperCase(dateString, Locale.ROOT));

        final boolean bm = beforeZero(dateTrimmed, calendarType);
        final String cleanDate = cleanDate(dateTrimmed, calendarType);
        final int[] fields = parseDateString(cleanDate, last, calendarType);
        int year = fields[0];
        final int mon = fields[1];
        final int day = fields[2];
        final int era = bm ? -1 : 1;

        if (bm) {
            year = -year + 1;
        }

        // test of the monthly
        if (mon > 12 || mon < 0) {
            throw new MCRException("The month of the date is inadmissible.");
        }
        // Test of the daily
        if (day > 30 || day < 1 || day > 6 && mon == 12) {
            throw new MCRException("The day of the date is inadmissible.");
        }

        return new int[] { year, mon, day, era };
    }

    /**
     * This method convert a CopticCalendar date to a CopticCalendar value. The
     * syntax for the coptic input is: <br>
     * <ul>
     * <li> [[[t]t.][m]m.][yyy]y [[A.|a.]M.]</li>
     * <li> y[yyy][-m[m][-t[t]]] [A.|a.]M.]</li>
     * </ul>
     *
     * @param dateString
     *            the date as string.
     * @param last
     *            the value is true if the date should be filled with the
     *            highest value of month or day like 12 or 30 else it fill the
     *            date with the lowest value 1 for month and day.
     *
     * @return the CopticCalendar date value or null if an error was occurred.
     * @exception MCRException if parsing has an error
     */
    protected static CopticCalendar getCalendarFromCopticDate(String dateString, boolean last) {
        try {
            final int[] fields = checkDateStringForCopticCalendar(dateString, last, CalendarType.Coptic);
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
     * @param dateString
     *            the date as string.
     * @param last
     *            the value is true if the date should be filled with the
     *            highest value of month or day like 13 or 30 else it fill the
     *            date with the lowest value 1 for month and day.
     *
     * @return the EthiopicCalendar date value or null if an error was occurred.
     * @exception MCRException if parsing has an error
     */
    protected static EthiopicCalendar getCalendarFromEthiopicDate(String dateString, boolean last) {
        try {
            final int[] fields = checkDateStringForCopticCalendar(dateString, last, CalendarType.Ethiopic);
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
     * <li> [[[t]t.][m]m.][H|M|S|T|R][yyy]y <br>
     * H: Heisei; M: Meiji, S: Showa, T: Taiso, R: Reiwa
     * </li>
     * <li> [H|M|S|T|R]y[yyy][-m[m][-t[t]]]</li>
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
        final String dateTrimmed = StringUtils.trim(StringUtils.upperCase(datestr, Locale.ROOT));
        final String cleanDate = cleanDate(dateTrimmed, CalendarType.Japanese);

        // japanese dates contain the era statement directly in the year e.g. 1.1.H2
        // before parsing we have to remove this
        final String eraToken;
        final int era;
        if (StringUtils.contains(cleanDate, "M")) {
            eraToken = "M";
            era = JapaneseCalendar.MEIJI;
        } else if (StringUtils.contains(cleanDate, "T")) {
            eraToken = "T";
            era = JapaneseCalendar.TAISHO;
        } else if (StringUtils.contains(cleanDate, "S")) {
            eraToken = "S";
            era = JapaneseCalendar.SHOWA;
        } else if (StringUtils.contains(cleanDate, "H")) {
            eraToken = "H";
            era = JapaneseCalendar.HEISEI;
        } else if (StringUtils.contains(cleanDate, "R")) {
            eraToken = "R";
            era = JapaneseCalendar.REIWA;
        } else {
            throw new MCRException("Japanese date " + datestr + " does not contain era statement!");
        }

        final String firstPart = StringUtils.substringBefore(cleanDate, eraToken);
        final String secondPart = StringUtils.substringAfter(cleanDate, eraToken);

        final int[] fields = parseDateString(firstPart + secondPart, last, CalendarType.Japanese);
        final int year = fields[0];
        final int mon = fields[1];
        final int day = fields[2];

        JapaneseCalendar jcal = new JapaneseCalendar();
        jcal.set(year, mon, day);
        jcal.set(Calendar.ERA, era);

        return jcal;
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
        // test before Buddhas
        final String dateTrimmed = StringUtils.trim(StringUtils.upperCase(datestr, Locale.ROOT));

        final boolean bb = beforeZero(dateTrimmed, CalendarType.Buddhist);
        final String cleanDate = cleanDate(dateTrimmed, CalendarType.Buddhist);
        final int[] fields = parseDateString(cleanDate, last, CalendarType.Buddhist);
        int year = fields[0];
        int mon = fields[1];
        int day = fields[2];

        if (bb) {
            year = -year + 1; // if before Buddha
        }

        if (year == 2125 && mon == 9 && day >= 5 && day < 15) {
            day = 15;
        }

        BuddhistCalendar budcal = new BuddhistCalendar();
        budcal.set(year, mon, day);

        return budcal;
    }

    /**
     * This method convert a PersicCalendar date to a GregorianCalendar value.
     * The syntax for the persian input is: <br>
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
            final String dateTrimmed = StringUtils.trim(StringUtils.upperCase(datestr, Locale.ROOT));

            final boolean bb = beforeZero(dateTrimmed, CalendarType.Persic);
            final String cleanDate = cleanDate(dateTrimmed, CalendarType.Persic);
            final int[] fields = parseDateString(cleanDate, last, CalendarType.Persic);
            final int year = fields[0];
            final int mon = fields[1];
            final int day = fields[2];

            final int njahr;
            if (bb) {
                njahr = -year + 1 + 621;
            } else {
                njahr = year + 621;
            }

            GregorianCalendar newdate = new GregorianCalendar();
            newdate.clear();
            newdate.set(njahr, Calendar.MARCH, 20); // yearly beginning to 20.3.

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

            final int min = b * 360 + 350 * c - d * 1440 + 720;
            if (njahr >= 0) {
                newdate.add(Calendar.MINUTE, min); // minute of day
                newdate.add(Calendar.DATE, begday); // day of the year
            } else {
                newdate.add(Calendar.DATE, begday + 2); // day of the year
                newdate.add(Calendar.MINUTE, min); // minute of day
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
        final String dateTrimmed = StringUtils.trim(StringUtils.upperCase(datestr, Locale.ROOT));

        final boolean before = beforeZero(dateTrimmed, CalendarType.Armenian);
        final String cleanDate = cleanDate(dateTrimmed, CalendarType.Armenian);
        final int[] fields = parseDateString(cleanDate, last, CalendarType.Armenian);
        int year = fields[0];
        int mon = fields[1];
        int day = fields[2];

        if (before) {
            year = -year + 1;
        }

        // Armenian calendar has every year an invariant of 365 days - these are added to the beginning of the
        // calendar defined in FIRST_ARMENIAN_DAY (13.7.552)
        int addedDays = (year - 1) * 365;
        if (mon == 12) {
            addedDays += 12 * 30;
        } else {
            addedDays += mon * 30;
        }
        addedDays += day - 1;

        final GregorianCalendar result = new GregorianCalendar();
        result.set(Calendar.JULIAN_DAY, (FIRST_ARMENIAN_DAY + addedDays));

        return result;
    }

    /**
     * This method convert a EgyptianCalendar date to a GregorianCalendar value.
     * The syntax for the egyptian (Nabonassar) input is: <br>
     * <ul>
     * <li> [-][[[t]t.][m]m.][yyy]y [A.N.]</li>
     * <li> [-] [[[t]t.][m]m.][yyy]y</li>
     * <li> [-] y[yyy][-m[m][-t[t]]] [A.N.]</li>
     * <li> [-] y[yyy][-m[m][-t[t]]]</li>
     * </ul>
     * <p>
     * For calculating the resulting Gregorian date, February, 18 747 BC is used as initial date for Egyptian calendar.
     *
     * @param datestr the date as string.
     * @param last    the value is true if the date should be filled with the
     *                highest value of month or day like 13 or 30 else it fill the
     *                date with the lowest value 1 for month and day.
     * @return the GregorianCalendar date value or null if an error was
     * occurred.
     */
    protected static GregorianCalendar getCalendarFromEgyptianDate(String datestr, boolean last) {
        final String dateTrimmed = StringUtils.trim(StringUtils.upperCase(datestr, Locale.ROOT));

        final boolean ba = beforeZero(dateTrimmed, CalendarType.Egyptian);
        final String cleanDate = cleanDate(dateTrimmed, CalendarType.Egyptian);
        final int[] fields = parseDateString(cleanDate, last, CalendarType.Egyptian);
        int year = fields[0];
        final int mon = fields[1];
        final int day = fields[2];

        if (ba) {
            year = -year + 1;
        }

        int addedDays = (year - 1) * 365;
        if (mon == 12) {
            addedDays += 12 * 30;
        } else {
            addedDays += mon * 30;
        }
        addedDays += day - 1;

        final GregorianCalendar result = new GregorianCalendar();
        result.set(Calendar.JULIAN_DAY, (FIRST_EGYPTIAN_DAY + addedDays));

        return result;
    }

    /**
     * This method return the Julian Day number for a given Calendar instance.
     *
     * @return the Julian Day number as Integer
     */
    public static int getJulianDayNumber(Calendar calendar) {
        return calendar.get(Calendar.JULIAN_DAY);
    }

    /**
     * This method return the Julian Day number for a given Calendar instance.
     *
     * @return the Julian Day number as String
     */
    public static String getJulianDayNumberAsString(Calendar calendar) {
        return Integer.toString(calendar.get(Calendar.JULIAN_DAY));
    }

    /**
     * This method get the Gregorian calendar form a given calendar
     *
     * @param calendar
     *            an instance of a Calendar
     * @return a Gregorian calendar
     */
    public static GregorianCalendar getGregorianCalendarOfACalendar(Calendar calendar) {
        int julianDay = getJulianDayNumber(calendar);
        GregorianCalendar ret = new GregorianCalendar();
        ret.set(Calendar.JULIAN_DAY, julianDay);
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
     * The method get a date String in format yyyy-MM-ddThh:mm:ssZ for ancient date values.
     *
     * @param date         the date string
     * @param useLastValue as boolean
     *                     - true if incomplete dates should be filled up with last month or last day
     * @param calendarName the name if the calendar defined in MCRCalendar
     * @return the date in format yyyy-MM-ddThh:mm:ssZ
     */
    public static String getISODateToFormattedString(String date, boolean useLastValue, String calendarName) {
        String formattedDate = null;
        try {
            Calendar calendar = MCRCalendar.getHistoryDateAsCalendar(date, useLastValue, calendarName);
            GregorianCalendar gregorianCalendar = MCRCalendar.getGregorianCalendarOfACalendar(calendar);
            formattedDate = MCRCalendar.getCalendarDateToFormattedString(gregorianCalendar, "yyyy-MM-dd")
                + "T00:00:00.000Z";
            if (gregorianCalendar.get(Calendar.ERA) == GregorianCalendar.BC) {
                formattedDate = "-" + formattedDate;
            }
        } catch (Exception e) {
            String errorMsg = "Error while converting date string : " + date + " - " + useLastValue +
                " - " + calendarName;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(errorMsg, e);
            }
            LOGGER.warn(errorMsg);
            return "";
        }
        return formattedDate;
    }

    /**
     * This method returns the calendar type as string.
     *
     * @param calendar the Calendar date
     * @return The calendar type as string. If Calendar is empty an empty string will be returned.
     */
    public static String getCalendarTypeString(Calendar calendar) {
        if (calendar == null) {
            return "";
        }
        if (calendar instanceof IslamicCalendar) {
            return TAG_ISLAMIC;
        } else if (calendar instanceof BuddhistCalendar) {
            return TAG_BUDDHIST;
        } else if (calendar instanceof CopticCalendar) {
            return TAG_COPTIC;
        } else if (calendar instanceof EthiopicCalendar) {
            return TAG_ETHIOPIC;
        } else if (calendar instanceof HebrewCalendar) {
            return TAG_HEBREW;
        } else if (calendar instanceof JapaneseCalendar) {
            return TAG_JAPANESE;
        } else if (calendar instanceof GregorianCalendar) {
            return TAG_GREGORIAN;
        } else {
            return TAG_JULIAN;
        }
    }

    /**
     * Parses a clean date string in German (d.m.y), English (d/m/y) or ISO (y-m-d) form and returns the year, month
     * and day as an array.
     *
     * @param dateString   the date to parse
     * @param last         flag to determine if the last month or day of a month is to be used when no month
     *                     or day is given
     * @param calendarType the calendar type to parse the date string for
     * @return a field containing year, month and day statements
     */
    public static int[] parseDateString(String dateString, boolean last, CalendarType calendarType) {
        // German, English or ISO?
        final boolean iso = isoFormat(dateString);
        final String delimiter = delimiter(dateString);

        // check for positions of year and month delimiters
        final int firstdot = StringUtils.indexOf(dateString, delimiter, 1);
        final int secdot = StringUtils.indexOf(dateString, delimiter, firstdot + 1);

        final int day;
        final int mon;
        final int year;
        if (secdot != -1) {
            // we have a date in the form of d.m.yy or y/m/d
            final int firstPart = Integer.parseInt(StringUtils.substring(dateString, 0, firstdot));
            final int secondPart = Integer.parseInt(StringUtils.substring(dateString, firstdot + 1, secdot));
            final int thirdPart = Integer.parseInt(StringUtils.substring(dateString, secdot + 1));
            if (iso) {
                year = firstPart;
                mon = secondPart - 1;
                day = thirdPart;
            } else {
                day = firstPart;
                mon = secondPart - 1;
                year = thirdPart;
            }
        } else {
            if (firstdot != -1) {
                // we have a date in form of m.y or y/m
                final int firstPart = Integer.parseInt(StringUtils.substring(dateString, 0, firstdot));
                final int secondPart = Integer.parseInt(StringUtils.substring(dateString, firstdot + 1));
                if (iso) {
                    year = firstPart;
                    mon = secondPart - 1;
                } else {
                    mon = firstPart - 1;
                    year = secondPart;
                }

                if (last) {
                    day = getLastDayOfMonth(mon, year, calendarType);
                } else {
                    day = 1;
                }
            } else {
                // we have just a year statement
                year = Integer.parseInt(dateString);
                if (last) {
                    mon = getLastMonth(year, calendarType);
                    day = getLastDayOfMonth(mon, year, calendarType);
                } else {
                    mon = getFirstMonth(calendarType);
                    day = 1;
                }
            }
        }

        return new int[] { year, mon, day };
    }

    /**
     * Returns true if the given input date is in ISO format (xx-xx-xx), otherwise false.
     *
     * @param input the input date to check
     * @return true if the given input date is in ISO format (xx-xx-xx), otherwise false
     */
    public static boolean isoFormat(String input) {
        return -1 != StringUtils.indexOf(input, "-", 1);
    }

    /**
     * Cleans a given date by removing era statements like -, AD, B.E. etc.
     *
     * @param input        the date to clean
     * @param calendarType the calendar type of the given date
     * @return the cleaned date containing only day, month and year statements
     */
    public static String cleanDate(String input, CalendarType calendarType) {
        final String date = StringUtils.trim(StringUtils.upperCase(input, Locale.ROOT));
        final int start;
        final int end;
        final int length = StringUtils.length(date);

        if (StringUtils.startsWith(date, "-")) {
            start = 1;
            end = length;
        } else {
            final int[] borders;
            switch (calendarType) {
            case Armenian: {
                borders = calculateArmenianDateBorders(date);
                break;
            }
            case Buddhist: {
                borders = calculateBuddhistDateBorders(date);
                break;
            }
            case Coptic:
            case Ethiopic: {
                borders = calculateCopticDateBorders(date);
                break;
            }
            case Egyptian: {
                borders = calculateEgyptianDateBorders(date);
                break;
            }
            case Gregorian:
            case Julian: {
                borders = calculateGregorianDateBorders(date);
                break;
            }
            case Hebrew: {
                borders = calculateHebrewDateBorders(date);
                break;
            }
            case Islamic: {
                borders = calculateIslamicDateBorders(date);
                break;
            }
            case Japanese: {
                borders = calculateJapaneseDateBorders(date);
                break;
            }
            case Persic: {
                borders = calculatePersianDateBorders(date);
                break;
            }
            default: {
                throw new MCRException(String.format(Locale.ROOT, MSG_CALENDAR_UNSUPPORTED, calendarType));
            }
            }

            start = borders[0];
            end = borders[1];
        }

        return StringUtils.trim(StringUtils.substring(date, start, end));
    }

    /**
     * Calculates the borders of an egyptian date.
     *
     * @param datestr the egyptian date contain era statements like -, A.N.
     * @return the indexes of the date string containing the date without era statements
     */
    public static int[] calculateEgyptianDateBorders(String datestr) {
        final int start;
        final int ende;
        final int length = StringUtils.length(datestr);

        if (StringUtils.startsWith(datestr, "-")) {
            start = 1;
        } else {
            start = 0;
        }

        if (StringUtils.contains(datestr, "A.N.")) {
            ende = StringUtils.indexOf(datestr, "A.N.");
        } else {
            ende = length;
        }

        return new int[] { start, ende };
    }

    /**
     * Calculates the borders of an armenian date.
     *
     * @param input the armenian date contain era statements like -
     * @return the indexes of the date string containing the date without era statements
     */
    public static int[] calculateArmenianDateBorders(String input) {
        final int start;
        if (StringUtils.startsWith(input, "-")) {
            start = 1;
        } else {
            start = 0;
        }

        return new int[] { start, StringUtils.length(input) };
    }

    /**
     * Calculates the borders of a japanese date.
     *
     * @param input the japanese date contain era statements like -
     * @return the indexes of the date string containing the date without era statements
     */
    public static int[] calculateJapaneseDateBorders(String input) {
        final int start;
        if (StringUtils.startsWith(input, "-")) {
            start = 1;
        } else {
            start = 0;
        }

        return new int[] { start, StringUtils.length(input) };
    }

    /**
     * Calculates the borders of a persian date.
     *
     * @param dateStr the persina date contain era statements like -
     * @return the indexes of the date string containing the date without era statements
     */
    public static int[] calculatePersianDateBorders(String dateStr) {
        final int start;
        if (StringUtils.startsWith(dateStr, "-")) {
            start = 1;
        } else {
            start = 0;
        }

        return new int[] { start, StringUtils.length(dateStr) };
    }

    /**
     * Calculates the borders of a coptic/ethiopian date.
     *
     * @param input the coptic/ethiopian date contain era statements like -, A.M, A.E.
     * @return the indexes of the date string containing the date without era statements
     */
    public static int[] calculateCopticDateBorders(String input) {
        final int start;
        final int end;
        final int length = StringUtils.length(input);

        if (StringUtils.startsWith(input, "-")) {
            start = 1;
            end = length;
        } else {
            start = 0;

            if (StringUtils.contains(input, "A.M")) {
                end = StringUtils.indexOf(input, "A.M.");
            } else if (StringUtils.contains(input, "E.E.")) {
                end = StringUtils.indexOf(input, "E.E.");
            } else {
                end = length;
            }
        }

        return new int[] { start, end };
    }

    /**
     * Calculates the borders of a hebrew date.
     *
     * @param input the hebrew date contain era statements like -
     * @return the indexes of the date string containing the date without era statements
     */
    public static int[] calculateHebrewDateBorders(String input) {
        return new int[] { 0, StringUtils.length(input) };
    }

    /**
     * Calculates the borders of an islamic date.
     *
     * @param dateString the islamic date contain era statements like -
     * @return the indexes of the date string containing the date without era statements
     */
    public static int[] calculateIslamicDateBorders(String dateString) {
        int start = 0;
        int ende = dateString.length();
        int i = dateString.indexOf("H.");
        if (i != -1) {
            ende = i;
        }
        if (dateString.length() > 10) {
            i = dateString.indexOf(".\u0647.\u0642");
            if (i != -1) {
                start = 3;
            } else {
                i = dateString.indexOf(".\u0647");
                if (i != -1) {
                    start = 2;
                }
            }
        }

        return new int[] { start, ende };
    }

    /**
     * Calculates the date borders for a Gregorian date in the form d.m.y [N. CHR|V.CHR|AD|BC]
     *
     * @param dateString the date string to parse
     * @return a field containing the start position of the date string in index 0 and the end position in index 1
     */
    public static int[] calculateGregorianDateBorders(String dateString) {
        final int start;
        final int end;
        final int length = StringUtils.length(dateString);

        if (StringUtils.contains(dateString, "N. CHR") || StringUtils.contains(dateString, "V. CHR")) {
            final int positionNChr = StringUtils.indexOf(dateString, "N. CHR");
            final int positionVChr = StringUtils.indexOf(dateString, "V. CHR");
            if (-1 != positionNChr) {
                if (0 == positionNChr) {
                    start = 7;
                    end = length;
                } else {
                    start = 0;
                    end = positionNChr - 1;
                }
            } else if (-1 != positionVChr) {
                if (0 == positionVChr) {
                    start = 7;
                    end = length;
                } else {
                    start = 0;
                    end = positionVChr - 1;
                }
            } else {
                start = 0;
                end = length;
            }
        } else if (StringUtils.contains(dateString, "AD") || StringUtils.contains(dateString, "BC")) {
            final int positionAD = StringUtils.indexOf(dateString, "AD");
            final int positionBC = StringUtils.indexOf(dateString, "BC");
            if (-1 != positionAD) {
                if (0 == positionAD) {
                    start = 2;
                    end = length;
                } else {
                    start = 0;
                    end = positionAD - 1;
                }
            } else if (-1 != positionBC) {
                if (0 == positionBC) {
                    start = 2;
                    end = length;
                } else {
                    start = 0;
                    end = positionBC - 1;
                }
            } else {
                start = 0;
                end = length;
            }
        } else {
            start = 0;
            end = length;
        }

        return new int[] { start, end };
    }

    /**
     * Calculates the date borders for a Buddhist date in the form d.m.y [B.E.]
     *
     * @param datestr the date string to parse
     * @return a field containing the start position of the date string in index 0 and the end position in index 1
     */
    public static int[] calculateBuddhistDateBorders(String datestr) {
        final int start;
        final int end;
        final int length = StringUtils.length(datestr);

        if (StringUtils.startsWith(datestr, "-")) {
            start = 1;
            end = length;
        } else {
            start = 0;

            if (StringUtils.contains(datestr, "B.E.")) {
                end = StringUtils.indexOf(datestr, "B.E.");
            } else {
                end = length;
            }
        }

        return new int[] { start, end };
    }

    /**
     * Returns the delimiter for the given date input: ., - or /.
     *
     * @param input the date input to check
     * @return the delimiter for the given date input
     */
    public static String delimiter(String input) {
        if (-1 != StringUtils.indexOf(input, "-", 1)) {
            return "-";
        } else if (-1 != StringUtils.indexOf(input, "/", 1)) {
            return "/";
        } else {
            return ".";
        }
    }

    /**
     * Returns true if the given date input is before the year zero of the given calendar type.
     * <p>
     * Examples:
     * <ul>
     * <li>1 BC is before zero for gregorian/julian calendars</li>
     * <li>-1 is before zero for all calendar types</li>
     * <li>1 AD is after zero for gregorian/julian calendars</li>
     * <li>1 is after zero for all calendar types</li>
     * </ul>
     *
     * @param input        the input date to check
     * @param calendarType the calendar type
     * @return true if the given input date is for the calendars zero date
     */
    public static boolean beforeZero(String input, CalendarType calendarType) {
        if (StringUtils.startsWith(input, "-")) {
            return true;
        }

        switch (calendarType) {
        case Buddhist: {
            return StringUtils.contains(input, "B.E.");
        }
        case Gregorian:
        case Julian: {
            return StringUtils.contains(input, "BC") || StringUtils.contains(input, "V. CHR");
        }
        case Coptic:
        case Hebrew:
        case Ethiopic:
        case Persic:
        case Chinese:
        case Islamic:
        case Armenian:
        case Egyptian:
        case Japanese: {
            // these calendars do not allow for an era statement other than -
            return false;
        }
        default: {
            throw new MCRException(String.format(Locale.ROOT, MSG_CALENDAR_UNSUPPORTED, calendarType));
        }
        }
    }

    /**
     * Returns the last day number for the given month, e.g. {@link GregorianCalendar#FEBRUARY} has 28 in normal years
     * and 29 days in leap years.
     *
     * @param month        the month number
     * @param year         the year
     * @param calendarType the calendar type to evaluate the last day for
     * @return the last day number for the given month
     */
    public static int getLastDayOfMonth(int month, int year, CalendarType calendarType) {
        final Calendar cal = Calendar.getInstance(calendarType.getLocale());

        if (calendarType == CalendarType.Julian) {
            ((GregorianCalendar) cal).setGregorianChange(new Date(Long.MAX_VALUE));
        }

        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.YEAR, year);

        return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    /**
     * Returns the first month of a year for the given calendar type, e.g. January for gregorian calendars.
     *
     * @param calendarType the calendar type to evaluate the first month for
     * @return the first month of a year for the given calendar type
     */
    public static int getFirstMonth(CalendarType calendarType) {
        switch (calendarType) {
        case Buddhist:
        case Gregorian:
        case Julian: {
            return GregorianCalendar.JANUARY;
        }
        case Coptic:
        case Egyptian: {
            return CopticCalendar.TOUT;
        }
        case Ethiopic: {
            return EthiopicCalendar.MESKEREM;
        }
        case Hebrew: {
            return HebrewCalendar.TISHRI;
        }
        case Islamic: {
            return IslamicCalendar.MUHARRAM;
        }
        case Armenian:
        case Persic: {
            return 0;
        }
        default: {
            throw new MCRException(String.format(Locale.ROOT, MSG_CALENDAR_UNSUPPORTED, calendarType));
        }
        }
    }

    /**
     * Returns the last month number of the given year for the given calendar type.
     *
     * @param year         the year to calculate last month number for
     * @param calendarType the calendar type
     * @return the last month number of the given year for the given calendar type
     */
    public static int getLastMonth(int year, CalendarType calendarType) {
        final Calendar cal = Calendar.getInstance(calendarType.getLocale());
        cal.set(Calendar.YEAR, year);

        return cal.getActualMaximum(Calendar.MONTH);
    }

    /**
     * Returns true, if the given year is a leap year in the given calendar type.
     *
     * @param year         the year to analyse
     * @param calendarType the calendar type
     * @return true, if the given year is a leap year in the given calendar type; otherwise false
     */
    public static boolean isLeapYear(int year, CalendarType calendarType) {
        switch (calendarType) {
        case Gregorian: {
            final GregorianCalendar cal = new GregorianCalendar();
            return cal.isLeapYear(year);
        }
        case Julian: {
            final GregorianCalendar cal = new GregorianCalendar();
            cal.setGregorianChange(new Date(Long.MAX_VALUE));
            return cal.isLeapYear(year);
        }
        default: {
            throw new MCRException(String.format(Locale.ROOT, MSG_CALENDAR_UNSUPPORTED, calendarType));
        }
        }
    }

    public enum CalendarType {
        Buddhist(TAG_BUDDHIST, new ULocale("@calendar=buddhist")),
        Chinese(TAG_CHINESE, new ULocale("@calendar=chinese")),
        Coptic(TAG_COPTIC, new ULocale("@calendar=coptic")),
        Ethiopic(TAG_ETHIOPIC, new ULocale("@calendar=ethiopic")),
        Gregorian(TAG_GREGORIAN, new ULocale("@calendar=gregorian")),
        Hebrew(TAG_HEBREW, new ULocale("@calendar=hebrew")),
        Islamic(TAG_ISLAMIC, new ULocale("@calendar=islamic-civil")),
        Japanese(TAG_JAPANESE, new ULocale("@calendar=japanese")),
        Julian(TAG_JULIAN, new ULocale("@calendar=gregorian")),
        Persic(TAG_PERSIC, new ULocale("@calendar=persian")),
        // Armenian calendar uses coptic calendar as a base, since both have 12 months + 5 days
        Armenian(TAG_ARMENIAN, new ULocale("@calendar=coptic")),
        // Egyptian calendar uses coptic calendar as a base, since both have 12 months + 5 days
        Egyptian(TAG_EGYPTIAN, new ULocale("@calendar=coptic"));

        private final String type;

        private final ULocale locale;

        CalendarType(String type, ULocale locale) {
            this.type = type;
            this.locale = locale;
        }

        public ULocale getLocale() {
            return locale;
        }

        public String getType() {
            return type;
        }

        public static CalendarType of(String type) {
            return Arrays.stream(CalendarType.values())
                .filter(current -> StringUtils.equals(current.getType(), type))
                .findFirst().orElseThrow();
        }
    }
}
