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

package org.mycore.test.neo4j.parser;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.language.MCRLanguageFactory;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.neo4jparser.MCRNeo4JParser;

public class MCRNeo4JParserTest extends AbstractNeo4JParserTest {
    @BeforeClass
    public static void beforeClass() {
        System.setProperty("log4j.configurationFile", "log4j2-test.xml");
    }

    @Test
    public void testParseMCRObject() throws Exception {
        MCRLanguageFactory.instance().getLanguage("xx");
        MCRConfiguration2.set("MCR.Metadata.Type.work", "true");
        MCRConfiguration2.set("MCR.Metadata.Type.manuscript", "true");

        MCRConfiguration2.set("MCR.Neo4J.NodeAttribute.manuscript.descriptor",
            "/mycoreobject/metadata/def.mss82");
        MCRConfiguration2.set("MCR.Neo4J.NodeAttribute.manuscript.signature",
            "/mycoreobject/metadata/def.mss02");
        MCRConfiguration2.set("MCR.Neo4J.ParserClass.MCRMetaLangText",
            "org.mycore.datamodel.metadata.neo4jparser.MCRNeo4JMetaLangTextParser");
        MCRConfiguration2.set("MCR.Neo4J.ParserClass.MCRMetaClassification",
            "org.mycore.datamodel.metadata.neo4jparser.MCRNeo4JMetaClassificationParser");
        MCRConfiguration2.set("MCR.Neo4J.ParserClass.MCRMetaHistoryDate",
            "org.mycore.datamodel.metadata.neo4jparser.MCRNeo4JMetaHistoryDateParser");

        final MCRNeo4JParser parser = new MCRNeo4JParser();
        final MCRObject manuscript = new MCRObject(read("/mcrobjects/a_mcrobject_00000001.xml"));
        final String result = parser.createNeo4JQuery(manuscript);
        System.out.println(result);
        assertNotNull(result);
    }

    protected Document read(String file) throws IOException, JDOMException {
        return (new SAXBuilder()).build(this.getClass().getResourceAsStream(file));
    }
}
