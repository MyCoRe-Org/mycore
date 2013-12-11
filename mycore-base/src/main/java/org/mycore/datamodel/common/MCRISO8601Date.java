package org.mycore.datamodel.common;

import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.mycore.common.MCRConfiguration;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.util.StringTokenizer;
import com.ibm.icu.util.TimeZone;

/**
 * holds info about a specific point in time.
 * 
 * This class is used for handling ISO 8601 like date and dateTime formatted strings.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRISO8601Date {

    public static final String PROPERTY_STRICT_PARSING = "MCR.Metadata.SimpleDateFormat.StrictParsing";

    private static final Logger LOGGER = Logger.getLogger(MCRISO8601Date.class);

    private DateTimeFormatter dateTimeFormatter = MCRISO8601FormatChooser.getFormatter(null, null);

    private DateTime dt;

    private MCRISO8601Format isoFormat;

    /**
     * creates an empty instance.
     * use {@link #setDate(String)} to parse a date/time by this instance.
     */
    public MCRISO8601Date() {

    }

    /**
     * same as {@link #MCRISO8601Date()} and {@link #setDate(String)}.
     * @param isoString a date or dateTime string as defined on <a href="http://www.w3.org/TR/NOTE-datetime">W3C Page</a>
     */
    public MCRISO8601Date(final String isoString) {
        this();
        setDate(isoString);
    }

    private static Locale getLocale(final String locale) {
        String lang = "", country = "";
        final int pos = locale.indexOf("_");
        if (pos > 0) {
            lang = locale.substring(0, pos);
            country = locale.substring(pos + 1);
        } else {
            lang = locale;
        }
        return new Locale(lang, country);
    }

    /**
     * formats the date to a String.
     * @param format as in {@link DateTimeFormat}
     * @param locale used by format process
     * @return null if date is not set yet
     */
    public String format(final String format, final Locale locale) {
        DateTimeFormatter df = DateTimeFormat.forPattern(format);
        if (locale != null) {
            df = df.withLocale(locale);
        }

        return dt == null ? null : format.indexOf("G") == -1 ? df.print(dt) : df.print(dt).replace("-", "");
    }

    /**
     * returns the Date representing this element.
     * 
     * @return a new Date instance of the time set in this element
     */
    public final Date getDate() {
        return dt == null ? null : (Date) dt.toDate().clone();
    }

    /**
     * @return the dt
     */
    public DateTime getDt() {
        return dt;
    }

    /**
     * @return the isoFormat
     */
    public MCRISO8601Format getIsoFormat() {
        return isoFormat;
    }

    /**
     * returns a ISO 8601 conform String using the current set format.
     * 
     * @return date in ISO 8601 format, or null if date is unset.
     */
    public final String getISOString() {
        return dt == null ? null : dateTimeFormatter.print(dt);
    }

    /**
     * sets the date for this meta data object.
     * 
     * @param dt
     *            Date object representing date String in Element
     */
    public void setDate(final Date dt) {
        if (dt == null) {
            this.dt = null;
        } else {
            this.dt = new DateTime(dt.getTime());
        }
    }

    /**
     * sets the date for this meta data object
     * 
     * @param isoString
     *            Date in any form that is a valid W3C dateTime
     */
    public final void setDate(final String isoString) {
        DateTime dt = null;
        try {
            dt = getDateTime(MCRISO8601FormatChooser.cropSecondFractions(isoString));
        } catch (final RuntimeException e) {
            final boolean strictParsingEnabled = MCRConfiguration.instance().getBoolean(PROPERTY_STRICT_PARSING, true);
            if (!strictParsingEnabled) {
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

    /**
     * sets the input and output format.
     * 
     * please use only the formats defined on the <a href="http://www.w3.org/TR/NOTE-datetime">W3C Page</a>, which are also
     * exported as static fields by this class.
     * 
     */
    public void setFormat(final MCRISO8601Format isoFormat) {
        this.isoFormat = isoFormat;
        dateTimeFormatter = MCRISO8601FormatChooser.getFormatter(null, this.isoFormat);
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
        setFormat(MCRISO8601Format.getFormat(format));
    }

    private DateTime getDateTime(final String timeString) {
        dateTimeFormatter = MCRISO8601FormatChooser.getFormatter(timeString, isoFormat);
        return dateTimeFormatter.parseDateTime(timeString);
    }

    private DateTime guessDateTime(final String date) {
        final String locales = MCRConfiguration.instance().getString("MCR.Metadata.SimpleDateFormat.Locales", "de_DE");
        final StringTokenizer tok = new StringTokenizer(locales, ",");
        while (tok.hasMoreTokens()) {
            final Locale locale = getLocale(tok.nextToken());
            final DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, locale);
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            df.setLenient(true);
            DateTime result = null;
            try {
                final Date pDate = df.parse(date);
                result = new DateTime(pDate.getTime());
                return result;
            } catch (final ParseException e) {
                LOGGER.warn("Date guess failed for locale: " + locale);
                //we need no big exception in the logs, if we can't guess what it is, a warning should be enough
            }
        }
        LOGGER.error("Error trying to guess date for string: " + date);
        return null;
    }

    private void setDateTime(final DateTime dt) {
        if (dt == null) {
            this.dt = null;
        } else {
            this.dt = dt;
        }
    }

}
