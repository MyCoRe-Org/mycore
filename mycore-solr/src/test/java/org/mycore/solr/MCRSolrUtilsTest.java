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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSolrUtilsTest {

    /**
     * Test method for {@link org.mycore.solr.MCRSolrUtils#escapeSearchValue(java.lang.String)}.
     */
    @Test
    public final void testEscapeSearchValue() {
        String restrictedChars = "+-&|!(){}[]^\"~:\\/";
        StringBuilder sb = new StringBuilder();
        for (char c : restrictedChars.toCharArray()) {
            sb.append("\\");
            sb.append(c);
        }
        String escapedChars = sb.toString();
        assertEquals("Not all required characters where escaped.", escapedChars,
            MCRSolrUtils.escapeSearchValue(restrictedChars));
    }

}
