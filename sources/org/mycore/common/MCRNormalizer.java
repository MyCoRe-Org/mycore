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
    static final Pattern AE_PATTERN = Pattern.compile("ä");

    static final Pattern OE_PATTERN = Pattern.compile("ö");

    static final Pattern UE_PATTERN = Pattern.compile("ü");

    static final Pattern SZ_PATTERN = Pattern.compile("ß");

    /**
     * This methode replace any characters of languages like german to
     * normalized values.
     * 
     * @param in
     *            a string
     * @return the converted string in lower case.
     */
    public static final String normalizeString(String in) {
        if (in == null) {
            return "";
        }

        in = in.toLowerCase(Locale.GERMANY);

        return AE_PATTERN.matcher( // replace "ä" by "ae"
                OE_PATTERN.matcher( // replace "ö" by "oe"
                        UE_PATTERN.matcher( // replace "ü" by "ue"
                                SZ_PATTERN.matcher(in).replaceAll("ss")) // replace
                                                                            // "ß"
                                                                            // by
                                                                            // "ss"
                                .replaceAll("ue")).replaceAll("oe")).replaceAll("ae");
    }
}
