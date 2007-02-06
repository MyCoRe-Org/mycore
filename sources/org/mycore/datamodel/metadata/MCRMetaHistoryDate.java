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

import java.util.Locale;

import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.GregorianCalendar;

import org.mycore.common.MCRException;

/**
 * This class implements all methods for handling with the MCRMetaHistoryDate
 * part of a metadata object. It use the GPL licensed ICU library of IBM.
 * 
 * @author Juergen Vogler
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 * @see http://icu.sourceforge.net/
 */
public class MCRMetaHistoryDate extends MCRMetaDefault {
    /** The maximal length of 'text' */
    public static final int MCRHISTORYDATE_MAX_TEXT = 128;

    /** Tag for buddhist calendar */
    public static String TAG_BUDDHIST = "buddhist";

    public static String TAG_CHINESE = "chinese";

    public static String TAG_COPTIC = "coptic";

    public static String TAG_ETHIOPIC = "ethiopic";

    public static String TAG_GREGORIAN = "gregorian";

    public static String TAG_HEBREW = "hebrew";

    public static String TAG_ISLAMIC = "islamic";

    public static String TAG_ISLAMIC_CIVIL = "islamic-civil";

    public static String TAG_JAPANESE = "japanese";

    /** Minimum Julian Day number is 0 = 01.01.4713 BC */
    public static int MIN_JULIAN_DAY_NUMBER = 0;

    /** Maximum Julian Day number is 3182057 = 31.12.3999 */
    public static int MAX_JULIAN_DAY_NUMBER = 3182057;

    // Data of this class
    private String text;

    private Calendar von;

    private Calendar bis;

    private int ivon;

    private int ibis;

    private String calendar;

    // all available calendars of ICU
    // private static String CALENDARS[] = { TAG_BUDDHIST, TAG_CHINESE,
    // TAG_COPTIC, TAG_ETHIOPIC, TAG_GREGORIAN, TAG_HEBREW, TAG_ISLAMIC,
    // TAG_ISLAMIC_CIVIL, TAG_JAPANESE };
    private static String CALENDARS[] = { TAG_GREGORIAN };

    /**
     * This is the constructor. <br>
     * The language element was set to <b>en </b>. The text element is set to an
     * empty string. The calendar is set to 'Gregorian Calendar'. The von value
     * is set to MIN_JULIAN_DAY_NUMBER, the bis value is set to
     * MAX_JULIAN_DAY_NUMBER;
     */
    public MCRMetaHistoryDate() {
        super();
        text = "";
        calendar = CALENDARS[0];
        setDefaultVon();
        setDefaultBis();
    }

    /**
     * This is the constructor. <br>
     * The language element was set. If the value of <em>default_lang</em> is
     * null, empty or false <b>en </b> was set. The subtag element was set to
     * the value of <em>set_subtag<em>. If the value of <em>set_subtag</em>
     * is null or empty an exception was throwed. The type element was set to
     * the value of <em>set_type<em>, if it is null, an empty string was set
     * to the type element.<br />
     * The text element is set to
     * an empty string. The calendar is set to 'Gregorian Calendar'. The von value 
     * is set to MIN_JULIAN_DAY_NUMBER, the bis value is set to MAX_JULIAN_DAY_NUMBER;
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
        calendar = CALENDARS[0];
        setDefaultVon();
        setDefaultBis();
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
     *            the calendar as String, one of CALENDARS. 'gregorian' is the
     *            default.
     * @return the GregorianCalendar date value or null if an error was occured.
     */
    public static final GregorianCalendar getGregorianHistoryDate(String datestr, boolean last) {
        return (GregorianCalendar) getHistoryDate(datestr, last, TAG_GREGORIAN);
    }

    /**
     * This method convert a ancient date to a Calendar value. The syntax for
     * the gregorian input is [-|AD|BC][[[t]t.][m]m.][yyy]y [AD|BC].
     * 
     * @param datestr
     *            the date as string.
     * @param last
     *            the value is true if the date should be filled with the
     *            highest value of month or day like 12 or 31 else it fill the
     *            date with the lowest value 1 for month and day.
     * @param calstr
     *            the calendar as String, one of CALENDARS . 'gregorian' is the
     *            default.
     * @return the GregorianCalendar date value or null if an error was occured.
     */
    public static final Calendar getHistoryDate(String datestr, boolean last, String calstr) {
        // check String
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
        // Check calendar
        String caltmp;
        if (calstr == null) {
            caltmp = TAG_GREGORIAN;
        } else {
            caltmp = null;
            for (int i = 0; i < CALENDARS.length; i++) {
                if (CALENDARS[i].equals(calstr)) {
                    caltmp = calstr;
                    break;
                }
            }
            if (caltmp == null) {
                caltmp = TAG_GREGORIAN;
            }
        }
        if (caltmp.equals("gregorian")) {
            return getDateAsGregorianCalendar(datestr, last);
        }
        return new GregorianCalendar();
    }

    /**
     * This method convert a ancient date to a GregorianCalendar value. The
     * syntax for the gregorian input is [-|AD|BC][[[t]t.][m]m.][yyy]y [AD|BC].
     * 
     * @param datestr
     *            the date as string.
     * @param last
     *            the value is true if the date should be filled with the
     *            highest value of month or day like 12 or 31 else it fill the
     *            date with the lowest value 1 for month and day.
     * @return the GregorianCalendar date value or null if an error was occured.
     */
    private static final GregorianCalendar getDateAsGregorianCalendar(String datestr, boolean last) {
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
                        year = Integer.parseInt(datestr.substring(start, firstdot)) - 1;
                        mon = Integer.parseInt(datestr.substring(firstdot + 1, datestr.length()));
                    } else {
                        mon = Integer.parseInt(datestr.substring(start, firstdot)) - 1;
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
            throw new MCRException("The ancient gregorian date is false.", e);
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
     * This methode returns the date as string.
     * 
     * @param date
     *            the GregorianCalendar date
     * 
     * @return the date string
     */
    public static final String getDateToGregorianString(Calendar date) {
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
            LOGGER.warn("The text field of MCRMeataHistoryDate is empty.");
            return;
        }
        if (set.length() <= MCRHISTORYDATE_MAX_TEXT) {
            text = set.trim();
        } else {
            text = set.substring(0, MCRHISTORYDATE_MAX_TEXT);
        }
    }

    /**
     * The method set the calendar String value.
     */
    public final void setCalendar(String calstr) {
        if (calstr == null) {
            calendar = TAG_GREGORIAN;
            LOGGER.warn("The calendar field of MCRMeataHistoryDate is set to .");
            return;
        }
        for (int i = 0; i < CALENDARS.length; i++) {
            if (CALENDARS[i].equals(calstr)) {
                calendar = calstr;
                return;
            }
        }
        calendar = TAG_GREGORIAN;
        LOGGER.warn("The calendar field of MCRMeataHistoryDate is set to " + TAG_GREGORIAN + ".");
    }

    /**
     * The method set the calendar String value.
     */
    public final void setCalendar(Calendar cal) {
        if (cal instanceof GregorianCalendar) {
            calendar = TAG_GREGORIAN;
            return;
        }
        calendar = TAG_GREGORIAN;
    }

    /**
     * The method set the von values to the default.
     */
    public final void setDefaultVon() {
        ivon = MIN_JULIAN_DAY_NUMBER;
        von = (Calendar) new GregorianCalendar();
        von.set(GregorianCalendar.JULIAN_DAY, MIN_JULIAN_DAY_NUMBER);
    }

    /**
     * The method set thebis values to the default.
     */
    public final void setDefaultBis() {
        ibis = MAX_JULIAN_DAY_NUMBER;
        bis = (Calendar) new GregorianCalendar();
        bis.set(GregorianCalendar.JULIAN_DAY, MAX_JULIAN_DAY_NUMBER);
    }

    /**
     * This methode set the von to the given date of a supported calendar.
     * 
     * @param set_date
     *            the date of a ICU supported calendar
     */
    public final void setVonDate(Calendar set_date) {
        if (set_date == null) {
            setDefaultVon();
            LOGGER.warn("The calendar to set 'von' is null, default is set.");
            return;
        }
        ivon = set_date.get(GregorianCalendar.JULIAN_DAY);
        von = set_date;
    }

    /**
     * This methode set the von to the given date.
     * 
     * @param set_date
     *            a date string
     * @param calstr
     *            the calendar as String, one of CALENDARS.
     */
    public final void setVonDate(String set_date, String calstr) {
        Calendar c = von;
        try {
            c = getHistoryDate(set_date, false, calstr);
        } catch (Exception e) {
            e.printStackTrace();
        }
        setVonDate(c);
    }

    /**
     * This methode set the bis to the given date of a supported calendar.
     * 
     * @param set_date
     *            the date of a ICU supported calendar
     */
    public final void setBisDate(Calendar set_date) {
        if (set_date == null) {
            setDefaultBis();
            LOGGER.warn("The calendar to set 'bis' is null, default is set.");
            return;
        }
        ibis = set_date.get(GregorianCalendar.JULIAN_DAY);
        bis = set_date;
    }

    /**
     * This methode set the bis to the given date.
     * 
     * @param set_date
     *            a date string
     * @param calstr
     *            the calendar as String, one of CALENDARS.
     */
    public final void setBisDate(String set_date, String calstr) {
        Calendar c = bis;
        try {
            c = getHistoryDate(set_date, true, calstr);
        } catch (Exception e) {
            e.printStackTrace();
        }
        setBisDate(c);
    }

    /**
     * This method get the 'text' text element.
     * 
     * @return the text string
     */
    public final String getText() {
        return text;
    }

    /**
     * This method get the 'calendar' text element.
     * 
     * @return the calendar string
     */
    public final String getCalendar() {
        return calendar;
    }

    /**
     * This method get the von element as ICU-Calendar.
     * 
     * @return the date
     */
    public final Calendar getVon() {
        return von;
    }

    /**
     * This methode return the von as string.
     * 
     * @return the date
     */
    public final String getVonToGregorianString() {
        return getDateToGregorianString(von);
    }

    /**
     * This method get the ivon element as Julian Day integer.
     * 
     * @return the date
     */
    public final int getIvon() {
        return ivon;
    }

    /**
     * This method get the bis element as ICU-Calendar.
     * 
     * @return the date
     */
    public final Calendar getBis() {
        return bis;
    }

    /**
     * This methode return the bis as string.
     * 
     * @return the date
     */
    public final String getBisToGregorianString() {
        return getDateToGregorianString(bis);
    }

    /**
     * This method get the ibis element as Julian Day integer.
     * 
     * @return the date
     */
    public final int getIbis() {
        return ibis;
    }

    /**
     * This method reads the XML input stream part from a DOM part for the
     * metadata of the document.
     * 
     * @param element
     *            a relevant JDOM element for the metadata
     */
    public void setFromDOM(org.jdom.Element element) {
        super.setFromDOM(element);
        setText(element.getChildTextTrim("text"));
        setCalendar(element.getChildTextTrim("calendar"));
        setVonDate(element.getChildTextTrim("von"), calendar);
        setBisDate(element.getChildTextTrim("bis"), calendar);
    }

    /**
     * This method creates a XML stream for all data in this class, defined by
     * the MyCoRe XML MCRMetaHistoryDate definition for the given subtag.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Element with the XML MCRMetaHistoryDate part
     */
    public org.jdom.Element createXML() throws MCRException {
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

        if ((calendar = calendar.trim()).length() != 0) {
            elm.addContent(new org.jdom.Element("calendar").addContent(calendar));
        }

        if (von != null) {
            elm.addContent(new org.jdom.Element("ivon").addContent(Integer.toString(ivon)));
            if (calendar.equals(TAG_GREGORIAN)) {
                elm.addContent(new org.jdom.Element("von").addContent(getVonToGregorianString()));
            } else {
                elm.addContent(new org.jdom.Element("von").addContent(getVonToGregorianString()));
            }
        }

        if (bis != null) {
            elm.addContent(new org.jdom.Element("ibis").addContent(Integer.toString(ibis)));
            if (calendar.equals(TAG_GREGORIAN)) {
                elm.addContent(new org.jdom.Element("bis").addContent(getBisToGregorianString()));
            } else {

                elm.addContent(new org.jdom.Element("bis").addContent(getBisToGregorianString()));
            }
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
    public boolean isValid() {
        if ((text == null) || (von == null) || (bis == null) || (calendar == null)) {
            return false;
        }

        return true;
    }

    /**
     * This method make a clone of this class.
     */
    public Object clone() {
        MCRMetaHistoryDate out = new MCRMetaHistoryDate(datapart, subtag, lang, type, inherited);
        out.setText(text);
        out.setVonDate(von);
        out.setBisDate(bis);
        out.setCalendar(calendar);

        return out;
    }

    /**
     * This method put debug data to the logger (for the debug mode).
     */
    public void debug() {
        LOGGER.debug("Start Class : MCRMetaHistoryDate");
        super.debugDefault();
        LOGGER.debug("Text               = " + text);
        LOGGER.debug("Calendar           = " + calendar);
        if (calendar.equals(TAG_GREGORIAN)) {
            LOGGER.debug("Von (String)       = " + getVonToGregorianString());
        }
        LOGGER.debug("Von (JulianDay)    = " + ivon);
        if (calendar.equals(TAG_GREGORIAN)) {
            LOGGER.debug("Bis (String)       = " + getBisToGregorianString());
        }
        LOGGER.debug("Bis (JulianDay)    = " + ibis);
        LOGGER.debug("Stop");
        LOGGER.debug("");
    }
}
