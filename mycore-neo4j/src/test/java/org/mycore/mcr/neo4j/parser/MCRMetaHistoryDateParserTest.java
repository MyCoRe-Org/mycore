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

package org.mycore.mcr.neo4j.parser;

import org.junit.Test;
import org.mycore.mcr.neo4j.datamodel.metadata.neo4jparser.MCRNeo4JMetaHistoryDateParser;
import org.mycore.mcr.neo4j.datamodel.metadata.neo4jutil.Neo4JNode;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class MCRMetaHistoryDateParserTest extends AbstractNeo4JParserTest {
    @Test
    public void testParse() {
        final List<Neo4JNode> nodes = new MCRNeo4JMetaHistoryDateParser().parse(metadata.getChild("def.mss28"));

        assertEquals(2, nodes.size());

        assertEquals("de", nodes.get(0).lang());
        assertEquals("12.11.1603", nodes.get(0).text());

        assertEquals("en", nodes.get(1).lang());
        assertEquals("1603-11-12", nodes.get(1).text());
    }
}
