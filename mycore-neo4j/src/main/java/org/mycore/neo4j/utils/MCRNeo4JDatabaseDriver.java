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

package org.mycore.neo4j.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.neo4jToJson.Neo4JMetaData;
import org.mycore.datamodel.metadata.neo4jToJson.Neo4JNodeJsonRecord;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mycore.datamodel.metadata.neo4jutil.MCRNeo4JConstants.NEO4J_CONFIG_PREFIX;
import static org.mycore.datamodel.metadata.neo4jutil.MCRNeo4JConstants.NEO4J_PARAMETER_SEPARATOR;
import static org.mycore.datamodel.metadata.neo4jutil.MCRNeo4JUtil.getClassificationLabel;

/**
 * The MCRNeo4JDatabaseDriver class is a Java driver implementation for connecting to a Neo4j database. It provides
 * methods to establish a connection, test connection settings, and execute queries.
 *
 * @author Andreas Kluge
 */
public class MCRNeo4JDatabaseDriver {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String LANGUAGE = MCRConfiguration2.getString("MCR.Metadata.DefaultLang").orElse("de");

    private static MCRNeo4JDatabaseDriver instance = null;

    private final String url;

    private String user;

    private String password;

    private Driver driver;

    /**
    * Constructs an MCRNeo4JDatabaseDriver object with connection settings loaded from a configuration file.
    */
    public MCRNeo4JDatabaseDriver() {
        this.url = MCRConfiguration2.getString(NEO4J_CONFIG_PREFIX + "ServerURL").orElse("");
        this.user = MCRConfiguration2.getString(NEO4J_CONFIG_PREFIX + "user").orElse("");
        this.password = MCRConfiguration2.getString(NEO4J_CONFIG_PREFIX + "password").orElse("");
    }

    /**
    * Constructs an MCRNeo4JDatabaseDriver object with the specified URL.
    *
    * @param url the URL of the Neo4j database
    */
    @SuppressWarnings("unused")
    public MCRNeo4JDatabaseDriver(String url) {
        this.url = url;
    }

    /**
    * Constructs an MCRNeo4JDatabaseDriver object with the specified URL, username, and password. It also creates a
    * connection to the database.
    *
    * @param url      the URL of the Neo4j database
    * @param user     the username for the database connection
    * @param password the password for the database connection
    */
    @SuppressWarnings("unused")
    public MCRNeo4JDatabaseDriver(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
        this.createConnection();
    }

    /**
    * Returns an instance of MCRNeo4JDatabaseDriver. If no instance exists, a new one is created.
    *
    * @return the MCRNeo4JDatabaseDriver instance
    */
    public static MCRNeo4JDatabaseDriver getInstance() {
        if (instance == null) {
            instance = new MCRNeo4JDatabaseDriver();
        }

        return instance;
    }

    /**
    * Executes a write-only query in Neo4j using the provided query string. This function is responsible for executing
    * queries that modify the Neo4j database.
    *
    * @param queryString the query string to be executed
    */
    public static void commitWriteOnlyQuery(String queryString) {
        //Driver driver = getConnection();
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
        driver.session().close();
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
                    Record record = result.next();
                    Map<String, String> keyMap = new HashMap<>();

                    for (String key : record.keys()) {
                        Value recordData = record.get(key);
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
                            pathSB.append(start).append(",");

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
                            keyMap.put(key, gson.toJson(record.asMap()));
                        }
                    }
                    records.add(keyMap);

                }
                return records;
            });
        }

    }

    private static String nodeToJson(Node startNode, Gson gson, String lang) {
        StringBuilder sb = new StringBuilder();

        String labels = gson.toJson(startNode.labels());
        // Long id = startNode.id();
        String elementId = startNode.elementId();
        boolean mcridBool = startNode.asMap().containsKey("id");

        List<Neo4JMetaData> neo4JMetaDataList = propertiesMapToJson(startNode.asMap(), lang);
        String neo4JMetaDataListString = gson.toJson(neo4JMetaDataList);

        sb.append("{");
        sb.append("\"type\":").append(labels).append(",");
        sb.append("\"id\":\"").append(elementId).append("\",");
        //sb.append("\"elementId\":\"").append(elementId).append("\",");
        if (mcridBool) {
            String mcrID = String.valueOf(startNode.asMap().get("id"));
            LOGGER.debug("MCRID: " + mcrID);
            sb.append("\"mcrid\":\"").append(mcrID).append("\",");
        }
        sb.append("\"metadata\":").append(neo4JMetaDataListString);
        sb.append("}");

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        objectMapper.configure(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED, true);
        objectMapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);

        try {
            Neo4JNodeJsonRecord node = objectMapper.readValue(sb.toString(), Neo4JNodeJsonRecord.class);
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            LOGGER.error(e.getLocalizedMessage());
        }

        return null;
    }

    private static List<Neo4JMetaData> propertiesMapToJson(Map<String, Object> map, String lang) {
        List<Neo4JMetaData> metaDataList = new LinkedList<>();

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
            // System.out.println("Value List: " + stringList);

            if (stringList.size() == 1) {
                if (stringList.get(0).contains(NEO4J_PARAMETER_SEPARATOR)) {
                    String[] sep = stringList.get(0).split(NEO4J_PARAMETER_SEPARATOR);
                    // System.out.println("Class: " + sep[0] + " Cate: " + sep[1]);
                    String classification = getClassificationLabel(sep[0], sep[1], lang);
                    metaDataList.add(new Neo4JMetaData(key, List.of(classification)));
                }
            } else {
                metaDataList.add(new Neo4JMetaData(key, stringList));
            }
        } else if (value instanceof String) {
            // System.out.println("Value String: " + value);
            if (((String) value).contains(NEO4J_PARAMETER_SEPARATOR)) {
                String[] sep = ((String) value).split(NEO4J_PARAMETER_SEPARATOR);
                // System.out.println("Class: " + sep[0] + " Cate: " + sep[0]);
                String classification = getClassificationLabel(sep[0], sep[1], lang);
                metaDataList.add(new Neo4JMetaData(key, List.of(classification)));
            } else {
                metaDataList.add(new Neo4JMetaData(key, List.of(value.toString())));
            }
        } else {
            System.out.println("Value ELSE: " + value);
        }
    }

    /**
    * Tests the connection settings by checking if the URL, username, and password are set, if the driver is
    * initialized and submits a test query.
    *
    * @return true if the connection settings are valid, false otherwise
    */
    public boolean testConnectionSettings() {
        if (url.isEmpty() || user.isEmpty() || password.isEmpty()) {
            if (url.isEmpty()) {
                LOGGER.info("No database URL");
            }
            if (user.isEmpty()) {
                LOGGER.info("No user");
            }
            if (password.isEmpty()) {
                LOGGER.info("No password");
            }
            return false;
        }
        if (driver == null) {
            LOGGER.info("driver is null");
            return false;
        }

        try {
            driver.verifyConnectivity();
        } catch (Exception e) {
            LOGGER.info("Verification failed");
            return false;
        }

        try (Session session = driver.session()) {
            String queryResult = session.executeWrite(tx -> {
                Query query = new Query("RETURN '1'");
                Result result = tx.run(query);
                return result.single().get(0).asString();
            });
            LOGGER.info("Test query result is: {}", queryResult);
            return queryResult.equals("1");
        } catch (Exception e) {
            LOGGER.info("Exception: {}", e.getMessage());
        }
        return false;
    }

    /**
    * Returns the driver used for the database connection. If the driver is not initialized, a connection is created.
    *
    * @return the Neo4j driver
    */
    public Driver getDriver() {
        if (this.driver == null) {
            createConnection();
        }
        return driver;
    }

    /**
    * Creates a connection to the Neo4j database using the stored connection settings.
    */
    private void createConnection() {
        this.driver = GraphDatabase.driver(url, AuthTokens.basic(user, password));
    }

    /**
    * Creates a connection to the Neo4j database using the specified URL, username, and password.
    *
    * @param url      the URL of the Neo4j database
    * @param user     the username for the database connection
    * @param password the password for the database connection
    */
    public void createConnection(String url, String user, String password) {
        this.driver = GraphDatabase.driver(url, AuthTokens.basic(user, password));
    }
}
