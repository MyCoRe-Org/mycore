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

import org.apache.commons.lang3.StringUtils;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.neo4jutil.Neo4JNode;
import org.mycore.datamodel.metadata.neo4jutil.Neo4JRelation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Andreas Kluge (ai112vezo)
 */
public class MCRNeo4JMetaLangTextParser extends MCRNeo4JAbstractDataModelParser {

   @Override
   public List<Neo4JRelation> parse(Element classElement, MCRObjectID sourceID) {
      return Collections.emptyList();
   }

   @Override
   public List<Neo4JNode> parse(Element rootTag) {
      final List<Neo4JNode> nodes = new ArrayList<>();

      for (Element element : rootTag.getChildren()) {
         if (element != null) {
            final String lang = element.getAttributeValue("lang", Namespace.XML_NAMESPACE);
            final String text = StringUtils.replace(element.getTextTrim(), "'", "");
            nodes.add(new Neo4JNode(lang, text));
         }
      }
      return nodes;
   }
}
