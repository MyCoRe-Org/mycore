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

package org.mycore.neo4j.proxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.neo4jToJson.Neo4JNodeJsonRecord;
import org.mycore.datamodel.metadata.neo4jToJson.Neo4JPathJsonRecord;
import org.mycore.datamodel.metadata.neo4jToJson.Neo4JRelationShipJsonRecord;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.neo4j.utils.MCRNeo4JDatabaseDriver;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mycore.datamodel.metadata.neo4jutil.MCRNeo4JConstants.DEFAULT_NEO4J_SERVER_URL;
import static org.mycore.datamodel.metadata.neo4jutil.MCRNeo4JConstants.NEO4J_CLASSID_CATEGID_SEPARATOR;
import static org.mycore.datamodel.metadata.neo4jutil.MCRNeo4JConstants.NEO4J_CONFIG_PREFIX;
import static org.mycore.datamodel.metadata.neo4jutil.MCRNeo4JUtil.getClassificationLabel;

/**
 * @author Andreas Kluge (ai112vezo)
 */

public class MCRNeo4JProxyServlet extends MCRServlet {

   private static final long serialVersionUID = 1L;

   static final Logger LOGGER = LogManager.getLogger(MCRNeo4JProxyServlet.class);

   private static final List<String> KEY_LIST = List.of("q", "id", "limit");

   private static final String SERVER_URL = MCRConfiguration2.getStringOrThrow(DEFAULT_NEO4J_SERVER_URL);

   private final ObjectMapper objectMapper = new ObjectMapper();

   public MCRNeo4JProxyServlet() {
      objectMapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
      objectMapper.configure(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED, true);
      objectMapper.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);
      objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

      LOGGER.error(SERVER_URL);
   }

   @Override
   protected void doGetPost(MCRServletJob job) throws Exception {
      final HttpServletRequest request = job.getRequest();
      final HttpServletResponse response = job.getResponse();

      if (request.getParameterMap().keySet().stream().noneMatch(KEY_LIST::contains)) {
         LOGGER.info("No valid query parameter {}", request.getParameterMap().keySet());
         response.sendError(400, "Query parameter \"id\" or \"q\" must be set.");
         return;
      }

      final String currentLanguage = MCRServlet.getSession(request).getCurrentLanguage();
      final String defaultLanguage = MCRConfiguration2.getString("MCR.Metadata.DefaultLang").orElse("de");
      final String language = currentLanguage != null ? currentLanguage : defaultLanguage;
      final String id = request.getParameter("id");
      final String limit = request.getParameter("limit");
      final String q = request.getParameter("q");
      final StringBuilder queryStringBuilder = new StringBuilder();
      String query = q;

      if (id != null) {
         queryStringBuilder.append("MATCH p=({id:\"");
         queryStringBuilder.append(id);
         queryStringBuilder.append("\"})-[*0..1]-(m)");

         if (limit != null) {
            queryStringBuilder.append(" WITH p,m LIMIT ");
            queryStringBuilder.append(limit);
         }
         queryStringBuilder.append(" OPTIONAL MATCH (m)-[r]-() RETURN p,r");
         query = queryStringBuilder.toString();
      }

      MCRNeo4JDatabaseDriver.getInstance().createConnection(SERVER_URL,
            MCRConfiguration2.getStringOrThrow(NEO4J_CONFIG_PREFIX + "user"),
            MCRConfiguration2.getStringOrThrow(NEO4J_CONFIG_PREFIX + "password"));

      final List<Map<String, String>> result = MCRNeo4JDatabaseDriver.commitReadOnlyQuery(query, language);

      if (result == null) {
         LOGGER.info("Result is null");
         response.sendError(500);
         return;
      }

      String finalResult = transformPath(result, language);

      response.setContentType("application/json");
      response.getWriter().write(finalResult);
   }

   private String transformPath(List<Map<String, String>> result, String lang) throws JsonProcessingException {

      Set<Neo4JNodeJsonRecord> nodes = new HashSet<>();
      Set<Neo4JRelationShipJsonRecord> relationShips = new HashSet<>();
      List<String> unprocessed = new LinkedList<>();

      for (Map<String, String> resultMap : result) {
         for (Map.Entry<String, String> entry : resultMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (StringUtils.contains(key, "node_")) {
               try {
                  Neo4JNodeJsonRecord nodeObject =
                        objectMapper.readerFor(Neo4JNodeJsonRecord.class).withRootName("n").readValue(value);
                  nodes.add(nodeObject);
               } catch (JsonProcessingException e) {
                  e.printStackTrace();
               }
            } else if (StringUtils.contains(key, "rel_")) {
               try {
                  Neo4JRelationShipJsonRecord relationShipObject =
                        objectMapper.readerFor(Neo4JRelationShipJsonRecord.class).withRootName("r").readValue(value);
                  relationShips.add(relationShipObject);
               } catch (JsonProcessingException e) {
                  e.printStackTrace();
               }
            } else if (StringUtils.contains(key, "path_")) {
               try {
                  Neo4JPathJsonRecord pathObject = objectMapper.readValue(value, Neo4JPathJsonRecord.class);
                  nodes.addAll(pathObject.nodes());
                  relationShips.addAll(pathObject.relationships());
               } catch (JsonProcessingException e) {
                  e.printStackTrace();
               }
            } else {
               unprocessed.add(value);
            }
         }
      }

      StringBuilder relationsBuilder = new StringBuilder();

      relationsBuilder.append('[');
      for (Neo4JRelationShipJsonRecord relationship : relationShips) {
         //translate relation
         String type = relationship.type();
         try {
            if (type.contains(NEO4J_CLASSID_CATEGID_SEPARATOR)) {
                String[] sep = type.split(NEO4J_CLASSID_CATEGID_SEPARATOR);
                type = getClassificationLabel(sep[0], sep[1], lang);
             }
        } catch (Exception e) {
            System.out.println(relationship);
            e.printStackTrace();
        }

         relationsBuilder.append("{\"from\":\"").append(relationship.startElementId()).append("\",\"to\":\"")
               .append(relationship.endElementId()).append("\",\"type\":\"").append(type).append("\"}");
         relationsBuilder.append(',');
      }
      if (relationsBuilder.length() > 1) {
         relationsBuilder.deleteCharAt(relationsBuilder.length() - 1);
      }
      relationsBuilder.append(']');

      StringBuilder resultBuilder = new StringBuilder();
      String nodesArray = objectMapper.writeValueAsString(nodes);
      if (nodes.size() == 1) {
         resultBuilder.append("{\"nodes\":[").append(nodesArray).append("],");
      } else {
         resultBuilder.append("{\"nodes\":").append(nodesArray).append(',');
      }
      resultBuilder.append("\"relations\":").append(relationsBuilder);
      if (!unprocessed.isEmpty()) {
         resultBuilder.append(",\"unprocessed\":").append(unprocessed);

      }
      resultBuilder.append("}");

      return resultBuilder.toString();

   }
}

