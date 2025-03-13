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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mycore.datamodel.classifications2.impl.MCRCategoryDAOImplTest.DAO;
import static org.mycore.datamodel.classifications2.impl.MCRCategoryDAOImplTest.WORLD_CLASS_RESOURCE_NAME;
import static org.mycore.test.MCRJPATestHelper.startNewTransaction;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRURLContent;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.datamodel.classifications2.MCRCategLinkReference;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

/**
 * @author Thomas Scheffler (yagee) Need to insert some things here
 */
@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
public class MCRCategLinkServiceImplTest {
    private static final MCRCategLinkReference ENGLAND_REFERENCE = new MCRCategLinkReference("England", "state");

    private static final MCRCategLinkReference LONDON_REFERENCE = new MCRCategLinkReference("London", "city");

    private MCRCategory category;

    private Collection<MCRCategoryLinkImpl> testLinks;

    private static MCRCategLinkServiceImpl SERVICE;

    private static final Logger LOGGER = LogManager.getLogger();

    @BeforeEach
    public void setUpLinks() throws Exception {
        if (SERVICE == null) {
            SERVICE = new MCRCategLinkServiceImpl();
        }
        loadWorldClassification();
        MCRCategoryImpl germany = (MCRCategoryImpl) category.getChildren().getFirst().getChildren().get(0);
        MCRCategoryImpl uk = (MCRCategoryImpl) category.getChildren().getFirst().getChildren().get(1);
        DAO.addCategory(null, category);
        testLinks = new ArrayList<>();
        testLinks.add(new MCRCategoryLinkImpl(germany, new MCRCategLinkReference("Jena", "city")));
        testLinks.add(new MCRCategoryLinkImpl(germany, new MCRCategLinkReference("Thüringen", "state")));
        testLinks.add(new MCRCategoryLinkImpl(germany, new MCRCategLinkReference("Hessen", "state")));
        testLinks.add(new MCRCategoryLinkImpl(germany, new MCRCategLinkReference("Saale", "river")));
        final MCRCategLinkReference northSeaReference = new MCRCategLinkReference("North Sea", "sea");
        testLinks.add(new MCRCategoryLinkImpl(germany, northSeaReference));
        testLinks.add(new MCRCategoryLinkImpl(uk, LONDON_REFERENCE));
        testLinks.add(new MCRCategoryLinkImpl(uk, ENGLAND_REFERENCE));
        testLinks.add(new MCRCategoryLinkImpl(uk, new MCRCategLinkReference("Thames", "river")));
        testLinks.add(new MCRCategoryLinkImpl(uk, northSeaReference));
    }

    /**
     * Test method for
     * {@link org.mycore.datamodel.classifications2.impl.MCRCategLinkServiceImpl#setLinks(org.mycore.datamodel.classifications2.MCRCategLinkReference, java.util.Collection)}
     * .
     */
    @Test
    public void setLinks() {
        addTestLinks();
        startNewTransaction();
        assertEquals(testLinks.size(), getLinkCount(), "Link count does not match.");
    }

    private int getLinkCount() {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Number> countQuery = cb.createQuery(Number.class);
        return em
            .createQuery(countQuery
                .select(cb
                    .count(countQuery
                        .from(MCRCategoryLinkImpl.class))))
            .getSingleResult()
            .intValue();
    }

    /**
     * Test method for {@link org.mycore.datamodel.classifications2.impl.MCRCategLinkServiceImpl#deleteLink(MCRCategLinkReference)}.
     */
    @Test
    public void deleteLink() {
        addTestLinks();
        startNewTransaction();
        SERVICE.deleteLink(LONDON_REFERENCE);
        assertEquals(testLinks.size() - 1, getLinkCount(), "Link count does not match.");
    }

    /**
     * Test method for {@link org.mycore.datamodel.classifications2.impl.MCRCategLinkServiceImpl#deleteLinks(java.util.Collection)}.
     */
    @Test
    public void deleteLinks() {
        addTestLinks();
        startNewTransaction();
        SERVICE.deleteLinks(Arrays.asList(LONDON_REFERENCE, ENGLAND_REFERENCE));
        assertEquals(testLinks.size() - 2, getLinkCount(), "Link count does not match.");
    }

    /**
     * Test method for {@link org.mycore.datamodel.classifications2.impl.MCRCategLinkServiceImpl#getLinksFromReference(MCRCategLinkReference)}.
     */
    @Test
    public void getLinksFromObject() {
        addTestLinks();
        startNewTransaction();
        MCRCategoryLinkImpl link = testLinks.iterator().next();
        assertTrue(SERVICE.getLinksFromReference(link.getObjectReference()).contains(link.getCategory().getId()),
            "Did not find category: " + link.getCategory().getId());
    }

    /**
     * Test method for {@link org.mycore.datamodel.classifications2.impl.MCRCategLinkServiceImpl#getLinksFromCategory(MCRCategoryID)}.
     */
    @Test
    public void getLinksFromCategory() {
        addTestLinks();
        startNewTransaction();
        MCRCategoryLinkImpl link = testLinks.iterator().next();
        assertTrue(
            SERVICE.getLinksFromCategory(link.getCategory().getId()).contains(link.getObjectReference().getObjectID()),
            "Did not find object: " + link.getObjectReference());
    }

    /**
     * Test method for {@link org.mycore.datamodel.classifications2.impl.MCRCategLinkServiceImpl#getLinksFromCategoryForType(MCRCategoryID, String)}.
     */
    @Test
    public void getLinksFromCategoryForType() {
        addTestLinks();
        startNewTransaction();
        MCRCategoryLinkImpl link = testLinks.iterator().next();
        final String objectType = link.getObjectReference().getType();
        final MCRCategoryID categoryID = link.getCategory().getId();
        final String objectID = link.getObjectReference().getObjectID();
        final Collection<String> result = SERVICE.getLinksFromCategoryForType(categoryID, objectType);
        assertTrue(result.contains(objectID), "Did not find object: " + link.getObjectReference());
        for (String id : result) {
            String type = getType(id);
            assertEquals(objectType, type, "Wrong return type detected: " + id);
        }
    }

    private String getType(String objectID) {
        return testLinks.stream()
            .filter(link -> link.getObjectReference().getObjectID().equals(objectID))
            .findFirst()
            .map(link -> link.getObjectReference().getType())
            .orElse(null);
    }

    /**
     * Test method for {@link org.mycore.datamodel.classifications2.impl.MCRCategLinkServiceImpl#countLinks(MCRCategory, boolean)}.
     */
    @Test
    public void countLinks() {
        addTestLinks();
        startNewTransaction();
        Map<MCRCategoryID, Number> map = SERVICE.countLinks(category, false);
        LOGGER.debug("****List of returned map");
        LOGGER.debug(map);
        assertEquals(getAllCategIDs(category).size(), map.size(), "Returned amount of MCRCategoryIDs does not match.");
        assertEquals(8, map.get(category.getChildren().getFirst().getId()).intValue(),
            "Count of Europe links does not match.");
        assertEquals(5, map.get(category.getChildren().getFirst().getChildren().getFirst().getId()).intValue(),
            "Count of Germany links does not match.");
        map = SERVICE.countLinks(category, true);
        assertEquals(8, map.get(category.getChildren().getFirst().getId()).intValue(),
            "Count of Europe links does not match.");
    }

    /**
     * Test method for {@link org.mycore.datamodel.classifications2.impl.MCRCategLinkServiceImpl#countLinksForType(MCRCategory, String, boolean)}.
     */
    @Test
    public void countLinksForType() {
        addTestLinks();
        startNewTransaction();
        Map<MCRCategoryID, Number> map = SERVICE.countLinksForType(category, "city", false);
        LOGGER.debug("****List of returned map");
        LOGGER.debug(map);
        assertEquals(getAllCategIDs(category).size(), map.size(), "Returned amount of MCRCategoryIDs does not match.");
        assertEquals(2, map.get(category.getChildren().getFirst().getId()).intValue(),
            "Count of Europe links does not match.");
    }

    @Test
    public void hasLinks() {
        MCRCategoryImpl germany = (MCRCategoryImpl) category.getChildren().getFirst().getChildren().getFirst();
        assertFalse(SERVICE.hasLinks(category).get(category.getId()), "Classification should not be in use");
        assertFalse(SERVICE.hasLinks(null).get(category.getId()), "Classification should not be in use");
        assertFalse(SERVICE.hasLinks(germany).get(germany.getId()), "Category should not be in use");
        addTestLinks();
        startNewTransaction();
        assertTrue(SERVICE.hasLinks(category).get(category.getId()), "Classification should be in use");
        assertTrue(SERVICE.hasLinks(null).get(category.getId()), "Classification should be in use");
        assertTrue(SERVICE.hasLinks(germany).get(germany.getId()), "Category should be in use");
    }

    @Test
    public void isInCategory() {
        MCRCategoryImpl germany = (MCRCategoryImpl) category.getChildren().get(0).getChildren().getFirst();
        MCRCategoryImpl europe = (MCRCategoryImpl) category.getChildren().get(0);
        MCRCategoryImpl asia = (MCRCategoryImpl) category.getChildren().get(1);
        MCRCategLinkReference jena = new MCRCategLinkReference("Jena", "city");
        addTestLinks();
        startNewTransaction();
        assertTrue(SERVICE.isInCategory(jena, germany.getId()), "Jena should be in Germany");
        assertTrue(SERVICE.isInCategory(jena, europe.getId()), "Jena should be in Europe");
        assertFalse(SERVICE.isInCategory(jena, asia.getId()), "Jena should not be in Asia");
    }

    @Test
    public void getReferences() {
        addTestLinks();
        startNewTransaction();
        String type = "state";
        Collection<MCRCategLinkReference> references = SERVICE.getReferences(type);
        assertNotNull(references, "Did not return a collection");
        assertFalse(references.isEmpty(), "Collection is empty");
        for (MCRCategLinkReference ref : references) {
            assertEquals(type, ref.getType(), "Type of reference is not correct.");
        }
        assertEquals(testLinks.stream()
            .filter(link -> link.getObjectReference().getType().equals(type))
            .count(),
            references.size(), "Collection is not complete");
    }

    public void loadWorldClassification() throws URISyntaxException, MCRException, IOException, JDOMException {
        LOGGER.error("Start loadWorldClassification");
        URL worlClassUrl = getClass().getResource(WORLD_CLASS_RESOURCE_NAME);
        Document xml = MCRXMLParserFactory.getParser().parseXML(new MCRURLContent(worlClassUrl));
        category = MCRXMLTransformer.getCategory(xml);
    }

    private void addTestLinks() {
        for (MCRCategoryLinkImpl link : testLinks) {
            SERVICE.setLinks(link.getObjectReference(), Collections.nCopies(1, link.getCategory().getId()));
        }
    }

    private static Collection<MCRCategoryID> getAllCategIDs(MCRCategory category) {
        HashSet<MCRCategoryID> ids = new HashSet<>();
        ids.add(category.getId());
        for (MCRCategory cat : category.getChildren()) {
            ids.addAll(getAllCategIDs(cat));
        }
        return ids;
    }

}
