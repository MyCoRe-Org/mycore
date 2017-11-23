/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

package org.mycore.user2;

import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.mycore.common.MCRJPATestCase;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.impl.MCRCategoryDAOImplTest;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
@Ignore
public class MCRUserTestCase extends MCRJPATestCase {

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        MCRCategory groupsCategory = MCRCategoryDAOImplTest.loadClassificationResource("/mcr-roles.xml");
        MCRCategoryDAO DAO = MCRCategoryDAOFactory.getInstance();
        DAO.addCategory(null, groupsCategory);
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put(MCRRealmFactory.REALMS_URI_CFG_KEY, MCRRealmFactory.RESOURCE_REALMS_URI);
        return testProperties;
    }

}
