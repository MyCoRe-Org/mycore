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

import org.junit.Test;
import org.mycore.datamodel.metadata.neo4jparser.MCRNeo4JMetaHistoryDateParser;
import org.mycore.datamodel.metadata.neo4jutil.Neo4JNode;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class MCRMetaHistoryDateParserTest extends AbstractNeo4JParserTest {
    @Test
    public void testParse() {
        final List<Neo4JNode> nodes = new MCRNeo4JMetaHistoryDateParser().parse(metadata.getChild("def.mss28"));

        assertThat(nodes.size(), is(2));

        assertThat(nodes.get(0).lang(), is("de"));
        assertThat(nodes.get(0).text(), is("12.11.1603"));

        assertThat(nodes.get(1).lang(), is("en"));
        assertThat(nodes.get(1).text(), is("1603-11-12"));
    }
}
