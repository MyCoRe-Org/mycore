/*
 * 
 * $Revision: 1.14 $ $Date: 2008/06/05 05:28:31 $
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

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.mycore.common.MCRCalendar;
import org.mycore.common.MCRException;

import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.GregorianCalendar;
import org.jdom.*;

/**
 * This class implements all methods for handling with the MCRMetaHistoryDate
 * part of a metadata object. It use the GPL licensed ICU library of IBM.
 * 
 * @author Juergen Vogler
 * @author Jens Kupferschmidt
 * @author Thomas Junge
 * @version $Revision: 1.14 $ $Date: 2008/06/05 05:28:31 $
 * @see http://icu.sourceforge.net/
 */
public class MCRMetaHistoryDate extends MCRMetaDefault {

    /** Logger */
    protected static Logger LOGGER = Logger.getLogger(MCRMetaHistoryDate.class.getName());

    /** The maximal length of 'text' */
    public static final int MCRHISTORYDATE_MAX_TEXT = 128;

    // Data of this class
    private ArrayList<MCRMetaHistoryDateTexts> texts;

    private Calendar von;

    private Calendar bis;

    private int ivon;

    private int ibis;

    private String calendar;

    /**
     * This is the constructor. <br>
     * The language element was set to configured default. The text element is
     * set to an empty string. The calendar is set to 'Gregorian Calendar'. The
     * von value is set to MIN_JULIAN_DAY_NUMBER, the bis value is set to
     * MAX_JULIAN_DAY_NUMBER;
     */
    public MCRMetaHistoryDate() {
        super();
        texts = new ArrayList<MCRMetaHistoryDateTexts>();
        calendar = MCRCalendar.CALENDARS_INPUT[0];
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
     * The text element is set to an empty string. The calendar is set to 'Gregorian Calendar'. The von value 
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
        texts = new ArrayList<MCRMetaHistoryDateTexts>();
        calendar = MCRCalendar.CALENDARS_INPUT[0];
        setDefaultVon();
        setDefaultBis();
    }

    /**
     * This method set the text field for the default language. If data exists,
     * it overwrites the value of text.
     * 
     * @param text
     *            the text string for a date or range
     */
    public final void setText(String set_text) {
        setText(set_text, lang);
    }

    /**
     * This method set the text field for the given language. If data exists, it
     * overwrites the value of text.
     * 
     * @param text
     *            the text string for a date or range
     * @param lang
     *            the language of the text in the ISO format
     */
    public final void setText(String set_text, String set_lang) {
        if (set_text == null) {
            LOGGER.warn("The text field of MCRMeataHistoryDate is empty.");
            return;
        }
        if (set_text.length() <= MCRHISTORYDATE_MAX_TEXT) {
            set_text = set_text.trim();
        } else {
            set_text = set_text.substring(0, MCRHISTORYDATE_MAX_TEXT);
        }
        if (set_lang == null || set_lang.length() == 0) {
            addText(set_text, lang);
        } else {
            addText(set_text, set_lang);
        }
    }

    /**
     * This method add a MCRMetaHistoryDateTexts instance to the ArrayList of
     * texts.
     * 
     * @param text
     *            the text- String
     * @param lang
     *            the lang- String
     */

    public final void addText(String set_text, String set_lang) {
        if (set_text == null) {
            LOGGER.warn("The text field of MCRMeataHistoryDate is empty.");
            return;
        }
        if (set_lang == null || set_lang.length() == 0) {
            LOGGER.warn("The lang field of MCRMeataHistoryDate is empty.");
            return;
        }
        for (int i = 0; i < texts.size(); i++) {
            if (texts.get(i).getLang().equals(set_lang)) {
                texts.remove(i);
                break;
            }
        }
        texts.add(new MCRMetaHistoryDateTexts(set_text, set_lang));
    }

    /**
     * This method return the MCRMetaHistoryDateTexts instance with the
     * corresponding language.
     * 
     * @param lang
     *            the language String in ISO format
     * @return an instance of MCRMetaHistoryDateTexts or null
     */
    public final MCRMetaHistoryDateTexts getText(String set_lang) {
        if (set_lang == null)
            return null;
        for (int i = 0; i < texts.size(); i++) {
            if (texts.get(i).getLang().equals(set_lang)) {
                return texts.get(i);
            }
        }
        return null;
    }

    /**
     * This method return the MCRMetaHistoryDateTexts instance of the indexed
     * element of the ArrayList.
     * 
     * @param index
     *            the index of ArryList texts
     * @return an instance of MCRMetaHistoryDateTexts or null
     */
    public final MCRMetaHistoryDateTexts getText(int index) {
        if ((index >= 0) && (index < texts.size())) {
            return texts.get(index);
        }
        return null;
    }

    /**
     * This method read the ArryList texts
     * 
     * @return an ArrayList of MCRMetaHistoryDateTexts instances
     */
    public final ArrayList<MCRMetaHistoryDateTexts> getTexts() {
        return this.texts;
    }

    /**
     * This method read the size of texts
     * 
     * @return the size of the ArrayList of language dependence texts
     */
    public final int TextSize() {
        return texts.size();
    }

    /**
     * The method set the calendar String value.
     * 
     * @param calstr
     *            the calendar as String, one of CALENDARS.
     */
    public final void setCalendar(String calstr) {
        if (calstr == null) {
            calendar = MCRCalendar.TAG_GREGORIAN;
            LOGGER.warn("The calendar field of MCRMeataHistoryDate is set to default " + MCRCalendar.TAG_GREGORIAN + ".");
            return;
        }
        for (int i = 0; i < MCRCalendar.CALENDARS_INPUT.length; i++) {
            if (MCRCalendar.CALENDARS_INPUT[i].equals(calstr)) {
                calendar = calstr;
                return;
            }
        }
        calendar = MCRCalendar.TAG_GREGORIAN;
        LOGGER.warn("The calendar field of MCRMeataHistoryDate is set to default " + MCRCalendar.TAG_GREGORIAN + ".");
    }

    /**
     * The method set the calendar String value.
     * 
     * @param cal
     *            the date of the calendar.
     */
    public final void setCalendar(Calendar cal) {
        if (cal instanceof GregorianCalendar) {
            calendar = MCRCalendar.TAG_GREGORIAN;
            return;
        }
        calendar = MCRCalendar.TAG_GREGORIAN;
    }

    /**
     * The method set the von values to the default.
     */
    public final void setDefaultVon() {
        ivon = MCRCalendar.MIN_JULIAN_DAY_NUMBER;
        von = (Calendar) new GregorianCalendar();
        von.set(GregorianCalendar.JULIAN_DAY, MCRCalendar.MIN_JULIAN_DAY_NUMBER);
    }

    /**
     * The method set the bis values to the default.
     */
    public final void setDefaultBis() {
        ibis = MCRCalendar.MAX_JULIAN_DAY_NUMBER;
        bis = (Calendar) new GregorianCalendar();
        bis.set(GregorianCalendar.JULIAN_DAY, MCRCalendar.MAX_JULIAN_DAY_NUMBER);
    }

    /**
     * This method set the von to the given date of a supported calendar.
     * 
     * @param set_date
     *            the date of a ICU supported calendar.
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
     * This method set the von to the given date.
     * 
     * @param set_date
     *            a date string
     * @param calstr
     *            the calendar as String, one of CALENDARS.
     */
    public final void setVonDate(String set_date, String calstr) {
        Calendar c = von;
        try {
            c = MCRCalendar.getGregorianHistoryDate(set_date, false, calstr);
        } catch (Exception e) {
            LOGGER.warn("The von date " + set_date + " for calendar " + calstr + " is false.");
            c = null;
        }
        setVonDate(c);
    }

    /**
     * This method set the bis to the given date of a supported calendar.
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
     * This method set the bis to the given date.
     * 
     * @param set_date
     *            a date string
     * @param calstr
     *            the calendar as String, one of CALENDARS.
     */
    public final void setBisDate(String set_date, String calstr) {
        Calendar c = bis;
        try {
            c = MCRCalendar.getGregorianHistoryDate(set_date, true, calstr);
        } catch (Exception e) {
            LOGGER.warn("The bis date " + set_date + " for calendar " + calstr + " is false.");
            c = null;
        }
        setBisDate(c);
    }

    /**
     * This method get the 'text' text element.
     * 
     * @return the text String of the default language or an empty String
     * @deprecated
     */
    public final String getText() {
        if (texts.size() > 0) {
            MCRMetaHistoryDateTexts h = getText(lang);
            if (h != null)
                return h.getText();
            else
                return "";
        }
        return "";
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
     * This method return the von as string.
     * 
     * @return the date
     */
    public final String getVonToGregorianString() {
        return MCRCalendar.getDateToFormattedString(von);
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
     * This method return the bis as string.
     * 
     * @return the date
     */
    public final String getBisToGregorianString() {
        return MCRCalendar.getDateToFormattedString(bis);
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
        texts.clear(); // clear

        String langn = "";
        String textn;
        Iterator<org.jdom.Element> textchild = element.getChildren("text").iterator();
        while (textchild.hasNext()) {
            Element elmt = (Element) textchild.next();
            textn = elmt.getText();
            langn = elmt.getAttributeValue("lang", org.jdom.Namespace.XML_NAMESPACE);
            if (langn != null) {
                setText(textn, langn);
            } else {
                setText(textn);
            }
        }
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
        for (int i = 0; i < texts.size(); i++) {
            org.jdom.Element elmt = new org.jdom.Element("text");
            elmt.addContent((String) texts.get(i).getText());
            elmt.setAttribute("lang", texts.get(i).getLang(), org.jdom.Namespace.XML_NAMESPACE);
            elm.addContent(elmt);
        }
        if ((type != null) && ((type = type.trim()).length() != 0)) {
            elm.setAttribute("type", type);
        }
        if ((calendar = calendar.trim()).length() != 0) {
            elm.addContent(new org.jdom.Element("calendar").addContent(calendar));
        }

        if (von != null) {
            elm.addContent(new org.jdom.Element("ivon").addContent(Integer.toString(ivon)));
            elm.addContent(new org.jdom.Element("von").addContent(getVonToGregorianString()));
        }

        if (bis != null) {
            elm.addContent(new org.jdom.Element("ibis").addContent(Integer.toString(ibis)));
            elm.addContent(new org.jdom.Element("bis").addContent(getBisToGregorianString()));
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
        if ((texts.size() == 0) || (von == null) || (bis == null) || (calendar == null)) {
            return false;
        }
        if (ibis < ivon) {
            Calendar swp = (Calendar) von.clone();
            setVonDate((Calendar) bis.clone());
            setBisDate(swp);
        }

        return true;
    }

    /**
     * This method make a clone of this class.
     */
    public Object clone() {
        MCRMetaHistoryDate out = new MCRMetaHistoryDate(datapart, subtag, lang, type, inherited);
        for (int i = 0; i < texts.size(); i++) {
            MCRMetaHistoryDateTexts h = texts.get(i);
            out.setText(h.getText(), h.getLang());
        }
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
        for (int i = 0; i < texts.size(); i++) {
            LOGGER.debug("Text / lang         = " + texts.get(i).getText() + " / " + texts.get(i).getLang());
        }
        LOGGER.debug("Calendar           = " + calendar);
        if (calendar.equals(MCRCalendar.TAG_GREGORIAN)) {
            LOGGER.debug("Von (String)       = " + getVonToGregorianString());
        }
        LOGGER.debug("Von (JulianDay)    = " + ivon);
        if (calendar.equals(MCRCalendar.TAG_GREGORIAN)) {
            LOGGER.debug("Bis (String)       = " + getBisToGregorianString());
        }
        LOGGER.debug("Bis (JulianDay)    = " + ibis);
        LOGGER.debug("Stop");
        LOGGER.debug("");
    }

    /**
     * This class describes the structure of pair of language an text. The
     * language notation is in the ISO format.
     * 
     */
    protected class MCRMetaHistoryDateTexts {
        private String datetext;

        private String lang;

        public MCRMetaHistoryDateTexts() {
        }

        public MCRMetaHistoryDateTexts(String datetext, String lang) {
            this.datetext = datetext;
            this.lang = lang;
        }

        /**
         * This method get the datetext element as field text (String) .
         * 
         * @return the datetext
         */

        public String getText() {
            return this.datetext;
        }

        /**
         * This method set the datetext element as field text (String) .
         * 
         * @param datetext
         *            the text String of a date value
         */
        public void setText(String datetext) {
            this.datetext = datetext;
        }

        /**
         * This method get the lang element as language field (String) .
         * 
         * @return the lang
         */
        public String getLang() {
            return this.lang;
        }

        /**
         * This method set the lang element as language field (String) .
         * 
         * @param the
         *            language String of a date value
         */
        public void setLang(String lang) {
            this.lang = lang;
        }

    }

}