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

package org.mycore.mcr.neo4j.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
                            String node = gson.toJson(nodeToNeo4JNodeJsonRecord(recordData.asNode(), lang));
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
                            String start = gson.toJson(nodeToNeo4JNodeJsonRecord(startNode, lang));
                            String end = gson.toJson(nodeToNeo4JNodeJsonRecord(endNode, lang));
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

    private static Neo4JNodeJsonRecord nodeToNeo4JNodeJsonRecord(Node startNode, String lang) {
        String elementId = startNode.elementId();
        boolean mcridBool = startNode.asMap().containsKey("id");
        List<Neo4JMetaData> neo4JMetaDataList = wrapPropertiesMapTranslation(startNode.asMap(), lang);

        List<String> labels = new ArrayList<>();
        for (String label : startNode.labels()) {
            labels.add(label);
        }
        if (mcridBool) {
            String mcrID = String.valueOf(startNode.asMap().get("id"));
            return new Neo4JNodeJsonRecord(labels, elementId, mcrID, neo4JMetaDataList);
        } else {
            return new Neo4JNodeJsonRecord(labels, elementId, "", neo4JMetaDataList);
        }
    }

    /**
    * Wrapper between MyCore i18n translation logic and (non language specific) neo4j database representation.
    * if lang is null -> use default language or de for translation
    * if key is longer than 3 chars and ends with _xy checks if xy equals lang, if true translate else skip this key
    * else case: translate value with corresponding lang
    *
    * @param map key-value-pairs, translate the values
    * @param lang MyCore Language short notation
    * @return List of translated Neo4JMetaData Objects
    */
    private static List<Neo4JMetaData> wrapPropertiesMapTranslation(Map<String, Object> map, String lang) {
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
