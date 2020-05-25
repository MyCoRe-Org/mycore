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
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationInputStream;
import org.mycore.solr.MCRSolrClientFactory;
import org.mycore.solr.MCRSolrUtils;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

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
     * @param configType the name of the configuration directory containg the Solr core configuration
     * @param coreID the ID of the core, which the configuration should be applied to
     */
    public static void processConfigFiles(String configType, String coreID) {
        LOGGER.info("Load config definitions for core " + coreID + " using configuration " + configType);
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String coreURL = MCRSolrClientFactory.get(coreID)
                .orElseThrow(() -> MCRSolrUtils.getCoreConfigMissingException(coreID)).getV1CoreURL();
            String observedTypesConfiguration = MCRConfiguration2.getString("MCR.Solr.ObserverConfigTypes").orElse("");
            List<String> observedTypes = Arrays.asList(observedTypesConfiguration.split("\\s*,\\s*"));
            JsonObject currentSolrConfig = retrieveCurrentSolrConfig(coreURL);

            List<byte[]> configFileContents = MCRConfigurationInputStream.getConfigFileContents(
                "solr/" + configType + "/" + SOLR_CONFIG_UPDATE_FILE_NAME);
            for (byte[] configFileData : configFileContents) {
                String content = new String(configFileData, StandardCharsets.UTF_8);
                JsonElement json = JsonParser.parseString(content);
                if (!json.isJsonArray()) {
                    JsonElement e = json;
                    json = new JsonArray();
                    json.getAsJsonArray().add(e);
                }

                for (JsonElement command : json.getAsJsonArray()) {
                    LOGGER.debug(command);
                    processConfigCommand(coreURL, command, currentSolrConfig, observedTypes);
                }
            }

        } catch (IOException e) {
            LOGGER.error(e);
        }
    }

    /**
     * processes a single SOLR config update command
     * @param coreURL - the URL of the core
     * @param command - the command in JSON syntax
     */
    private static void processConfigCommand(String coreURL, JsonElement command, JsonObject currentSolrConfig,
        List<String> observedTypes) {
        if (command.isJsonObject()) {
            try {
                //get first and only? property of the command object
                final JsonObject commandJsonObject = command.getAsJsonObject();
                Entry<String, JsonElement> commandObject = commandJsonObject.entrySet().iterator().next();
                final String configCommand = commandObject.getKey();
                final String configType = StringUtils.substringAfter(configCommand, "add-");

                if (isKnownSolrConfigCommmand(configCommand)) {

                    if (observedTypes.contains(configType) && configCommand.startsWith("add-") &&
                        commandObject.getValue() instanceof JsonObject) {
                        final JsonElement configCommandName = commandObject.getValue().getAsJsonObject().get("name");
                        if (isConfigTypeAlreadyAdded(configType, configCommandName, currentSolrConfig)) {
                            LOGGER.info("Current configuration has already an " + configCommand
                                + " with name " + configCommandName.getAsString()
                                + ". Rewrite config command as update-"
                                + configType);
                            commandJsonObject.add("update-" + configType, commandJsonObject.get(configCommand));
                            commandJsonObject.remove(configCommand);
                        }
                    }
                    executeSolrCommand(coreURL, commandJsonObject.toString());
                }
            } catch (IOException e) {
                LOGGER.error(e);
            }
        }

    }

    /**
     * Sends a command to solr
     * @param coreURL to which the command will be send
     * @param command the command as string
     * @throws UnsupportedEncodingException if command encoding is not supported
     */
    private static void executeSolrCommand(String coreURL, String command) throws UnsupportedEncodingException {
        HttpPost post = new HttpPost(coreURL + "/config");
        post.setHeader("Content-type", "application/json");
        post.setEntity(new StringEntity(command));
        String commandprefix = command.indexOf('-') != -1 ? command.substring(2, command.indexOf('-'))
            : "unknown command";
        HttpResponse response;
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            response = httpClient.execute(post);
            String respContent = new String(ByteStreams.toByteArray(response.getEntity().getContent()),
                StandardCharsets.UTF_8);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                LOGGER.debug("SOLR config " + commandprefix + " command was successful \n" + respContent);
            } else {
                LOGGER
                    .error("SOLR config " + commandprefix + " error: " + response.getStatusLine().getStatusCode() + " "
                        + response.getStatusLine().getReasonPhrase() + "\n" + respContent);
            }

        } catch (IOException e) {
            LOGGER.error(
                "Could not execute the following Solr config " + commandprefix + " command\n" + command, e);
        }
    }

    /**
     * Checks if a given configType with passed name already was added to current solr configuration
     * @param configType - Type of localized solr component e.g. request handlers, search components
     * @param name - identification of config type
     * @param solrConfig - current solr configuration
     * @return - Is there already an entry in current solr configuration
     */
    private static boolean isConfigTypeAlreadyAdded(String configType, JsonElement name, JsonObject solrConfig) {

        JsonObject configPart = solrConfig.getAsJsonObject("config");
        JsonObject observedConfig = configPart.getAsJsonObject(configType);

        return observedConfig.has(name.getAsString());
    }

    /**
     * retrieves the current solr configuration for the given core 
     * @param coreURL from which the current solr configuration will be load
     * @return the config as JSON object
     */
    private static JsonObject retrieveCurrentSolrConfig(String coreURL) {
        HttpGet getConfig = new HttpGet(coreURL + "/config");
        JsonObject convertedObject = null;
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            CloseableHttpResponse response = httpClient.execute(getConfig);
            StatusLine statusLine = response.getStatusLine();
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String configAsString = EntityUtils.toString(response.getEntity(), "UTF-8");
                convertedObject = new Gson().fromJson(configAsString, JsonObject.class);
            } else {
                LOGGER.error("Could not retrieve current Solr configuration from solr server. Http Status: "
                    + statusLine.getStatusCode(), statusLine.getReasonPhrase());
            }

        } catch (IOException e) {
            LOGGER.error("Could not read current Solr configuration", e);
        } catch (JsonSyntaxException e) {
            LOGGER.error("Current json configuration is not a valid json", e);
        }
        return convertedObject;
    }

    /**
     *
     * @param cmd
     * @return true, if the command is in the list of known solr commands.
     */

    private static boolean isKnownSolrConfigCommmand(String cmd) {
        String cfgObjName = cmd.substring(cmd.indexOf("-") + 1).toLowerCase(Locale.ROOT);
        return ((cmd.startsWith("add-") || cmd.startsWith("update-") || cmd.startsWith("delete-"))
            && (SOLR_CONFIG_OBJECT_NAMES.containsKey(cfgObjName)))
            || SOLR_CONFIG_PROPERTY_COMMANDS.contains(cmd);
    }

}
