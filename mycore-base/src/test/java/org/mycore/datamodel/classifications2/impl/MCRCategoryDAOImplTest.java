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
package org.mycore.datamodel.classifications2.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mycore.test.MCRJPATestHelper.endTransaction;
import static org.mycore.test.MCRJPATestHelper.printTable;
import static org.mycore.test.MCRJPATestHelper.startNewTransaction;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRException;
import org.mycore.common.MCRStreamUtils;
import org.mycore.common.content.MCRURLContent;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.datamodel.classifications2.utils.MCRCategoryTransformer;
import org.mycore.datamodel.classifications2.utils.MCRStringTransformer;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;
import org.xml.sax.SAXParseException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
public class MCRCategoryDAOImplTest {

    static final String WORLD_CLASS_RESOURCE_NAME = "/worldclass.xml";

    private static final String WORLD_CLASS2_RESOURCE_NAME = "/worldclass2.xml";

    static final String CATEGORY_MAPPING_RESOURCE_NAME =
        "/org/mycore/datamodel/classifications2/impl/MCRCategoryImpl.hbm.xml";

    static final MCRCategoryDAOImpl DAO = new MCRCategoryDAOImpl();

    private MCRCategory category;
    private MCRCategory category2;

    @BeforeEach
    public void setUp() throws Exception {
        loadWorldClassification();
    }

    @AfterEach
    public void tearDown() throws Exception {
        startNewTransaction();
        MCRCategoryImpl rootNode = getRootCategoryFromSession();
        MCRCategoryImpl rootNode2 = (MCRCategoryImpl) DAO.getCategory(category.getId(), -1);
        if (rootNode != null) {
            try {
                checkLeftRightLevelValue(rootNode, 0, 0);
                checkLeftRightLevelValue(rootNode2, 0, 0);
            } catch (AssertionError e) {
                LogManager.getLogger().error("Error while checking left, right an level values in database.");
                new XMLOutputter(Format.getPrettyFormat())
                    .output(MCRCategoryTransformer.getMetaDataDocument(rootNode, false), System.out);
                printTable("MCRCategory");
                throw e;
            }
        }
    }

    @Test
    public void testLicenses() throws Exception {
        MCRCategory licenses = loadClassificationResource("/mycore-classifications/mir_licenses.xml");
        DAO.addCategory(null, licenses);
        MCRCategoryID cc_30 = new MCRCategoryID(licenses.getId().getRootID(), "cc_3.0");
        DAO.deleteCategory(cc_30);
        startNewTransaction();
    }

    @Test
    public void testClassEditorBatch() throws Exception {
        EntityManager entityManager = MCREntityManagerProvider.getCurrentEntityManager();
        MCRCategory nameIdentifier = loadClassificationResource("/mycore-classifications/nameIdentifier.xml");
        MCRCategory secondCateg = nameIdentifier.getChildren().get(1);
        DAO.addCategory(null, nameIdentifier);
        startNewTransaction();
        MCRCategLinkServiceFactory.obtainInstance().getLinksFromCategory(secondCateg.getId());
        assertTrue(DAO.exist(secondCateg.getId()), secondCateg.getId() + " should exist.");
        //re-set labels
        DAO.setLabels(secondCateg.getId(), new TreeSet<>(secondCateg.getLabels().stream().collect(Collectors.toSet())));
        //re-set URI
        entityManager.detach(DAO.setURI(secondCateg.getId(), secondCateg.getURI()));
        //move to new index
        DAO.moveCategory(secondCateg.getId(), secondCateg.getParent().getId(), 0);
        startNewTransaction();
        MCRCategoryImpl copyOfDB = MCRCategoryDAOImpl.getByNaturalID(entityManager, secondCateg.getId());
        assertNotNull(copyOfDB.getParent(), secondCateg.getId() + " must hav a parent.");
    }

    @Test
    public void addCategory() throws MCRException {
        addWorldClassification();
        assertTrue(DAO.exist(category.getId()), "Exist check failed for Category " + category.getId());
        MCRCategoryImpl india = new MCRCategoryImpl();
        india.setId(new MCRCategoryID(category.getId().getRootID(), "India"));
        india.setLabels(new TreeSet<>());
        india.getLabels().add(new MCRLabel("de", "Indien", null));
        india.getLabels().add(new MCRLabel("en", "India", null));
        DAO.addCategory(new MCRCategoryID(category.getId().getRootID(), "Asia"), india);
        startNewTransaction();
        assertTrue(DAO.exist(india.getId()), "Exist check failed for Category " + india.getId());
        MCRCategoryImpl rootCategory = getRootCategoryFromSession();
        assertEquals(category.getChildren().size(), rootCategory.getChildren().size(),
            "Child category count does not match.");
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Number> countQuery = cb.createQuery(Number.class);
        long allNodes = em
            .createQuery(countQuery
                .select(cb
                    .count(countQuery
                        .from(MCRCategoryImpl.class))))
            .getSingleResult()
            .longValue();
        // category + india
        long expected = countNodes(category) + 1;
        assertEquals(expected, allNodes, "Complete category count does not match.");
        assertNotNull(rootCategory.getRoot(), "No root category present");
    }

    /**
     * Test case for https://sourceforge.net/p/mycore/bugs/612/
     */
    @Test
    public void addCategorySingleSteps() {
        MCRCategory root = new MCRCategoryImpl();
        MCRCategoryID rootID = new MCRCategoryID("junit");
        root.setId(rootID);
        DAO.addCategory(null, root);
        startNewTransaction();
        MCRCategory a = new MCRCategoryImpl();
        MCRCategoryID aId = new MCRCategoryID(rootID.getRootID(), "a");
        a.setId(aId);
        MCRCategory b = new MCRCategoryImpl();
        MCRCategoryID bId = new MCRCategoryID(rootID.getRootID(), "b");
        b.setId(bId);
        MCRCategory c = new MCRCategoryImpl();
        MCRCategoryID cId = new MCRCategoryID(rootID.getRootID(), "c");
        c.setId(cId);
        DAO.addCategory(rootID, a);
        DAO.addCategory(rootID, c);
        DAO.addCategory(aId, b);
        startNewTransaction();
        assertTrue(DAO.getChildren(cId).isEmpty(), "Category c should not contain child categories.");
        assertFalse(DAO.getChildren(aId).isEmpty(), "Category a should contain child categories.");
        root = DAO.getCategory(rootID, -1);
        checkLeftRightLevelValue((MCRCategoryImpl) root, 0, 0);
        assertEquals(4, countNodes(root), "Did not get all categories");
        List<MCRCategory> children = root.getChildren();
        assertEquals(2, children.size(), "Children count mismatch");
        a = children.get(0);
        c = children.get(1);
        assertEquals(aId, a.getId(), "Wrong order of children");
        assertEquals(cId, c.getId(), "Wrong order of children");
        assertTrue(c.getChildren().isEmpty(), "Category c should not contain child categories.");
        assertFalse(a.getChildren().isEmpty(), "Category a should contain child categories.");
    }

    /**
     * Test case for https://sourceforge.net/p/mycore/bugs/664/
     */
    @Test
    public void addCategoryToPosition() {
        addWorldClassification();
        MCRCategoryImpl america = new MCRCategoryImpl();
        MCRCategoryID categoryID = new MCRCategoryID(category.getId().getRootID(), "America");
        america.setId(categoryID);
        america.setLabels(new TreeSet<>());
        america.getLabels().add(new MCRLabel("de", "Amerika", null));
        america.getLabels().add(new MCRLabel("en", "America", null));
        DAO.addCategory(category.getId(), america, 1);
        startNewTransaction();
        america =
            MCRCategoryDAOImpl.getByNaturalID(MCREntityManagerProvider.getCurrentEntityManager(), america.getId());
        assertNotNull(america, categoryID + " was not added to database.");
        assertEquals(1, america.getPositionInParent(), "invalid position in parent");
    }

    @Test
    public void deleteRootCategory() {
        addWorldClassification();
        testDelete(category);
    }

    @Test
    public void deleteSubCategory() {
        addWorldClassification();
        MCRCategory deleteCategory = category.getChildren().getFirst();
        assertTrue(DAO.exist(deleteCategory.getId()), "Sub category does not exist.");
        testDelete(deleteCategory);
    }

    @Test
    public void deleteMultipleCategories() {
        //check for MCR-1863
        addWorldClassification();
        List<MCRCategoryID> europeChildrenIds = category.getChildren()
            .getFirst()
            .getChildren()
            .stream()
            .map(MCRCategory::getId)
            .collect(Collectors.toList());
        DAO.deleteCategory(europeChildrenIds.get(0));
        DAO.deleteCategory(europeChildrenIds.get(1));
        DAO.deleteCategory(europeChildrenIds.get(2));
    }

    private void testDelete(MCRCategory deleteCategory) {
        DAO.deleteCategory(deleteCategory.getId());
        startNewTransaction();
        // check if classification is present
        assertFalse(DAO.exist(deleteCategory.getId()), "Category is not deleted: " + deleteCategory.getId());
        // check if any subcategory is present
        assertFalse(DAO.exist(deleteCategory.getChildren().getFirst().getId()),
            "Category is not deleted: " + deleteCategory.getChildren().getFirst().getId());
    }

    @Test
    public void getCategoriesByLabel() {
        addWorldClassification();
        MCRCategory find = category.getChildren().get(0).getChildren().getFirst();
        MCRCategory dontFind = category.getChildren().get(1);
        MCRLabel label = find.getLabels().getFirst();
        List<MCRCategory> results = DAO.getCategoriesByLabel(category.getId(), label.getLang(), label.getText());
        assertFalse(results.isEmpty(), "No search results found");
        assertTrue(results.getFirst().getLabels().contains(label), "Could not find Category: " + find.getId());
        assertTrue(DAO.getCategoriesByLabel(dontFind.getId(), label.getLang(), label.getText()).isEmpty(),
            "No search result expected.");
        results = DAO.getCategoriesByLabel(label.getLang(), label.getText());
        assertFalse(results.isEmpty(), "No search results found");
        assertTrue(results.getFirst().getLabels().contains(label), "Could not find Category: " + find.getId());
    }

    @Test
    public void getCategory() {
        addWorldClassification();
        MCRCategory rootCategory = DAO.getCategory(category.getId(), 0);
        assertTrue(rootCategory.getChildren().isEmpty(), "Children present with child Level 0.");
        rootCategory = DAO.getCategory(category.getId(), 1);
        MCRCategory origSubCategory = rootCategory.getChildren().getFirst();
        assertTrue(origSubCategory.getChildren().isEmpty(), "Children present with child Level 1.");
        assertEquals(category.getChildren().size(), rootCategory.getChildren().size(),
            "Category count does not match with child Level 1.\n" + MCRStringTransformer.getString(rootCategory));
        assertEquals(1, origSubCategory.getLevel(),
            "Children of Level 1 do not know that they are at the first level.\n"
                + MCRStringTransformer.getString(rootCategory));
        MCRCategory europe = DAO.getCategory(category.getChildren().getFirst().getId(), -1);
        assertFalse(europe.getChildren().isEmpty(), "No children present in " + europe.getId());
        europe = DAO.getCategory(category.getChildren().getFirst().getId(), 1);
        assertFalse(europe.getChildren().isEmpty(), "No children present in " + europe.getId());
        rootCategory = DAO.getCategory(category.getId(), -1);
        String msg = "Did not get all categories." + MCRStringTransformer.getString(rootCategory);
        assertEquals(countNodes(category), countNodes(rootCategory), msg);
        assertEquals(category.getChildren().size(), rootCategory.getChildren().size(),
            "Children of Level 1 do not match");
        MCRCategory subCategory = DAO.getCategory(origSubCategory.getId(), 0);
        assertNotNull(subCategory, "Did not return ");
        assertEquals(origSubCategory.getId(), subCategory.getId(), "ObjectIDs did not match");
        assertNotNull(subCategory.getRoot(), "Root category may not null.");
        assertEquals(origSubCategory.getRoot().getId(), subCategory.getRoot().getId(), "Root does not match");
        MCRCategory germanyResource = find(category, "Germany").get();
        MCRCategory germanyDB = DAO.getCategory(germanyResource.getId(), 1);
        printTable("MCRCategory");
        assertEquals(germanyResource.getChildren().size(), germanyDB.getChildren().size(),
            "Children of Level 1 do not match");
    }

    @Test
    public void getChildren() {
        addWorldClassification();
        List<MCRCategory> children = DAO.getChildren(category.getId());
        assertEquals(category.getChildren().size(), children.size(),
            "Did not get all children of :" + category.getId());
        for (int i = 0; i < children.size(); i++) {
            assertEquals(category.getChildren().get(i).getId(), children.get(i).getId(),
                "Category IDs of children do not match.");
        }
    }

    @Test
    public void getParents() {
        addWorldClassification();
        MCRCategory find = category.getChildren().getFirst().getChildren().getFirst();
        List<MCRCategory> parents = DAO.getParents(find.getId());
        MCRCategory findParent = find;
        for (MCRCategory parent : parents) {
            findParent = findParent.getParent();
            assertNotNull(findParent, "Returned too much parents.");
            assertEquals(findParent.getId(), parent.getId(), "Parents did not match.");
        }
    }

    @Test
    public void getRootCategoryIDs() {
        addWorldClassification();
        MCRCategoryID find = category.getId();
        List<MCRCategoryID> classIds = DAO.getRootCategoryIDs();
        assertEquals(1, classIds.size(), "Result size does not match.");
        assertEquals(find, classIds.getFirst(), "Returned MCRCategoryID does not match.");
    }

    @Test
    public void getRootCategories() {
        addWorldClassification();
        MCRCategoryID find = category.getId();
        List<MCRCategory> classes = DAO.getRootCategories();
        assertEquals(1, classes.size(), "Result size does not match.");
        assertEquals(find, classes.getFirst().getId(), "Returned MCRCategoryID does not match.");
    }

    @Test
    public void getRootCategory() {
        addWorldClassification();
        // Europe
        MCRCategory find = category.getChildren().getFirst();
        MCRCategory rootCategory = DAO.getRootCategory(find.getId(), 0);
        assertEquals(2, countNodes(rootCategory), "Category count does not match.");
        assertEquals(find.getRoot().getId(), rootCategory.getId(), "Did not get root Category.");
        rootCategory = DAO.getRootCategory(find.getId(), -1);
        assertEquals(1 + countNodes(find), countNodes(rootCategory), "Category count does not match.");
    }

    @Test
    public void children() {
        addWorldClassification();
        assertTrue(DAO.hasChildren(category.getId()), "Category '" + category.getId() + "' should have children.");
        assertFalse(DAO.hasChildren(category.getChildren().get(1).getId()),
            "Category '" + category.getChildren().get(1).getId() + "' shouldn't have children.");
    }

    @Test
    public void moveCategoryWithoutIndex() {
        addWorldClassification();
        checkLeftRightLevelValue(getRootCategoryFromSession(), 0, 0);
        startNewTransaction();
        MCRCategory moveNode = category.getChildren().get(1);
        // Europe conquer Asia
        DAO.moveCategory(moveNode.getId(), category.getChildren().get(0).getId());
        startNewTransaction();
        MCRCategoryImpl rootNode = getRootCategoryFromSession();
        checkLeftRightLevelValue(rootNode, 0, 0);
    }

    @Test
    public void moveCategoryInParent() {
        addWorldClassification();
        MCRCategory moveNode = category.getChildren().get(1);
        DAO.moveCategory(moveNode.getId(), moveNode.getParent().getId(), 0);
        startNewTransaction();
        MCRCategoryImpl rootNode = getRootCategoryFromSession();
        checkLeftRightLevelValue(rootNode, 0, 0);
        MCRCategory movedNode = rootNode.getChildren().getFirst();
        assertEquals(moveNode.getId(), movedNode.getId(), "Did not expect this category on position 0.");
    }

    @Test
    public void moveRightCategory() {
        String rootIDStr = "rootID";
        MCRCategoryID rootID = new MCRCategoryID(rootIDStr);
        MCRCategoryID child1ID = new MCRCategoryID(rootIDStr, "child1");
        MCRCategoryID child2ID = new MCRCategoryID(rootIDStr, "child2");
        MCRCategoryID child3ID = new MCRCategoryID(rootIDStr, "child3");
        MCRCategoryImpl root = newCategory(rootID, "root node");
        addChild(root, newCategory(child1ID, "child node 1"));
        addChild(root, newCategory(child2ID, "child node 2"));
        addChild(root, newCategory(child3ID, "child node 3"));
        startNewTransaction();
        DAO.addCategory(null, root);
        endTransaction();
        startNewTransaction();
        printTable("MCRCategory");
        endTransaction();
        assertLeftRightVal(rootID, 0, 7);
        assertLeftRightVal(child1ID, 1, 2);
        assertLeftRightVal(child2ID, 3, 4);
        assertLeftRightVal(child3ID, 5, 6);

        startNewTransaction();
        DAO.moveCategory(child2ID, child1ID, 0);
        DAO.moveCategory(child3ID, child1ID, 1);
        endTransaction();
        startNewTransaction();
        printTable("MCRCategory");
        endTransaction();
        assertLeftRightVal(rootID, 0, 7);
        assertLeftRightVal(child1ID, 1, 6);
        assertLeftRightVal(child2ID, 2, 3);
        assertLeftRightVal(child3ID, 4, 5);
    }

    @Test
    public void moveCategoryUp() {
        String rootIDStr = "rootID";
        MCRCategoryID rootID = new MCRCategoryID(rootIDStr);
        MCRCategoryID child1ID = new MCRCategoryID(rootIDStr, "child1");
        MCRCategoryID child2ID = new MCRCategoryID(rootIDStr, "child2");
        MCRCategoryImpl root = newCategory(rootID, "root node");
        MCRCategoryImpl child1 = newCategory(child1ID, "child node 1");
        addChild(root, child1);
        addChild(child1, newCategory(child2ID, "child node 2"));

        startNewTransaction();
        DAO.addCategory(null, root);
        endTransaction();
        assertLeftRightVal(rootID, 0, 5);
        assertLeftRightVal(child1ID, 1, 4);
        assertLeftRightVal(child2ID, 2, 3);

        startNewTransaction();
        DAO.moveCategory(child2ID, rootID);
        endTransaction();
        startNewTransaction();
        printTable("MCRCategory");
        endTransaction();
        assertLeftRightVal(rootID, 0, 5);
        assertLeftRightVal(child1ID, 1, 2);
        assertLeftRightVal(child2ID, 3, 4);
    }

    @Test
    public void moveCategoryDeep() {
        String rootIDStr = "rootID";
        MCRCategoryID rootID = new MCRCategoryID(rootIDStr);
        MCRCategoryID child1ID = new MCRCategoryID(rootIDStr, "child1");
        MCRCategoryID child2ID = new MCRCategoryID(rootIDStr, "child2");
        MCRCategoryID child3ID = new MCRCategoryID(rootIDStr, "child3");
        MCRCategoryID child4ID = new MCRCategoryID(rootIDStr, "child4");

        MCRCategoryImpl root = newCategory(rootID, "root node");
        MCRCategoryImpl a = newCategory(child1ID, "child node 1");
        MCRCategoryImpl aa = newCategory(child2ID, "child node 2");
        MCRCategoryImpl aaa = newCategory(child3ID, "child node 3");
        MCRCategoryImpl aab = newCategory(child4ID, "child node 4");

        addChild(root, a);
        addChild(a, aa);
        addChild(aa, aaa);
        addChild(aa, aab);

        startNewTransaction();
        DAO.addCategory(null, root);
        endTransaction();

        startNewTransaction();
        DAO.moveCategory(child4ID, child3ID);
        endTransaction();
    }

    private void assertLeftRightVal(MCRCategoryID categID, int expectedLeftVal, int expectedRightVal) {
        startNewTransaction();
        MCRCategoryImpl retrievedRoot = (MCRCategoryImpl) DAO.getCategory(categID, 0);
        endTransaction();
        assertNotNull(retrievedRoot);
        assertEquals(expectedLeftVal, retrievedRoot.getLeft(), "Left value should be " + expectedLeftVal + ".");
        assertEquals(expectedRightVal, retrievedRoot.getRight(), "Right value should be " + expectedRightVal + ".");
    }

    private void addChild(MCRCategoryImpl parent, MCRCategoryImpl child) {
        List<MCRCategory> children = parent.getChildren();
        if (children == null) {
            parent.setChildren(new ArrayList<>());
            children = parent.getChildren();
        }
        children.add(child);
    }

    private MCRCategoryImpl newCategory(MCRCategoryID id, String description) {
        MCRCategoryImpl newCateg = new MCRCategoryImpl();
        newCateg.setId(id);
        SortedSet<MCRLabel> labels = new TreeSet<>();
        labels.add(new MCRLabel("en", id.toString(), description));
        newCateg.setLabels(labels);
        return newCateg;
    }

    @Test
    public void removeLabel() {
        addWorldClassification();
        final MCRCategory labelNode = category.getChildren().getFirst();
        int labelCount = labelNode.getLabels().size();
        DAO.removeLabel(labelNode.getId(), "en");
        startNewTransaction();
        final MCRCategory labelNodeNew = getRootCategoryFromSession().getChildren().getFirst();
        assertEquals(labelCount - 1, labelNodeNew.getLabels().size(), "Label count did not match.");
    }

    @Test
    public void replaceCategory() throws URISyntaxException, MCRException, IOException, JDOMException {
        loadWorldClassification2();
        addWorldClassification();
        DAO.replaceCategory(category2);
        startNewTransaction();
        MCRCategoryImpl rootNode = getRootCategoryFromSession();
        assertEquals(countNodes(category2), countNodes(rootNode), "Category count does not match.");
        assertEquals(category2.getChildren().getFirst().getLabels().size(),
            rootNode.getChildren().getFirst().getLabels().size(), "Label count does not match.");
        checkLeftRightLevelValue(rootNode, 0, 0);
        final URI germanURI = new URI("http://www.deutschland.de");
        final MCRCategory germany = rootNode.getChildren().getFirst().getChildren().getFirst();
        assertEquals(germanURI, germany.getURI(), "URI was not updated");
    }

    @Test
    public void replaceCategoryWithAdoption() throws URISyntaxException, MCRException, IOException, JDOMException {
        MCRCategory gc1 = loadClassificationResource("/grandchild.xml");
        MCRCategory gc2 = loadClassificationResource("/grandchild2.xml");
        DAO.addCategory(null, gc1);
        startNewTransaction();
        DAO.replaceCategory(gc2);
        startNewTransaction();
        MCRCategoryImpl rootNode = (MCRCategoryImpl) DAO.getRootCategory(gc2.getId(), -1);
        assertEquals(countNodes(gc2), countNodes(rootNode), "Category count does not match.");
        checkLeftRightLevelValue(rootNode, 0, 0);
    }

    @Test
    public void replaceSameCategory() throws Exception {
        loadWorldClassification2();
        addWorldClassification();
        MCRCategory oldCategory = DAO.getCategory(new MCRCategoryID("World", "Europe"), -1);
        DAO.replaceCategory(oldCategory);
    }

    @Test
    public void replaceMarcRelator() throws Exception {
        MCRCategory marcrelator = loadClassificationResource("/marcrelator-test.xml");
        DAO.addCategory(null, marcrelator);
        startNewTransaction();
        marcrelator = loadClassificationResource("/marcrelator-test2.xml");
        DAO.replaceCategory(marcrelator);
    }

    @Test
    public void setLabel() {
        addWorldClassification();
        startNewTransaction();
        // test add
        int count = category.getLabels().size();
        final String lang = "ju";
        final String text = "JUnit-Test";
        DAO.setLabel(category.getId(), new MCRLabel(lang, text, "Added by JUnit"));
        startNewTransaction();
        MCRCategory rootNode = getRootCategoryFromSession();
        assertEquals(count + 1, rootNode.getLabels().size(), "Label count does not match.");
        // test modify
        String description = "Modified by JUnit";
        DAO.setLabel(category.getId(), new MCRLabel(lang, text, description));
        startNewTransaction();
        rootNode = getRootCategoryFromSession();
        assertEquals(count + 1, rootNode.getLabels().size(), "Label count does not match.");
        assertEquals(description, rootNode.getLabel(lang).get().getDescription(), "Label does not match.");
    }

    @Test
    public void setLabels() {
        addWorldClassification();
        startNewTransaction();
        MCRCategory germany = DAO.getCategory(MCRCategoryID.ofString("World:Germany"), 0);
        MCRCategory france = DAO.getCategory(MCRCategoryID.ofString("World:France"), 0);
        SortedSet<MCRLabel> labels1 = new TreeSet<>();
        labels1.add(new MCRLabel("de", "deutschland", null));
        DAO.setLabels(germany.getId(), labels1);
        startNewTransaction();
        SortedSet<MCRLabel> labels2 = new TreeSet<>();
        labels2.add(new MCRLabel("de", "frankreich", null));
        DAO.setLabels(france.getId(), labels2);
        startNewTransaction();
        germany = DAO.getCategory(MCRCategoryID.ofString("World:Germany"), 0);
        france = DAO.getCategory(MCRCategoryID.ofString("World:France"), 0);
        assertEquals(1, germany.getLabels().size());
        assertEquals(1, france.getLabels().size());
        assertEquals("deutschland", germany.getLabel("de").get().getText());
        assertEquals("frankreich", france.getLabel("de").get().getText());
    }

    /**
     * tests relink child to grandparent and removal of parent.
     *
     */
    @Test
    public void replaceCategoryShiftCase() {
        addWorldClassification();
        MCRCategory europe = category.getChildren().getFirst();
        MCRCategory germany = europe.getChildren().getFirst();
        europe.getChildren().removeFirst();
        assertNull(germany.getParent(), "Germany should not have a parent right now");
        category.getChildren().removeFirst();
        assertNull(europe.getParent(), "Europe should not have a parent right now");
        category.getChildren().add(germany);
        assertEquals(category.getId(), germany.getParent().getId(),
            "Germany should not have world as parent right now");
        DAO.replaceCategory(category);
        startNewTransaction();
        MCRCategory rootNode = getRootCategoryFromSession();
        assertEquals(countNodes(category), countNodes(rootNode), "Category count does not match.");
        assertEquals(category.getChildren().getFirst().getLabels().size(),
            rootNode.getChildren().getFirst().getLabels().size(), "Label count does not match.");
    }

    @Test
    public void replaceCategoryWithItself() {
        addWorldClassification();
        MCRCategory europe = category.getChildren().getFirst();
        MCRCategory germany = europe.getChildren().getFirst();
        DAO.replaceCategory(germany);
        startNewTransaction();
        getRootCategoryFromSession();
    }

    /**
     * tests top category child to new parent
     *
     */
    public void testReplaceCategoryNewParent() {
        addWorldClassification();
        MCRCategory europe = category.getChildren().getFirst();
        MCRCategoryImpl test = new MCRCategoryImpl();
        test.setId(new MCRCategoryID(category.getId().getRootID(), "test"));
        test.setLabels(new TreeSet<>());
        test.getLabels().add(new MCRLabel("de", "JUnit testcase", null));
        category.getChildren().add(test);
        category.getChildren().removeFirst();
        test.getChildren().add(europe);
        DAO.replaceCategory(category);
        startNewTransaction();
        MCRCategory rootNode = getRootCategoryFromSession();
        assertEquals(countNodes(category), countNodes(rootNode), "Category count does not match.");
        assertEquals(category.getChildren().getFirst().getLabels().size(),
            rootNode.getChildren().getFirst().getLabels().size(), "Label count does not match.");
    }

    private MCRCategoryImpl getRootCategoryFromSession() {
        return MCREntityManagerProvider.getCurrentEntityManager().find(MCRCategoryImpl.class,
            ((MCRCategoryImpl) category).getInternalID());
    }

    private void addWorldClassification() {
        DAO.addCategory(null, category);
        startNewTransaction();
    }

    /**
     * @throws URISyntaxException
     * @throws SAXParseException
     * @throws MCRException
     */
    private void loadWorldClassification() throws URISyntaxException, MCRException, IOException, JDOMException {
        category = loadClassificationResource(WORLD_CLASS_RESOURCE_NAME);
    }

    public static MCRCategory loadClassificationResource(String resourceName)
        throws URISyntaxException, IOException, JDOMException {
        URL classResourceUrl = MCRCategoryDAOImplTest.class.getResource(resourceName);
        Document xml = MCRXMLParserFactory.getParser().parseXML(new MCRURLContent(classResourceUrl));
        return MCRXMLTransformer.getCategory(xml);
    }

    private void loadWorldClassification2() throws URISyntaxException, MCRException, IOException, JDOMException {
        category2 = loadClassificationResource(WORLD_CLASS2_RESOURCE_NAME);
    }

    private static int countNodes(MCRCategory category) {
        return (int) MCRStreamUtils.flatten(category, MCRCategory::getChildren, Collection::stream)
            .count();
    }

    private int checkLeftRightLevelValue(MCRCategoryImpl node, int leftStart, int levelStart) {
        int curValue = leftStart;
        final int nextLevel = levelStart + 1;
        String msg2 = "Left value did not match on ID: " + node.getId();
        assertEquals(leftStart, node.getLeft(), msg2);
        String msg1 = "Level value did not match on ID: " + node.getId();
        assertEquals(levelStart, node.getLevel(), msg1);
        for (MCRCategory child : node.getChildren()) {
            curValue = checkLeftRightLevelValue((MCRCategoryImpl) child, ++curValue, nextLevel);
        }
        String msg = "Right value did not match on ID: " + node.getId();
        assertEquals(++curValue, node.getRight(), msg);
        return curValue;
    }

    private Optional<MCRCategory> find(MCRCategory base, String id) {
        MCRCategoryID lookFor = new MCRCategoryID(base.getId().getRootID(), id);
        return MCRStreamUtils
            .flatten(base, MCRCategory::getChildren, Collection::stream)
            .filter(c -> c.getId().equals(lookFor))
            .findAny();
    }

}
