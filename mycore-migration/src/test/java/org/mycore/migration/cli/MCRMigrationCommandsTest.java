package org.mycore.migration.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mycore.migration.cli.MCRMigrationCommands.CHILDREN_ORDER_STRATEGY_PROPERTY;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectStructure;
import org.mycore.migration.strategy.MCRAlwaysAddChildrenOrderStrategy;
import org.mycore.mods.classification.mapping.MCRMODSGeneratorClassificationMapper;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MCRMetadataExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
@ExtendWith(MCRMetadataExtension.class)
public class MCRMigrationCommandsTest {

    public static final String TEST_FILE_DIRECTORY = "MCRMigrationCommandsTest/";
    public static final String TEST_FILE_1 = "mir_mods_00000001.xml";
    public static final String TEST_FILE_2 = "mir_mods_00000002.xml";
    public static final String TEST_FILE_3 = "mir_mods_00000003.xml";
    public static final String TEST_FILE_4 = "mir_mods_00000004.xml";
    public static final String TEST_FILE_5 = "mir_mods_00000005.xml";
    public static final String TEST_FILE_6 = "mir_mods_00000006.xml";
    public static final String TEST_FILE_7 = "mir_mods_00000007.xml";
    public static final String TEST_FILE_8 = "mir_mods_00000008.xml";
    public static final String TEST_FILE_9 = "mir_mods_00000009.xml";
    public static final String TEST_FILE_10 = "mir_mods_00000010.xml";
    public static final String TEST_FILE_11 = "mir_mods_00000011.xml";
    public static final String TEST_FILE_12 = "mir_mods_00000012.xml";
    public static final String TEST_FILE_13 = "mir_mods_00000013.xml";
    public static final String TEST_DERIVATE_FILE = "mir_derivate_00000011.xml";

    private static void verifyNoChildrenElementLeft(Document migratedObject1) {
        // check if there are no children elements anymore in structure
        assertNull(migratedObject1.getRootElement()
            .getChild(MCRObjectStructure.XML_NAME)
            .getChild("children"), "There should be no children elements anymore");
    }

    private static void checkChildrenOrder(Document migratedObject1, MCRObjectID testID5, MCRObjectID testID2,
        MCRObjectID testID4) {
        List<String> hrefs = migratedObject1.getRootElement()
            .getChild(MCRObjectStructure.XML_NAME)
            .getChild(MCRObjectStructure.CHILDREN_ORDER_ELEMENT_NAME)
            .getChildren(MCRObjectStructure.CHILD_ELEMENT_NAME)
            .stream()
            .map(el -> el.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE))
            .toList();

        assertEquals(3, hrefs.size(), "The should be 3 children");
        assertEquals(testID5.toString(), hrefs.get(0), "The first child should be " + testID5);
        assertEquals(testID2.toString(), hrefs.get(1), "The second child should be " + testID2);
        assertEquals(testID4.toString(), hrefs.get(2), "The third child should be " + testID4);
    }

    private static void checkNoGeneratedClassifications(Document migratedObject1) {
        List<Element> list = XPathFactory.instance()
            .compile(".//mods:classification[contains(@generator, '"
                + MCRMODSGeneratorClassificationMapper.GENERATOR_SUFFIX +
                "')]", Filters.element(), null, MCRConstants.MODS_NAMESPACE, MCRConstants.XLINK_NAMESPACE)
            .evaluate(migratedObject1);
        assertEquals(0, list.size(), "There should be no generated classifications anymore!");
    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "MCR.Metadata.Type.mods", string = "true"),
            @MCRTestProperty(key = CHILDREN_ORDER_STRATEGY_PROPERTY,
                classNameOf = MCRAlwaysAddChildrenOrderStrategy.class),
        })
    public void migrateChildrenOrder() throws IOException, JDOMException {
        MCRObjectID testID1 = createTestData(TEST_FILE_DIRECTORY + TEST_FILE_1);
        MCRObjectID testID2 = createTestData(TEST_FILE_DIRECTORY + TEST_FILE_2);
        MCRObjectID testID3 = createTestData(TEST_FILE_DIRECTORY + TEST_FILE_3);
        MCRObjectID testID4 = createTestData(TEST_FILE_DIRECTORY + TEST_FILE_4);
        MCRObjectID testID5 = createTestData(TEST_FILE_DIRECTORY + TEST_FILE_5);
        MCRObjectID testID6 = createTestData(TEST_FILE_DIRECTORY + TEST_FILE_6);
        MCRObjectID testID7 = createTestData(TEST_FILE_DIRECTORY + TEST_FILE_7);
        createTestData(TEST_FILE_DIRECTORY + TEST_FILE_8);
        createTestData(TEST_FILE_DIRECTORY + TEST_FILE_9);
        createTestData(TEST_FILE_DIRECTORY + TEST_FILE_10);
        createTestData(TEST_FILE_DIRECTORY + TEST_FILE_11);
        createTestData(TEST_FILE_DIRECTORY + TEST_FILE_12);
        createTestData(TEST_FILE_DIRECTORY + TEST_FILE_13);
        createTestData(TEST_FILE_DIRECTORY + TEST_DERIVATE_FILE);

        MCRMigrationCommands.migrateNormalizedObject(testID1.toString());

        Document migratedObject1 = MCRXMLMetadataManager.obtainInstance().retrieveXML(testID1);
        checkChildrenOrder(migratedObject1, testID5, testID2, testID4);
        verifyNoChildrenElementLeft(migratedObject1);
        checkNoGeneratedClassifications(migratedObject1);

        // check if the derivates will be removed
        assertNull(migratedObject1.getRootElement()
            .getChild(MCRObjectStructure.XML_NAME)
            .getChild(MCRObjectStructure.ELEMENT_DERIVATE_OBJECTS), "The derobjects should be removed");

        MCRMigrationCommands.migrateNormalizedObject(testID2.toString());
        Document migratedObject2 = MCRXMLMetadataManager.obtainInstance().retrieveXML(testID2);
        checkChildrenOrder(migratedObject2, testID7, testID3, testID6);
        verifyNoChildrenElementLeft(migratedObject2);
        checkNoGeneratedClassifications(migratedObject2);
        checkRelatedItemIsEmpty(migratedObject2);

        MCRMigrationCommands.migrateNormalizedObject(testID7.toString());
        Document migratedObject7 = MCRXMLMetadataManager.obtainInstance().retrieveXML(testID7);

        checkNoGeneratedClassifications(migratedObject7);
        checkRelatedItemIsEmpty(migratedObject7);
    }

    private static void checkRelatedItemIsEmpty(Document migratedObject1) {
        List<Element> relatedItems = XPathFactory.instance()
            .compile(".//mods:relatedItem", Filters.element(), null,
                MCRConstants.MODS_NAMESPACE, MCRConstants.XLINK_NAMESPACE)
            .evaluate(migratedObject1);
        assertEquals(1, relatedItems.size(), "There should be one relatedItem");

        assertEquals(0, relatedItems.get(0).getChildren().size(), "It should have no children");
    }

    private MCRObjectID createTestData(String file) throws IOException, JDOMException {
        MCRObjectID objectID;
        try (InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(file)) {
            SAXBuilder builder = new SAXBuilder();
            Document build = builder.build(resourceAsStream);
            String id = build.getRootElement().getAttributeValue("ID");
            objectID = MCRObjectID.getInstance(id);
            MCRXMLMetadataManager.obtainInstance().create(objectID, build, new Date());
        }
        return objectID;
    }
}
