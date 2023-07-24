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

package org.mycore.datamodel.metadata.neo4jparser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jdom2.Element;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.neo4jutil.Neo4JNode;
import org.mycore.datamodel.metadata.neo4jutil.Neo4JRelation;

/**
 * @author Jens Kupferschmidt
 */
public class MCRNeo4JMetaISO8601DateParser extends MCRNeo4JAbstractDataModelParser {

    static SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd");
    static SimpleDateFormat deFormat = new SimpleDateFormat("dd.MM.yyyy");
    static SimpleDateFormat enFormat = new SimpleDateFormat("dd-MM-yyyy");

    @Override
    public List<Neo4JRelation> parse(Element classElement, MCRObjectID sourceID) {
        return Collections.emptyList();
    }

    @Override
    public List<Neo4JNode> parse(Element rootTag) {
        List<Neo4JNode> values = new ArrayList<>();
        for (Element element : rootTag.getChildren()) {
            final String text = element.getTextTrim();
            if (text != null && text.length() > 0) {
                try {
                    final String formattedDe = deFormat.format(isoFormat.parse(text));
                    values.add(new Neo4JNode("de", formattedDe));
                    final String formattedEn = enFormat.format(isoFormat.parse(text));
                    values.add(new Neo4JNode("en", formattedEn));
                } catch (ParseException e) {
                    values.add(new Neo4JNode("de", text));
                    values.add(new Neo4JNode("en", text));
                }
            }
        }
        return values;
    }

}
