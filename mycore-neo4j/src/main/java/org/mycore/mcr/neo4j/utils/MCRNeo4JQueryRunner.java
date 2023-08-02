package org.mycore.mcr.neo4j.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.mcr.neo4j.datamodel.metadata.neo4jtojson.Neo4JMetaData;
import org.mycore.mcr.neo4j.datamodel.metadata.neo4jtojson.Neo4JNodeJsonRecord;
import org.mycore.mcr.neo4j.index.MCRNeo4JIndexEventHandler;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Query;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mycore.mcr.neo4j.datamodel.metadata.neo4jutil.MCRNeo4JConstants.NEO4J_PARAMETER_SEPARATOR;
import static org.mycore.mcr.neo4j.datamodel.metadata.neo4jutil.MCRNeo4JUtil.getClassificationLabel;

public class MCRNeo4JQueryRunner {

   private static final Logger LOGGER = LogManager.getLogger(MCRNeo4JIndexEventHandler.class);

   private static final String LANGUAGE = MCRConfiguration2.getString("MCR.Metadata.DefaultLang").orElse("de");

   /**
    * Executes a write-only query in Neo4j using the provided query string. This function is responsible for executing
    * queries that modify the Neo4j database.
    *
    * @param queryString the query string to be executed
    */
   public static void commitWriteOnlyQuery(String queryString) {
      Driver driver = MCRNeo4JDatabaseDriver.getInstance().getDriver();
      try {
         driver.verifyConnectivity();
      } catch (Exception e) {
         LOGGER.error("Neo4J connection failed.", e.getCause());
         return;
      }
      try (Session session = driver.session()) {
         session.executeWrite(tx -> {
            Query query = new Query(queryString);
            return tx.run(query);
         });
      }
   }

   /**
    * Executes a read-only query in Neo4j using the provided query string and returns the result as a list of JSON
    * strings. This function is responsible for executing queries that retrieve data from the Neo4j database.
    *
    * @param queryString the query string to be executed
    * @param lang        the language for parsing
    * @return a list of a Map with keys containing the type_recordKey and as value the JSON strings representing the
    * query result for the given key
    */
   public static List<Map<String, String>> commitReadOnlyQuery(String queryString, String lang) {
      Driver driver = MCRNeo4JDatabaseDriver.getInstance().getDriver();
      Gson gson = new GsonBuilder().disableHtmlEscaping().create();
      AtomicInteger counter = new AtomicInteger();
      try (Session session = driver.session()) {
         return session.executeRead(tx -> {
            List<Map<String, String>> records = new ArrayList<>();
            Result result = tx.run(queryString);
            while (result.hasNext()) {
               Record thisRecord = result.next();
               Map<String, String> keyMap = new HashMap<>();

               for (String key : thisRecord.keys()) {
                  Value recordData = thisRecord.get(key);
                  counter.getAndAdd(1);
                  if (StringUtils.equals(recordData.type().name(), "NODE")) {
                     String node = nodeToJson(recordData.asNode(), gson, lang);
                     String nodeSB = "{\"n\":" + node + "}";
                     LOGGER.debug("record is Node");
                     keyMap.put("node_" + key, nodeSB);

                  } else if (StringUtils.equals(recordData.type().name(), "RELATIONSHIP")) {
                     LOGGER.debug("record is Relationship");
                     keyMap.put("rel_" + key, "{\"r\":" + gson.toJson(recordData.asRelationship()) + "}");
                  } else if (StringUtils.equals(recordData.type().name(), "PATH")) {
                     StringBuilder pathSB = new StringBuilder();
                     LOGGER.debug("record is Path");
                     //Gather Stuff
                     Path neo4jPath = recordData.asPath();
                     Node startNode = neo4jPath.start();
                     Node endNode = neo4jPath.end();
                     Iterable<Relationship> relationships = neo4jPath.relationships();

                     // Parse Stuff to use full Json
                     String start = nodeToJson(startNode, gson, lang);
                     String end = nodeToJson(endNode, gson, lang);
                     String relationshipJson = gson.toJson(relationships);

                     pathSB.append("{\"p\":{\"nodes\":[");
                     pathSB.append(start).append(',');

                     pathSB.append(end).append("],");
                     pathSB.append("\"relationships\":").append(relationshipJson);

                     pathSB.append("}}");

                     keyMap.put("path_" + key, pathSB.toString());
                  } else if (StringUtils.equals(recordData.type().name(), "NULL")) {
                     LOGGER.warn("Got record of type {} for key {} which is not parsed and is ignored",
                           recordData.type().name(), key);
                  } else {
                     LOGGER.warn("Got record of type {} for key {} which is not parsed",
                           recordData.type().name(), key);
                     keyMap.put(key, gson.toJson(thisRecord.asMap()));
                  }
               }
               records.add(keyMap);

            }
            return records;
         });
      }

   }

   private static String nodeToJson(Node startNode, Gson gson, String lang) {
      String elementId = startNode.elementId();
      boolean mcridBool = startNode.asMap().containsKey("id");

      List<Neo4JMetaData> neo4JMetaDataList = propertiesMapToJson(startNode.asMap(), lang);

      JsonObject jsonNode = new JsonObject();
      JsonArray labelArray = new JsonArray();
      for (String label: startNode.labels()){
         labelArray.add(label);
      }
      jsonNode.add("type", labelArray);
      jsonNode.addProperty( "id", elementId);
      if (mcridBool) {
         String mcrID = String.valueOf(startNode.asMap().get("id"));
         LOGGER.debug("MCRID: {}", mcrID);
         jsonNode.addProperty("mcrid", mcrID);
      }
      JsonArray jsonArray = new JsonArray();
      for (Neo4JMetaData metaData: neo4JMetaDataList) {
         JsonObject jsO = new JsonObject();
         jsO.addProperty("title", metaData.title());
         JsonArray innerArray = new JsonArray();
         for (String contentString: metaData.content()) {
            innerArray.add(contentString);
         }
         jsO.add("content", innerArray);
         jsonArray.add(jsO);
      }

      jsonNode.add("metadata", jsonArray);

      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
      objectMapper.configure(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED, true);
      objectMapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);

      try {
         Neo4JNodeJsonRecord node = objectMapper.readValue(gson.toJson(jsonNode), Neo4JNodeJsonRecord.class);
         return objectMapper.writeValueAsString(node);
      } catch (JsonProcessingException e) {
         LOGGER.error(e.getLocalizedMessage());
      }

      return null;
   }

   private static List<Neo4JMetaData> propertiesMapToJson(Map<String, Object> map, String lang) {
      List<Neo4JMetaData> metaDataList = new ArrayList<>();

      for (Map.Entry<String, Object> entry : map.entrySet()) {
         String key = entry.getKey();
         Object value = entry.getValue();

         if (lang == null) {
            translateAndMapProperties(metaDataList, key, value, LANGUAGE);
         } else {
            if (key.length() > 3 && key.charAt(key.length() - 3) == '_') {
               if (key.substring(key.length() - 2).equals(lang)) {
                  translateAndMapProperties(metaDataList, key, value, lang);
               }
            } else {
               translateAndMapProperties(metaDataList, key, value, lang);
            }
         }
      }

      return metaDataList;
   }

   private static void translateAndMapProperties(List<Neo4JMetaData> metaDataList, String key, Object value,
         String lang) {
      if (value instanceof List<?>) {
         List<String> stringList = ((List<?>) value).stream().map(Object::toString).toList();

         if (stringList.size() == 1) {
            if (stringList.get(0).contains(NEO4J_PARAMETER_SEPARATOR)) {
               String[] sep = stringList.get(0).split(NEO4J_PARAMETER_SEPARATOR);
               String classification = getClassificationLabel(sep[0], sep[1], lang);
               metaDataList.add(new Neo4JMetaData(key, List.of(classification)));
            }
         } else {
            metaDataList.add(new Neo4JMetaData(key, stringList));
         }
      } else if (value instanceof String) {
         if (((String) value).contains(NEO4J_PARAMETER_SEPARATOR)) {
            String[] sep = ((String) value).split(NEO4J_PARAMETER_SEPARATOR);
            String classification = getClassificationLabel(sep[0], sep[1], lang);
            metaDataList.add(new Neo4JMetaData(key, List.of(classification)));
         } else {
            metaDataList.add(new Neo4JMetaData(key, List.of(value.toString())));
         }
      } else {
         LOGGER.info("Value ELSE: {}", value);
      }
   }
}
