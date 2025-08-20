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

package org.mycore.mcr.neo4j.parser;

import java.io.IOException;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.mycore.common.MCRException;
import org.mycore.common.MCRStoreTestCase;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;
import org.mycore.datamodel.metadata.MCRObjectMetadata;

@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.mcrobject", string = "true"),
    @MCRTestProperty(key = "MCR.Metadata.Type.derivate", string = "true"),
    @MCRTestProperty(key = "MCR.Metadata.ObjectID.NumberPattern", string = "00000000")
})
public abstract class AbstractNeo4JParserTest {
    protected final Document doc;

    protected final Element metadata;

    public AbstractNeo4JParserTest() {
        try {
            doc = new SAXBuilder().build(getClass().getResourceAsStream("/mcrobjects/a_mcrobject_00000001.xml"));
        } catch (JDOMException | IOException e) {
            throw new MCRException("Error while loading Resource:", e);
        }
        metadata = doc.getRootElement().getChild(MCRObjectMetadata.XML_NAME);
    }

    protected void addClassification(String file) throws Exception {
        Document classification = (new SAXBuilder()).build(this.getClass().getResourceAsStream(file));
        MCRCategory category = MCRXMLTransformer.getCategory(classification);
        MCRCategoryDAOFactory.obtainInstance().addCategory((MCRCategoryID) null, category);
    }
}
