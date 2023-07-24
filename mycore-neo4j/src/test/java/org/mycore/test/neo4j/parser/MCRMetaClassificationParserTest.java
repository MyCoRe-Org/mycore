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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;

import org.junit.Test;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.neo4jparser.MCRNeo4JMetaClassificationParser;
import org.mycore.datamodel.metadata.neo4jutil.Neo4JNode;
import org.mycore.datamodel.metadata.neo4jutil.Neo4JRelation;

public class MCRMetaClassificationParserTest extends AbstractNeo4JParserTest {
    @Test
    public void testCreateClassificationNodes() {
        final List<Neo4JNode> nodes = new MCRNeo4JMetaClassificationParser().parse(metadata.getChild("def.class_test"));

        assertThat(nodes.size(), is(2));

        assertThat(nodes.get(0).lang(), is(nullValue()));
        assertThat(nodes.get(0).text(), is("classification_id_-_category_1"));

        assertThat(nodes.get(1).lang(), is(nullValue()));
        assertThat(nodes.get(1).text(), is("classification_id_-_category_2"));
    }

    @Test
    public void testCreateNoRelationsForUnlinkedCategories() {
        final List<Neo4JRelation> relations = new MCRNeo4JMetaClassificationParser().parse(
            metadata.getChild("def.unlinked_class"),
            MCRObjectID.getInstance("a_mcrobject_00000001"));

        assertThat(relations.size(), is(0));
    }

    @Test
    public void testCreateRelationsForLinkedCategories() throws Exception {
        addClassification("/TestOwner.xml");

        final List<Neo4JRelation> relations = new MCRNeo4JMetaClassificationParser().parse(
            metadata.getChild("def.linked_class"),
            MCRObjectID.getInstance("a_mcrobject_00000001"));

        assertThat(relations.size(), is(2));

        assertThat(relations.get(0).sourceNodeID(), is("a_mcrobject_00000001"));
        assertThat(relations.get(0).targetNodeID(), is("a_mcrobject_00000002"));
        assertThat(relations.get(0).relationshipType(), is("MyMssOwner:owner_de0"));

        assertThat(relations.get(1).sourceNodeID(), is("a_mcrobject_00000001"));
        assertThat(relations.get(1).targetNodeID(), is("a_mcrobject_12345678"));
        assertThat(relations.get(1).relationshipType(), is("MyMssOwner:owner_de1"));
    }
}
