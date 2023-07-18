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
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.neo4jutil.Neo4JNode;
import org.mycore.datamodel.metadata.neo4jutil.Neo4JRelation;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.mycore.datamodel.metadata.neo4jutil.MCRNeo4JConstants.NEO4J_CONFIG_PREFIX;

/**
 * @author Andreas Kluge (ai112vezo)
 */
public class MCRNeo4JMetaXMLParser extends MCRNeo4JAbstractDataModelParser {

    private final Map<String, MCRNeo4JAbstractDataModelParser> parserMap;

    private static final Logger LOGGER = LogManager.getLogger();

    public MCRNeo4JMetaXMLParser() {
        Map<String, String> propertiesMap = MCRConfiguration2.getSubPropertiesMap(NEO4J_CONFIG_PREFIX + "ParserClass.");
        parserMap = new HashMap<>();
        propertiesMap.forEach((k, v) -> {
            // avoid loop instantiation
            if (!Objects.equals(k, "MCRMetaXML")) {
                parserMap.put(k, MCRConfiguration2.getOrThrow(NEO4J_CONFIG_PREFIX + "ParserClass." + k,
                    MCRConfiguration2::instantiateClass));
            }
        });
    }

    @Override
    public List<Neo4JRelation> parse(Element classElement, MCRObjectID sourceID) {
        return Collections.emptyList();
    }

    @Override
    public List<Neo4JNode> parse(Element rootTag) {
        if (rootTag.getChildren().size() > 0) {
            Element child = rootTag.getChildren().get(0);
            if (child.getChildren().size() > 0) {
                Element grandChild = child.getChildren().get(0);
                String nameTag = grandChild.getName();
                MCRNeo4JAbstractDataModelParser clazz = parserMap.get("MCRMetaXML." + nameTag.toString());
                if (null == clazz) {
                    LOGGER.warn("Parser class for " + "MCRMetaXML." + nameTag.toString() + " not set!");
                } else {
                    List<Neo4JNode> list = clazz.parse(grandChild);
                    return list;
                }
            }
        }
        return Collections.emptyList();
    }
}
