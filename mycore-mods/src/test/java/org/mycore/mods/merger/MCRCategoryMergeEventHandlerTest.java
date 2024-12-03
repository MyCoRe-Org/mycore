package org.mycore.mods.merger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mycore.common.MCRConstants.MODS_NAMESPACE;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.Test;
import org.mycore.common.MCRJPATestCase;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTransactionHelper;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.resource.MCRResourceHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MCRCategoryMergeEventHandlerTest extends MCRJPATestCase {

    public static final String TEST_DIRECTORY = "MCRCategoryMergeEventHandlerTest/";

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void setUp() throws Exception {
        super.setUp();

        MCRSessionMgr.getCurrentSession();
        MCRTransactionHelper.isTransactionActive();

        MCRCategoryDAO categoryDao = MCRCategoryDAOFactory.getInstance();
        categoryDao.addCategory(null, MCRXMLTransformer.getCategory(loadXml("genre.xml")));
        categoryDao.addCategory(null, MCRXMLTransformer.getCategory(loadXml("rfc5646.xml")));
        categoryDao.addCategory(null, MCRXMLTransformer.getCategory(loadXml("mir_licenses.xml")));
        categoryDao.addCategory(null, MCRXMLTransformer.getCategory(loadXml("typeOfResource.xml")));
        categoryDao.addCategory(null, MCRXMLTransformer.getCategory(loadXml("marcrelator.xml")));
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

        MCRCategoryMergeEventHandler mergeEventHandler = new MCRCategoryMergeEventHandler();
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
    public void redundantLanguageTermsInSameLanguageAreRemoved() throws Exception {

        Element mods = loadMods("modsLanguageTermsSameLanguage.xml");

        List<Element> languageTerms = new ArrayList<>();
        List<Element> languages = mods.getChildren("language", MODS_NAMESPACE);
        for (Element language : languages) {
            languageTerms.addAll(language.getChildren("languageTerm", MODS_NAMESPACE));
        }

        assertEquals(2, languageTerms.size());
        assertEquals("x-1-1", getCategoryIdFromTextValue(languageTerms.get(0)));
        assertEquals("y", getCategoryIdFromTextValue(languageTerms.get(1)));

    }

    @Test
    public void redundantLanguageTermsInDifferingLanguageAreRemoved() throws Exception {

        Element mods = loadMods("modsLanguageTermsDifferingLanguage.xml");

        List<Element> languageTerms = new ArrayList<>();
        List<Element> languages = mods.getChildren("language", MODS_NAMESPACE);
        for (Element element : languages) {
            languageTerms.addAll(element.getChildren("languageTerm", MODS_NAMESPACE));
        }

        assertEquals(2, languageTerms.size());
        assertEquals("x-1-1", getCategoryIdFromTextValue(languageTerms.get(0)));
        assertEquals("y", getCategoryIdFromTextValue(languageTerms.get(1)));

        for (Element language : languages) {
            assertFalse(language.getChildren().isEmpty());
        }

    }

    @Test
    public void redundantLanguageTermsInRelatedItemAreKept() throws Exception {

        Element mods = loadMods("modsLanguageTermsInRelatedItem.xml");
        Element relatedItem = mods.getChildren("relatedItem", MODS_NAMESPACE).getFirst();

        List<Element> languageTerms = new ArrayList<>();
        List<Element> languages = relatedItem.getChildren("language", MODS_NAMESPACE);
        for (Element element : languages) {
            languageTerms.addAll(element.getChildren("languageTerm", MODS_NAMESPACE));
        }

        assertEquals(4, languageTerms.size());
        assertEquals("x", getCategoryIdFromTextValue(languageTerms.get(0)));
        assertEquals("x-1", getCategoryIdFromTextValue(languageTerms.get(1)));
        assertEquals("x-1-1", getCategoryIdFromTextValue(languageTerms.get(2)));
        assertEquals("y", getCategoryIdFromTextValue(languageTerms.get(3)));

    }

    @Test
    public void redundantAccessConditionsWithSameTypeAreRemoved() throws Exception {

        Element mods = loadMods("modsAccessConditionsSameType.xml");

        List<Element> accessConditions = mods.getChildren("accessCondition", MODS_NAMESPACE);

        assertEquals(2, accessConditions.size());
        assertEquals("foo:x-1-1", getTypeFromAttributeAndCategoryIdFromTextValue(accessConditions.get(0)));
        assertEquals("foo:y", getTypeFromAttributeAndCategoryIdFromTextValue(accessConditions.get(1)));

    }

    @Test
    public void redundantAccessConditionsWithDifferingTypeAreKept() throws Exception {

        Element mods = loadMods("modsAccessConditionsDifferingType.xml");

        List<Element> accessConditions = mods.getChildren("accessCondition", MODS_NAMESPACE);

        assertEquals(4, accessConditions.size());
        assertEquals("foo:x", getTypeFromAttributeAndCategoryIdFromTextValue(accessConditions.get(0)));
        assertEquals("bar:x-1", getTypeFromAttributeAndCategoryIdFromTextValue(accessConditions.get(1)));
        assertEquals("baz:x-1-1", getTypeFromAttributeAndCategoryIdFromTextValue(accessConditions.get(2)));
        assertEquals("foo:y", getTypeFromAttributeAndCategoryIdFromTextValue(accessConditions.get(3)));

    }

    @Test
    public void redundantTypesOfResourceAreRemoved() throws Exception {

        Element mods = loadMods("modsTypesOfResource.xml");

        List<Element> typesOfResource = mods.getChildren("typeOfResource", MODS_NAMESPACE);

        assertEquals(2, typesOfResource.size());
        assertEquals("x-1-1", getCategoryIdFromTextValue(typesOfResource.get(0)));
        assertEquals("y", getCategoryIdFromTextValue(typesOfResource.get(1)));

    }

    @Test
    public void redundantRoleTermsInSameNameSameRoleAreRemoved() throws Exception {

        Element mods = loadMods("modsRoleTermsSameNameSameRole.xml");
        Element name = mods.getChildren("name", MODS_NAMESPACE).getFirst();

        List<Element> roleTerms = new ArrayList<>();
        List<Element> roles = name.getChildren("role", MODS_NAMESPACE);
        for (Element role : roles) {
            roleTerms.addAll(role.getChildren("roleTerm", MODS_NAMESPACE));
        }

        assertEquals(2, roleTerms.size());
        assertEquals("foo:x-1-1", getDisplayFormFromAttributeAndCategoryIdFromTextValue(roleTerms.get(0)));
        assertEquals("foo:y", getDisplayFormFromAttributeAndCategoryIdFromTextValue(roleTerms.get(1)));

    }

    @Test
    public void redundantRoleTermsInSameNameDifferingRoleAreRemoved() throws Exception {

        Element mods = loadMods("modsRoleTermsSameNameDifferingRole.xml");
        Element name = mods.getChildren("name", MODS_NAMESPACE).getFirst();

        List<Element> roleTerms = new ArrayList<>();
        List<Element> roles = name.getChildren("role", MODS_NAMESPACE);
        for (Element role : roles) {
            roleTerms.addAll(role.getChildren("roleTerm", MODS_NAMESPACE));
        }

        assertEquals(2, roleTerms.size());
        assertEquals("foo:x-1-1", getDisplayFormFromAttributeAndCategoryIdFromTextValue(roleTerms.get(0)));
        assertEquals("foo:y", getDisplayFormFromAttributeAndCategoryIdFromTextValue(roleTerms.get(1)));

        for (Element role : roles) {
            assertFalse(role.getChildren().isEmpty());
        }

    }

    @Test
    public void redundantRoleTermsInDifferingNameAreKept() throws Exception {

        Element mods = loadMods("modsRoleTermDifferingName.xml");
        List<Element> names = mods.getChildren("name", MODS_NAMESPACE);

        List<Element> roleTerms = new ArrayList<>();
        for (Element name : names) {
            List<Element> roles = name.getChildren("role", MODS_NAMESPACE);
            for (Element role : roles) {
                roleTerms.addAll(role.getChildren("roleTerm", MODS_NAMESPACE));
            }
        }

        assertEquals(4, roleTerms.size());
        assertEquals("foo:x", getDisplayFormFromAttributeAndCategoryIdFromTextValue(roleTerms.get(0)));
        assertEquals("bar:x-1", getDisplayFormFromAttributeAndCategoryIdFromTextValue(roleTerms.get(1)));
        assertEquals("baz:x-1-1", getDisplayFormFromAttributeAndCategoryIdFromTextValue(roleTerms.get(2)));
        assertEquals("foo:y", getDisplayFormFromAttributeAndCategoryIdFromTextValue(roleTerms.get(3)));

    }

    @Test
    public void redundantClassificationsWithSameAuthorityAndLabelAreRemoved() throws Exception {

        Element mods = loadMods("modsClassificationSameAuthoritySameLabel.xml");

        List<Element> accessConditions = mods.getChildren("classification", MODS_NAMESPACE);

        assertEquals(2, accessConditions.size());
        assertEquals("foo:x-1-1", getLabelFromAttributeAndCategoryIdFromTextValue(accessConditions.get(0)));
        assertEquals("foo:y", getLabelFromAttributeAndCategoryIdFromTextValue(accessConditions.get(1)));

    }

    @Test
    public void redundantClassificationsWithSameAuthorityAndDifferingLabelAreKept() throws Exception {

        Element mods = loadMods("modsClassificationSameAuthorityDifferingLabel.xml");

        List<Element> accessConditions = mods.getChildren("classification", MODS_NAMESPACE);

        assertEquals(4, accessConditions.size());
        assertEquals("foo:x", getLabelFromAttributeAndCategoryIdFromTextValue(accessConditions.get(0)));
        assertEquals("bar:x-1", getLabelFromAttributeAndCategoryIdFromTextValue(accessConditions.get(1)));
        assertEquals("baz:x-1-1", getLabelFromAttributeAndCategoryIdFromTextValue(accessConditions.get(2)));
        assertEquals("foo:y", getLabelFromAttributeAndCategoryIdFromTextValue(accessConditions.get(3)));

    }

    @Test
    public void redundantClassificationsWithDifferingAuthorityAreKept() throws Exception {

        Element mods = loadMods("modsClassificationDifferingAuthority.xml");

        List<Element> accessConditions = mods.getChildren("classification", MODS_NAMESPACE);

        assertEquals(4, accessConditions.size());
        assertEquals("foo:x", getLabelFromAttributeAndCategoryIdFromTextValue(accessConditions.get(0)));
        assertEquals("foo:x-1", getLabelFromAttributeAndCategoryIdFromTextValue(accessConditions.get(1)));
        assertEquals("foo:x-1-1", getLabelFromAttributeAndCategoryIdFromTextValue(accessConditions.get(2)));
        assertEquals("foo:y", getLabelFromAttributeAndCategoryIdFromTextValue(accessConditions.get(3)));

    }


    private String getCategoryIdFromValueUri(Element element) {
        String uri = element.getAttribute("valueURI").getValue();
        return uri.substring(uri.indexOf('#') + 1);
    }

    private String getCategoryIdFromTextValue(Element element) {
        return element.getText().trim();
    }

    private String getTypeFromAttributeAndCategoryIdFromTextValue(Element element) {
        return element.getAttributeValue("type") + ":" + element.getText().trim();
    }

    private String getDisplayFormFromAttributeAndCategoryIdFromTextValue(Element element) {
        Element name = element.getParentElement().getParentElement();
        Element displayForm = name.getChildren("displayForm", MODS_NAMESPACE).getFirst();
        return displayForm.getText() + ":" + element.getText().trim();
    }

    private String getLabelFromAttributeAndCategoryIdFromTextValue(Element element) {
        return element.getAttributeValue("displayLabel") + ":" + element.getText().trim();
    }

}
