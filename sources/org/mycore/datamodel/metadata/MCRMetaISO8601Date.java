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
package org.mycore.datamodel.metadata;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.Namespace;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import org.mycore.common.MCRException;

/**
 * provides support for a restricted range of formats, all of which are valid
 * ISO 8601 dates and times.
 * 
 * The range of supported formats is exactly the same range that is suggested by
 * the W3C <a href="http://www.w3.org/TR/NOTE-datetime">datetime profile</a> in
 * its version from 1997-09-15.
 * 
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 * @since 1.3
 */
public final class MCRMetaISO8601Date extends MCRMetaDefault implements MCRMetaInterface {

    private Element export;

    private boolean changed = true;

    private static final Namespace DEFAULT_NAMESPACE = Namespace.NO_NAMESPACE;

    private DateTime dt;

    private String format;

    /**
     * Year.
     */
    public final static String YEAR = "YYYY";

    /**
     * Year and month.
     */
    public final static String YEAR_MONTH = "YYYY-MM";

    /**
     * Complete date.
     */
    public final static String COMPLETE = "YYYY-MM-DD";

    /**
     * Complete date plus hours and minutes.
     */
    public final static String COMPLETE_HH_MM = "YYYY-MM-DDThh:mmTZD";

    /**
     * Complete date plus hours, minutes and seconds.
     */
    public final static String COMPLETE_HH_MM_SS = "YYYY-MM-DDThh:mm:ssTZD";

    /**
     * Complete date plus hours, minutes, seconds and a decimal fraction of a
     * second.
     * 
     * As opposed to the standard (unlimited number of digits in "decimal
     * fraction of a second") only the maximum of 3 digits (milliseconds) is
     * supported. More digits are allowed but get omited.
     */
    public final static String COMPLETE_HH_MM_SS_SSS = "YYYY-MM-DDThh:mm:ss.sTZD";

    private final static Set AVAILABLE_FORMATS;
    static {
        AVAILABLE_FORMATS = new HashSet(7, 1l);
        AVAILABLE_FORMATS.add(YEAR);
        AVAILABLE_FORMATS.add(YEAR_MONTH);
        AVAILABLE_FORMATS.add(COMPLETE);
        AVAILABLE_FORMATS.add(COMPLETE_HH_MM);
        AVAILABLE_FORMATS.add(COMPLETE_HH_MM_SS);
        AVAILABLE_FORMATS.add(COMPLETE_HH_MM_SS_SSS);
    }

    private final static DateTimeFormatter YEAR_FORMAT = ISODateTimeFormat.year();

    private final static DateTimeFormatter YEAR_MONTH_FORMAT = ISODateTimeFormat.yearMonth();

    private final static DateTimeFormatter COMPLETE_FORMAT = ISODateTimeFormat.date();

    // TODO: check complete_hh_mm
    private final static DateTimeFormatter COMPLETE_HH_MM_FORMAT = ISODateTimeFormat.dateHourMinute();

    private final static DateTimeFormatter COMPLETE_HH_MM_SS_FORMAT = ISODateTimeFormat.dateTimeNoMillis();

    private final static DateTimeFormatter COMPLETE_HH_MM_SS_SSS_FORMAT = ISODateTimeFormat.dateTime();

    private static final boolean USE_UTC = true;

    private DateTimeFormatter dateTimeFormatter = USE_UTC ? COMPLETE_HH_MM_SS_SSS_FORMAT.withZone(DateTimeZone.UTC) : COMPLETE_HH_MM_SS_SSS_FORMAT;

    private boolean valid = false;

    private static final Logger LOGGER = Logger.getLogger(MCRMetaISO8601Date.class);

    private static final Pattern MILLI_CHECK_PATTERN = Pattern.compile("\\.\\d{4,}\\+");

    /**
     * constructs a empty instance.
     * 
     * @see MCRMetaDefault#MCRMetaDefault()
     */
    public MCRMetaISO8601Date() {
        super();
    }

    /**
     * same as superImplentation but sets lang attribute to "null"
     * 
     * @see MCRMetaDefault#MCRMetaDefault(String, String, String, String, int)
     */
    public MCRMetaISO8601Date(String set_datapart, String set_subtag, String set_type, int set_inherted) {
        super(set_datapart, set_subtag, null, set_type, set_inherted);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.datamodel.metadata.MCRMetaDefault#createXML()
     */
    public Element createXML() throws MCRException {
        if (!changed) {
            return (Element) export.clone();
        }
        if (!isValid()) {
            debug();
            throw new MCRException("The content of MCRMetaXML is not valid.");
        }
        export = new org.jdom.Element(subtag, DEFAULT_NAMESPACE);
        export.setAttribute("inherited", (new Integer(inherited)).toString());
        if (!(this.format == null || this.format == COMPLETE_HH_MM_SS_SSS)) {
            export.setAttribute("format", this.format);
        }
        if ((type != null) && ((type = type.trim()).length() != 0)) {
            export.setAttribute("type", type);
        }
        export.setText(getISOString());
        changed = false;
        return (Element) export.clone();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.datamodel.metadata.MCRMetaDefault#setFromDOM(org.jdom.Element)
     */
    public void setFromDOM(org.jdom.Element element) {
        super.setFromDOM(element);
        setFormat(element.getAttributeValue("format"));
        setDate(element.getTextTrim());
    }

    /**
     * returns the namespace of this element
     * 
     * @return Returns the ns.
     */
    protected static Namespace getNs() {
        return DEFAULT_NAMESPACE;
    }

    /**
     * sets the date for this meta data object
     * 
     * @param String
     *            Date in any form that is a valid xsd:dateTime
     */
    public final void setDate(String isoString) {
        DateTime dt = null;
        try {
            dt = getDateTime(fixDate(isoString));
        } catch (RuntimeException e) {
            LOGGER.debug("Error while parsing date, set date to NULL.",e);
            dt=null;
        }
        setDateTime(dt);
    }

    private final static String fixDate(String isoString) {
        Matcher matcher = MILLI_CHECK_PATTERN.matcher(isoString);
        boolean result = matcher.find();
        if (result) {
            return matcher.replaceFirst(isoString.substring(matcher.start(), matcher.start() + 4) + "+");
        } else
            return isoString;
    }

    /**
     * returns the Date representing this element.
     * 
     * @return a new Date instance of the time set in this element
     */
    public final Date getDate() {
        return (dt==null)? null:new Date(dt.getMillis());
    }

    /**
     * sets the date for this meta data object
     * 
     * @param dt
     *            Date object representing date String in Element
     */
    public void setDate(Date dt) {
        if (dt == null) {
            this.dt = null;
            valid = false;
        } else {
            this.dt = new DateTime(dt.getTime());
            valid = true;
        }
        changed = true;
    }

    private void setDateTime(DateTime dt) {
        if (dt == null) {
            this.dt = null;
            valid = false;
        } else {
            this.dt = dt;
            valid = true;
        }
        changed = true;
    }

    private DateTime getDateTime(String timeString) {
        return dateTimeFormatter.parseDateTime(timeString);
    }

    /**
     * returns a ISO 8601 conform String using the current set format.
     * @return date in ISO 8601 format, or null if date is unset.
     */
    public final String getISOString() {
        return (dt==null)? null:dateTimeFormatter.print(this.dt);
    }

    private void setFormatter(String formatter) {
        if (!AVAILABLE_FORMATS.contains(formatter)) {
            dateTimeFormatter = COMPLETE_HH_MM_SS_SSS_FORMAT;
        } else if (formatter.equals(YEAR)) {
            dateTimeFormatter = YEAR_FORMAT;
        } else if (formatter.equals(YEAR_MONTH)) {
            dateTimeFormatter = YEAR_MONTH_FORMAT;
        } else if (formatter.equals(COMPLETE)) {
            dateTimeFormatter = COMPLETE_FORMAT;
        } else if (formatter.equals(COMPLETE_HH_MM)) {
            dateTimeFormatter = COMPLETE_HH_MM_FORMAT;
        } else if (formatter.equals(COMPLETE_HH_MM_SS)) {
            dateTimeFormatter = COMPLETE_HH_MM_SS_FORMAT;
        } else if (formatter.equals(COMPLETE_HH_MM_SS_SSS)) {
            dateTimeFormatter = COMPLETE_HH_MM_SS_SSS_FORMAT;
        } else {
            LOGGER.warn("Somebody forgot to add " + formatter + " to AVAILABLE_FORMATS");
            dateTimeFormatter = COMPLETE_HH_MM_SS_SSS_FORMAT;
        }
        if (USE_UTC) {
            dateTimeFormatter = dateTimeFormatter.withZone(DateTimeZone.UTC);
        }
    }

    /**
     * sets the input and output format.
     * 
     * please use only the formats defined on the <a
     * href="http://www.w3.org/TR/NOTE-datetime">W3C Page</a>, which are also
     * exported as static fields by this class.
     * 
     * @param format
     *            a format string that is valid conforming to xsd:dateTime
     *            schema type.
     * 
     */
    public void setFormat(String format) {
        if (format == null) {
            LOGGER.debug("Format was not given (null), fallback to:" + COMPLETE_HH_MM_SS_SSS);
            this.format = COMPLETE_HH_MM_SS_SSS;
        } else if (AVAILABLE_FORMATS.contains(format)) {
            this.format = format;
        } else {
            LOGGER.debug(format + " is a unknown format, fallback to:" + COMPLETE_HH_MM_SS_SSS);
            this.format = COMPLETE_HH_MM_SS_SSS;
        }
        setFormatter(this.format);
    }

    /**
     * gets the input and output format.
     * 
     * this is allways a String that is also exported as static fields by this class.
     * 
     * @return a format string that is valid conforming to xsd:dateTime schema type.
     * 
     */
    public String getFormat() {
        return (this.format==null)? COMPLETE_HH_MM_SS_SSS:this.format;
    }

    /*
     * 
     * @see org.mycore.datamodel.metadata.MCRMetaDefault#createTextSearch(boolean)
     */
    public String createTextSearch(boolean textsearch) throws MCRException {
        return "";
    }

    /**
     * This method put debug data to the logger (for the debug mode).
     */
    public void debug() {
        LOGGER.debug("Start Class : MCRMetaTimestamp");
        super.debugDefault();
        LOGGER.debug("Date=" + dateTimeFormatter.print(dt));
    }

    /**
     * clone of this instance
     * 
     * you will get a (deep) clone of this element
     * 
     * @see java.lang.Object#clone()
     */
    public Object clone() {
        MCRMetaISO8601Date out = null;

        try {
            out = (MCRMetaISO8601Date) super.clone();
        } catch (CloneNotSupportedException e) {
            LOGGER.warn(new StringBuffer(MCRMetaISO8601Date.class.getName()).append(" could not be cloned."), e);

            return null;
        }

        out.changed = true;

        return out;
    }

    /**
     * checks the formal correctness of this element.
     * 
     * This check includes:
     * <ol>
     * <li>the included date is set</li>
     * <li>the super implementation returns true</li>
     * </ol>
     * 
     * @see MCRMetaDefault#isValid()
     * @return false, if any test fails and the instance should not be used for
     *         persistence purposes
     */
    public boolean isValid() {
        if (!valid || !super.isValid()) {
            return false;
        }
        return true;
    }
}