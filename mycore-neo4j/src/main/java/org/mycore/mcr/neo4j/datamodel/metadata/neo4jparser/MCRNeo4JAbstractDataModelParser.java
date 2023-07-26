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

package org.mycore.mcr.neo4j.datamodel.metadata.neo4jparser;

import java.util.List;

import org.jdom2.Element;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.neo4j.datamodel.metadata.neo4jutil.Neo4JNode;
import org.mycore.mcr.neo4j.datamodel.metadata.neo4jutil.Neo4JRelation;

/**
 * @author Andreas Kluge (ai112vezo)
 */
public abstract class MCRNeo4JAbstractDataModelParser {

   public abstract List<Neo4JRelation> parse(Element classElement, MCRObjectID sourceID);

   /**
    * The corresponding DataModel Parser extracts the information within the <rootTag> and return the information as <T>
    * The calling MCRNeo4JMetaParser implementation may require different return types.
    * <p>
    * The implementation of MCRNeo4JParser requires either {@code List<String>} or {@code Map<String, List<String>>}
    *
    * @param rootTag jdom2 Element
    * @return {@code <T>} Handled by the calling MCRNeo4JMetaParser implementation
    */
   public abstract List<Neo4JNode> parse(Element rootTag);
}
