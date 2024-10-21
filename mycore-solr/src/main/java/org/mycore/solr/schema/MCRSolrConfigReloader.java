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

import static java.util.Map.entry;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationInputStream;
import org.mycore.services.http.MCRHttpUtils;
import org.mycore.solr.MCRSolrCore;
import org.mycore.solr.MCRSolrCoreManager;
import org.mycore.solr.MCRSolrUtils;
import org.mycore.solr.auth.MCRSolrAuthenticationLevel;
import org.mycore.solr.auth.MCRSolrAuthenticationManager;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * This class provides methods to reload a SOLR configuration using the SOLR configuration API
 * see https://lucene.apache.org/solr/guide/8_6/config-api.html
 *
 * @author Robert Stephan
 * @author Jens Kupferschmidt
 */
public class MCRSolrConfigReloader {

    /**
    from https://lucene.apache.org/solr/guide/8_6/config-api.html
    key = lowercase object name from config api command (add-*, update-* delete-*)
    value = keyName in SOLR config json (retrieved via URL ../core-name/config)
    */
    private static final Map<String, String> SOLR_CONFIG_OBJECT_NAMES = Map.ofEntries(
        entry("requesthandler", "requestHandler"), //checked -> id in subfield "name"
        entry("searchcomponent", "searchComponent"), //checked  -> id in subfield "name"
        entry("initparams", "initParams"), //checked - id in key (TODO special handling)
        entry("queryresponsewriter", "queryResponseWriter"), //checked  -> id in subfield "name"
        entry("queryparser", "queryParser"),
        entry("valuesourceparser", "valueSourceParser"),
        entry("transformer", "transformer"),
        entry("updateprocessor", "updateProcessor"), //checked  -> id in subfield "name"
        entry("queryconverter", "queryConverter"),
        entry("listener", "listener"), //checked -> id in subfield "event" -> special handling
        entry("runtimelib", "runtimeLib"));

    private static final List<String> SOLR_CONFIG_PROPERTY_COMMANDS = List.of("set-property", "unset-property");

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String SOLR_CONFIG_UPDATE_FILE_NAME = "solr-config.json";

    /**
     * Removed items from SOLR configuration overlay. This removal works over all in the property
     * MCR.Solr.ObserverConfigTypes defined SOLR configuration parts. For each entry the
     * method will process a SOLR delete command via API.
     *
     * @param configType the name of the configuration directory containing the SOLR core configuration
     * @param coreID the ID of the core, which the configuration should be applied to
     */
    public static void reset(String configType, String coreID) {
        LOGGER.info(() -> "Resetting config definitions for core " + coreID + " using configuration " + configType);
        String coreURL = MCRSolrCoreManager.get(coreID)
            .map(MCRSolrCore::getV1CoreURL)
            .orElseThrow(() -> MCRSolrUtils.getCoreConfigMissingException(coreID));
        JsonObject currentSolrConfig = retrieveCurrentSolrConfigOverlay(coreURL);
        JsonObject configPart = currentSolrConfig.getAsJsonObject("overlay");

        for (String observedType : getObserverConfigTypes()) {
            JsonObject overlaySection = configPart.getAsJsonObject(observedType);
            if (overlaySection == null) {
                continue;
            }
            Set<Map.Entry<String, JsonElement>> configuredComponents = overlaySection.entrySet();
            final String deleteSectionCommand = "delete-" + observedType.toLowerCase(Locale.ROOT);
            if (configuredComponents.isEmpty() || !isKnownSolrConfigCommmand(deleteSectionCommand)) {
                continue;
            }
            for (Map.Entry<String, JsonElement> configuredComponent : configuredComponents) {
                final JsonObject deleteCommand = new JsonObject();
                deleteCommand.addProperty(deleteSectionCommand, configuredComponent.getKey());
                LOGGER.debug(deleteCommand);
                try {
                    executeSolrCommand(coreURL, deleteCommand);
                } catch (IOException e) {
                    LOGGER.error(() -> "Exception while executing '" + deleteCommand + "'.", e);
                }
            }
        }
    }

    /**
     * This method modified the SOLR configuration definition based on all solr/{coreType}/solr-config.json 
     * in the MyCoRe-Maven modules resource path.
     *
     * @param configType the name of the configuration directory containing the SOLR core configuration
     * @param coreID the ID of the core, which the configuration should be applied to
     */
    public static void processConfigFiles(String configType, String coreID) {
        LOGGER.info(() -> "Load config definitions for core " + coreID + " using configuration " + configType);
        try {
            String coreURL = MCRSolrCoreManager.get(coreID)
                .orElseThrow(() -> MCRSolrUtils.getCoreConfigMissingException(coreID)).getV1CoreURL();
            List<String> observedTypes = getObserverConfigTypes();
            JsonObject currentSolrConfig = retrieveCurrentSolrConfig(coreURL);

            Map<String, byte[]> configFileContents = MCRConfigurationInputStream.getConfigFileContents(
                "solr/" + configType + "/" + SOLR_CONFIG_UPDATE_FILE_NAME);
            for (byte[] configFileData : configFileContents.values()) {
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
     * get the content of property MCR.Solr.ObserverConfigTypes as List
     * @return the list of observed SOLR configuration types, a.k.a. top-level sections of config API
     */
    private static List<String> getObserverConfigTypes() {
        return MCRConfiguration2
            .getString("MCR.Solr.ObserverConfigTypes")
            .map(MCRConfiguration2::splitValue)
            .orElseGet(Stream::empty)
            .collect(Collectors.toList());
    }

    /**
     * processes a single SOLR configuration command
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
                            LOGGER.info(() -> "Current configuration has already an " + configCommand
                                + " with name " + configCommandName.getAsString()
                                + ". Rewrite config command as update-" + configType);
                            commandJsonObject.add("update-" + configType, commandJsonObject.get(configCommand));
                            commandJsonObject.remove(configCommand);
                        }
                    }
                    executeSolrCommand(coreURL, commandJsonObject);
                }
            } catch (IOException e) {
                LOGGER.error(e);
            }
        }

    }

    /**
     * Sends a command to SOLR server
     * @param coreURL to which the command will be send
     * @param command the command
     * @throws UnsupportedEncodingException if command encoding is not supported
     */
    private static void executeSolrCommand(String coreURL, JsonObject command) throws UnsupportedEncodingException {
        HttpRequest.Builder requestBuilder = MCRSolrUtils.getRequestBuilder()
            .uri(URI.create(coreURL + "/config"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(command.toString()));

        MCRSolrAuthenticationManager.getInstance().applyAuthentication(requestBuilder,
            MCRSolrAuthenticationLevel.ADMIN);
        String commandprefix = command.keySet().stream().findFirst().orElse("unknown command");
        try (HttpClient httpClient = MCRHttpUtils.getHttpClient()) {
            HttpResponse<String> response
                = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                LOGGER.debug(() -> "SOLR config " + commandprefix + " command was successful \n" + response.body());
            } else {
                LOGGER
                    .error(() -> "SOLR config " + commandprefix + " error: " + response.statusCode() + " "
                        + MCRHttpUtils.getReasonPhrase(response.statusCode()) + "\n"
                        + response.body());
            }

        } catch (InterruptedException | IOException e) {
            LOGGER.error(() -> "Could not execute the following Solr config command:\n" + command, e);
        }
    }

    /**
     * Checks if a given configType with passed name already was added to current SORL configuration
     * @param configType - Type of localized SOLR component e.g. request handlers, search components
     * @param name - identification of configuration type
     * @param solrConfig - current SOLR configuration
     * @return - Is there already an entry in current SOLR configuration
     */
    private static boolean isConfigTypeAlreadyAdded(String configType, JsonElement name, JsonObject solrConfig) {

        JsonObject configPart = solrConfig.getAsJsonObject("config");
        JsonObject observedConfig = configPart.getAsJsonObject(configType);

        return observedConfig.has(name.getAsString());
    }

    /**
     * retrieves the current SOLR configuration for the given core 
     * @param coreURL from which the current SOLR configuration will be load
     * @return the configuration as JSON object
     */
    private static JsonObject retrieveCurrentSolrConfig(String coreURL) {
        HttpRequest.Builder solrRequestBuilder = MCRSolrUtils.getRequestBuilder()
            .uri(URI.create(coreURL + "/config"));
        MCRSolrAuthenticationManager.getInstance().applyAuthentication(solrRequestBuilder,
            MCRSolrAuthenticationLevel.ADMIN);
        return getJSON(solrRequestBuilder.build());
    }

    /**
     * retrieves the current SOLR configuration overlay for the given core
     * @param coreURL from which the current SOLR configuration will be load
     * @return the configuration as JSON object
     */
    private static JsonObject retrieveCurrentSolrConfigOverlay(String coreURL) {
        HttpRequest.Builder solrRequestBuilder = MCRSolrUtils.getRequestBuilder()
            .uri(URI.create(coreURL + "/config/overlay"));
        MCRSolrAuthenticationManager.getInstance().applyAuthentication(solrRequestBuilder,
            MCRSolrAuthenticationLevel.ADMIN);
        return getJSON(solrRequestBuilder.build());
    }

    private static JsonObject getJSON(HttpRequest getConfig) {
        JsonObject convertedObject = null;
        try (HttpClient httpClient = MCRHttpUtils.getHttpClient()) {
            HttpResponse<String> response = httpClient.send(getConfig, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                convertedObject = new Gson().fromJson(response.body(), JsonObject.class);
            } else {
                LOGGER.error(() -> "Could not retrieve current Solr configuration from solr server. Http Status: "
                    + response.statusCode() + " " + MCRHttpUtils.getReasonPhrase(response.statusCode()));
            }

        } catch (IOException | InterruptedException e) {
            LOGGER.error("Could not read current Solr configuration", e);
        } catch (JsonSyntaxException e) {
            LOGGER.error("Current json configuration is not a valid json", e);
        }
        return convertedObject;
    }

    /**
     *
     * @param cmd the SOLR API command
     * @return true, if the command is in the list of known SOLR commands.
     */

    private static boolean isKnownSolrConfigCommmand(String cmd) {
        String cfgObjName = cmd.substring(cmd.indexOf("-") + 1).toLowerCase(Locale.ROOT);
        return ((cmd.startsWith("add-") || cmd.startsWith("update-") || cmd.startsWith("delete-"))
            && (SOLR_CONFIG_OBJECT_NAMES.containsKey(cfgObjName)))
            || SOLR_CONFIG_PROPERTY_COMMANDS.contains(cmd);
    }

}
