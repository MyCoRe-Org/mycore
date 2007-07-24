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

import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * This class implements only static methods to normalize text values. Rules
 * written as x&gt;u .You can configure this normalization with two property
 * values<br>
 * <ul>
 * <li>MCR.Metadata.Normalize.AddRule - add more rules to the default rule</li>
 * <li>MCR.Metadata.Normalize.SetRule - replace the default rule</li>
 * </ul>
 * 
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 * @author Jens Kupferschmidt
 * 
 * @version $Revision$ $Date$
 */
public class MCRNormalizer {
    static Logger logger = Logger.getLogger(MCRNormalizer.class);
    
    /** List of characters that will be replaced */                                                                   /* small u with ring small c with caron  small thorn */ 
    private static String rules = "ä>ae ö>oe ü>ue ß>ss à>a á>a â>a å>a è>e é>e ê>e ì>i í>i î>i ò>o ó>o ô>o ù>u ú>u û>u \uc5af>u \uc48d>c š>s Þ>th";

    private static Pattern[] patterns;

    private static String[] replace;

    private static boolean normalize = true;

    private static MCRConfiguration config = MCRConfiguration.instance();

    private static String addRule = config.getString("MCR.Metadata.Normalize.AddRule", "");

    private static String setRule = config.getString("MCR.Metadata.Normalize.SetRule", "");

    static {
        if ((setRule != null) && (setRule.trim().length() != 0)) {
            rules = setRule;
        } else {
            if ((addRule != null) && (addRule.trim().length() != 0)) {
                rules = rules + " " + addRule;
            }

        }
        StringTokenizer st = new StringTokenizer(rules, "> ");
        int numPatterns = st.countTokens() / 2;

        patterns = new Pattern[numPatterns];
        replace = new String[numPatterns];

        for (int i = 0; i < numPatterns; i++) {
            patterns[i] = Pattern.compile(st.nextToken());
            replace[i] = st.nextToken();
            logger.debug("normalize -->"+patterns[i]+" to -->"+replace[i]);
        }
    }

    /**
     * This method replaces umlauts and other special characters of languages
     * like german to normalized lowercase a-z characters.
     * 
     * @param in
     *            the String to be normalized
     * @return the normalized String in lower case.
     */
    public static final String normalizeString(String in) {
        return normalizeString(in, normalize);
    }

    public static final String normalizeString(String in, boolean reallyNormalize) {
        if ((in == null) || (in.trim().length() == 0))
            return "";

        if (!reallyNormalize)
            return in;

        // in = in.toLowerCase(Locale.GERMANY).trim();
        in = in.toLowerCase().trim();

        for (int i = 0; i < patterns.length; i++)
            in = patterns[i].matcher(in).replaceAll(replace[i]);

        return in;
    }

    /**
     * Activates or deactivates normalizing. Used in miless software to make
     * indexing of scorm and searching possible
     * 
     * @param value
     *            true normalize strings false do not normalize strings
     * 
     */
    public static final void setDoNormalize(boolean value) {
        normalize = value;
    }
}
