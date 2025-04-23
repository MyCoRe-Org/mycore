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

package org.mycore.user2;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.impl.MCRCategoryDAOImplTest;
import org.mycore.test.MCRTestExtension;

/**
 * JUnit 5 extension for User testing in the MyCoRe framework.
 * <p>
 * This extension adds user groups from <code>mcr-roles.xml</code> into tha database.
 * <p>
 * Use this extension with {@code @ExtendWith({MCRJPAExtension.class, MCRUserExtension.class})} on your test class.
 */
public class MCRUserExtension implements Extension, BeforeEachCallback, BeforeAllCallback {

    /**
     * Initializes user configuration before all tests in the class are executed.
     * Adds <code>Realms.URI</code> property.
     *
     * @param context the extension context
     */
    @Override
    public void beforeAll(ExtensionContext context) {
        Map<String, String> classProperties = MCRTestExtension.getClassProperties(context);
        classProperties.put(MCRRealmFactory.REALMS_URI_CFG_KEY, MCRRealmFactory.RESOURCE_REALMS_URI);
    }

    /**
     * Prepares the test environment before each test method.
     * Adds user groups from <code>mcr-roles.xml</code> into tha database.
     *
     * @param context the extension context
     * @throws IOException if preparation fails
     */
    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        MCRCategoryDAO dao = MCRCategoryDAOFactory.obtainInstance();
        dao.addCategory(null, MCRCategoryDAOImplTest.loadClassificationResource("/mcr-roles.xml"));
        dao.addCategory(null, MCRCategoryDAOImplTest.loadClassificationResource("/ext-roles.xml"));
    }

}
