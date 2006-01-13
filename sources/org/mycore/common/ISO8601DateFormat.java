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

package org.mycore.common;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.apache.log4j.Logger;

/**
 * Format and parse an ISO 8601 DateTimeFormat used in XML documents. This
 * lexical representation is the ISO 8601 extended format CCYY-MM-DDThh:mm:ss
 * where "CC" represents the century, "YY" the year, "MM" the month and "DD" the
 * day, preceded by an optional leading "-" sign to indicate a negative number.
 * If the sign is omitted, "+" is assumed. The letter "T" is the date/time
 * separator and "hh", "mm", "ss" represent hour, minute and second
 * respectively. This representation may be immediately followed by a "Z" to
 * indicate Coordinated Universal Time (UTC) or, to indicate the time zone, i.e.
 * the difference between the local time and Coordinated Universal Time,
 * immediately followed by a sign, + or -, followed by the difference from UTC
 * represented as hh:mm. For more information see <a
 * href="http://www.w3.org/TR/NOTE-datetime">W3C Info about ISO 8601</a>
 * 
 * @author Thomas Scheffler (yagee)
 * @author skaringa.sf.net
 * 
 * @version $Revision$ $Date$
 */
public class ISO8601DateFormat extends DateFormat {
    private static final long serialVersionUID = -874208053425536283L;
    
    private static final Logger LOGGER=Logger.getLogger(ISO8601DateFormat.class);

    private static final boolean useUTC=true;//MCRConfiguration.instance().getBoolean("MCR.dateformat_utc",true);

    /**
     * Construct a new ISO8601DateTimeFormat using the default time zone.
     * 
     */
    public ISO8601DateFormat() {
        setCalendar(Calendar.getInstance());
    }

    /**
     * Construct a new ISO8601DateTimeFormat using a specific time zone.
     * 
     * @param tz
     *            The time zone used to format and parse the date.
     */
    public ISO8601DateFormat(TimeZone tz) {
        setCalendar(Calendar.getInstance(tz));
    }

    /**
     * @see DateFormat#parse(String, ParsePosition)
     */
    public Date parse(String text, ParsePosition pos) {

        int i = pos.getIndex();

        try {
            int year = Integer.valueOf(text.substring(i, i + 4)).intValue();
            i += 4;

            if (text.charAt(i) != '-') {
                throw new NumberFormatException();
            }
            i++;

            int month = Integer.valueOf(text.substring(i, i + 2)).intValue() - 1;
            i += 2;

            if (text.charAt(i) != '-') {
                throw new NumberFormatException();
            }
            i++;

            int day = Integer.valueOf(text.substring(i, i + 2)).intValue();
            i += 2;

            if (text.charAt(i) != 'T') {
                throw new NumberFormatException();
            }
            i++;

            int hour = Integer.valueOf(text.substring(i, i + 2)).intValue();
            i += 2;

            if (text.charAt(i) != ':') {
                throw new NumberFormatException();
            }
            i++;

            int mins = Integer.valueOf(text.substring(i, i + 2)).intValue();
            i += 2;

            int secs = 0;
            if (i < text.length() && text.charAt(i) == ':') {
                // handle seconds flexible
                i++;

                secs = Integer.valueOf(text.substring(i, i + 2)).intValue();
                i += 2;
            }
            
            int millis=0;
            if (i < text.length() && text.charAt(i) == '.') {
                // found fraction of seconds
                i++;
                for (int j=1;isNumberCharacter(text.charAt(i));i++,j++){
                    /* we just handle up to milliseconds, but we don't know
                     * how long the frections of a seconds are. */
                    if (j<4){
                        millis=millis*10;
                        millis+=Integer.valueOf(text.substring(i,i+1)).intValue();
                    }
                }
            }

            calendar.set(year, month, day, hour, mins, secs);
            calendar.set(Calendar.MILLISECOND, millis);

            i = parseTZ(i, text);

        } catch (NumberFormatException ex) {
            pos.setErrorIndex(i);
            LOGGER.fatal(new StringBuffer("Error while parsing date: ").append(text).append(" at position ").append(i).append('.').toString(),ex);
            return null;
        } catch (IndexOutOfBoundsException ex) {
            pos.setErrorIndex(i);
            LOGGER.fatal(new StringBuffer("Error while parsing date: ").append(text).append(" at position ").append(i).append('.').toString(),ex);
            return null;
        } finally {
            pos.setIndex(i);
        }

        return calendar.getTime();
    }
    
    private boolean isNumberCharacter(char c){
        switch (c) {
        case '0':
            return true;
        case '1':
            return true;
        case '2':
            return true;
        case '3':
            return true;
        case '4':
            return true;
        case '5':
            return true;
        case '6':
            return true;
        case '7':
            return true;
        case '8':
            return true;
        case '9':
            return true;
        default:
            return false;
        }
    }

    /**
     * Parse the time zone.
     * 
     * @param i
     *            The position to start parsing.
     * @param text
     *            The text to parse.
     * @return The position after parsing has finished.
     */
    protected final int parseTZ(int i, String text) {
        if (i < text.length()) {
            // check and handle the zone/dst offset
            int offset = 0;
            if (text.charAt(i) == 'Z') {
                offset = 0;
                i++;
            } else {
                int sign = 1;
                if (text.charAt(i) == '-') {
                    sign = -1;
                } else if (text.charAt(i) != '+') {
                    throw new NumberFormatException("Expected character '+', '-' or 'Z' and got: "+text.charAt(i));
                }
                i++;

                int offsetHour = Integer.valueOf(text.substring(i, i + 2)).intValue();
                i += 2;

                if (text.charAt(i) != ':') {
                    throw new NumberFormatException();
                }
                i++;

                int offsetMin = Integer.valueOf(text.substring(i, i + 2)).intValue();
                i += 2;
                offset = ((offsetHour * 60) + offsetMin) * 60000 * sign;
            }
            int offsetCal = calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET);
            calendar.add(Calendar.MILLISECOND, offsetCal - offset);
        }
        return i;
    }

    /**
     * @see DateFormat#format(Date, StringBuffer, FieldPosition)
     */
    public StringBuffer format(Date date, StringBuffer sbuf, FieldPosition fieldPosition) {

        if (useUTC){
            calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        }
        calendar.setTime(date);

        writeCCYYMM(sbuf);

        sbuf.append('T');

        writehhmmss(sbuf);

        writeTZ(sbuf);

        return sbuf;
    }

    /**
     * Write the time zone string.
     * 
     * @param sbuf
     *            The buffer to append the time zone.
     */
    protected final void writeTZ(StringBuffer sbuf) {
        int offset = calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET);

        if (offset == 0) {
            sbuf.append('Z');
        } else {
            int offsetHour = offset / 3600000;
            int offsetMin = (offset % 3600000) / 60000;
            if (offset >= 0) {
                sbuf.append('+');
            } else {
                sbuf.append('-');
                offsetHour = 0 - offsetHour;
                offsetMin = 0 - offsetMin;
            }
            appendInt(sbuf, offsetHour, 2);
            sbuf.append(':');
            appendInt(sbuf, offsetMin, 2);
        }
    }

    /**
     * Write hour, minutes, and seconds and millis (if > 0).
     * 
     * @param sbuf
     *            The buffer to append the string.
     */
    protected final void writehhmmss(StringBuffer sbuf) {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        appendInt(sbuf, hour, 2);
        sbuf.append(':');

        int mins = calendar.get(Calendar.MINUTE);
        appendInt(sbuf, mins, 2);
        sbuf.append(':');

        int secs = calendar.get(Calendar.SECOND);
        appendInt(sbuf, secs, 2);
        
        int millis = calendar.get(Calendar.MILLISECOND);
        if (millis>0){
            sbuf.append('.');
            appendInt(sbuf, millis, 3);
        }
    }

    /**
     * Write century, year, and months.
     * 
     * @param sbuf
     *            The buffer to append the string.
     */
    protected final void writeCCYYMM(StringBuffer sbuf) {
        int year = calendar.get(Calendar.YEAR);
        appendInt(sbuf, year, 4);

        String month;
        switch (calendar.get(Calendar.MONTH)) {
        case Calendar.JANUARY:
            month = "-01-";
            break;
        case Calendar.FEBRUARY:
            month = "-02-";
            break;
        case Calendar.MARCH:
            month = "-03-";
            break;
        case Calendar.APRIL:
            month = "-04-";
            break;
        case Calendar.MAY:
            month = "-05-";
            break;
        case Calendar.JUNE:
            month = "-06-";
            break;
        case Calendar.JULY:
            month = "-07-";
            break;
        case Calendar.AUGUST:
            month = "-08-";
            break;
        case Calendar.SEPTEMBER:
            month = "-09-";
            break;
        case Calendar.OCTOBER:
            month = "-10-";
            break;
        case Calendar.NOVEMBER:
            month = "-11-";
            break;
        case Calendar.DECEMBER:
            month = "-12-";
            break;
        default:
            month = "-NA-";
            break;
        }
        sbuf.append(month);

        int day = calendar.get(Calendar.DAY_OF_MONTH);
        appendInt(sbuf, day, 2);
    }

    /**
     * Write an integer value with leading zeros.
     * 
     * @param buf
     *            The buffer to append the string.
     * @param value
     *            The value to write.
     * @param length
     *            The length of the string to write.
     */
    protected final void appendInt(StringBuffer buf, int value, int length) {
        int len1 = buf.length();
        buf.append(value);
        int len2 = buf.length();
        for (int i = len2; i < len1 + length; ++i) {
            buf.insert(len1, '0');
        }
    }
}
