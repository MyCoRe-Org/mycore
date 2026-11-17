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

package org.mycore.common.xsl;

import static org.junit.Assert.assertEquals;
import static org.mycore.common.util.MCRTestCaseXSLTUtil.prepareTestDocument;
import static org.mycore.common.util.MCRTestCaseXSLTUtil.transform;

import java.util.Map;

import org.junit.Test;
import org.mycore.common.MCRTestCase;

public class MCRXSLI18NTest extends MCRTestCase {
    private static final String XSL = "/xslt/functions/i18n-test.xsl";

    @Test
    public void testTranslateWithParams() throws Exception {
        assertEquals("foo und bar", translateWithParams("foo", "bar"));
    }

    @Test
    public void testTranslateWithParamsSpecialCharacters() throws Exception {
        // characters with a special meaning in a fn:replace replacement string are inserted literally
        assertEquals("Journal $ Economics und bar", translateWithParams("Journal $ Economics", "bar"));
        assertEquals("Price $1 und bar", translateWithParams("Price $1", "bar"));
        assertEquals("A\\B und bar", translateWithParams("A\\B", "bar"));
    }

    private String translateWithParams(String arg0, String arg1) throws Exception {
        return transform(prepareTestDocument("test-translate-with-params"), XSL,
            Map.of("key", "junit.params", "arg0", arg0, "arg1", arg1)).getRootElement().getText();
    }

}
