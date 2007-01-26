/*
 * $RCSfile$
 * $Revision$ $Date$
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

package org.mycore.datamodel.metadata;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import org.mycore.common.MCRException;

/**
 * This class implements all methods for handling with the MCRMetaHistoryDate
 * part of a metadata object.
 * 
 * @author Juergen Vogler
 * @version $Revision$ $Date$
 */
final public class MCRMetaHistoryDate extends MCRMetaDefault {
    /** The maximal length of 'text' */
    public static final int MCRHISTORYDATE_MAX_TEXT = 128;

    /** The first day of Julian Date */
    public static final String min = "BC01.01.4712";

    /** The last day of our era :=)) */
    public static final String max = "AD31.12.3999";

    /** The first day of Gregorian Calendar */
    public static final String shift = "AD15.10.1582";

    // MetaHistoryDate data
    private static GregorianCalendar default_von = getHistoryDate(min, false);

    private static GregorianCalendar default_bis = getHistoryDate(max, true);

    private static GregorianCalendar default_shift = getHistoryDate(shift, true);

    private String text;

    private GregorianCalendar von;

    private GregorianCalendar bis;

    private int ivon;

    private int ibis;

    private static int GREGORIAN = 0;

    private static int JULIAN = 1;

    private static int ISLAM = 2;

    /**
     * This is the constructor. <br>
     * The language element was set to <b>en </b>. All other elemnts are set to
     * an empty string.
     */
    public MCRMetaHistoryDate() {
        super();
        text = "";
        von = default_von;
        ivon = getJulianDayNumber(von);
        bis = default_bis;
        ibis = getJulianDayNumber(bis);
    }

    /**
     * This is the constructor. <br>
     * The language element was set. If the value of <em>default_lang</em> is
     * null, empty or false <b>en </b> was set. The subtag element was set to
     * the value of <em>set_subtag<em>. If the value of <em>set_subtag</em>
     * is null or empty an exception was throwed. The type element was set to
     * the value of <em>set_type<em>, if it is null, an empty string was set
     * to the type element.
     *
     * @param set_datapart     the global part of the elements like 'metadata'
     *                         or 'service'
     * @param set_subtag      the name of the subtag
     * @param default_lang    the default language
     * @param set_type        the optional type string
     * @param set_inherted    a value >= 0
     * @exception MCRException if the parameter values are invalid
     */
    public MCRMetaHistoryDate(String set_datapart, String set_subtag, String default_lang, String set_type, int set_inherted) throws MCRException {
        super(set_datapart, set_subtag, default_lang, set_type, set_inherted);
        text = "";
        von = default_von;
        ivon = getJulianDayNumber(von);
        bis = default_bis;
        ibis = getJulianDayNumber(bis);
    }

    /**
     * This method convert a ancient date to a GregorianCalendar value. The
     * syntax for the input is [-|AD|BC][[[t]t.][m]m.][yyy]y [AD|BC] in a a
     * given calendar.
     * 
     * @param datestr
     *            the date as string.
     * @param last
     *            the value is true if the date should be filled with the
     *            highest value of month or day like 12 or 31 else it fill the
     *            date with the lowest value 1 for month and day.
     * @param cal
     *            the calendar as String 'gregorian' or 'julian'. 'gregorian' is
     *            the default.
     * @return the GregorianCalendar date value or null if an error was occured.
     */
    public static final GregorianCalendar getHistoryDate(String datestr, boolean last) {
        return getHistoryDate(datestr, last, "gregorian");
    }

    /**
     * This method convert a ancient date to a GregorianCalendar value. The
     * syntax for the input is [-|AD|BC][[[t]t.][m]m.][yyy]y [AD|BC] in a a
     * given calendar.
     * 
     * @param datestr
     *            the date as string.
     * @param last
     *            the value is true if the date should be filled with the
     *            highest value of month or day like 12 or 31 else it fill the
     *            date with the lowest value 1 for month and day.
     * @param cal
     *            the calendar as String 'gregorian' or 'julian'. 'gregorian' is
     *            the default.
     * @return the GregorianCalendar date value or null if an error was occured.
     */
    public static final GregorianCalendar getHistoryDate(String datestr, boolean last, String cal) {
        // check
        datestr = datestr.trim();
        if (datestr == null) {
            return null;
        }
        if (datestr.length() > 18) {
            return null;
        }
        if (datestr.length() == 0) {
            return null;
        }
        int calint;
        if (cal == null)
            calint = GREGORIAN;
        else {
            cal = cal.trim().toLowerCase();
            if (!cal.equals("gregorian") && !cal.equals("julian"))
                calint = GREGORIAN;
            if (cal.equals("gregorian"))
                calint = GREGORIAN;
            if (cal.equals("julian"))
                calint = JULIAN;
        }
        // check data
        try {
            // suche nach v. Chr.
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
                    int i = datestr.indexOf("n. Chr.");
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
                    i = datestr.indexOf("v. Chr.");
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

            // deutsch oder ISO?
            start = 0;
            boolean iso = false;
            String token = ".";

            if (datestr.indexOf("-", start + 1) != -1) {
                iso = true;
                token = "-";
            }

            // Punkte/Striche ermitteln
            int firstdot = datestr.indexOf(token, start + 1);
            int secdot = -1;

            if (firstdot != -1) {
                secdot = datestr.indexOf(token, firstdot + 1);
            }

            // selektiern der Werte
            int day = 0;

            // selektiern der Werte
            int mon = 0;

            // selektiern der Werte
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
                        mon = Integer.parseInt(datestr.substring(firstdot + 1, datestr.length()));
                    } else {
                        mon = Integer.parseInt(datestr.substring(start, firstdot));
                        year = Integer.parseInt(datestr.substring(firstdot + 1, datestr.length()));
                    }

                    if (last) {
                        if (mon == 0) {
                            day = 31;
                        }

                        if (mon == 1) {
                            day = 28;
                        }

                        if (mon == 2) {
                            day = 31;
                        }

                        if (mon == 3) {
                            day = 30;
                        }

                        if (mon == 4) {
                            day = 31;
                        }

                        if (mon == 5) {
                            day = 30;
                        }

                        if (mon == 6) {
                            day = 31;
                        }

                        if (mon == 7) {
                            day = 31;
                        }

                        if (mon == 8) {
                            day = 30;
                        }

                        if (mon == 9) {
                            day = 31;
                        }

                        if (mon == 10) {
                            day = 30;
                        }

                        if (mon == 11) {
                            day = 31;
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

            // setzten AD/BC
            GregorianCalendar newdate = new GregorianCalendar();
            newdate.set(year, mon, day);

            if (bc) {
                newdate.set(GregorianCalendar.ERA, GregorianCalendar.BC);
            } else {
                newdate.set(GregorianCalendar.ERA, GregorianCalendar.AD);
            }

            return newdate;
        } catch (Exception e) {
            throw new MCRException("The ancient date is false.", e);
        }
    }

    /**
     * This methode returns the date as string.
     * 
     * @param date
     *            the GregorianCalendar date
     * 
     * @return the date string
     */
    public static final String getDateToString(GregorianCalendar date) {
        if (date == null) {
            return "";
        }

        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy G", (new Locale("en")));
        formatter.setCalendar(date);

        return formatter.format(date.getTime());
    }

    /**
     * The method set the text value.
     */
    public final void setText(String set) {
        if (set == null) {
            text = "";

            return;
        }

        if (set.length() <= MCRHISTORYDATE_MAX_TEXT) {
            text = set.trim();
        } else {
            text = set.substring(0, MCRHISTORYDATE_MAX_TEXT);
        }
    }

    /**
     * The method set the von and bis values to the default.
     */
    public final void setDefaultVonBis() {
        von = default_von;
        ivon = getJulianDayNumber(von);
        bis = default_bis;
        ibis = getJulianDayNumber(bis);
    }

    /**
     * The method checks and accepts the numeric date.
     */
    public final void setDateVonBis(String numdat) {
        numdat = numdat.trim();

        int i = numdat.indexOf("bis");

        if (i == -1) {
            setVonDate(numdat);
            setBisDate(numdat);
        } else {
            setVonDate(numdat.substring(0, i));
            setBisDate(numdat.substring(i + 3));
        }

        ivon = getJulianDayNumber(von);
        ibis = getJulianDayNumber(bis);
    }

    /**
     * This methode set the von to the given date.
     * 
     * @param set_date
     *            the date as GregorianCalendar
     */
    public final void setVonDate(GregorianCalendar set_date) {
        von = default_von;

        if (set_date != null) {
            von = set_date;
        }

        ivon = getJulianDayNumber(von);
    }

    /**
     * This methode set the von to the given date.
     * 
     * @param set_date
     *            a date string
     */
    public final void setVonDate(String set_date) {
        try {
            von = getHistoryDate(set_date, false);
        } catch (Exception e) {
            von = default_von;
        }

        ivon = getJulianDayNumber(von);
    }

    /**
     * This methode set the bis to the given date.
     * 
     * @param set_date
     *            the date as GregorianCalendar
     */
    public final void setBisDate(GregorianCalendar set_date) {
        bis = default_bis;

        if (set_date != null) {
            bis = set_date;
        }

        ibis = getJulianDayNumber(bis);
    }

    /**
     * This methode set the bis to the given date.
     * 
     * @param set_date
     *            a date string
     */
    public final void setBisDate(String set_date) throws MCRException {
        try {
            bis = getHistoryDate(set_date, true);
        } catch (Exception e) {
            bis = default_bis;
        }

        ibis = getJulianDayNumber(bis);
    }

    /**
     * This method get the 'text' text element.
     * 
     * @return the fundort
     */
    public final String getText() {
        return text;
    }

    /**
     * This method get the von element as GregorianCalendar.
     * 
     * @return the date
     */
    public final GregorianCalendar getVon() {
        return von;
    }

    /**
     * This methode return the von as string.
     * 
     * @return the date
     */
    public final String getVonToString() {
        return getDateToString(von);
    }

    /**
     * This method get the bis element as GregorianCalendar.
     * 
     * @return the date
     */
    public final GregorianCalendar getBis() {
        return bis;
    }

    /**
     * This methode return the bis as string.
     * 
     * @return the date
     */
    public final String getBisToString() {
        return getDateToString(bis);
    }

    /**
     * This method reads the XML input stream part from a DOM part for the
     * metadata of the document.
     * 
     * @param element
     *            a relevant JDOM element for the metadata
     */
    public final void setFromDOM(org.jdom.Element element) {
        super.setFromDOM(element);
        setText(element.getChildTextTrim("text"));
        setVonDate(element.getChildTextTrim("von"));
        setBisDate(element.getChildTextTrim("bis"));
    }

    /**
     * This method creates a XML stream for all data in this class, defined by
     * the MyCoRe XML MCRMetaHistoryDate definition for the given subtag.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Element with the XML MCRMetaHistoryDate part
     */
    public final org.jdom.Element createXML() throws MCRException {
        if (!isValid()) {
            debug();
            throw new MCRException("The content of MCRMetaHistoryDate is not valid.");
        }

        org.jdom.Element elm = new org.jdom.Element(subtag);
        elm.setAttribute("lang", lang, org.jdom.Namespace.XML_NAMESPACE);
        elm.setAttribute("inherited", Integer.toString(inherited));

        if ((type != null) && ((type = type.trim()).length() != 0)) {
            elm.setAttribute("type", type);
        }

        if ((text = text.trim()).length() != 0) {
            elm.addContent(new org.jdom.Element("text").addContent(text));
        }

        if (von != null) {
            elm.addContent(new org.jdom.Element("von").addContent(getVonToString()));
            elm.addContent(new org.jdom.Element("ivon").addContent(Integer.toString(ivon)));
        }

        if (bis != null) {
            elm.addContent(new org.jdom.Element("bis").addContent(getBisToString()));
            elm.addContent(new org.jdom.Element("ibis").addContent(Integer.toString(ibis)));
        }

        return elm;
    }

    /**
     * This method checks the validation of the content of this class. The
     * method returns <em>false</em> if
     * <ul>
     * <li>the text is null or
     * <li>the von is null or
     * <li>the bis is null
     * </ul>
     * otherwise the method returns <em>true</em>.
     * 
     * @return a boolean value
     */
    public final boolean isValid() {
        if ((text == null) || (von == null) || (bis == null)) {
            return false;
        }

        return true;
    }

    /**
     * Convert the GregorianCalendar to an Julian Day number.
     * 
     * @param date
     *            the GregorianCalendar date
     * @return the represented interger of Julian Day number
     */
    public static final int getJulianDayNumber(GregorianCalendar date) {
        int number = 0;
        boolean gregorian = false;
        int y = date.get(Calendar.YEAR);
        int m = date.get(Calendar.MONTH) + 1; // month starts with 0
        int d = date.get(Calendar.DAY_OF_MONTH);
        if (date.get(Calendar.ERA) == GregorianCalendar.BC) {
            y *= -1;
        }
        // Check Julian / Gregorian Calendar
        if ((y > 1582) || (y == 1582 && m > 10) || (y == 1582 && m == 10 && d >= 15)) {
            gregorian = true;
        }
        // Shift 2 month
        if (m <= 2) {
            y -= 1;
            m += 12;
        }
        // comute Julian Day
        int B = 0;
        if (gregorian) {
            int A = (new Double(y / 100)).intValue();
            B = 2 - A + (new Double(A / 4)).intValue();
        }
        number = (new Double(365.25 * (y + 4716))).intValue() + (new Double(30.6001 * (m + 1))).intValue() + d + B - 1524;
        return number;
    }

    /**
     * This method convert the Julian Day number to a Gregorian Calendar date.
     * 
     * @param jday
     *            the Jualian Day number
     * @return the Grgorian Calendar date. If jday < 0 it return the first day
     *         BC4712-01-01. If jday > 3182029 it return 3999-12-31.
     */
    public static final GregorianCalendar getGregorianCalendar(int jday) {
        GregorianCalendar greg;
        if (jday < 0)
            return default_von;
        if (jday > 3182029)
            return default_bis;
        // check date >= 1582-10-15
        double A = jday;
        if (jday >= 2299161) {
            int g = (new Double((jday - 1867216.25) / 36524.25)).intValue();
            A = jday + 1 + g - (new Double(g / 4)).intValue();
        }
        // compute date
        double B = A + 1524;
        int C = (new Double((new Double(B - 122.1)).intValue() / 365.25)).intValue();
        int D = (new Double(365.25 * C)).intValue();
        int E = (new Double((B - D) / 30.6001)).intValue();
        int d = (new Double(B)).intValue() - D - (new Double(E * 30.6001)).intValue();
        int m;
        if (E < 14)
            m = E - 1;
        else
            m = E - 13;
        int y;
        if (m > 2)
            y = C - 4716;
        else
            y = C - 4715;
        if (jday < 1721424)
            y += 1;
        greg = new GregorianCalendar(y, m - 1, d);
        return greg;
    }

    /**
     * This method make a clone of this class.
     */
    public final Object clone() {
        MCRMetaHistoryDate out = new MCRMetaHistoryDate(datapart, subtag, lang, type, inherited);
        out.setText(text);
        out.setVonDate(von);
        out.setBisDate(bis);

        return out;
    }

    /**
     * This method put debug data to the logger (for the debug mode).
     */
    public final void debug() {
        LOGGER.debug("Start Class : MCRMetaHistoryDate");
        super.debugDefault();
        LOGGER.debug("Text               = " + text);
        LOGGER.debug("Von (String)       = " + getVonToString());
        LOGGER.debug("Von (JulianDay)    = " + ivon);
        LOGGER.debug("Bis (String)       = " + getBisToString());
        LOGGER.debug("Bis (JulianDay)    = " + ibis);
        LOGGER.debug("Stop");
        LOGGER.debug("");
    }
}
