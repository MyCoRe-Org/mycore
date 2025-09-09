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
package org.mycore.datamodel.classifications2.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Set;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.MCRTransactionManager;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true")
})
public class MCRXPathClassificationMappingConditionTest {

    public static final String TEST_DIRECTORY =
        MCRXPathClassificationMappingConditionTest.class.getSimpleName() + "/";

    public MCRCategoryDAO getDAO() {
        return MCRCategoryDAOFactory.obtainInstance();
    }

    @Test
    public void testEvaluation() throws IOException, JDOMException {

        MCRSessionMgr.getCurrentSession();
        MCRTransactionManager.hasActiveTransactions();
        ClassLoader classLoader = getClass().getClassLoader();
        SAXBuilder saxBuilder = new SAXBuilder();

        Document document = saxBuilder.build(classLoader.getResourceAsStream(TEST_DIRECTORY + "testMcrObject.xml"));
        MCRObject mcro = new MCRObject(document);

        String xPath = "/mycoreobject/metadata/element[@class='MCRMetaLangText']/publishedin/@type" ;

        Set<String> parentGenres = new MCRXPathClassificationMappingCondition(xPath).evaluate(mcro);

        assertEquals(Set.of("journal"), parentGenres);

    }

}
