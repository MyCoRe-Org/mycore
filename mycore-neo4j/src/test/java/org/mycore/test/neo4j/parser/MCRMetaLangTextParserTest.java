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

import org.jdom2.Element;
import org.junit.Test;
import org.mycore.datamodel.metadata.neo4jparser.MCRNeo4JMetaLangTextParser;
import org.mycore.datamodel.metadata.neo4jutil.Neo4JNode;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class MCRMetaLangTextParserTest extends AbstractNeo4JParserTest {
    @Test
    public void testParseWithoutLang() {
        final Element mss01 = metadata.getChild("def.mss01");

        final List<Neo4JNode> result = new MCRNeo4JMetaLangTextParser().parse(mss01);

        assertThat(result.size(), is(1));
        assertThat(result.get(0).lang(), is(nullValue()));
        assertThat(result.get(0).text(), is("Test 1"));
    }

    @Test
    public void testParseWithLang() {
        final Element mss82 = metadata.getChild("def.mss82");

        final List<Neo4JNode> result = new MCRNeo4JMetaLangTextParser().parse(mss82);

        assertThat(result.size(), is(4));

        assertThat(result.get(0).lang(), is("de"));
        assertThat(result.get(0).text(), is("deutscher text 1"));

        assertThat(result.get(1).lang(), is("de"));
        assertThat(result.get(1).text(), is("deutscher text 2"));

        assertThat(result.get(2).lang(), is("de"));
        assertThat(result.get(2).text(), is("deutscher text 3"));

        assertThat(result.get(3).lang(), is("en"));
        assertThat(result.get(3).text(), is("english text"));
    }
}
