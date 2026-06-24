/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.mods.xsl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.jdom2.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRConstants;
import org.mycore.common.util.MCRTestCaseXSLTUtil;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRMODSFunctionsTests {

    @Test
    @DisplayName("mcrmods:pages-to-extent: start and end page")
    public void testStartEnd() throws Exception {
        Element extent = transform("pp. 3-4");
        assertEquals("3", extent.getChildText("start", MCRConstants.MODS_NAMESPACE));
        assertEquals("4", extent.getChildText("end", MCRConstants.MODS_NAMESPACE));
    }

    @Test
    @DisplayName("mcrmods:pages-to-extent: incomplete end page is completed")
    public void testEndPageCompleted() throws Exception {
        Element extent = transform("S. 3845 - 53");
        assertEquals("3845", extent.getChildText("start", MCRConstants.MODS_NAMESPACE));
        assertEquals("3853", extent.getChildText("end", MCRConstants.MODS_NAMESPACE));
    }

    @Test
    @DisplayName("mcrmods:pages-to-extent: total pages only")
    public void testTotalOnly() throws Exception {
        Element extent = transform("123 pages");
        assertEquals("123", extent.getChildText("total", MCRConstants.MODS_NAMESPACE));
    }

    private Element transform(String pages) throws TransformerException {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("pages", pages);
        parameters.put("fn-name", "pages-to-extent");
        Element result = MCRTestCaseXSLTUtil.transform("/xslt/functions/mods-test.xsl", parameters).getRootElement();
        Element extent = result.getChild("extent", MCRConstants.MODS_NAMESPACE);
        assertEquals("pages", extent.getAttributeValue("unit"));
        return extent;
    }
}
