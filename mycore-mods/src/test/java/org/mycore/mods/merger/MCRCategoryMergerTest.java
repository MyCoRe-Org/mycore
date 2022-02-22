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

package org.mycore.mods.merger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.Test;
import org.mycore.common.MCRJPATestCase;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTransactionHelper;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;

public class MCRCategoryMergerTest extends MCRJPATestCase {

    private static final String TEST_DIRECTORY = "MCRCategoryMergerTest/";

    @Override
    public void setUp() throws Exception {
        super.setUp();

        MCRSessionMgr.getCurrentSession();
        MCRTransactionHelper.isTransactionActive();
        loadCategory("institutes.xml");
        loadCategory("genre.xml");
        loadCategory("oa.xml");
    }

    private void loadCategory(String categoryFileName) throws URISyntaxException, JDOMException, IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        SAXBuilder saxBuilder = new SAXBuilder();
        InputStream categoryStream = classLoader.getResourceAsStream(TEST_DIRECTORY + categoryFileName);
        MCRCategory category = MCRXMLTransformer.getCategory(saxBuilder.build(categoryStream));
        MCRCategoryDAOFactory.getInstance().addCategory(null, category);
    }

    @Test
    public void testUnsupported() throws Exception {
        String a = "[mods:classification[@authority='foo']='xy123']";
        String b = "[mods:classification[@authority='foo']='yz456']";
        MCRMergerTest.test(a, b, a + b);
    }

    @Test
    public void testIdentical() throws Exception {
        String unsupported = "[mods:classification[@authority='foo']='xy123']";
        MCRMergerTest.test(unsupported, unsupported, unsupported);

        String supported = "[mods:classification[@authority='UDE']='CEN']";
        MCRMergerTest.test(supported, supported, supported);
    }

    @Test
    public void testIsDescendantCheck() throws Exception {
        MCRCategoryID cen = MCRCategoryID.fromString("institutes:CEN");
        MCRCategoryID cinch = MCRCategoryID.fromString("institutes:CINCH");
        MCRCategoryID ican = MCRCategoryID.fromString("institutes:ICAN");

        assertFalse(MCRCategoryMerger.oneIsDescendantOfTheOther(cinch, ican));
        assertFalse(MCRCategoryMerger.oneIsDescendantOfTheOther(ican, cinch));
        assertTrue(MCRCategoryMerger.oneIsDescendantOfTheOther(cen, ican));
        assertTrue(MCRCategoryMerger.oneIsDescendantOfTheOther(ican, cen));
    }

    @Test
    public void testMergeNotRedundant() throws Exception {
        String a = "[mods:classification[@authority='UDE']='CINCH']";
        String b = "[mods:classification[@authority='UDE']='ICAN']";
        MCRMergerTest.test(a, b, a + b);
    }

    @Test
    public void testMergeRedundant() throws Exception {
        String a = "[mods:classification[@authority='UDE']='CEN']";
        String b = "[mods:classification[@authority='UDE']='ICAN']";
        MCRMergerTest.test(a, b, b);
        MCRMergerTest.test(b, a, b);
    }

    @Test
    public void testMixedClassificationsWithinSameMODSElement() throws Exception {
        String a = "[mods:genre[@authority='marcgt']='article']";
        String b = "[mods:genre[@authority='marcgt']='book']";
        MCRMergerTest.test(a, b, a + b);

        String uri = "http://www.mycore.org/classifications/mir_genres";
        String c = "[mods:genre[@authorityURI='" + uri + "'][@valueURI='" + uri + "#collection']]";
        String d = "[mods:genre[@authorityURI='" + uri + "'][@valueURI='" + uri + "#proceedings']]";
        MCRMergerTest.test(a + c, b + d, a + d + b);
    }

    @Test
    public void testNonRepeatable() throws Exception {
        MCRConfiguration2.set("MCR.MODS.Merger.CategoryMerger.Repeatable.oa", "false");

        String uri = "http://www.mycore.org/classifications/oa";
        String green = "[mods:genre[@authorityURI='" + uri + "'][@valueURI='" + uri + "#green']]";
        String gold = "[mods:genre[@authorityURI='" + uri + "'][@valueURI='" + uri + "#gold']]";
        MCRMergerTest.test(green, gold, green);

        String platin = "[mods:genre[@authorityURI='" + uri + "'][@valueURI='" + uri + "#platin']]";
        MCRMergerTest.test(gold, platin, platin);
    }
}
