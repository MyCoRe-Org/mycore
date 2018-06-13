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

package org.mycore.solr.schema;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfigurationInputStream;
import org.mycore.solr.MCRSolrClientFactory;
import org.mycore.solr.MCRSolrCore;
import org.mycore.solr.MCRSolrUtils;

import com.google.common.io.ByteStreams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * This class provides methods to reload a SOLR config using the SOLR config API
 * see https://lucene.apache.org/solr/guide/7_3/config-api.html
 *
 * @author Robert Stephan
 * @author Jens Kupferschmidt
 */
public class MCRSolrConfigReloader {
    private static final Map<String, String> SOLR_CONFIG_OBJECT_NAMES = new HashMap<>();

    private static final List<String> SOLR_CONFIG_PROPERTY_COMMANDS = Arrays.asList("set-property", "unset-property");

    //from https://lucene.apache.org/solr/guide/7_3/config-api.html
    //key = lowercase object name from config api command (add-*, update-* delete-*) 
    //value = keyName in SOLR config json (retrieved via URL ../core-name/config)

    private static Logger LOGGER = LogManager.getLogger(MCRSolrConfigReloader.class);

    private static String SOLR_CONFIG_UPDATE_FILE_NAME = "solr-config.json";

    //at the moment we check the existence of the following config object (having a 'name' property)
    //-> be careful when you try to modify other config objects
    private static List<String> CHECKED_CONFIG_OBJECTS = Arrays.asList("requesthandler", "searchcomponent",
        "queryresponsewriter", "updateprocessor", "listener");

    static {
        SOLR_CONFIG_OBJECT_NAMES.put("requesthandler", "requestHandler"); //checked -> id in subfield "name"
        SOLR_CONFIG_OBJECT_NAMES.put("searchcomponent", "searchComponent"); //checked  -> id in subfield "name"
        SOLR_CONFIG_OBJECT_NAMES.put("initparams", "initParams"); //checked - id in key (TODO special handling)
        SOLR_CONFIG_OBJECT_NAMES.put("queryresponsewriter", "queryResponseWriter"); //checked  -> id in subfield "name"
        SOLR_CONFIG_OBJECT_NAMES.put("queryparser", "queryParser");
        SOLR_CONFIG_OBJECT_NAMES.put("valuesourceparser", "valueSourceParser");
        SOLR_CONFIG_OBJECT_NAMES.put("transformer", "transformer");
        SOLR_CONFIG_OBJECT_NAMES.put("updateprocessor", "updateProcessor"); //checked  -> id in subfield "name"
        SOLR_CONFIG_OBJECT_NAMES.put("queryconverter", "queryConverter");
        SOLR_CONFIG_OBJECT_NAMES.put("listener", "listener"); //checked -> id in subfield "event" -> special handling
        SOLR_CONFIG_OBJECT_NAMES.put("runtimelib", "runtimeLib");
    }

    /**
     * This method modified the SOLR config definition based on all solr/{coreType}/solr-config.json 
     * in the MyCoRe-Maven modules resource path.
     *
     * @param coreType the type string of the core, use <b>default-core</b> for the MyCoRe default application core
     */
    public static void processConfigFiles(String coreType) {
        MCRSolrCore solrCore = MCRSolrClientFactory.get(coreType)
            .orElseThrow(() -> MCRSolrUtils.getCoreConfigMissingException(coreType));

        LOGGER.info("Load config definitions for core type " + coreType + " in core " + solrCore.getName());
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            List<byte[]> configFileContents = MCRConfigurationInputStream.getConfigFileContents(
                "solr/" + coreType + "/" + SOLR_CONFIG_UPDATE_FILE_NAME);
            for (byte[] configFileData : configFileContents) {
                String content = new String(configFileData, StandardCharsets.UTF_8);
                JsonParser parser = new JsonParser();
                JsonElement json = parser.parse(content);
                if (!json.isJsonArray()) {
                    JsonElement e = json;
                    json = new JsonArray();
                    json.getAsJsonArray().add(e);
                }

                JsonObject currentSolrConfig = retrieveCurrentSolrConfig(coreType);

                for (JsonElement command : json.getAsJsonArray()) {
                    processConfigCommand(coreType, command, currentSolrConfig);
                }

            }

        } catch (IOException e) {
            LOGGER.error(e);
        }
    }

    /**
     * processes a single SOLR config update command
     * @param coreType - then name of the core
     * @param command - the command in JSON syntax
     * @param currentSolrConfig - the current Solr configuration as JSONObject
     */
    private static void processConfigCommand(String coreType, JsonElement command, JsonObject currentSolrConfig) {
        if (command.isJsonObject()) {
            try {
                //get first and only? property of the command object
                Entry<String, JsonElement> entry = command.getAsJsonObject().entrySet().iterator().next();
                String currentConfigObjectName = entry.getKey().substring(entry.getKey().indexOf("-") + 1)
                    .toLowerCase(Locale.ROOT);

                if (isKnownSolrConfigCommmand(entry.getKey())) {
                    if (entry.getKey().startsWith("add-")
                        && CHECKED_CONFIG_OBJECTS.contains(currentConfigObjectName)) {
                        String key = "listener".equals(currentConfigObjectName) ? "event" : "name";
                        String name = entry.getValue().getAsJsonObject().get(key).getAsString();
                        JsonElement jeSolrConfigObj = currentSolrConfig
                            .get(SOLR_CONFIG_OBJECT_NAMES.get(currentConfigObjectName));
                        if (jeSolrConfigObj != null && jeSolrConfigObj.getAsJsonObject().get(name) != null) {
                            //the config object does exist in current Solr config 
                            //--> convert command: add-* to update-*
                            command.getAsJsonObject().add(entry.getKey().replace("add-", "update-"),
                                entry.getValue());
                            command.getAsJsonObject().remove(entry.getKey());
                            LOGGER.debug(
                                "The object to be added does exist in Solr -> switch mode from add-* to update-*\n"
                                    + command.toString());
                        }
                    }

                    if (entry.getKey().startsWith("update-")
                        && CHECKED_CONFIG_OBJECTS.contains(currentConfigObjectName)) {
                        String key = "listener".equals(currentConfigObjectName) ? "event" : "name";
                        String name = entry.getValue().getAsJsonObject().get(key).getAsString();
                        JsonElement jeSolrConfigObj = currentSolrConfig
                            .get(SOLR_CONFIG_OBJECT_NAMES.get(currentConfigObjectName));
                        if (jeSolrConfigObj == null || jeSolrConfigObj.getAsJsonObject().get(name) == null) {
                            //if the config object does not exist in current Solr config 
                            //--> convert command: update-* to add-*
                            command.getAsJsonObject().add(entry.getKey().replace("update-", "add-"),
                                entry.getValue());
                            command.getAsJsonObject().remove(entry.getKey());
                            LOGGER.debug(
                                "The object to be updated does not exist in Solr -> switch mode from update-* to add-*\n"
                                    + command.toString());
                        }
                    }

                    if (entry.getKey().startsWith("delete-")) {
                        String name = entry.getValue().getAsString();
                        JsonElement jeSolrConfigObj = currentSolrConfig
                            .get(SOLR_CONFIG_OBJECT_NAMES.get(currentConfigObjectName));
                        if (jeSolrConfigObj == null || jeSolrConfigObj.getAsJsonObject().get(name) == null) {
                            //if the config object does not exist in current Solr config
                            //--> ignore command.
                            LOGGER.debug(
                                "The following operation was not executed, because the configuration object did not exist\n"
                                    + command.toString());
                            return;
                        }
                    }
                }

                String coreURL = MCRSolrClientFactory.get(coreType)
                    .orElseThrow(() -> MCRSolrUtils.getCoreConfigMissingException(coreType)).getV1CoreURL();
                HttpPost post = new HttpPost(coreURL
                    + "/config");
                post.setHeader("Content-type", "application/json");
                post.setEntity(new StringEntity(command.toString()));
                HttpResponse response;
                try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                    response = httpClient.execute(post);
                    String respContent = new String(ByteStreams.toByteArray(response.getEntity().getContent()),
                        StandardCharsets.UTF_8);
                    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                        LOGGER.debug("SOLR config update command was successful \n" + respContent);
                    } else {
                        LOGGER.error("SOLR config update error: " + response.getStatusLine().getStatusCode() + " "
                            + response.getStatusLine().getReasonPhrase() + "\n" + respContent);
                    }

                } catch (IOException e) {
                    LOGGER.error(
                        "Could not execute the following Solr config update command\n" + command.toString(), e);
                }

            } catch (IOException e) {
                LOGGER.error(e);
            }
        }

    }

    /**
     * retrieves the current solr configuration for the given core
     * @param coreType - the name of the solr core
     * @return the config as JSON object
     */
    private static JsonObject retrieveCurrentSolrConfig(String coreType) {
        String coreURL = MCRSolrClientFactory.get(coreType)
            .orElseThrow(() -> MCRSolrUtils.getCoreConfigMissingException(coreType)).getV1CoreURL();
        HttpGet get = new HttpGet(coreURL + "/config");
        HttpResponse response;
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            response = httpClient.execute(get);
            JsonParser jsonParser = new JsonParser();
            JsonElement jeResponse = jsonParser
                .parse(new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8));
            return jeResponse.getAsJsonObject().get("config").getAsJsonObject();
        } catch (IOException e) {
            throw new MCRException("Could not read Solr configuration", e);
        }
    }

    /**
     *
     * @param cmd
     * @return true, if the command is in the list of known solr commands.
     */

    private static boolean isKnownSolrConfigCommmand(String cmd) {
        String cfgObjName = cmd.substring(cmd.indexOf("-") + 1).toLowerCase(Locale.ROOT);
        return ((cmd.startsWith("add-") || cmd.startsWith("update-") || cmd.startsWith("delete-"))
            && (SOLR_CONFIG_OBJECT_NAMES.keySet().contains(cfgObjName)))
            || SOLR_CONFIG_PROPERTY_COMMANDS.contains(cmd);
    }


}
