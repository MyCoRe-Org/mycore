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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mycore.common.MCRConstants.MODS_NAMESPACE;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.resource.MCRResourceHelper;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
public class MCRRedundantModsGenreEventHandlerTest {

    public static final String TEST_DIRECTORY = "MCRRedundantModsGenreEventHandlerTest/";

    private static final Logger LOGGER = LogManager.getLogger();

    @BeforeEach
    public void setUp() throws Exception {
        MCRCategoryDAO categoryDao = MCRCategoryDAOFactory.obtainInstance();
        categoryDao.addCategory(null, MCRXMLTransformer.getCategory(loadXml("genre.xml")));
        categoryDao.addCategory(null, MCRXMLTransformer.getCategory(loadXml("genre2.xml")));
    }

    private Element loadMods(String fileName) throws Exception {
        MCRObject object = new MCRObject();
        Document modsDocument = loadXml(fileName);

        MCRMODSWrapper modsWrapper = new MCRMODSWrapper(object);
        modsWrapper.setMODS(modsDocument.getRootElement().detach());
        modsWrapper.setID("junit", 1);

        MCRRedundantModsGenreEventHandler mergeEventHandler = new MCRRedundantModsGenreEventHandler();
        mergeEventHandler.handleObjectCreated(null, object);

        LOGGER.info(new MCRJDOMContent(modsWrapper.getMODS()).asString());

        return modsWrapper.getMODS();
    }

    private static Document loadXml(String fileName) throws JDOMException, IOException {
        return new SAXBuilder().build(MCRResourceHelper.getResourceAsStream(TEST_DIRECTORY + fileName));
    }

    @Test
    public void redundantGenresAreRemovedAscending() throws Exception {
        Element mods = loadMods("modsGenresAscending.xml");
        List<Element> genres = mods.getChildren("genre", MODS_NAMESPACE);

        assertEquals(2, genres.size());
        assertEquals("x-1-1", getCategoryIdFromValueUri(genres.get(0)));
        assertEquals("y", getCategoryIdFromValueUri(genres.get(1)));
    }

    @Test
    public void redundantGenresAreRemovedDescending() throws Exception {
        Element mods = loadMods("modsGenresDescending.xml");
        List<Element> genres = mods.getChildren("genre", MODS_NAMESPACE);

        assertEquals(2, genres.size());
        assertEquals("y", getCategoryIdFromValueUri(genres.get(0)));
        assertEquals("x-1-1", getCategoryIdFromValueUri(genres.get(1)));
    }

    @Test
    public void redundantGenresInRelatedItemAreKept() throws Exception {
        Element mods = loadMods("modsGenresInRelatedItem.xml");
        Element relatedItem = mods.getChildren("relatedItem", MODS_NAMESPACE).getFirst();
        List<Element> genres = relatedItem.getChildren("genre", MODS_NAMESPACE);

        assertEquals(4, genres.size());
        assertEquals("x", getCategoryIdFromValueUri(genres.get(0)));
        assertEquals("x-1", getCategoryIdFromValueUri(genres.get(1)));
        assertEquals("x-1-1", getCategoryIdFromValueUri(genres.get(2)));
        assertEquals("y", getCategoryIdFromValueUri(genres.get(3)));
    }

    @Test
    public void redundantGenresInRelatedItemWithoutXlinkAreRemoved() throws Exception {
        Element mods = loadMods("modsGenresInRelatedItemNoXlink.xml");
        Element relatedItem = mods.getChildren("relatedItem", MODS_NAMESPACE).getFirst();
        List<Element> genres = relatedItem.getChildren("genre", MODS_NAMESPACE);

        assertEquals(2, genres.size());
        assertEquals("x-1-1", getCategoryIdFromValueUri(genres.get(0)));
        assertEquals("y", getCategoryIdFromValueUri(genres.get(1)));
    }

    @Test
    public void redundantGenresWithSameAuthorityAndDifferingLabelAreKept() throws Exception {
        Element mods = loadMods("modsGenreSameAuthorityDifferingLabel.xml");
        List<Element> genres = mods.getChildren("genre", MODS_NAMESPACE);

        assertEquals(4, genres.size());
        assertEquals("foo:x", getLabelFromAttributeAndCategoryIdFromValueUri(genres.get(0)));
        assertEquals("bar:x-1", getLabelFromAttributeAndCategoryIdFromValueUri(genres.get(1)));
        assertEquals("baz:x-1-1", getLabelFromAttributeAndCategoryIdFromValueUri(genres.get(2)));
        assertEquals("foo:y", getLabelFromAttributeAndCategoryIdFromValueUri(genres.get(3)));
    }

    @Test
    public void redundantGenresWithSameAuthorityAndDifferingTypeAreKept() throws Exception {
        Element mods = loadMods("modsGenreSameAuthorityDifferingType.xml");
        List<Element> genres = mods.getChildren("genre", MODS_NAMESPACE);

        assertEquals(4, genres.size());
        assertEquals("foo:x", getTypeFromAttributeAndCategoryIdFromValueUri(genres.get(0)));
        assertEquals("bar:x-1", getTypeFromAttributeAndCategoryIdFromValueUri(genres.get(1)));
        assertEquals("baz:x-1-1", getTypeFromAttributeAndCategoryIdFromValueUri(genres.get(2)));
        assertEquals("foo:y", getTypeFromAttributeAndCategoryIdFromValueUri(genres.get(3)));
    }

    @Test
    public void redundantGenresWithDifferingAuthorityAreKept() throws Exception {
        Element mods = loadMods("modsGenreDifferingAuthority.xml");
        List<Element> genres = mods.getChildren("genre", MODS_NAMESPACE);

        assertEquals(3, genres.size());
        assertEquals("intern:y", getTypeFromAttributeAndCategoryIdFromValueUri(genres.get(0)));
        assertEquals("intern:x-1-1", getTypeFromAttributeAndCategoryIdFromValueUri(genres.get(1)));
        assertEquals("intern:x-1", getTypeFromAttributeAndCategoryIdFromValueUri(genres.get(2)));
    }

    @Test
    public void redundantGenresInModsAndRelatedItemNoInteraction() throws Exception {
        Element mods = loadMods("modsGenresInAndOutsideRelatedItem.xml");
        List<Element> genresOutside = mods.getChildren("genre", MODS_NAMESPACE);


        assertEquals(1, genresOutside.size());
        assertEquals("intern:x-1", getTypeFromAttributeAndCategoryIdFromValueUri(genresOutside.getFirst()));

        List<Element> genresInside = mods.getChild("relatedItem", MODS_NAMESPACE)
            .getChildren("genre", MODS_NAMESPACE);

        assertEquals(2, genresInside.size());
        assertEquals("intern:x-1-1-1", getTypeFromAttributeAndCategoryIdFromValueUri(genresInside.get(0)));
        assertEquals("intern:y", getTypeFromAttributeAndCategoryIdFromValueUri(genresInside.get(1)));
    }

    @Test
    public void redundantGenresInNestedRelatedItem() throws Exception {
        Element mods = loadMods("modsGenresInNestedRelatedItem.xml");
        List<Element> genresInnerRelatedItem = mods.getChild("relatedItem", MODS_NAMESPACE)
            .getChild("relatedItem", MODS_NAMESPACE).getChildren("genre", MODS_NAMESPACE);

        assertEquals(2, genresInnerRelatedItem.size());
        assertEquals("intern:x-1-1", getTypeFromAttributeAndCategoryIdFromValueUri(genresInnerRelatedItem.get(0)));
        assertEquals("intern:y", getTypeFromAttributeAndCategoryIdFromValueUri(genresInnerRelatedItem.get(1)));
    }

    private String getCategoryIdFromValueUri(Element element) {
        String uri = element.getAttribute("valueURI").getValue();
        return uri.substring(uri.indexOf('#') + 1);
    }

    private String getLabelFromAttributeAndCategoryIdFromValueUri(Element element) {
        return element.getAttributeValue("displayLabel") + ":" + getCategoryIdFromValueUri(element);
    }

    private String getTypeFromAttributeAndCategoryIdFromValueUri(Element element) {
        return element.getAttributeValue("type") + ":" + getCategoryIdFromValueUri(element);
    }

}
