/*
 * 
 * $Revision: 15007 $ $Date: 2009-03-25 10:43:02 +0100 (Mi, 25. Mär 2009) $
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

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.jdom.Document;
import org.mycore.common.MCRTestCase;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRCategoryImplTest extends MCRTestCase {
    static final String WORLD_CLASS_RESOURCE_NAME = "/org/mycore/datamodel/classifications2/impl/resources/worldclass.xml";

    private MCRCategoryImpl category;

    /* (non-Javadoc)
     * @see org.mycore.common.MCRTestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Test method for {@link org.mycore.datamodel.classifications2.impl.MCRCategoryImpl#calculateLeftRightAndLevel(int, int)}.
     */
    public void testCalculateLeftRightAndLevel() {
        MCRCategoryImpl rootNode = buildNode(MCRCategoryID.rootID("co1"));
        final int leftStart = 1;
        final int levelStart = 0;
        assertEquals(2, rootNode.calculateLeftRightAndLevel(leftStart, levelStart));
        assertEquals(levelStart, rootNode.getLevel());
        MCRCategoryImpl co2 = buildNode(new MCRCategoryID(rootNode.getId().getRootID(), "co2"));
        rootNode.getChildren().add(co2);
        assertEquals(4, rootNode.calculateLeftRightAndLevel(leftStart, levelStart));
        assertEquals(leftStart, co2.getLevel());
        MCRCategoryImpl co3 = buildNode(new MCRCategoryID(rootNode.getId().getRootID(), "co3"));
        rootNode.getChildren().add(co3);
        assertEquals(6, rootNode.calculateLeftRightAndLevel(leftStart, levelStart));
        assertEquals(leftStart, co3.getLevel());
        MCRCategoryImpl co4 = buildNode(new MCRCategoryID(rootNode.getId().getRootID(), "co4"));
        co3.getChildren().add(co4);
        assertEquals(8, rootNode.calculateLeftRightAndLevel(leftStart, levelStart));
        assertEquals(2, co4.getLevel());
    }

    public void testGetLeftSiblingOrOfAncestor() throws URISyntaxException {
        loadWorldClassification();
        MCRCategory europe = category.getChildren().get(0);
        MCRCategoryImpl asia = (MCRCategoryImpl) category.getChildren().get(1);
        MCRCategoryImpl germany = (MCRCategoryImpl) europe.getChildren().get(0);
        assertEquals("Did not get Europe as left sibling of Asia", europe.getId(), asia.getLeftSiblingOrOfAncestor().getId());
        assertEquals("Did not get World as left sibling or ancestor of Germany", category.getId(), germany.getLeftSiblingOrOfAncestor()
                .getId());
        MCRCategoryImpl america = buildNode(new MCRCategoryID(category.getRootID(), "America"));
        category.getChildren().add(0, america);
        assertEquals("Did not get America as left sibling or ancestor of Germany", america.getId(), germany.getLeftSiblingOrOfAncestor()
                .getId());
    }

    public void testGetLeftSiblingOrParent() throws URISyntaxException {
        loadWorldClassification();
        MCRCategory europe = category.getChildren().get(0);
        MCRCategoryImpl asia = (MCRCategoryImpl) category.getChildren().get(1);
        MCRCategoryImpl germany = (MCRCategoryImpl) europe.getChildren().get(0);
        assertEquals("Did not get Europe as left sibling of Asia", europe.getId(), asia.getLeftSiblingOrParent().getId());
        assertEquals("Did not get Europa as left sibling or ancestor of Germany", europe.getId(), germany.getLeftSiblingOrParent().getId());
    }

    public void testGetRightSiblingOrOfAncestor() throws URISyntaxException {
        loadWorldClassification();
        MCRCategoryImpl europe = (MCRCategoryImpl) category.getChildren().get(0);
        MCRCategoryImpl asia = (MCRCategoryImpl) category.getChildren().get(1);
        MCRCategoryImpl spain = (MCRCategoryImpl) europe.getChildren().get(3);
        assertEquals("Did not get Asia as right sibling of Europe", asia.getId(), europe.getRightSiblingOrOfAncestor().getId());
        assertEquals("Did not get Asia as right sibling or ancestor of Spain", asia.getId(), spain.getRightSiblingOrOfAncestor().getId());
        assertEquals("Did not get World as right sibling or ancestor of Asia", category.getId(), asia.getRightSiblingOrOfAncestor().getId());
    }

    public void testGetRightSiblingOrParent() throws URISyntaxException {
        loadWorldClassification();
        MCRCategoryImpl europe = (MCRCategoryImpl) category.getChildren().get(0);
        MCRCategoryImpl asia = (MCRCategoryImpl) category.getChildren().get(1);
        MCRCategoryImpl spain = (MCRCategoryImpl) europe.getChildren().get(3);
        assertEquals("Did not get Asia as right sibling of Europe", asia.getId(), europe.getRightSiblingOrParent().getId());
        assertEquals("Did not get Europa as right sibling or ancestor of Spain", europe.getId(), spain.getRightSiblingOrParent().getId());
    }

    /**
     * @throws URISyntaxException
     */
    private void loadWorldClassification() throws URISyntaxException {
        URL worlClassUrl = this.getClass().getResource(WORLD_CLASS_RESOURCE_NAME);
        Document xml = MCRXMLHelper.parseURI(worlClassUrl.toURI().toString());
        category = MCRCategoryImpl.wrapCategory(MCRXMLTransformer.getCategory(xml), null, null);
        category.calculateLeftRightAndLevel(1, 0);
    }

    private static MCRCategoryImpl buildNode(MCRCategoryID id) {
        MCRCategoryImpl rootNode = new MCRCategoryImpl();
        rootNode.setId(id);
        final List<MCRCategory> emptyList = Collections.emptyList();
        rootNode.setChildren(emptyList);
        return rootNode;
    }

}
