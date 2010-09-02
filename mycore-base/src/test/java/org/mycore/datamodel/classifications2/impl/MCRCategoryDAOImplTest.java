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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;

import org.hibernate.criterion.Projections;
import org.jdom.Document;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRException;
import org.mycore.common.MCRHibTestCase;
import org.mycore.common.xml.MCRXMLHelper;
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
        long allNodes = ((Number) sessionFactory.getCurrentSession().createCriteria(MCRCategoryImpl.class).setProjection(Projections.rowCount()).uniqueResult()).longValue();
        // category + india
        assertEquals("Complete category count does not match.", countNodes(category) + 1, allNodes);
        assertTrue("No root category present", rootCategory.getRoot() != null);
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
    }

    @Test
    public void getCategory() {
        addWorldClassification();
        MCRCategory rootCategory = DAO.getCategory(category.getId(), 0);
        assertTrue("Children present with child Level 0.", rootCategory.getChildren().isEmpty());
        rootCategory = DAO.getCategory(category.getId(), 1);
        MCRCategory origSubCategory = rootCategory.getChildren().get(0);
        assertTrue("Children present with child Level 1.", origSubCategory.getChildren().isEmpty());
        assertEquals("Category count does not match with child Level 1.\n" + MCRStringTransformer.getString(rootCategory), category.getChildren().size(),
                rootCategory.getChildren().size());
        assertEquals("Children of Level 1 do not know that they are at the first level.\n" + MCRStringTransformer.getString(rootCategory), 1, origSubCategory
                .getLevel());
        MCRCategory europe = DAO.getCategory(category.getChildren().get(0).getId(), -1);
        assertFalse("No children present in " + europe.getId(), europe.getChildren().isEmpty());
        europe = DAO.getCategory(category.getChildren().get(0).getId(), 1);
        assertFalse("No children present in " + europe.getId(), europe.getChildren().isEmpty());
        rootCategory = DAO.getCategory(category.getId(), -1);
        assertEquals("Did not get all categories." + MCRStringTransformer.getString(rootCategory), countNodes(category), countNodes(rootCategory));
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
        assertFalse("Category '" + category.getChildren().get(1).getId() + "' shouldn't have children.", DAO.hasChildren(category.getChildren().get(1).getId()));
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
    public void replaceCategory() throws URISyntaxException, MCRException, SAXParseException {
        loadWorldClassification2();
        addWorldClassification();
        DAO.replaceCategory(category2);
        startNewTransaction();
        MCRCategoryImpl rootNode = getRootCategoryFromSession();
        assertEquals("Category count does not match.", countNodes(category2), countNodes(rootNode));
        assertEquals("Label count does not match.", category2.getChildren().get(0).getLabels().size(), rootNode.getChildren().get(0).getLabels().size());
        checkLeftRightLevelValue(rootNode, 0, 0);
        final URI germanURI = new URI("http://www.deutschland.de");
        final MCRCategory germany = rootNode.getChildren().get(0).getChildren().get(0);
        assertEquals("URI was not updated", germanURI, germany.getURI());
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
        assertEquals("Label count does not match.", category.getChildren().get(0).getLabels().size(), rootNode.getChildren().get(0).getLabels().size());
    }

    /**
     * tests top category child to new parent
     * 
     * @throws URISyntaxException
     */
    public void testReplaceCategoryNewParent() {
        addWorldClassification();
        MCRCategory europe = category.getChildren().get(0);
        MCRCategoryImpl test=new MCRCategoryImpl();
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
        assertEquals("Label count does not match.", category.getChildren().get(0).getLabels().size(), rootNode.getChildren().get(0)
                .getLabels().size());
    }

    private MCRCategoryImpl getRootCategoryFromSession() {
        return (MCRCategoryImpl) sessionFactory.getCurrentSession().get(MCRCategoryImpl.class, ((MCRCategoryImpl) category).getInternalID());
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
    private void loadWorldClassification() throws URISyntaxException, MCRException, SAXParseException {
        URL worlClassUrl = this.getClass().getResource(WORLD_CLASS_RESOURCE_NAME);
        Document xml = MCRXMLHelper.parseURI(worlClassUrl.toURI());
        category = MCRXMLTransformer.getCategory(xml);
    }

    private void loadWorldClassification2() throws URISyntaxException, MCRException, SAXParseException {
        URL worlClassUrl = this.getClass().getResource(WORLD_CLASS2_RESOURCE_NAME);
        Document xml = MCRXMLHelper.parseURI(worlClassUrl.toURI());
        category2 = MCRXMLTransformer.getCategory(xml);
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
        for (MCRCategory child : node.children) {
            curValue = checkLeftRightLevelValue((MCRCategoryImpl) child, ++curValue, nextLevel);
        }
        assertEquals("Right value did not match on ID: " + node.getId(), ++curValue, node.getRight());
        return curValue;
    }

}
