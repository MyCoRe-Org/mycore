/*
 * 
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 */
package org.mycore.datamodel.classifications2.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.jdom2.Document;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRException;
import org.mycore.common.MCRHibTestCase;
import org.mycore.common.content.MCRVFSContent;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.datamodel.classifications2.utils.MCRStringTransformer;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;
import org.xml.sax.SAXParseException;

public class MCRCategoryDAOImplTest extends MCRHibTestCase {

    static final String WORLD_CLASS_RESOURCE_NAME = "/worldclass.xml";

    private static final String WORLD_CLASS2_RESOURCE_NAME = "/worldclass2.xml";

    static final String CATEGORY_MAPPING_RESOURCE_NAME = "/org/mycore/datamodel/classifications2/impl/MCRCategoryImpl.hbm.xml";

    static final MCRCategoryDAOImpl DAO = new MCRCategoryDAOImpl();

    private MCRCategory category, category2;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        loadWorldClassification();
    }

    @Test
    public void addCategory() throws MCRException {
        addWorldClassification();
        assertTrue("Exist check failed for Category " + category.getId(), DAO.exist(category.getId()));
        MCRCategoryImpl india = new MCRCategoryImpl();
        india.setId(new MCRCategoryID(category.getId().getRootID(), "India"));
        india.setLabels(new HashSet<MCRLabel>());
        india.getLabels().add(new MCRLabel("de", "Indien", null));
        india.getLabels().add(new MCRLabel("en", "India", null));
        DAO.addCategory(new MCRCategoryID(category.getId().getRootID(), "Asia"), india);
        startNewTransaction();
        assertTrue("Exist check failed for Category " + india.getId(), DAO.exist(india.getId()));
        MCRCategoryImpl rootCategory = getRootCategoryFromSession();
        assertEquals("Child category count does not match.", category.getChildren().size(), rootCategory.getChildren().size());
        long allNodes = ((Number) SESSION_FACTORY
            .getCurrentSession()
            .createCriteria(MCRCategoryImpl.class)
            .setProjection(Projections.rowCount())
            .uniqueResult()).longValue();
        // category + india
        assertEquals("Complete category count does not match.", countNodes(category) + 1, allNodes);
        assertTrue("No root category present", rootCategory.getRoot() != null);
    }

    /**
     * Test case for https://sourceforge.net/p/mycore/bugs/612/
     */
    @Test
    public void addCategorySingleSteps() {
        MCRCategory root = new MCRCategoryImpl();
        MCRCategoryID rootID = MCRCategoryID.rootID("junit");
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
        assertTrue("Category c should not contain child categories.", DAO.getChildren(cId).isEmpty());
        assertFalse("Category a should contain child categories.", DAO.getChildren(aId).isEmpty());
        root = DAO.getCategory(rootID, -1);
        checkLeftRightLevelValue((MCRCategoryImpl) root, 0, 0);
        assertEquals("Did not get all categories", 4, countNodes(root));
        List<MCRCategory> children = root.getChildren();
        assertEquals("Children count mismatch", 2, children.size());
        a = children.get(0);
        c = children.get(1);
        assertEquals("Wrong order of children", aId, a.getId());
        assertEquals("Wrong order of children", cId, c.getId());
        assertTrue("Category c should not contain child categories.", c.getChildren().isEmpty());
        assertFalse("Category a should contain child categories.", a.getChildren().isEmpty());
    }

    @Test
    public void deleteCategory() {
        addWorldClassification();
        DAO.deleteCategory(category.getId());
        startNewTransaction();
        // check if classification is present
        assertFalse("Category is not deleted: " + category.getId(), DAO.exist(category.getId()));
        // check if any subcategory is present
        assertFalse("Category is not deleted: " + category.getChildren().get(0).getId(), DAO.exist(category.getChildren().get(0).getId()));
    }

    @Test
    public void getCategoriesByLabel() {
        addWorldClassification();
        MCRCategory find = category.getChildren().get(0).getChildren().get(0);
        MCRCategory dontFind = category.getChildren().get(1);
        MCRLabel label = find.getLabels().iterator().next();
        List<MCRCategory> results = DAO.getCategoriesByLabel(category.getId(), label.getLang(), label.getText());
        assertFalse("No search results found", results.isEmpty());
        assertTrue("Could not find Category: " + find.getId(), results.get(0).getLabels().contains(label));
        assertTrue("No search result expected.", DAO.getCategoriesByLabel(dontFind.getId(), label.getLang(), label.getText()).isEmpty());
        results = DAO.getCategoriesByLabel(label.getLang(), label.getText());
        assertFalse("No search results found", results.isEmpty());
        assertTrue("Could not find Category: " + find.getId(), results.get(0).getLabels().contains(label));
    }

    @Test
    public void getCategory() {
        addWorldClassification();
        MCRCategory rootCategory = DAO.getCategory(category.getId(), 0);
        assertTrue("Children present with child Level 0.", rootCategory.getChildren().isEmpty());
        rootCategory = DAO.getCategory(category.getId(), 1);
        MCRCategory origSubCategory = rootCategory.getChildren().get(0);
        assertTrue("Children present with child Level 1.", origSubCategory.getChildren().isEmpty());
        assertEquals("Category count does not match with child Level 1.\n" + MCRStringTransformer.getString(rootCategory), category
            .getChildren()
            .size(), rootCategory.getChildren().size());
        assertEquals("Children of Level 1 do not know that they are at the first level.\n" + MCRStringTransformer.getString(rootCategory),
            1, origSubCategory.getLevel());
        MCRCategory europe = DAO.getCategory(category.getChildren().get(0).getId(), -1);
        assertFalse("No children present in " + europe.getId(), europe.getChildren().isEmpty());
        europe = DAO.getCategory(category.getChildren().get(0).getId(), 1);
        assertFalse("No children present in " + europe.getId(), europe.getChildren().isEmpty());
        rootCategory = DAO.getCategory(category.getId(), -1);
        assertEquals("Did not get all categories." + MCRStringTransformer.getString(rootCategory), countNodes(category),
            countNodes(rootCategory));
        assertEquals("Children of Level 1 do not match", category.getChildren().size(), rootCategory.getChildren().size());
        MCRCategory subCategory = DAO.getCategory(origSubCategory.getId(), 0);
        assertNotNull("Did not return ", subCategory);
        assertEquals("ObjectIDs did not match", origSubCategory.getId(), subCategory.getId());
    }

    @Test
    public void getChildren() {
        addWorldClassification();
        List<MCRCategory> children = DAO.getChildren(category.getId());
        assertEquals("Did not get all children of :" + category.getId(), category.getChildren().size(), children.size());
        for (int i = 0; i < children.size(); i++) {
            assertEquals("Category IDs of children do not match.", category.getChildren().get(i).getId(), children.get(i).getId());
        }
    }

    @Test
    public void getParents() {
        addWorldClassification();
        MCRCategory find = category.getChildren().get(0).getChildren().get(0);
        List<MCRCategory> parents = DAO.getParents(find.getId());
        MCRCategory findParent = find;
        for (MCRCategory parent : parents) {
            findParent = findParent.getParent();
            assertNotNull("Returned too much parents.", findParent);
            assertEquals("Parents did not match.", findParent.getId(), parent.getId());
        }
    }

    @Test
    public void getRootCategoryIDs() {
        addWorldClassification();
        MCRCategoryID find = category.getId();
        List<MCRCategoryID> classIds = DAO.getRootCategoryIDs();
        assertEquals("Result size does not match.", 1, classIds.size());
        assertEquals("Returned MCRCategoryID does not match.", find, classIds.get(0));
    }

    @Test
    public void getRootCategories() {
        addWorldClassification();
        MCRCategoryID find = category.getId();
        List<MCRCategory> classes = DAO.getRootCategories();
        assertEquals("Result size does not match.", 1, classes.size());
        assertEquals("Returned MCRCategoryID does not match.", find, classes.get(0).getId());
    }

    @Test
    public void getRootCategory() {
        addWorldClassification();
        // Europe
        MCRCategory find = category.getChildren().get(0);
        MCRCategory rootCategory = DAO.getRootCategory(find.getId(), 0);
        assertEquals("Category count does not match.", 2, countNodes(rootCategory));
        assertEquals("Did not get root Category.", find.getRoot().getId(), rootCategory.getId());
        rootCategory = DAO.getRootCategory(find.getId(), -1);
        assertEquals("Category count does not match.", 1 + countNodes(find), countNodes(rootCategory));
    }

    @Test
    public void children() {
        addWorldClassification();
        assertTrue("Category '" + category.getId() + "' should have children.", DAO.hasChildren(category.getId()));
        assertFalse("Category '" + category.getChildren().get(1).getId() + "' shouldn't have children.",
            DAO.hasChildren(category.getChildren().get(1).getId()));
    }

    @Test
    public void moveCategoryWithoutIndex() throws SQLException {
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
    public void moveCategoryInParent() throws SQLException {
        addWorldClassification();
        MCRCategory moveNode = category.getChildren().get(1);
        DAO.moveCategory(moveNode.getId(), moveNode.getParent().getId(), 0);
        startNewTransaction();
        MCRCategoryImpl rootNode = getRootCategoryFromSession();
        checkLeftRightLevelValue(rootNode, 0, 0);
        MCRCategory movedNode = rootNode.getChildren().get(0);
        assertEquals("Did not expect this category on position 0.", moveNode.getId(), movedNode.getId());
    }

    @Test
    public void moveRightCategory() throws SQLException {
        String rootIDStr = "rootID";
        MCRCategoryID rootID = MCRCategoryID.rootID(rootIDStr);
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
        printCategoryTable();
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
        printCategoryTable();
        endTransaction();
        assertLeftRightVal(rootID, 0, 7);
        assertLeftRightVal(child1ID, 1, 6);
        assertLeftRightVal(child2ID, 2, 3);
        assertLeftRightVal(child3ID, 4, 5);
    }

    @Test
    public void moveCategoryUp() {
        String rootIDStr = "rootID";
        MCRCategoryID rootID = MCRCategoryID.rootID(rootIDStr);
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
        printCategoryTable();
        endTransaction();
        assertLeftRightVal(rootID, 0, 5);
        assertLeftRightVal(child1ID, 1, 2);
        assertLeftRightVal(child2ID, 3, 4);
    }

    private void assertLeftRightVal(MCRCategoryID categID, int expectedLeftVal, int expectedRightVal) {
        startNewTransaction();
        MCRCategoryImpl retrievedRoot = (MCRCategoryImpl) DAO.getCategory(categID, 0);
        endTransaction();
        assertNotNull(retrievedRoot);
        assertEquals("Left value should be " + expectedLeftVal + ".", expectedLeftVal, retrievedRoot.getLeft());
        assertEquals("Right value should be " + expectedRightVal + ".", expectedRightVal, retrievedRoot.getRight());
    }

    private void addChild(MCRCategoryImpl parent, MCRCategoryImpl child) {
        List<MCRCategory> children = parent.getChildren();
        if (children == null) {
            parent.setChildren(new ArrayList<MCRCategory>());
            children = parent.getChildren();
        }
        children.add(child);
    }

    private MCRCategoryImpl newCategory(MCRCategoryID id, String description) {
        MCRCategoryImpl newCateg = new MCRCategoryImpl();
        newCateg.setId(id);
        Set<MCRLabel> labels = new HashSet<MCRLabel>();
        labels.add(new MCRLabel("en", id.toString(), description));
        newCateg.setLabels(labels);
        return newCateg;
    }

    @Test
    public void removeLabel() {
        addWorldClassification();
        final MCRCategory labelNode = category.getChildren().get(0);
        int labelCount = labelNode.getLabels().size();
        DAO.removeLabel(labelNode.getId(), "en");
        startNewTransaction();
        final MCRCategory labelNodeNew = getRootCategoryFromSession().getChildren().get(0);
        assertEquals("Label count did not match.", labelCount - 1, labelNodeNew.getLabels().size());
    }

    @Test
    public void replaceCategory() throws URISyntaxException, MCRException, SAXParseException, IOException {
        loadWorldClassification2();
        addWorldClassification();
        DAO.replaceCategory(category2);
        startNewTransaction();
        MCRCategoryImpl rootNode = getRootCategoryFromSession();
        assertEquals("Category count does not match.", countNodes(category2), countNodes(rootNode));
        assertEquals("Label count does not match.", category2.getChildren().get(0).getLabels().size(), rootNode
            .getChildren()
            .get(0)
            .getLabels()
            .size());
        checkLeftRightLevelValue(rootNode, 0, 0);
        final URI germanURI = new URI("http://www.deutschland.de");
        final MCRCategory germany = rootNode.getChildren().get(0).getChildren().get(0);
        assertEquals("URI was not updated", germanURI, germany.getURI());
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
        assertEquals("Label count does not match.", count + 1, rootNode.getLabels().size());
        // test modify
        String description = "Modified by JUnit";
        DAO.setLabel(category.getId(), new MCRLabel(lang, text, description));
        startNewTransaction();
        rootNode = getRootCategoryFromSession();
        assertEquals("Label count does not match.", count + 1, rootNode.getLabels().size());
        assertEquals("Label does not match.", description, rootNode.getLabel(lang).getDescription());
    }

    /**
     * tests relink child to grantparent and removal of parent.
     * 
     * @throws URISyntaxException
     */
    @Test
    public void replaceCategoryShiftCase() {
        addWorldClassification();
        MCRCategory europe = category.getChildren().get(0);
        MCRCategory germany = europe.getChildren().get(0);
        europe.getChildren().remove(0);
        category.getChildren().remove(0);
        category.getChildren().add(germany);
        DAO.replaceCategory(category);
        startNewTransaction();
        MCRCategory rootNode = getRootCategoryFromSession();
        assertEquals("Category count does not match.", countNodes(category), countNodes(rootNode));
        assertEquals("Label count does not match.", category.getChildren().get(0).getLabels().size(), rootNode
            .getChildren()
            .get(0)
            .getLabels()
            .size());
    }

    //  @Test
    public void replaceCategoryWithItself() {
        addWorldClassification();
        MCRCategory europe = category.getChildren().get(0);
        MCRCategory germany = europe.getChildren().get(0);
        DAO.replaceCategory(germany);
        startNewTransaction();
        MCRCategory rootNode = getRootCategoryFromSession();
    }

    /**
     * tests top category child to new parent
     * 
     * @throws URISyntaxException
     */
    public void testReplaceCategoryNewParent() {
        addWorldClassification();
        MCRCategory europe = category.getChildren().get(0);
        MCRCategoryImpl test = new MCRCategoryImpl();
        test.setId(new MCRCategoryID(category.getId().getRootID(), "test"));
        test.setLabels(new HashSet<MCRLabel>());
        test.getLabels().add(new MCRLabel("de", "JUnit testcase", null));
        category.getChildren().add(test);
        category.getChildren().remove(0);
        test.getChildren().add(europe);
        DAO.replaceCategory(category);
        startNewTransaction();
        MCRCategory rootNode = getRootCategoryFromSession();
        assertEquals("Category count does not match.", countNodes(category), countNodes(rootNode));
        assertEquals("Label count does not match.", category.getChildren().get(0).getLabels().size(), rootNode
            .getChildren()
            .get(0)
            .getLabels()
            .size());
    }

    private MCRCategoryImpl getRootCategoryFromSession() {
        return (MCRCategoryImpl) SESSION_FACTORY.getCurrentSession().get(MCRCategoryImpl.class,
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
    private void loadWorldClassification() throws URISyntaxException, MCRException, SAXParseException, IOException {
        category = loadClassificationResource(WORLD_CLASS_RESOURCE_NAME);
    }

    public static MCRCategory loadClassificationResource(String resourceName) throws SAXParseException, IOException, URISyntaxException {
        URL classResourceUrl = MCRCategoryDAOImplTest.class.getResource(resourceName);
        Document xml = MCRXMLParserFactory.getParser().parseXML(new MCRVFSContent(classResourceUrl));
        MCRCategory category = MCRXMLTransformer.getCategory(xml);
        return category;
    }

    private void loadWorldClassification2() throws URISyntaxException, MCRException, SAXParseException, IOException {
        category2 = loadClassificationResource(WORLD_CLASS2_RESOURCE_NAME);
    }

    private static int countNodes(MCRCategory category) {
        int i = 1;
        for (MCRCategory child : category.getChildren()) {
            i += countNodes(child);
        }
        return i;
    }

    private int checkLeftRightLevelValue(MCRCategoryImpl node, int leftStart, int levelStart) {
        int curValue = leftStart;
        final int nextLevel = levelStart + 1;
        assertEquals("Left value did not match on ID: " + node.getId(), leftStart, node.getLeft());
        assertEquals("Level value did not match on ID: " + node.getId(), levelStart, node.getLevel());
        for (MCRCategory child : node.getChildren()) {
            curValue = checkLeftRightLevelValue((MCRCategoryImpl) child, ++curValue, nextLevel);
        }
        assertEquals("Right value did not match on ID: " + node.getId(), ++curValue, node.getRight());
        return curValue;
    }

    private void printCategoryTable() {
        Session session = SESSION_FACTORY.getCurrentSession();
        Connection connection = session.connection();
        try {
            Statement statement = connection.createStatement();
            try {
                ResultSet resultSet = statement.executeQuery("SELECT * FROM MCRCategory");
                printResultSet(resultSet, System.out);
            } catch (SQLException e) {
                Logger.getLogger(getClass()).warn("Error while querying MCRCategory", e);
            } finally {
                statement.close();
            }
        } catch (SQLException e) {
            Logger.getLogger(getClass()).warn("Error while querying MCRCategory", e);
        }
    }

}
