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
