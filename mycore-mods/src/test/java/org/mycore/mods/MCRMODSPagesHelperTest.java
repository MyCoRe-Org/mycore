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

package org.mycore.mods;

import static org.junit.Assert.assertEquals;

import org.jdom2.Element;
import org.junit.Test;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRTestCase;

public class MCRMODSPagesHelperTest extends MCRTestCase {

    @Test
    public void testPages2Extent() throws Exception {
        testPattern("4-8", "start=4", "end=8");
        testPattern(" 234 - 238", "start=234", "end=238");
        testPattern("S. 234 - 238", "start=234", "end=238");
        testPattern("pp. 234 - 238", "start=234", "end=238");
        testPattern(" 3ABC8 \u2010 3ABC567", "start=3ABC8", "end=3ABC567");
        testPattern("123-45 - 123-49", "start=123-45", "end=123-49");

        testPattern("234 (5 pages)", "start=234", "total=5");
        testPattern("234 (5 Seiten)", "start=234", "total=5");
        testPattern("234 (5 S.)", "start=234", "total=5");
        testPattern("p. 234 (5 pages)", "start=234", "total=5");
        testPattern("3ABC8 (13 pages)", "start=3ABC8", "total=13");

        testPattern("234", "start=234");
        testPattern("S. 123", "start=123");
        testPattern("S. 123 f.", "start=123");
        testPattern("S. 123 ff.", "start=123");
        testPattern("S. 123 ff", "start=123");

        testPattern("11 pages", "total=11");
        testPattern("20 Seiten", "total=20");
        testPattern("15 S.", "total=15");

        testPattern("3-4 und 7-8", "list=3-4 und 7-8");
    }

    @Test
    public void testEndPageCompletion() throws Exception {
        testPattern("3845 - 53", "start=3845", "end=3853");
        testPattern("123 - 7", "start=123", "end=127");
    }

    private void testPattern(String input, String... expected) throws Exception {
        Element extent = MCRMODSPagesHelper.buildExtentPages(input);
        assertEquals(expected.length, extent.getChildren().size());
        for (String token : expected) {
            String name = token.split("=")[0];
            String value = token.split("=")[1];
            assertEquals(value, extent.getChildText(name, MCRConstants.MODS_NAMESPACE));
        }
    }
}
