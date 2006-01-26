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

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;

/**
 * This class implements all methods for handling with the MCRMetaHistoryDate
 * part of a metadata object. The MCRMetaHistoryDate class is a special class
 * for the datamodel of Papyrus-Project Jena-Halle-Leipzig.
 * 
 * @author Juergen Vogler
 * @version $Revision$ $Date$
 */
final public class MCRMetaHistoryDate extends MCRMetaDefault implements MCRMetaInterface {
    // MetaHistoryDate data
    private GregorianCalendar default_von;

    private GregorianCalendar default_bis;

    private String text;

    private GregorianCalendar von;

    private GregorianCalendar bis;

    private int ivon;

    private int ibis;

    /** The maximal length of 'text' */
    public static final int PAPANTIKDATE_MAX_TEXT = 128;

    /**
     * This is the constructor. <br>
     * The language element was set to <b>en </b>. All other elemnts are set to
     * an empty string.
     */
    public MCRMetaHistoryDate() {
        super();

        MCRConfiguration config = MCRConfiguration.instance();
        String min = config.getString("MCR.history_date_min", "BC01.01.3000");
        String max = config.getString("MCR.history_date_max", "AD31.12.3000");

        try {
            default_von = getAntikDate(min, false);
        } catch (MCRException e) {
            throw new MCRException("The default_von date is false.", e);
        }

        try {
            default_bis = getAntikDate(max, true);
        } catch (MCRException e) {
            throw new MCRException("The default_bis date is false.", e);
        }

        text = "";
        von = default_von;
        ivon = getIntDate(von);
        bis = default_bis;
        ibis = getIntDate(bis);
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

        MCRConfiguration config = MCRConfiguration.instance();
        String min = config.getString("PAP.ancient_date_min");
        String max = config.getString("PAP.ancient_date_max");

        try {
            default_von = getAntikDate(min, false);
        } catch (MCRException e) {
            throw new MCRException("The default_von date is false.", e);
        }

        try {
            default_bis = getAntikDate(max, true);
        } catch (MCRException e) {
            throw new MCRException("The default_bis date is false.", e);
        }

        text = "";
        von = default_von;
        ivon = getIntDate(von);
        bis = default_bis;
        ibis = getIntDate(bis);
    }

    /**
     * This method convert a ancient date to a GregorianCalendar value. The
     * syntax for the input is [-|AD|BC][[[t]t.][m]m.][yyy]y in a Gregorian
     * calendar date.
     * 
     * @param datestr
     *            the date as string.
     * @param last
     *            the value is true if the defaults should be high values like
     *            12 or 31 else it is 1
     * @return the GregorianCalendar date value or null if an error was occured.
     */
    public static final GregorianCalendar getAntikDate(String datestr, boolean last) {
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

        try {
            // suche nach v. Chr.
            boolean bc = false;
            int start = 0;

            if (datestr.substring(0, 1).equals("-")) {
                bc = true;
                start = 1;
            }

            if (datestr.length() > 2) {
                if (datestr.substring(0, 2).equals("AD")) {
                    bc = false;
                    start = 2;
                }

                if (datestr.substring(0, 2).equals("BC")) {
                    bc = true;
                    start = 2;
                }
            }

            if (datestr.length() > 7) {
                if (datestr.substring(0, 7).equals("n. Chr.")) {
                    bc = false;
                    start = 7;
                }

                if (datestr.substring(0, 7).equals("v. Chr.")) {
                    bc = true;
                    start = 7;
                }
            }

            // deutsch oder ISO?
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
    private final String getDateToString(GregorianCalendar date) {
        if (date == null) {
            return "";
        }

        SimpleDateFormat formatter = new SimpleDateFormat("Gdd.MM.yyyy");
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

        if (set.length() <= PAPANTIKDATE_MAX_TEXT) {
            text = set.trim();
        } else {
            text = set.substring(0, PAPANTIKDATE_MAX_TEXT);
        }
    }

    /**
     * The method set the von and bis values to the default.
     */
    public final void setDefaultVonBis() {
        von = default_von;
        ivon = getIntDate(von);
        bis = default_bis;
        ibis = getIntDate(bis);
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

        ivon = getIntDate(von);
        ibis = getIntDate(bis);
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

        ivon = getIntDate(von);
    }

    /**
     * This methode set the von to the given date.
     * 
     * @param set_date
     *            a date string
     */
    public final void setVonDate(String set_date) {
        try {
            von = getAntikDate(set_date, false);
        } catch (Exception e) {
            von = default_von;
        }

        ivon = getIntDate(von);
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

        ibis = getIntDate(bis);
    }

    /**
     * This methode set the bis to the given date.
     * 
     * @param set_date
     *            a date string
     */
    public final void setBisDate(String set_date) throws MCRException {
        try {
            bis = getAntikDate(set_date, true);
        } catch (Exception e) {
            bis = default_bis;
        }

        ibis = getIntDate(bis);
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
        elm.setAttribute("inherited", (new Integer(inherited)).toString());

        if ((type != null) && ((type = type.trim()).length() != 0)) {
            elm.setAttribute("type", type);
        }

        if ((text = text.trim()).length() != 0) {
            elm.addContent(new org.jdom.Element("text").addContent(text));
        }

        if (von != null) {
            elm.addContent(new org.jdom.Element("von").addContent(getVonToString()));
            elm.addContent(new org.jdom.Element("ivon").addContent((new Integer(ivon)).toString()));
        }

        if (bis != null) {
            elm.addContent(new org.jdom.Element("bis").addContent(getBisToString()));
            elm.addContent(new org.jdom.Element("ibis").addContent((new Integer(ibis)).toString()));
        }

        return elm;
    }

    /**
     * This methode create a String for all text searchable data in this
     * instance.
     * 
     * @param textsearch
     *            true if the data should text searchable
     * @exception MCRException
     *                if the content of this class is not valid
     * @return an empty String, because the content is not text searchable.
     */
    public final String createTextSearch(boolean textsearch) throws MCRException {
        return "";
    }

    /**
     * This method checks the validation of the content of this class. The
     * method returns <em>false</em> if
     * <ul>
     * <li>the text is empty and
     * <li>the von is empty and
     * <li>the bis is empty and
     * </ul>
     * otherwise the method returns <em>true</em>.
     * 
     * @return a boolean value
     */
    public final boolean isValid() {
        if (((text = text.trim()).length() == 0) && (von == null) && (bis == null)) {
            return false;
        }

        return true;
    }

    /**
     * Convert the GregorianCalendar to an integer.
     * 
     * @param date
     *            the GregorianCalendar date
     * @return the represented interger
     */
    public static final int getIntDate(GregorianCalendar date) {
        int number = 0;

        if (date.get(Calendar.ERA) == GregorianCalendar.AD) {
            number = ((4000 + date.get(Calendar.YEAR)) * 10000) + (date.get(Calendar.MONTH) * 100) + date.get(Calendar.DAY_OF_MONTH);
        } else {
            number = ((4000 - date.get(Calendar.YEAR)) * 10000) + (date.get(Calendar.MONTH) * 100) + date.get(Calendar.DAY_OF_MONTH);
        }

        return number;
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
        LOGGER.debug("Von                = " + getVonToString());
        LOGGER.debug("Bis                = " + getBisToString());
        LOGGER.debug("Stop");
        LOGGER.debug("");
    }
}
