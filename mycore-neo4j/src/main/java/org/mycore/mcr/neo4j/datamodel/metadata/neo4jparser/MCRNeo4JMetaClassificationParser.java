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

package org.mycore.mcr.neo4j.datamodel.metadata.neo4jparser;

import static org.mycore.mcr.neo4j.datamodel.metadata.neo4jutil.MCRNeo4JConstants.NEO4J_PARAMETER_SEPARATOR;
import static org.mycore.mcr.neo4j.datamodel.metadata.neo4jutil.MCRNeo4JUtil.getClassLabel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.jdom2.Element;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.neo4j.datamodel.metadata.neo4jutil.Neo4JNode;
import org.mycore.mcr.neo4j.datamodel.metadata.neo4jutil.Neo4JRelation;

/**
 * Neo4J DataModelParser for Classifications
 * @author Andreas Kluge (ai112vezo)
 * @author Jens Kupferschmidt
 * @author Michael Becker
 */
public class MCRNeo4JMetaClassificationParser extends MCRNeo4JAbstractDataModelParser {
    @Override
    public List<Neo4JRelation> parse(Element classElement, MCRObjectID sourceID) {
        final List<Neo4JRelation> relations = new ArrayList<>(classElement.getChildren().size());
        final String sourceNodeID = sourceID.toString();

        for (Element element : classElement.getChildren()) {
            final String classID = element.getAttributeValue("classid");
            final String categID = element.getAttributeValue("categid");
            final String relationshipType = classID + ":" + categID;

            if (StringUtils.isBlank(classID) || StringUtils.isBlank(categID)) {
                continue;
            }

            final Optional<String> href = getClassLabel(classID, categID, "x-mcrid");
            href.ifPresent(
                targetNodeID -> relations.add(new Neo4JRelation(sourceNodeID, targetNodeID, relationshipType)));
        }

        return relations;
    }

    @Override
    public List<Neo4JNode> parse(Element rootTag) {
        List<Neo4JNode> values = new ArrayList<>();
        for (Element element : rootTag.getChildren()) {
            String classidString = element.getAttributeValue("classid");
            String categidString = element.getAttributeValue("categid");

            values.add(new Neo4JNode(null, classidString + NEO4J_PARAMETER_SEPARATOR + categidString));
        }
        return values;
    }
}
