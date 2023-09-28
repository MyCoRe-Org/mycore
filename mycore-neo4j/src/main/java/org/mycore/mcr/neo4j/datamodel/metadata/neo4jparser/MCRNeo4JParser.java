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

import static org.mycore.mcr.neo4j.datamodel.metadata.neo4jutil.MCRNeo4JConstants.NEO4J_CONFIG_PREFIX;
import static org.mycore.mcr.neo4j.datamodel.metadata.neo4jutil.MCRNeo4JUtil.getMCRNeo4JInstantiatedParserMap;
import static org.mycore.mcr.neo4j.utils.MCRNeo4JUtilsConfigurationHelper.getConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.neo4j.datamodel.metadata.neo4jutil.Neo4JNode;
import org.mycore.mcr.neo4j.datamodel.metadata.neo4jutil.Neo4JRelation;

/**
 * Parser Class for Neo4J
 * @author Andreas Kluge (ai112vezo)
 */
public class MCRNeo4JParser implements MCRNeo4JMetaParser {

   private static final Logger LOGGER = LogManager.getLogger(MCRNeo4JParser.class);
   private static final String LANG_UNSET = "__unset__";
   private final Map<String, MCRNeo4JAbstractDataModelParser> parserMap;

   private static final String CLASS_KEY = "BaseParser";

   public MCRNeo4JParser() {
      Map<String, String> propertiesMap = MCRConfiguration2.getSubPropertiesMap(NEO4J_CONFIG_PREFIX + "ParserClass.");
      parserMap = getMCRNeo4JInstantiatedParserMap(propertiesMap, CLASS_KEY);
   }

   private String parseSourceNodeInformation(MCRObject mcrObject, XPathFactory xpf,
      Map<String, String> attributes, Document xml) {

      // "Set a:<type>, a:id='Wert', a.name='MYNAME', a.descriptor='desc'"
      StringBuilder sbNode = new StringBuilder();

      MCRObjectID id = mcrObject.getId();

      sbNode.append("    SET a:").append(id.getTypeId()).append(" , a.id='").append(id).append('\'');

      attributes.forEach((k, v) -> LOGGER.debug("Neo4J property configuration {} {}", k, v));
      parserMap.forEach((k, v) -> LOGGER.debug("Neo4J configured Parser {} {}", k, v));

      attributes.forEach((k, v) -> {
         if (k.startsWith("link") && v.length() > 0) {
            return;
         }

         XPathExpression<Element> xpath = xpf.compile(v, Filters.element(), null);
         List<Element> elements = xpath.evaluate(xml);
         if (elements.isEmpty()) {
            LOGGER.warn("No entries for path {}", v);
            return;
         }

         for (Element parent : elements) {
            LOGGER.debug("current Element: {}", parent);
            Attribute classAttribute = parent.getAttribute("class");
            if (classAttribute == null) {
               LOGGER.error("Parent of current Element: {}", parent.getParent());
               LOGGER.error("NULL Class printing current Element {}", parent);
               LOGGER.error("Parser Attributes {}", parent.getAttributes());
               // TODO: return (no crash/error message) or no return and let it crash
               return;
            }

            LOGGER.debug("parse: {}", classAttribute);
            MCRNeo4JAbstractDataModelParser clazz = parserMap.get(classAttribute.getValue());

            if (null == clazz) {
               throw new MCRException("Parser class for " + classAttribute.getValue() + " not set!");
            }

            for (Element elm : elements) {
               final List<Neo4JNode> nodes = clazz.parse(elm);
               final Map<String, List<String>> langNodes = new HashMap<>(nodes.size());

               // add nodes with a language to their respective key
               nodes.stream()
                  .filter(node -> StringUtils.isNotBlank(node.lang()))
                  .forEach(
                     node -> langNodes.computeIfAbsent(node.lang(), val -> new ArrayList<>()).add(node.text()));

               // add nodes without a language to the unset language key
               nodes.stream()
                  .filter(node -> StringUtils.isBlank(node.lang()))
                  .forEach(node -> langNodes.computeIfAbsent(LANG_UNSET, val -> new ArrayList<>()).add(node.text()));

               for (Map.Entry<String, List<String>> entries : langNodes.entrySet()) {
                  final String lang = entries.getKey();
                  final List<String> text = entries.getValue();
                  final String key;
                  if (StringUtils.equals(lang, LANG_UNSET)) {
                     key = k;
                  } else {
                     key = k + "_" + lang;
                  }

                  final String cleaned = text.stream()
                     .map(value -> StringUtils.replace(value, "'", ""))
                     .collect(Collectors.joining("', '", "['", "']"));
                  sbNode.append(", a.").append(key).append('=').append(cleaned);
               }
            }

         }
      });

      sbNode.append('\n');
      return sbNode.toString();
   }

   public String createNeo4JQuery(MCRObject mcrObject) {

      MCRObjectID id = mcrObject.getId();

      String type = id.getTypeId();

      XPathFactory xpf = XPathFactory.instance();
      Map<String, String> attributes = getConfiguration(type);
      Document xml = mcrObject.createXML();

      String sourceNodeInformation = parseSourceNodeInformation(mcrObject, xpf, attributes, xml);

      StringBuilder sbQuery = new StringBuilder();
      // CHECK if Node is Already existing
      sbQuery.append("MERGE (a {id: '").append(id).append("'})\n");
      // IF not found, Create it
      sbQuery.append("ON CREATE\n");
      sbQuery.append(sourceNodeInformation);
      // IF already exists though created by Relation Target
      sbQuery.append("ON Match\n").append(sourceNodeInformation)
         .append(", a.temp = ''\n")
         .append("WITH *\n")
         .append("CALL { WITH a\n").append("  WITH a WHERE a.temp IS NOT NULL\n").append("  REMOVE a.temp\n")
         .append("  REMOVE a:AutoGenerated}\n");

      attributes.forEach((k, v) -> {
         if (k.startsWith("link") && v.length() > 0) {
            XPathExpression<Element> xpath = xpf.compile(v, Filters.element(), null);
            List<Element> elms = xpath.evaluate(xml);
            if (elms.isEmpty()) {
               LOGGER.warn("No entries for path {}", v);
               return;
            }

            for (Element parent : elms) {
               LOGGER.info("current Element: {}", parent);
               Attribute classAttribute = parent.getAttribute("class");
               LOGGER.info("parse: {}", classAttribute.getValue());

               MCRNeo4JAbstractDataModelParser clazz = parserMap.get(classAttribute.getValue());
               for (Element elm : elms) {
                  // should only one elm in elms
                  List<Neo4JRelation> relations = clazz.parse(elm, id);
                  relations.stream().forEach((relation) -> {
                     sbQuery.append(relation.toAppendQuery());
                     sbQuery.append('\n');
                  });
               }
            }
         }
      });

      return sbQuery.toString();
   }

   @Override
   public String createNeo4JUpdateQuery(MCRObject mcrObject) {
      MCRObjectID id = mcrObject.getId();

      StringBuilder queryBuilder = new StringBuilder();
      // remove outgoing Relationships and reset all properties besides node id and mcrid
      queryBuilder.append("MATCH (n {id:'").append(id).append("'}) \n");
      queryBuilder.append("OPTIONAL MATCH (n)-[r]->() DELETE r \n");
      queryBuilder.append("SET n = {id: '").append(id).append("'} \n");
      return queryBuilder.toString();
   }
}
