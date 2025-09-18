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

import java.util.List;

import org.jdom2.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.mcr.neo4j.datamodel.metadata.neo4jparser.MCRNeo4JMetaLangTextParser;
import org.mycore.mcr.neo4j.datamodel.metadata.neo4jutil.Neo4JNode;
import org.mycore.test.MCRMetadataExtension;
import org.mycore.test.MyCoReTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@MyCoReTest
@ExtendWith(MCRMetadataExtension.class)
public class MCRMetaLangTextParserTest extends AbstractNeo4JParserTest {
    @Test
    public void testParseWithoutLang() {
        final Element mss01 = metadata.getChild("def.mss01");

        final List<Neo4JNode> result = new MCRNeo4JMetaLangTextParser().parse(mss01);

        assertEquals(1, result.size());
        assertNull(result.getFirst().lang());
        assertEquals("Test 1", result.getFirst().text());
    }

    @Test
    public void testParseWithLang() {
        final Element mss82 = metadata.getChild("def.mss82");

        final List<Neo4JNode> result = new MCRNeo4JMetaLangTextParser().parse(mss82);

        assertEquals(4, result.size());

        assertEquals("de", result.get(0).lang());
        assertEquals("deutscher text 1", result.get(0).text());

        assertEquals("de", result.get(1).lang());
        assertEquals("deutscher text 2", result.get(1).text());

        assertEquals("de", result.get(2).lang());
        assertEquals("deutscher text 3", result.get(2).text());

        assertEquals("en", result.get(3).lang());
        assertEquals("english text", result.get(3).text());
    }
}
