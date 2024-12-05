package org.mycore.mods.merger;

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

import static org.junit.Assert.assertEquals;
import static org.mycore.common.MCRConstants.MODS_NAMESPACE;

public class MCRRedundantModsGenreEventHandlerTest extends MCRJPATestCase {

    public static final String TEST_DIRECTORY = "MCRRedundantModsGenreEventHandlerTest/";

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void setUp() throws Exception {
        super.setUp();

        MCRCategoryDAO categoryDao = MCRCategoryDAOFactory.getInstance();
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
        List<Element> classifications = mods.getChildren("genre", MODS_NAMESPACE);

        assertEquals(3, classifications.size());
        assertEquals("intern:y", getTypeFromAttributeAndCategoryIdFromValueUri(classifications.get(0)));
        assertEquals("intern:x-1-1", getTypeFromAttributeAndCategoryIdFromValueUri(classifications.get(1)));
        assertEquals("intern:x-1", getTypeFromAttributeAndCategoryIdFromValueUri(classifications.get(2)));


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
