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
