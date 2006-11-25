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
import java.util.Locale;

import org.jdom.Namespace;

/**
 * This class replaces the deprecated MCRDefaults interface and provides some
 * final static fields of common use.
 * 
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public final class MCRConstants {
    /** The URL of the XLink */
    private final static String XLINK_URL = "http://www.w3.org/1999/xlink";
    public final static Namespace XLINK_NAMESPACE = Namespace.getNamespace("xlink", XLINK_URL);

    /** The URL of the XSI */
    private final static String XSI_URL = "http://www.w3.org/2001/XMLSchema-instance";
    public final static Namespace XSI_NAMESPACE = Namespace.getNamespace("xsi", XSI_URL);

    /** The URL of the XSL */
    private final static String XSL_URL = "http://www.w3.org/1999/XSL/Transform";
    public final static Namespace XSL_NAMESPACE = Namespace.getNamespace("xsl", XSL_URL);

    /** The default encoding */
    public final static String DEFAULT_ENCODING = "UTF-8";

    /** The supported language codes (like xml:lang definition) * */
    public static String[] SUPPORTED_LANG = {

    // ISO-639 with ISO-3166 extendions
            "de", "de-DE", "en", "en-UK", "en-US",
            // extendion for historical languages
            "x-egy", // egypt
            "x-ara", // arabic
            "x-grc", // old greek
            "x-lat", // latin
            "x-cop", // coptic

            // extension for DINI Specification Labels (OAI-Interface)
            "x-dini" };

    /** The date format for the supported languages * */
    public static DateFormat[] DATE_FORMAT = {

    // ISO-639 with ISO-3166 extendions
            DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.GERMAN), // de
            DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.GERMANY), // de-DE
            DateFormat.getDateInstance(DateFormat.SHORT, Locale.UK), // en
            DateFormat.getDateInstance(DateFormat.SHORT, Locale.UK), // en-UK
            DateFormat.getDateInstance(DateFormat.SHORT, Locale.US), // en-US

            // extendion for historical languages (all use the default)
            DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()), // x-egy
            DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()), // x-ara
            DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()), // x-grc
            DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()), // x-lat
            DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()) // x-cop
    };
}
