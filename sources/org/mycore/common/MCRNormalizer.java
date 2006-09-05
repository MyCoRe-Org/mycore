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

import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * This class implements only static methods to normalize text values.
 * 
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 */
public class MCRNormalizer {
    /** List of characters that will be replaced */
    private static String rules = "ä>ae ö>oe ü>ue ß>ss à>a á>a â>a è>e é>e ê>e ì>i í>i î>i ò>o ó>o ô>o ù>u ú>u û>u";

    private static Pattern[] patterns;

    private static String[] replace;

    private static boolean normalize = true; 
    static {
        StringTokenizer st = new StringTokenizer(rules, "> ");
        int numPatterns = st.countTokens() / 2;

        patterns = new Pattern[numPatterns];
        replace = new String[numPatterns];

        for (int i = 0; i < numPatterns; i++) {
            patterns[i] = Pattern.compile(st.nextToken());
            replace[i] = st.nextToken();
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
        if ((in == null) || (in.trim().length() == 0))
            return "";

        if ( !normalize )
          return in;
        
        in = in.toLowerCase(Locale.GERMANY).trim();

        for (int i = 0; i < patterns.length; i++)
            in = patterns[i].matcher(in).replaceAll(replace[i]);

        return in;
    }
    
    /**
     * This method activatesor deactivates Normalizing
     * use from miless software to make indexing of scorm and searching possible
     * 
     * @param value
     *            true  normalize strings
     *            false do not normalize strings
     *            
     */
    public static final void setStatus(boolean value)
    {
      normalize = value;
    }
}
