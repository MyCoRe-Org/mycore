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

package org.mycore.datamodel.metadata;

import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.mycore.common.MCRCalendar;
import org.mycore.common.MCRException;

import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.GregorianCalendar;

/**
 * This class implements all methods for handling the MCRMetaHistoryDate
 * part of a metadata object. It uses the GPL licensed ICU library of IBM.
 * 
 * @author Juergen Vogler
 * @author Jens Kupferschmidt
 * @author Thomas Junge
 * @version $Revision$ $Date$
 * @see <a href="http://www.icu-project.org/">http://www.icu-project.org/</a>
 */
public class MCRMetaHistoryDate extends MCRMetaDefault {

    /** Logger */
    private static final Logger LOGGER = LogManager.getLogger();

    /** The maximal length of 'text' */
    public static final int MCRHISTORYDATE_MAX_TEXT = 512;

    // Data of this class
    private ArrayList<MCRMetaHistoryDateText> texts;

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
        texts = new ArrayList<>();
        calendar = MCRCalendar.CALENDARS_LIST.get(0);
        setDefaultVon();
        setDefaultBis();
    }

    /**
     * This is the constructor. <br>
     * The language element was set. If the value of <em>default_lang</em> is
     * null, empty or false <b>en </b> was set. The subtag element was set to
     * the value of <em>subtag</em>. If the value of <em>subtag</em>
     * is null or empty an exception was thrown. The type element was set to
     * the value of <em>type</em>, if it is null, an empty string was set
     * to the type element.<br>
     * The text element is set to an empty string. The calendar is set to 'Gregorian Calendar'. The von value 
     * is set to MIN_JULIAN_DAY_NUMBER, the bis value is set to MAX_JULIAN_DAY_NUMBER;
     * @param subtag      the name of the subtag
     * @param type        the optional type string
     * @param inherted    a value &gt;= 0
     *
     * @exception MCRException if the parameter values are invalid
     */
    public MCRMetaHistoryDate(String subtag, String type, int inherted) throws MCRException {
        super(subtag, null, type, inherted);
        texts = new ArrayList<>();
        calendar = MCRCalendar.CALENDARS_LIST.get(0);
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
    @Deprecated
    public final void setText(String text) {
        setText(text, lang);
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
    public final void setText(String text, String lang) {
        if (text == null || text.length() == 0) {
            LOGGER.warn("The text field of MCRMeataHistoryDate is empty.");
            return;
        }
        if (text.length() <= MCRHISTORYDATE_MAX_TEXT) {
            text = text.trim();
        } else {
            text = text.substring(0, MCRHISTORYDATE_MAX_TEXT);
        }
        if (lang == null || lang.length() == 0) {
            addText(text, this.lang);
        } else {
            addText(text, lang);
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

    public final void addText(String text, String lang) {
        if (text == null || text.length() == 0) {
            LOGGER.warn("The text field of MCRMeataHistoryDate is empty.");
            return;
        }
        if (lang == null || lang.length() == 0) {
            LOGGER.warn("The lang field of MCRMeataHistoryDate is empty.");
            return;
        }
        for (int i = 0; i < texts.size(); i++) {
            if (texts.get(i).getLang().equals(lang)) {
                texts.remove(i);
                break;
            }
        }
        texts.add(new MCRMetaHistoryDateText(text, lang));
    }

    /**
     * This method return the MCRMetaHistoryDateTexts instance with the
     * corresponding language.
     * 
     * @param lang
     *            the language String in ISO format
     * @return an instance of MCRMetaHistoryDateTexts or null
     */
    public final MCRMetaHistoryDateText getText(String lang) {
        if (lang == null) {
            return null;
        }
        return texts.stream()
            .filter(text -> text.getLang().equals(lang))
            .findFirst()
            .orElse(null);
    }

    /**
     * This method return the MCRMetaHistoryDateTexts instance of the indexed
     * element of the ArrayList.
     * 
     * @param index
     *            the index of ArryList texts
     * @return an instance of MCRMetaHistoryDateTexts or null
     */
    public final MCRMetaHistoryDateText getText(int index) {
        if (index >= 0 && index < texts.size()) {
            return texts.get(index);
        }
        return null;
    }

    /**
     * This method read the ArryList texts
     * 
     * @return an ArrayList of MCRMetaHistoryDateTexts instances
     */
    public final ArrayList<MCRMetaHistoryDateText> getTexts() {
        return texts;
    }

    /**
     * This method read the size of texts
     * 
     * @return the size of the ArrayList of language dependence texts
     */
    public final int textSize() {
        return texts.size();
    }

    /**
     * The method set the calendar String value.
     * 
     * @param calstr
     *            the calendar as String, one of CALENDARS.
     */
    public final void setCalendar(String calstr) {
        if (calstr == null || calstr.trim().length() == 0 || (!MCRCalendar.CALENDARS_LIST.contains(calstr))) {
            calendar = MCRCalendar.TAG_GREGORIAN;
            LOGGER.warn("The calendar field of MCRMeataHistoryDate is set to default " + MCRCalendar.TAG_GREGORIAN
                + ".");
            return;
        }
        calendar = calstr;
    }

    /**
     * The method set the calendar String value.
     * 
     * @param calendar
     *            the date of the calendar.
     */
    public final void setCalendar(Calendar calendar) {
        this.calendar = MCRCalendar.getCalendarTypeString(calendar);
    }

    /**
     * The method set the von values to the default.
     */
    public final void setDefaultVon() {
        von = new GregorianCalendar();
        von.set(Calendar.JULIAN_DAY, MCRCalendar.MIN_JULIAN_DAY_NUMBER);
        ivon = MCRCalendar.MIN_JULIAN_DAY_NUMBER;
    }

    /**
     * The method set the bis values to the default.
     */
    public final void setDefaultBis() {
        bis = new GregorianCalendar();
        bis.set(Calendar.JULIAN_DAY, MCRCalendar.MAX_JULIAN_DAY_NUMBER);
        ibis = MCRCalendar.MAX_JULIAN_DAY_NUMBER;
    }

    /**
     * This method set the von to the given date of a supported calendar.
     * 
     * @param calendar
     *            the date of a ICU supported calendar.
     */
    public final void setVonDate(Calendar calendar) {
        if (calendar == null) {
            setDefaultVon();
            LOGGER.warn("The calendar to set 'von' is null, default is set.");
        } else {
            von = calendar;
            ivon = von.get(Calendar.JULIAN_DAY);
        }
    }

    /**
     * This method set the von to the given date.
     * 
     * @param date
     *            a date string
     * @param calendar
     *            the calendar as String, one of CALENDARS.
     */
    public final void setVonDate(String date, String calendar) {
        try {
            von = MCRCalendar.getHistoryDateAsCalendar(date, false, calendar);
            ivon = von.get(Calendar.JULIAN_DAY);
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.warn("The von date {} for calendar {} is false. Set to default!", date, calendar);
            setDefaultVon();
        }
    }

    /**
     * This method set the bis to the given date of a supported calendar.
     * 
     * @param calendar
     *            the date of a ICU supported calendar
     */
    public final void setBisDate(Calendar calendar) {
        if (calendar == null) {
            setDefaultBis();
            LOGGER.warn("The calendar to set 'bis' is null, default is set.");
        } else {
            bis = calendar;
            ibis = bis.get(Calendar.JULIAN_DAY);
        }
    }

    /**
     * This method set the bis to the given date.
     * 
     * @param date
     *            a date string
     * @param calendar
     *            the calendar as String, one of CALENDARS.
     */
    public final void setBisDate(String date, String calendar) {
        Calendar c = bis;
        try {
            c = MCRCalendar.getHistoryDateAsCalendar(date, true, calendar);
        } catch (Exception e) {
            LOGGER.warn("The bis date {} for calendar {} is false.", date, calendar);
            c = null;
        }
        setBisDate(c);
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
    public final String getVonToString() {
        return MCRCalendar.getCalendarDateToFormattedString(von);
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
    public final String getBisToString() {
        return MCRCalendar.getCalendarDateToFormattedString(bis);
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
    @Override
    public void setFromDOM(org.jdom2.Element element) {
        super.setFromDOM(element);
        texts.clear(); // clear

        for (Element textElement : element.getChildren("text")) {
            String text = textElement.getText();
            String lang = textElement.getAttributeValue("lang", Namespace.XML_NAMESPACE);
            if (lang != null) {
                setText(text, lang);
            } else {
                setText(text);
            }
        }
        setCalendar(element.getChildTextTrim("calendar"));
        setVonDate(element.getChildTextTrim("von"), calendar);
        setBisDate(element.getChildTextTrim("bis"), calendar);
        /** 
         * If dates higher than 1582 and calendar is Julian the calendar must switch to 
         * Gregorian cause the date transforming is implicit in the calendar methods. In
         * other cases before 1582 both calendar are equal.
         * */
        if (calendar.equals(MCRCalendar.TAG_JULIAN)) {
            calendar = MCRCalendar.TAG_GREGORIAN;
        }
    }

    /**
     * This method creates a XML stream for all data in this class, defined by
     * the MyCoRe XML MCRMetaHistoryDate definition for the given subtag.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Element with the XML MCRMetaHistoryDate part
     */
    @Override
    public org.jdom2.Element createXML() throws MCRException {
        Element elm = super.createXML();
        for (MCRMetaHistoryDateText text : texts) {
            Element elmt = new Element("text");
            elmt.addContent(text.getText());
            elmt.setAttribute("lang", text.getLang(), Namespace.XML_NAMESPACE);
            elm.addContent(elmt);
        }
        elm.addContent(new org.jdom2.Element("calendar").addContent(calendar));

        elm.addContent(new org.jdom2.Element("ivon").addContent(Integer.toString(ivon)));
        elm.addContent(new org.jdom2.Element("von").addContent(getVonToString()));

        elm.addContent(new org.jdom2.Element("ibis").addContent(Integer.toString(ibis)));
        elm.addContent(new org.jdom2.Element("bis").addContent(getBisToString()));
        return elm;
    }

    /**
     * Validates this MCRMetaHistoryDate. This method throws an exception if:
     * <ul>
     * <li>the subtag is not null or empty</li>
     * <li>the lang value was supported</li>
     * <li>the inherited value is lower than zero</li>
     * <li>the number of texts is 0 (empty texts are delete)</li>
     * <li>von is null or bis is null or calendar is null</li>
     * </ul>
     * 
     * @throws MCRException the MCRMetaHistoryDate is invalid
     */
    @Override
    public void validate() throws MCRException {
        super.validate();
        texts.removeIf(textItem -> !textItem.isValid());
        if (texts.size() == 0) {
            throw new MCRException(getSubTag() + ": no texts defined");
        }
        if (von == null || bis == null || calendar == null) {
            throw new MCRException(getSubTag() + ": von,bis or calendar are null");
        }
        if (ibis < ivon) {
            Calendar swp = (Calendar) von.clone();
            setVonDate((Calendar) bis.clone());
            setBisDate(swp);
        }
    }

    /**
     * clone of this instance
     * 
     * you will get a (deep) clone of this element
     * 
     * @see java.lang.Object#clone()
     */
    @Override
    public MCRMetaHistoryDate clone() {
        MCRMetaHistoryDate clone = (MCRMetaHistoryDate) super.clone();

        clone.texts = this.texts.stream().map(MCRMetaHistoryDateText::clone)
            .collect(Collectors.toCollection(ArrayList::new));
        clone.bis = (Calendar) this.bis.clone();
        clone.von = (Calendar) this.von.clone();
        clone.ibis = this.ibis;
        clone.ivon = this.ivon;
        clone.calendar = this.calendar;

        return clone;
    }

    /**
     * This method put debug data to the logger (for the debug mode).
     */
    @Override
    public void debug() {
        if (LOGGER.isDebugEnabled()) {
            super.debugDefault();
            for (MCRMetaHistoryDateText text : texts) {
                LOGGER.debug("Text / lang         = {} / {}", text.getText(), text.getLang());
            }
            LOGGER.debug("Calendar           = {}", calendar);
            LOGGER.debug("Von (String)       = {}", getVonToString());
            LOGGER.debug("Von (JulianDay)    = {}", ivon);
            LOGGER.debug("Bis (String)       = {}", getBisToString());
            LOGGER.debug("Bis (JulianDay)    = {}", ibis);
            LOGGER.debug("");
        }
    }

    /**
     * This method compares this instance with a MCRMetaHistoryDate object
     */
    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        final MCRMetaHistoryDate other = (MCRMetaHistoryDate) obj;
        boolean fieldTest = Objects.equals(this.calendar, other.calendar) && Objects.equals(this.ivon, other.ivon);
        boolean textTest = equalText(other.getTexts());
        return fieldTest && textTest;
    }

    private boolean equalText(ArrayList<MCRMetaHistoryDateText> objtexts) {
        boolean testflag = true;
        int size = texts.size() < objtexts.size() ? texts.size() : objtexts.size();
        for (int i = 0; i < size; i++) {
            testflag &= texts.get(i).equals(objtexts.get(i));
        }
        return testflag;
    }

    /**
     * This class describes the structure of pair of language an text. The
     * language notation is in the ISO format.
     * 
     */
    public static class MCRMetaHistoryDateText implements Cloneable {
        private String text;

        private String lang;

        public MCRMetaHistoryDateText() {
            text = "";
            lang = DEFAULT_LANGUAGE;
        }

        public MCRMetaHistoryDateText(String text, String lang) {
            setText(text, lang);
        }

        /**
         * This method get the text element as String.
         * 
         * @return the text
         */

        public String getText() {
            return text;
        }

        /**
         * This method get the language element as String of ISO code.
         * 
         * @return the language
         */
        public String getLang() {
            return lang;
        }

        /**
         * This method set the text element with the corresponding language.
         * 
         * @param text
         *            the text String
         * @param lang
         *            the language String
         */
        public void setText(String text, String lang) {
            if (lang == null || lang.length() == 0) {
                this.lang = DEFAULT_LANGUAGE;
            } else {
                this.lang = lang;
            }
            if (text == null || text.length() == 0) {
                this.text = "";
            } else {
                this.text = text;
            }
        }

        /**
         * This method set the text element.
         * 
         * @param text
         *            the text String of a date value
         */
        @Deprecated
        public void setText(String text) {
            if (text == null || text.length() == 0) {
                this.text = "";
            } else {
                this.text = text;
            }
        }

        /**
         * This method set the lang element.
         * 
         * @param lang
         *            the language String of a date value
         */
        @Deprecated
        public void setLang(String lang) {
            if (lang == null || lang.length() == 0) {
                this.lang = DEFAULT_LANGUAGE;
            } else {
                this.lang = lang;
            }
        }

        /**
         * This method validate the content. If lang and text are not empty, it return true otherwise it return false.
         * 
         * @return true if the content is valid.
         */
        public boolean isValid() {
            return !(lang.length() == 0 || text.length() == 0);
        }

        /**
         * This method check the equivalence of lang and text between this object
         * and a given MCRMetaHistoryDateText object.
         * 
         * @param obj a MCRMetaHistoryDateText instance
         * @return true if both parts are equal
         */
        public boolean equals(MCRMetaHistoryDateText obj) {
            return lang.equals(obj.getLang()) && text.equals(obj.getText());
        }

        @Override
        protected MCRMetaHistoryDateText clone() {
            MCRMetaHistoryDateText clone = null;
            try {
                clone = (MCRMetaHistoryDateText) super.clone();
            } catch (Exception e) {
                // this can not happen!
            }

            clone.text = this.text;
            clone.lang = this.lang;

            return clone;
        }
    }
}
