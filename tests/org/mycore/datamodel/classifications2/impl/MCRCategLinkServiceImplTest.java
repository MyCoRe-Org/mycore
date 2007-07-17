/**
 * $RCSfile$
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
 **/
package org.mycore.datamodel.classifications2.impl;

import static org.mycore.datamodel.classifications2.impl.MCRCategoryDAOImplTest.DAO;
import static org.mycore.datamodel.classifications2.impl.MCRCategoryDAOImplTest.WORLD_CLASS_RESOURCE_NAME;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jdom.Document;

import org.mycore.common.MCRHibTestCase;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRObjectReference;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;

;

/**
 * @author Thomas Scheffler (yagee)
 * 
 * Need to insert some things here
 * 
 */
public class MCRCategLinkServiceImplTest extends MCRHibTestCase {
    private MCRCategory category;

    private Collection<MCRCategoryLink> testLinks;

    private static MCRCategLinkServiceImpl SERVICE = new MCRCategLinkServiceImpl();

    private static final Logger LOGGER = Logger.getLogger(MCRCategLinkServiceImplTest.class);

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.common.MCRHibTestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        loadWorldClassification();
        MCRCategoryImpl germany = (MCRCategoryImpl) category.getChildren().get(0).getChildren().get(0);
        MCRCategoryImpl uk = (MCRCategoryImpl) category.getChildren().get(0).getChildren().get(1);
        DAO.addCategory(null, category);
        testLinks = new ArrayList<MCRCategoryLink>();
        testLinks.add(new MCRCategoryLink(germany, new MCRObjectReference("Jena", "city")));
        testLinks.add(new MCRCategoryLink(germany, new MCRObjectReference("Thüringen", "state")));
        testLinks.add(new MCRCategoryLink(germany, new MCRObjectReference("Saale", "river")));
        testLinks.add(new MCRCategoryLink(uk, new MCRObjectReference("London", "city")));
        testLinks.add(new MCRCategoryLink(uk, new MCRObjectReference("England", "state")));
        testLinks.add(new MCRCategoryLink(uk, new MCRObjectReference("Thames", "river")));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.common.MCRHibTestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for
     * {@link org.mycore.datamodel.classifications2.impl.MCRCategLinkServiceImpl#setLinks(org.mycore.datamodel.classifications2.MCRObjectReference, java.util.Collection)}.
     */
    public void testSetLinks() {
        addTestLinks();
        startNewTransaction();
        assertEquals("Link count does not match.", testLinks.size(), sessionFactory.getCurrentSession().createCriteria(MCRCategoryLink.class).list().size());
    }

    /**
     * Test method for
     * {@link org.mycore.datamodel.classifications2.impl.MCRCategLinkServiceImpl#deleteLink(java.lang.String)}.
     */
    public void testDeleteLink() {
        addTestLinks();
        startNewTransaction();
        SERVICE.deleteLink("London");
        assertEquals("Link count does not match.", testLinks.size() - 1, sessionFactory.getCurrentSession().createCriteria(MCRCategoryLink.class).list().size());
    }

    /**
     * Test method for
     * {@link org.mycore.datamodel.classifications2.impl.MCRCategLinkServiceImpl#deleteLinks(java.util.Collection)}.
     */
    public void testDeleteLinks() {
        addTestLinks();
        startNewTransaction();
        SERVICE.deleteLinks(Arrays.asList("London", "England"));
        assertEquals("Link count does not match.", testLinks.size() - 2, sessionFactory.getCurrentSession().createCriteria(MCRCategoryLink.class).list().size());
    }

    /**
     * Test method for
     * {@link org.mycore.datamodel.classifications2.impl.MCRCategLinkServiceImpl#getLinksFromObject(java.lang.String)}.
     */
    public void testGetLinksFromObject() {
        addTestLinks();
        startNewTransaction();
        MCRCategoryLink link = testLinks.iterator().next();
        assertTrue("Did not find category: " + link.getCategory().getId(), SERVICE.getLinksFromObject(link.getObjectReference().getObjectID()).contains(
                link.getCategory().getId()));
    }

    /**
     * Test method for
     * {@link org.mycore.datamodel.classifications2.impl.MCRCategLinkServiceImpl#countLinks(java.util.Collection)}.
     */
    public void testCountLinks() {
        addTestLinks();
        startNewTransaction();
        List<MCRCategoryID> categIDs = Arrays.asList(category.getId(), category.getChildren().get(0).getId(), category.getChildren().get(0).getChildren()
                .get(0).getId(), category.getChildren().get(0).getChildren().get(1).getId(), category.getChildren().get(0).getChildren().get(2).getId());
        Map<MCRCategoryID, Number> map = SERVICE.countLinks(categIDs);
        LOGGER.debug("****List of returned map");
        LOGGER.debug(map);
        assertEquals("Returned amount of MCRCategoryIDs does not match.", categIDs.size(), map.size());
        assertEquals("Count of Europe links does not match.", 6, map.get(category.getChildren().get(0).getId()).intValue());
    }

    /**
     * Test method for
     * {@link org.mycore.datamodel.classifications2.impl.MCRCategLinkServiceImpl#countLinksForType(Collection, String)}.
     */
    public void testCountLinksForType() {
        fail("Not yet implemented");
    }

    private void loadWorldClassification() throws URISyntaxException {
        URL worlClassUrl = this.getClass().getResource(WORLD_CLASS_RESOURCE_NAME);
        Document xml = MCRXMLHelper.parseURI(worlClassUrl.toURI().toString());
        category = MCRXMLTransformer.getCategory(xml);
    }

    private void addTestLinks() {
        for (MCRCategoryLink link : testLinks) {
            SERVICE.setLinks(link.getObjectReference(), Collections.nCopies(1, link.getCategory().getId()));
        }
    }

    @Override
    protected boolean isDebugEnabled() {
        return true;
    }

}
