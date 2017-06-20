package org.mycore.datamodel.common;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;

/**
 * holds info about a specific point in time. This class is used for handling ISO 8601 like date and dateTime formatted
 * strings.
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRISO8601Date {

    public static final String PROPERTY_STRICT_PARSING = "MCR.Metadata.SimpleDateFormat.StrictParsing";

    private static final Logger LOGGER = LogManager.getLogger(MCRISO8601Date.class);

    private DateTimeFormatter dateTimeFormatter = MCRISO8601FormatChooser.getFormatter(null, null);

    private TemporalAccessor dt;

    private MCRISO8601Format isoFormat;

    /**
     * creates an empty instance. use {@link #setDate(String)} to parse a date/time by this instance.
     */
    public MCRISO8601Date() {

    }

    /**
     * same as {@link #MCRISO8601Date()} and {@link #setDate(String)}.
     * 
     * @param isoString
     *            a date or dateTime string as defined on <a href="http://www.w3.org/TR/NOTE-datetime">W3C Page</a>
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

    public static MCRISO8601Date now() {
        MCRISO8601Date instance = new MCRISO8601Date();
        instance.setInstant(Instant.now());
        return instance;
    }

    /**
     * formats the date to a String.
     * 
     * @param format
     *            as in {@link MCRISO8601Format}
     * @param locale
     *            used by format process
     * @return null if date is not set yet
     */
    public String format(final String format, final Locale locale) {
        return format(format, locale, null);
    }

    /**
     * formats the date to a String.
     * 
     * @param format
     *            as in {@link MCRISO8601Format}
     * @param locale
     *            used by format process
     * @param timeZone
     *            valid timeZone id, e.g. "Europe/Berlin", or null
     * @return null if date is not set yet
     */
    public String format(final String format, final Locale locale, String timeZone) {
        DateTimeFormatter df = DateTimeFormatter.ofPattern(format);
        if (locale != null) {
            df = df.withLocale(locale);
        }
        ZoneId zone = null;
        if (timeZone != null) {
            try {
                zone = ZoneId.of(timeZone);
            } catch (DateTimeException e) {
                LOGGER.warn(e.getMessage());
            }
        }
        if (zone == null) {
            zone = ZoneId.systemDefault();
        }
        df = df.withZone(zone);
        if (LOGGER.isDebugEnabled()) {
            String msg = MessageFormat.format("DateTime ''{0}'', using time zone ''{1}'', formatted: {2}", dt, zone,
                dt != null ? df.format(dt) : null);
            LOGGER.debug(msg);
        }
        String formatted = null;
        try {
            formatted = dt == null ? null : format.indexOf("G") == -1 ? df.format(dt) : df.format(dt).replace("-", "");
        } catch (Exception e) {
            LOGGER.error("Could not format date", e);
        }
        return formatted;
    }

    /**
     * returns the Date representing this element.
     * 
     * @return a new Date instance of the time set in this element
     */
    public final Date getDate() {
        return dt == null ? null : Date.from(Instant.from(dt));
    }

    /**
     * @return the dt
     */
    public TemporalAccessor getDt() {
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
        return dt == null ? null : dateTimeFormatter.format(dt);
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
            this.dt = dt.toInstant();
        }
    }

    /**
     * sets the date for this meta data object
     * 
     * @param isoString
     *            Date in any form that is a valid W3C dateTime
     */
    public final void setDate(final String isoString) {
        TemporalAccessor dt = null;
        try {
            dt = getDateTime(MCRISO8601FormatChooser.cropSecondFractions(isoString));
        } catch (final RuntimeException e) {
            final boolean strictParsingEnabled = true;
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
        setInstant(dt);
    }

    /**
     * sets the input and output format. please use only the formats defined on the
     * <a href="http://www.w3.org/TR/NOTE-datetime">W3C Page</a>, which are also exported as static fields by this
     * class.
     */
    public void setFormat(final MCRISO8601Format isoFormat) {
        this.isoFormat = isoFormat;
        dateTimeFormatter = MCRISO8601FormatChooser.getFormatter(null, this.isoFormat);
    }

    /**
     * sets the input and output format. please use only the formats defined on the
     * <a href="http://www.w3.org/TR/NOTE-datetime">W3C Page</a>, which are also exported as static fields by this
     * class.
     * 
     * @param format
     *            a format string that is valid conforming to xsd:duration schema type.
     */
    public void setFormat(String format) {
        setFormat(MCRISO8601Format.getFormat(format));
    }

    private TemporalAccessor getDateTime(final String timeString) {
        dateTimeFormatter = MCRISO8601FormatChooser.getFormatter(timeString, isoFormat);
        return dateTimeFormatter.parseBest(timeString, ZonedDateTime::from, LocalDateTime::from, LocalDate::from,
            YearMonth::from,
            Year::from);
    }

    private TemporalAccessor guessDateTime(final String date) {
        final String locales = MCRConfiguration.instance().getString("MCR.Metadata.SimpleDateFormat.Locales", "de_DE");
        final StringTokenizer tok = new StringTokenizer(locales, ",");
        while (tok.hasMoreTokens()) {
            final Locale locale = getLocale(tok.nextToken());
            final DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, locale);
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            df.setLenient(true);
            TemporalAccessor result = null;
            try {
                final Date pDate = df.parse(date);
                result = pDate.toInstant();
                return result;
            } catch (final ParseException e) {
                LOGGER.warn("Date guess failed for locale: " + locale);
                //we need no big exception in the logs, if we can't guess what it is, a warning should be enough
            }
        }
        LOGGER.error("Error trying to guess date for string: " + date);
        return null;
    }

    private void setInstant(final TemporalAccessor dt) {
        if (dt == null) {
            this.dt = null;
        } else {
            this.dt = dt;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final MCRISO8601Date other = (MCRISO8601Date) obj;
        return Objects.equals(this.dt, other.dt);
    }

}
