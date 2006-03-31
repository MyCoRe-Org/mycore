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

import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import org.mycore.common.MCRConfiguration;
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
    
    private static final MCRConfiguration CONFIG=MCRConfiguration.instance();

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

    private DateTimeFormatter dateTimeFormatter = FormatChooser.getFormatter(null, null);

    private boolean valid = false;

    private static final Logger LOGGER = Logger.getLogger(MCRMetaISO8601Date.class);

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
            throw new MCRException("The content of MCRMetaISO8601Date is not valid.");
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
        this.export=(Element)element.clone();
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
     * @param isoString
     *            Date in any form that is a valid W3C dateTime
     */
    public final void setDate(String isoString) {
        DateTime dt = null;
        try {
            dt = getDateTime(FormatChooser.cropSecondFractions(isoString));
        } catch (RuntimeException e) {
            boolean strictParsingEnabled=CONFIG.getBoolean("MCR.SimpleDateFormat.strictParsing",true);
            if (!strictParsingEnabled){
                /*
                 * Last line of defence against the worst dates of the universe ;o)
                 */
                LOGGER.warn("Strict date parsing is disabled. This may result in incorrect dates.");
                dt = guessDateTime(isoString);
            } else {
                LOGGER.debug("Error while parsing date, set date to NULL.", e);
                dt = null;
            }
        }
        setDateTime(dt);
    }
    
    private DateTime guessDateTime(String date){
        String locales=CONFIG.getString("MCR.SimpleDateFormat.locales","de_DE");
        StringTokenizer tok=new StringTokenizer(locales,",");
        while (tok.hasMoreTokens()){
            Locale locale= getLocale(tok.nextToken());
            DateFormat df= DateFormat.getDateInstance(DateFormat.SHORT,locale);
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            df.setLenient(true);
            DateTime result=null;
            try {
                Date pDate=df.parse(date);
                result=new DateTime(pDate.getTime());
                return result;
            } catch (ParseException e) {
                LOGGER.debug("Date guess failed for locale: "+locale,e);
            }
        }
        LOGGER.error("Error trying to guess date for string: "+date);
        return null;
    }
    
    private static Locale getLocale(String locale){
        String lang="",country="";
        int pos=locale.indexOf("_");
        if (pos>0){
            lang=locale.substring(0,pos);
            country=locale.substring(pos+1);
        } else {
            lang=locale;
        }
        return new Locale(lang,country);
    }

    /**
     * returns the Date representing this element.
     * 
     * @return a new Date instance of the time set in this element
     */
    public final Date getDate() {
        return (dt == null) ? null : new Date(dt.getMillis());
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
        dateTimeFormatter = FormatChooser.getFormatter(timeString, this.format);
        return dateTimeFormatter.parseDateTime(timeString);
    }

    /**
     * returns a ISO 8601 conform String using the current set format.
     * 
     * @return date in ISO 8601 format, or null if date is unset.
     */
    public final String getISOString() {
        return (dt == null) ? null : dateTimeFormatter.print(this.dt);
    }

    /**
     * sets the input and output format.
     * 
     * please use only the formats defined on the <a
     * href="http://www.w3.org/TR/NOTE-datetime">W3C Page</a>, which are also
     * exported as static fields by this class.
     * 
     * @param format
     *            a format string that is valid conforming to xsd:duration
     *            schema type.
     * 
     */
    public void setFormat(String format) {
        if (format == null) {
            this.format = null;
        } else if (AVAILABLE_FORMATS.contains(format)) {
            this.format = format;
        } else {
            LOGGER.debug(format + " is a unknown format, fallback to: null");
            this.format = null;
        }
        dateTimeFormatter = FormatChooser.getFormatter(null, this.format);
    }

    /**
     * gets the input and output format.
     * 
     * this is a String that is also exported as static fields by this class, or
     * null if not defined.
     * 
     * @return a format string that is valid conforming to xsd:duration schema
     *         type, or null if not defined.
     * 
     */
    public String getFormat() {
        return this.format;
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
        LOGGER.debug("Start Class : MCRMetaISO8601Date");
        super.debugDefault();
        LOGGER.debug("Date=" + ((dt==null)?null:dateTimeFormatter.print(dt)));
        LOGGER.debug("Format=" + this.format);
        XMLOutputter xout=new XMLOutputter(Format.getPrettyFormat());
        StringWriter sw=new StringWriter();
        try {
            xout.output(this.export,sw);
            LOGGER.debug("JDOM=" + sw.toString());
        } catch (IOException e) {
            //ignore
        }
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

    /**
     * is a helper class for MCRMetaISO8601Date.
     * 
     * Please be aware that this class is not supported. It may disappear some day or methods get removed.
     * 
     * @author Thomas Scheffler (yagee)
     *
     * @version $Revision$ $Date$
     * @since 1.3
     */
    protected static final class FormatChooser {
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

        protected final static DateTimeFormatter YEAR_FORMAT = ISODateTimeFormat.year();

        protected final static DateTimeFormatter YEAR_MONTH_FORMAT = ISODateTimeFormat.yearMonth();

        protected final static DateTimeFormatter COMPLETE_FORMAT = ISODateTimeFormat.date();

        protected final static DateTimeFormatter COMPLETE_HH_MM_FORMAT = ISODateTimeFormat.dateHourMinute();

        protected final static DateTimeFormatter COMPLETE_HH_MM_SS_FORMAT = ISODateTimeFormat.dateTimeNoMillis();

        protected final static DateTimeFormatter COMPLETE_HH_MM_SS_SSS_FORMAT = ISODateTimeFormat.dateTime();

        protected final static DateTimeFormatter UTC_YEAR_FORMAT = ISODateTimeFormat.year().withZone(DateTimeZone.UTC);

        protected final static DateTimeFormatter UTC_YEAR_MONTH_FORMAT = ISODateTimeFormat.yearMonth().withZone(DateTimeZone.UTC);

        protected final static DateTimeFormatter UTC_COMPLETE_FORMAT = ISODateTimeFormat.date().withZone(DateTimeZone.UTC);

        protected final static DateTimeFormatter UTC_COMPLETE_HH_MM_FORMAT = ISODateTimeFormat.dateHourMinute().withZone(DateTimeZone.UTC);

        protected final static DateTimeFormatter UTC_COMPLETE_HH_MM_SS_FORMAT = ISODateTimeFormat.dateTimeNoMillis().withZone(DateTimeZone.UTC);

        protected final static DateTimeFormatter UTC_COMPLETE_HH_MM_SS_SSS_FORMAT = ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC);

        private static final Pattern MILLI_CHECK_PATTERN = Pattern.compile("\\.\\d{4,}\\+");

        private static final boolean USE_UTC = true;

        /**
         * returns a DateTimeFormatter for the given isoString or format.
         * 
         * This method prefers the format parameter. So if it's not null or not
         * zero length this method will interpret the format string. You can
         * also get a formatter for e specific iso String. In either case if the
         * underlying algorithm can not determine an exact matching formatter it
         * will allway fall back to a default. So this method will never return
         * null.
         * 
         * @param isoString
         *            an ISO 8601 formatted time String, or null
         * @param format
         *            a valid format String, or null
         * @return returns a specific DateTimeFormatter
         */
        public static DateTimeFormatter getFormatter(String isoString, String format) {
            DateTimeFormatter df;
            if ((format != null) && (format.length() != 0)) {
                df = getFormatterForFormat(format);
            } else if ((isoString != null) && (isoString.length() != 0)) {
                df = getFormatterForDuration(isoString);
            } else {
                df = COMPLETE_HH_MM_SS_SSS_FORMAT;
            }
            if (USE_UTC) {
                df = df.withZone(DateTimeZone.UTC);
            }
            return df;
        }

        private static DateTimeFormatter getFormatterForFormat(String format) {
            if (!AVAILABLE_FORMATS.contains(format)) {
                return USE_UTC ? UTC_COMPLETE_HH_MM_SS_SSS_FORMAT : COMPLETE_HH_MM_SS_SSS_FORMAT;
            } else if (format.equals(YEAR)) {
                return USE_UTC ? UTC_YEAR_FORMAT : YEAR_FORMAT;
            } else if (format.equals(YEAR_MONTH)) {
                return USE_UTC ? UTC_YEAR_MONTH_FORMAT : YEAR_MONTH_FORMAT;
            } else if (format.equals(COMPLETE)) {
                return USE_UTC ? UTC_COMPLETE_FORMAT : COMPLETE_FORMAT;
            } else if (format.equals(COMPLETE_HH_MM)) {
                return USE_UTC ? UTC_COMPLETE_HH_MM_FORMAT : COMPLETE_HH_MM_FORMAT;
            } else if (format.equals(COMPLETE_HH_MM_SS)) {
                return USE_UTC ? UTC_COMPLETE_HH_MM_SS_FORMAT : COMPLETE_HH_MM_SS_FORMAT;
            } else if (format.equals(COMPLETE_HH_MM_SS_SSS)) {
                return USE_UTC ? UTC_COMPLETE_HH_MM_SS_SSS_FORMAT : COMPLETE_HH_MM_SS_SSS_FORMAT;
            } else {
                LOGGER.warn("Somebody forgot to add " + format + " to AVAILABLE_FORMATS");
                return USE_UTC ? UTC_COMPLETE_HH_MM_SS_SSS_FORMAT : COMPLETE_HH_MM_SS_SSS_FORMAT;
            }
        }

        private static DateTimeFormatter getFormatterForDuration(String isoString) {
            boolean test = false;
            switch (isoString.length()) {
            case 4:
                return USE_UTC ? UTC_YEAR_FORMAT : YEAR_FORMAT;
            case 7:
                return USE_UTC ? UTC_YEAR_MONTH_FORMAT : YEAR_MONTH_FORMAT;
            case 10:
                return USE_UTC ? UTC_COMPLETE_FORMAT : COMPLETE_FORMAT;
            case 17: // YYYY-MM-DDThh:mm'Z'
                test = true;
            case 22:
                if (test || !isoString.endsWith("Z")) {
                    // YYYY-MM-DDThh:mm[+-]hh:mm
                    return USE_UTC ? UTC_COMPLETE_HH_MM_FORMAT : COMPLETE_HH_MM_FORMAT;
                }
                // YYYY-MM-DDThh:mm:ss.s'Z'
                return USE_UTC ? UTC_COMPLETE_HH_MM_SS_SSS_FORMAT : COMPLETE_HH_MM_SS_SSS_FORMAT;
            case 20: // YYYY-MM-DDThh:mm:ss'Z'
            case 25: // YYYY-MM-DDThh:mm:ss[+-]hh:mm
                return USE_UTC ? UTC_COMPLETE_HH_MM_SS_FORMAT : COMPLETE_HH_MM_SS_FORMAT;
            case 23: // YYYY-MM-DDThh:mm:ss.ss'Z'
            case 24: // YYYY-MM-DDThh:mm:ss.sss'Z'
            case 27: // YYYY-MM-DDThh:mm:ss.s[+-]hh:mm
            case 28: // YYYY-MM-DDThh:mm:ss.ss[+-]hh:mm
            case 29: // YYYY-MM-DDThh:mm:ss.ss[+-]hh:mm
                return USE_UTC ? UTC_COMPLETE_HH_MM_SS_SSS_FORMAT : COMPLETE_HH_MM_SS_SSS_FORMAT;
            default:
                return USE_UTC ? UTC_COMPLETE_HH_MM_SS_SSS_FORMAT : COMPLETE_HH_MM_SS_SSS_FORMAT;
            }
        }

        /**
         * returns a String that has not more than 3 digits representing
         * "fractions of a second".
         * 
         * If isoString has no or not more than 3 digits this method simply
         * returns isoString.
         * 
         * @param isoString
         *            an ISO 8601 formatted time String
         * @return an ISO 8601 formatted time String with at max 3 digits for
         *         fractions of a second
         */
        public final static String cropSecondFractions(String isoString) {
            Matcher matcher = MILLI_CHECK_PATTERN.matcher(isoString);
            boolean result = matcher.find();
            if (result) {
                return matcher.replaceFirst(isoString.substring(matcher.start(), matcher.start() + 4) + "+");
            }
            return isoString;
        }

    }
}