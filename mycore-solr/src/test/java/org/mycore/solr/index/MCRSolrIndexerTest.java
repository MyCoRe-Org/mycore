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

package org.mycore.solr.index;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.util.Date;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.test.MCRMetadataExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRMetadataExtension.class)
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.derivate", string = "true"),
    @MCRTestProperty(key = "MCR.Metadata.Type.document", string = "true")
})
public class MCRSolrIndexerTest {

    @Test
    public void retrieveIndexContentKeepsDerivateData() throws Exception {
        MCRObjectID derivateId = MCRObjectID.getInstance("test_derivate_00000001");
        try (InputStream is = getClass().getResourceAsStream("/MCRSolrIndexerTest/test_derivate_00000001.xml")) {
            Document xml = new SAXBuilder().build(is);
            MCRXMLMetadataManager.getInstance().create(derivateId, xml, new Date());
        }

        Element root = MCRSolrIndexer.retrieveIndexContent(derivateId).asXML().getRootElement();

        assertEquals("mycorederivate", root.getName());
        assertEquals("test_document_00000001",
            root.getChild("derivate").getChild("linkmetas").getChild("linkmeta")
                .getAttributeValue("href", MCRConstants.XLINK_NAMESPACE));
        assertEquals("document.pdf",
            root.getChild("derivate").getChild("internals").getChild("internal").getAttributeValue("maindoc"));
    }
}
