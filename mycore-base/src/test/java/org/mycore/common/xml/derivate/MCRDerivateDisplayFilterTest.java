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

package org.mycore.common.xml.derivate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.xml.derivate.MCRDerivateTypeDerivateDisplayFilter.Mapping;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaClassification;
import org.mycore.datamodel.metadata.MCRObjectService;
import org.mycore.resource.MCRResourceHelper;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
public class MCRDerivateDisplayFilterTest {

    public static final String TEST_DIRECTORY = MCRDerivateDisplayFilterTest.class.getSimpleName() + "/";

    public MCRCategoryDAO getDAO() {
        return MCRCategoryDAO.obtainInstance();
    }

    @BeforeEach
    public void setUp() throws Exception {
        MCRCategoryDAO categoryDao = MCRCategoryDAO.obtainInstance();
        categoryDao.addCategory(null, MCRXMLTransformer.getCategory(loadXml("derivate_types.xml")));
    }

    private static Document loadXml(String fileName) throws JDOMException, IOException {
        return new SAXBuilder().build(MCRResourceHelper.getResourceAsStream(TEST_DIRECTORY + fileName));
    }

    @Test
    public void noop() throws Exception {

        MCRDerivateDisplayFilter filter = new MCRNoOpDerivateDisplayFilter();

        MCRDerivate derivate = getDerivate();

        assertNull(filter.isDisplayEnabled(derivate, null));
        assertNull(filter.isDisplayEnabled(derivate, "foo"));

    }

    @Test
    public void serviceFlag() throws Exception {

        MCRDerivateDisplayFilter filter = new MCRServiceFlagDerivateDisplayFilter("ftp");

        MCRDerivate derivate = getDerivate();

        setServiceFlag(derivate, "ftp-foo", "true");
        setServiceFlag(derivate, "ftp-bar", "false");

        assertNull(filter.isDisplayEnabled(derivate, null));
        assertTrue(filter.isDisplayEnabled(derivate, "foo"));
        assertFalse(filter.isDisplayEnabled(derivate, "bar"));
        assertNull(filter.isDisplayEnabled(derivate, "baz"));

    }

    private MCRDerivate getDerivate() throws JDOMException, IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        SAXBuilder saxBuilder = new SAXBuilder();

        Document document = saxBuilder.build(classLoader.getResourceAsStream(TEST_DIRECTORY + "testMcrDerivate.xml"));
        MCRDerivate derivate = new MCRDerivate(document);
        return derivate;
    }

    @Test
    public void derivateType() throws Exception {

        MCRDerivateDisplayFilter filter = new MCRDerivateTypeDerivateDisplayFilter(Map.of(
            "foo", new Mapping(Map.of("content", true, "toc", false)),
            "bar", new Mapping(Map.of("content", false, "toc", true))));

        MCRDerivate derivate = getDerivate();

        setDerivateType(derivate, "content");

        assertNull(filter.isDisplayEnabled(derivate, null));
        assertTrue(filter.isDisplayEnabled(derivate, "foo"));
        assertFalse(filter.isDisplayEnabled(derivate, "bar"));
        assertNull(filter.isDisplayEnabled(derivate, "baz"));

        setDerivateType(derivate, "toc");

        assertNull(filter.isDisplayEnabled(derivate, null));
        assertFalse(filter.isDisplayEnabled(derivate, "foo"));
        assertTrue(filter.isDisplayEnabled(derivate, "bar"));
        assertNull(filter.isDisplayEnabled(derivate, "baz"));

    }

    @Test
    public void combinedFlag() throws Exception {

        MCRDerivateDisplayFilter filter = new MCRCombinedDerivateDisplayFilter(
            (_, intent) -> Objects.equals(intent, "foo") ? true : null,
            (_, intent) -> Objects.equals(intent, "bar") ? false : null);

        MCRDerivate derivate = getDerivate();

        assertNull(filter.isDisplayEnabled(derivate, null));
        assertTrue(filter.isDisplayEnabled(derivate, "foo"));
        assertFalse(filter.isDisplayEnabled(derivate, "bar"));
        assertNull(filter.isDisplayEnabled(derivate, "baz"));

    }

    private static void setDerivateType(MCRDerivate derivate, String value) {
        List<MCRMetaClassification> classifications = derivate.getDerivate().getClassifications();
        classifications.clear();
        classifications.add(new MCRMetaClassification("classification", 0, null, "derivate_types", value));
    }

    private static void setServiceFlag(MCRDerivate derivate, String type, String value) {
        MCRObjectService service = derivate.getService();
        service.removeFlags(type);
        service.addFlag(type, value);
    }

}
