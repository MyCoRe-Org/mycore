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

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRURLContent;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;
import org.mycore.test.MyCoReTest;
import org.xml.sax.SAXParseException;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
@MyCoReTest
public class MCRCategoryImplTest {
    static final String WORLD_CLASS_RESOURCE_NAME = "/worldclass.xml";

    private MCRCategoryImpl category;

    /**
     * Test method for {@link org.mycore.datamodel.classifications2.impl.MCRCategoryImpl#calculateLeftRightAndLevel(int, int)}.
     */
    @Test
    public void calculateLeftRightAndLevel() {
        MCRCategoryImpl rootNode = buildNode(new MCRCategoryID("co1"));
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

    @Test
    public void getLeftSiblingOrOfAncestor() throws URISyntaxException, MCRException, IOException, JDOMException {
        loadWorldClassification();
        MCRCategory europe = category.getChildren().get(0);
        MCRCategoryImpl asia = (MCRCategoryImpl) category.getChildren().get(1);
        MCRCategoryImpl germany = (MCRCategoryImpl) europe.getChildren().getFirst();
        assertEquals(europe.getId(), asia.getLeftSiblingOrOfAncestor().getId(),
            "Did not get Europe as left sibling of Asia");
        assertEquals(category.getId(), germany.getLeftSiblingOrOfAncestor().getId(),
            "Did not get World as left sibling or ancestor of Germany");
        MCRCategoryImpl america = buildNode(new MCRCategoryID(category.getRootID(), "America"));
        category.getChildren().addFirst(america);
        assertEquals(america.getId(), germany.getLeftSiblingOrOfAncestor().getId(),
            "Did not get America as left sibling or ancestor of Germany");
    }

    @Test
    public void getLeftSiblingOrParent() throws URISyntaxException, MCRException, IOException, JDOMException {
        loadWorldClassification();
        MCRCategory europe = category.getChildren().get(0);
        MCRCategoryImpl asia = (MCRCategoryImpl) category.getChildren().get(1);
        MCRCategoryImpl germany = (MCRCategoryImpl) europe.getChildren().getFirst();
        assertEquals(europe.getId(), asia.getLeftSiblingOrParent().getId(),
            "Did not get Europe as left sibling of Asia");
        assertEquals(europe.getId(), germany.getLeftSiblingOrParent().getId(),
            "Did not get Europa as left sibling or ancestor of Germany");
    }

    @Test
    public void getRightSiblingOrOfAncestor() throws URISyntaxException, MCRException, IOException, JDOMException {
        loadWorldClassification();
        MCRCategoryImpl europe = (MCRCategoryImpl) category.getChildren().get(0);
        MCRCategoryImpl asia = (MCRCategoryImpl) category.getChildren().get(1);
        MCRCategoryImpl spain = (MCRCategoryImpl) europe.getChildren().get(3);
        assertEquals(asia.getId(), europe.getRightSiblingOrOfAncestor().getId(),
            "Did not get Asia as right sibling of Europe");
        assertEquals(asia.getId(), spain.getRightSiblingOrOfAncestor().getId(),
            "Did not get Asia as right sibling or ancestor of Spain");
        assertEquals(category.getId(), asia.getRightSiblingOrOfAncestor().getId(),
            "Did not get World as right sibling or ancestor of Asia");
    }

    @Test
    public void getRightSiblingOrParent() throws URISyntaxException, MCRException, IOException, JDOMException {
        loadWorldClassification();
        MCRCategoryImpl europe = (MCRCategoryImpl) category.getChildren().get(0);
        MCRCategoryImpl asia = (MCRCategoryImpl) category.getChildren().get(1);
        MCRCategoryImpl spain = (MCRCategoryImpl) europe.getChildren().get(3);
        assertEquals(asia.getId(), europe.getRightSiblingOrParent().getId(),
            "Did not get Asia as right sibling of Europe");
        assertEquals(europe.getId(), spain.getRightSiblingOrParent().getId(),
            "Did not get Europa as right sibling or ancestor of Spain");
    }

    /**
     * @throws URISyntaxException
     * @throws SAXParseException
     * @throws MCRException
     */
    private void loadWorldClassification() throws URISyntaxException, MCRException, IOException, JDOMException {
        URL worlClassUrl = this.getClass().getResource(WORLD_CLASS_RESOURCE_NAME);
        Document xml = MCRXMLParserFactory.getParser().parseXML(new MCRURLContent(worlClassUrl));
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
