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

package org.mycore.mods.merger;

import static org.junit.Assert.assertEquals;
import static org.mycore.common.MCRConstants.MODS_NAMESPACE;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.Test;
import org.mycore.common.MCRJPATestCase;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.resource.MCRResourceHelper;

import java.io.IOException;
import java.util.List;

public class MCRRedundantModsClassificationEventHandlerTest extends MCRJPATestCase {

    public static final String TEST_DIRECTORY = "MCRRedundantModsClassificationEventHandlerTest/";

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void setUp() throws Exception {
        super.setUp();

        MCRCategoryDAO categoryDao = MCRCategoryDAOFactory.obtainInstance();
        categoryDao.addCategory(null, MCRXMLTransformer.getCategory(loadXml("sdnb.xml")));
        categoryDao.addCategory(null, MCRXMLTransformer.getCategory(loadXml("sdnb2.xml")));
        categoryDao.addCategory(null, MCRXMLTransformer.getCategory(loadXml("sdnb3.xml")));
    }

    private Element loadMods(String fileName) throws Exception {

        MCRObject object = new MCRObject();
        Document modsDocument = loadXml(fileName);

        MCRMODSWrapper modsWrapper = new MCRMODSWrapper(object);
        modsWrapper.setMODS(modsDocument.getRootElement().detach());
        modsWrapper.setID("junit", 1);

        MCRRedundantModsClassificationEventHandler mergeEventHandler = new MCRRedundantModsClassificationEventHandler();
        mergeEventHandler.handleObjectCreated(null, object);

        LOGGER.info(new MCRJDOMContent(modsWrapper.getMODS()).asString());

        return modsWrapper.getMODS();

    }

    private static Document loadXml(String fileName) throws JDOMException, IOException {
        return new SAXBuilder().build(MCRResourceHelper.getResourceAsStream(TEST_DIRECTORY + fileName));
    }

    @Test
    public void redundantClassificationsWithSameAuthorityAndLabelAreRemoved() throws Exception {
        Element mods = loadMods("modsClassificationSameAuthoritySameLabel.xml");
        List<Element> classifications = mods.getChildren("classification", MODS_NAMESPACE);

        assertEquals(2, classifications.size());
        assertEquals("foo:x-1-1", getLabelFromAttributeAndCategoryIdFromTextValue(classifications.get(0)));
        assertEquals("foo:y", getLabelFromAttributeAndCategoryIdFromTextValue(classifications.get(1)));
    }

    @Test
    public void redundantClassificationsWithSameAuthorityAndDifferingLabelAreKept() throws Exception {
        Element mods = loadMods("modsClassificationSameAuthorityDifferingLabel.xml");
        List<Element> classifications = mods.getChildren("classification", MODS_NAMESPACE);

        assertEquals(4, classifications.size());
        assertEquals("foo:x", getLabelFromAttributeAndCategoryIdFromTextValue(classifications.get(0)));
        assertEquals("bar:x-1", getLabelFromAttributeAndCategoryIdFromTextValue(classifications.get(1)));
        assertEquals("baz:x-1-1", getLabelFromAttributeAndCategoryIdFromTextValue(classifications.get(2)));
        assertEquals("foo:y", getLabelFromAttributeAndCategoryIdFromTextValue(classifications.get(3)));
    }

    @Test
    public void redundantClassificationsWithDifferingAuthorityAreKept() throws Exception {
        Element mods = loadMods("modsClassificationDifferingAuthority.xml");
        List<Element> classifications = mods.getChildren("classification", MODS_NAMESPACE);

        assertEquals(4, classifications.size());
        assertEquals("foo:x", getLabelFromAttributeAndCategoryIdFromTextValue(classifications.get(0)));
        assertEquals("foo:x-1", getLabelFromAttributeAndCategoryIdFromTextValue(classifications.get(1)));
        assertEquals("foo:x-1-1", getLabelFromAttributeAndCategoryIdFromTextValue(classifications.get(2)));
        assertEquals("foo:y", getLabelFromAttributeAndCategoryIdFromTextValue(classifications.get(3)));
    }

    @Test
    public void redundantClassificationsInRelatedItemAreKept() throws Exception {
        Element mods = loadMods("modsClassificationsInRelatedItem.xml");
        Element relatedItem = mods.getChildren("relatedItem", MODS_NAMESPACE).getFirst();
        List<Element> classifications = relatedItem.getChildren("classification", MODS_NAMESPACE);

        assertEquals(4, classifications.size());
        assertEquals("foo:x", getLabelFromAttributeAndCategoryIdFromTextValue(classifications.get(0)));
        assertEquals("foo:x-1", getLabelFromAttributeAndCategoryIdFromTextValue(classifications.get(1)));
        assertEquals("foo:x-1-1", getLabelFromAttributeAndCategoryIdFromTextValue(classifications.get(2)));
        assertEquals("foo:y", getLabelFromAttributeAndCategoryIdFromTextValue(classifications.get(3)));
    }

    @Test
    public void redundantClassificationsInRelatedItemWithoutXlinkAreRemoved() throws Exception {
        Element mods = loadMods("modsClassificationsInRelatedItemNoXlink.xml");
        Element relatedItem = mods.getChildren("relatedItem", MODS_NAMESPACE).getFirst();
        List<Element> classifications = relatedItem.getChildren("classification", MODS_NAMESPACE);

        assertEquals(2, classifications.size());
        assertEquals("foo:x-1-1", getLabelFromAttributeAndCategoryIdFromTextValue(classifications.get(0)));
        assertEquals("foo:y", getLabelFromAttributeAndCategoryIdFromTextValue(classifications.get(1)));
    }

    @Test
    public void redundantClassificationsInModsAndRelatedItemNoInteraction() throws Exception {
        Element mods = loadMods("modsClassificationsInAndOutsideRelatedItem.xml");
        List<Element> classificationsOutside = mods.getChildren("classification", MODS_NAMESPACE);

        assertEquals(1, classificationsOutside.size());
        assertEquals("foo:x-1", getLabelFromAttributeAndCategoryIdFromTextValue(classificationsOutside.getFirst()));

        List<Element> classificationsInside = mods.getChild("relatedItem", MODS_NAMESPACE)
            .getChildren("classification", MODS_NAMESPACE);

        assertEquals(3, classificationsInside.size());
        assertEquals("foo:x-1-1", getLabelFromAttributeAndCategoryIdFromTextValue(classificationsInside.get(0)));
        assertEquals("foo:x-1-1-1", getLabelFromAttributeAndCategoryIdFromTextValue(classificationsInside.get(1)));
        assertEquals("foo:y", getLabelFromAttributeAndCategoryIdFromTextValue(classificationsInside.get(2)));
    }

    private String getLabelFromAttributeAndCategoryIdFromTextValue(Element element) {
        return element.getAttributeValue("displayLabel") + ":" + element.getText().trim();
    }

    private String getCategoryIdFromValueUri(Element element) {
        String uri = element.getAttribute("valueURI").getValue();
        return uri.substring(uri.indexOf('#') + 1);
    }

}
