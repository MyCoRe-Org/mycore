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

package org.mycore.solr;

import java.util.regex.Pattern;

import org.mycore.common.config.MCRConfiguration;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSolrUtils {
    //    private static String specialChars = "!&|+-(){}[]\"~*?:\\/^";
    /* '*' and '?' should always be threatened as special character */
    private static String specialChars = "!&|+-(){}[]\"~:\\/^";

    private static Pattern PATTERN_RESTRICTED = Pattern.compile("([\\Q" + specialChars + "\\E])");

    /**
     * Escapes characters in search values that need to be escaped for SOLR.
     * @see <a href="http://lucene.apache.org/core/4_3_0/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#Escaping_Special_Characters">List of special characters</a>
     * @param value any value to look for in a field
     * @return null if value is null
     */
    public static String escapeSearchValue(final String value) {
        if (value == null) {
            return null;
        }
        return PATTERN_RESTRICTED.matcher(value).replaceAll("\\\\$1");
    }

    /**
     * Checks if the application uses nested documents. If so, each reindex requires
     * an extra deletion. Using nested documents slows the solr index performance.
     *
     * @return true if nested documents are used, otherwise false
     */
    public static boolean useNestedDocuments() {
        return MCRConfiguration.instance().getBoolean("MCR.Module-solr.NestedDocuments", true);
    }

}
