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

package org.mycore.ocfl.metadata.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.common.MCRMetadataVersionType;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.ocfl.metadata.migration.MCROCFLMigration.ContentSupplier;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true")
})
public class MCROCFLCombineIgnoreXPathPrunerTest {

    public static final String JUNIT_TEST_00000001 = "junit_test_00000001";
    public static final String AUTOR_1 = "Sebastian";
    public static final String AUTOR_2 = "Hans";
    public static final String AUTOR_3 = "Peter";
    public static final ContentSupplier CONTENT_1 = () -> {
        Element test = new Element("test");
        Document testDoc = new Document(test);
        test.addContent(new Element("a").setText("1"));
        test.addContent(new Element("b").setText("2"));
        test.addContent(new Element("c").setText("3"));
        return new MCRJDOMContent(testDoc);
    };
    public static final ContentSupplier CONTENT_2 = () -> {
        Element test = new Element("test");
        Document testDoc = new Document(test);
        test.addContent(new Element("a").setText("4"));
        test.addContent(new Element("b").setText("5"));
        test.addContent(new Element("c").setText("3"));
        return new MCRJDOMContent(testDoc);
    };
    public static final ContentSupplier CONTENT_3 = () -> {
        Element test = new Element("test");
        Document testDoc = new Document(test);
        test.addContent(new Element("a").setText("6"));
        test.addContent(new Element("b").setText("7"));
        test.addContent(new Element("c").setText("4"));
        return new MCRJDOMContent(testDoc);
    };
    private static final Logger LOGGER = LogManager.getLogger();

    @Test
    public void testPrune() throws IOException, JDOMException {

        MCROCFLCombineIgnoreXPathPruner xPathPruner = new MCROCFLCombineIgnoreXPathPruner();
        xPathPruner.setXpath("/test/a|/test/b");
        xPathPruner.setFirstAuthorWins(true);
        xPathPruner.setFirstDateWins(true);

        MCRObjectID objectID = MCRObjectID.getInstance(JUNIT_TEST_00000001);
        Date baseDate = new Date();

        MCROCFLRevision r1 =
            new MCROCFLRevision(MCRMetadataVersionType.CREATED, CONTENT_1, AUTOR_1, baseDate, objectID);

        MCROCFLRevision r2 = new MCROCFLRevision(MCRMetadataVersionType.MODIFIED, CONTENT_2, AUTOR_2,
            new Date(baseDate.getTime() + 1000), objectID);

        MCROCFLRevision r3 = new MCROCFLRevision(MCRMetadataVersionType.MODIFIED, CONTENT_3, AUTOR_3,
            new Date(baseDate.getTime() + 2000), objectID);

        MCROCFLRevision r4 =
            new MCROCFLRevision(MCRMetadataVersionType.DELETED, null, AUTOR_3, new Date(baseDate.getTime() + 3000),
                objectID);

        MCROCFLRevision r5 = new MCROCFLRevision(MCRMetadataVersionType.CREATED, CONTENT_1, AUTOR_1,
            new Date(baseDate.getTime() + 4000), objectID);

        MCROCFLRevision r6 = new MCROCFLRevision(MCRMetadataVersionType.MODIFIED, CONTENT_2, AUTOR_2,
            new Date(baseDate.getTime() + 5000), objectID);

        MCROCFLRevision r7 = new MCROCFLRevision(MCRMetadataVersionType.MODIFIED, CONTENT_1, AUTOR_3,
            new Date(baseDate.getTime() + 6000), objectID);

        List<MCROCFLRevision> prune = xPathPruner.prune(List.of(r1, r2, r3, r4, r5, r6, r7));

        assertEquals(4, prune.size(), "r2, r6 and r7 should be pruned away");
        assertEquals(AUTOR_1, prune.get(0).user(), "r1 autor should be original r1 autor");
        assertEquals(AUTOR_3, prune.get(1).user(), "r2 autor should be original r3 autor");
        assertEquals(baseDate, prune.get(0).date(), "r1 date should be original r1 date");
        assertEquals(new Date(baseDate.getTime() + 2000), prune.get(1).date(), "r2 date should be original r3 date");
        assertTrue(MCRXMLHelper.deepEqual(prune.get(0).contentSupplier().get().asXML(), CONTENT_2.get().asXML()),
                "r1 content should be original r2 content");
        assertTrue(MCRXMLHelper.deepEqual(prune.get(1).contentSupplier().get().asXML(), CONTENT_3.get().asXML()),
                "r2 content should be original r3 content");

        LOGGER.info("First Result: " + prune.stream().map(MCROCFLRevision::toString).collect(Collectors.joining("\n")));

        xPathPruner.setFirstDateWins(false);
        xPathPruner.setFirstAuthorWins(false);
        prune = xPathPruner.prune(List.of(r1, r2, r3, r4, r5, r6, r7));
        assertEquals(4, prune.size(), "r2, r6 and r7 should be pruned away");
        assertEquals(AUTOR_2, prune.get(0).user(), "r1 autor should be original r2 autor");
        assertEquals(AUTOR_3, prune.get(1).user(), "r2 autor should be original r3 autor");
        assertEquals(new Date(baseDate.getTime() + 1000), prune.get(0).date(), "r1 date should be original r2 date");
        assertEquals(new Date(baseDate.getTime() + 2000), prune.get(1).date(), "r2 date should be original r3 date");
        assertTrue(MCRXMLHelper.deepEqual(prune.get(0).contentSupplier().get().asXML(), CONTENT_2.get().asXML()),
                "r1 content should be original r2 content");
        assertTrue(MCRXMLHelper.deepEqual(prune.get(1).contentSupplier().get().asXML(), CONTENT_3.get().asXML()),
                "r2 content should be original r3 content");

        LOGGER
            .info("Second Result: " + prune.stream().map(MCROCFLRevision::toString).collect(Collectors.joining("\n")));

    }

}
