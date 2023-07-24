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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRConstants;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.neo4jutil.Neo4JNode;
import org.mycore.datamodel.metadata.neo4jutil.Neo4JRelation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Andreas Kluge (ai112vezo)
 */
public class MCRNeo4JMetaLinkIDParser extends MCRNeo4JAbstractDataModelParser {

   private static final Logger LOGGER = LogManager.getLogger();

   @Override
   public List<Neo4JRelation> parse(Element classElement, MCRObjectID sourceID) {
      List<Neo4JRelation> relations = new ArrayList<>();

      for (Element element : classElement.getChildren()) {
         String linkType = element.getAttributeValue("type");
         String linkHref = element.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE);
         if (linkHref != null && linkHref.trim().length() > 0) {
            try {
               MCRObjectID.getInstance(linkHref);
               LOGGER.debug("Got MCRObjectID from {}", linkHref);
            } catch (Exception e) {
               LOGGER.warn("The xlink:href is not a MCRObjectID");
               continue;
            }
         }

         if (linkHref == null) {
            LOGGER.error("No relationship target for node {}", sourceID);
            continue;
         }

         if (linkType == null || linkType.trim().length() == 0) {
            LOGGER.warn("Set default link type reference for " + sourceID);
            linkType = "reference";
         }

         relations.add(new Neo4JRelation(sourceID.getTypeId(), linkHref, linkType));
      }
      return relations;
   }

   @Override
   public List<Neo4JNode> parse(Element rootTag) {
      return Collections.emptyList();
   }
}
